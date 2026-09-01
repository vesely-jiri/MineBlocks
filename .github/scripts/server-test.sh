#!/usr/bin/env bash
#
# Boots a real Paper server with the built plugin and checks that it actually works.
#
# Compiling against the API only proves the symbols exist. This catches the things that fail at
# runtime: a plugin.yml the server rejects, a config the loader chokes on, an API that is now a
# no-op, holograms that never spawn.
#
# Usage: server-test.sh <paper-version> <plugin-jar> [run-dir]

set -euo pipefail

PAPER_VERSION="${1:?paper version, e.g. 26.2}"
PLUGIN_JAR="$(readlink -f "${2:?path to the plugin jar}")"
RUN_DIR="${3:-run}"

[ -f "$PLUGIN_JAR" ] || { echo "plugin jar not found: $PLUGIN_JAR" >&2; exit 1; }

BOOT_TIMEOUT=300   # seconds to wait for the whole server run
READY_TIMEOUT=240  # seconds to wait for "Done (" before giving up

log_step() { printf '\n\033[1;34m==> %s\033[0m\n' "$1"; }
fail() { printf '\033[1;31mFAIL: %s\033[0m\n' "$1" >&2; exit 1; }

# --- fetch the server ---------------------------------------------------------------------------

log_step "Resolving the latest stable Paper $PAPER_VERSION build"

# The v2 API is sunset; v3 (fill) is the current one.
BUILDS_URL="https://fill.papermc.io/v3/projects/paper/versions/${PAPER_VERSION}/builds?channel=STABLE"
BUILD_JSON="$(curl -sSfL -H 'User-Agent: MineBlocks-CI' "$BUILDS_URL")"

BUILD_ID="$(jq -r '.[0].id' <<<"$BUILD_JSON")"
DOWNLOAD_URL="$(jq -r '.[0].downloads["server:default"].url' <<<"$BUILD_JSON")"
DOWNLOAD_SHA="$(jq -r '.[0].downloads["server:default"].checksums.sha256' <<<"$BUILD_JSON")"

[ "$DOWNLOAD_URL" != "null" ] || fail "no stable build found for Paper $PAPER_VERSION"
echo "Paper $PAPER_VERSION build $BUILD_ID"

mkdir -p "$RUN_DIR/plugins"
cd "$RUN_DIR"

curl -sSfL -H 'User-Agent: MineBlocks-CI' -o paper.jar "$DOWNLOAD_URL"
echo "${DOWNLOAD_SHA}  paper.jar" | sha256sum -c - || fail "downloaded server jar failed its checksum"

# --- prepare the server -------------------------------------------------------------------------

log_step "Preparing the server"

cp "$PLUGIN_JAR" plugins/

# Accepting the Minecraft EULA for a throwaway CI container that generates a flat world and is
# deleted at the end of the job.
echo "eula=true" > eula.txt

cat > server.properties <<'PROPERTIES'
level-name=world
level-type=minecraft\:flat
online-mode=false
spawn-protection=0
max-players=1
view-distance=4
simulation-distance=4
enable-command-block=false
motd=MineBlocks CI
PROPERTIES

# The plugin ships a config with five blocks in a world called "world" at y=100. Letting it write
# its own defaults means this test also proves the shipped config loads.

# --- run it -------------------------------------------------------------------------------------

log_step "Starting the server"

# Commands are fed on stdin once the log reports the server is ready. The subshell is the writing
# end of the pipe, so it must not exit before the server has consumed everything.
{
    waited=0
    until grep -q 'Done (' server.log 2>/dev/null; do
        if [ "$waited" -ge "$READY_TIMEOUT" ]; then
            echo "stop"
            exit 0
        fi
        sleep 2
        waited=$((waited + 2))
    done
    sleep 5

    # Commands the plugin owns.
    echo "mb version"
    echo "mb list"
    echo "mb sethealth stone 5"
    echo "mb reset stone"
    echo "mb hologram show stone"
    # Edits the config through the plugin, so the write path is exercised too.
    echo "mb hologram addline stone &7CI_PERSISTED_LINE"
    sleep 3

    # With nobody online the world unloads right after startup, and every read of it comes back
    # empty ("That position is not loaded"). Force load the chunks holding the five blocks first.
    # This doubles as a test of the chunk-load path: hologram entities are not persisted, so the
    # ones found below have to have been respawned by the plugin when the chunks came back.
    echo "forceload add 0 0 40 0"
    sleep 6

    # Vanilla checks of what the plugin did to the world. Each prints its own marker, so the
    # assertions below can tell "it worked" apart from "the command silently did nothing".
    echo "execute if block 0 100 0 minecraft:stone run say CI_BLOCK_PLACED"
    echo "execute if block 40 100 0 minecraft:crying_obsidian run say CI_LAST_BLOCK_PLACED"
    echo "execute if entity @e[type=minecraft:text_display] run say CI_HOLOGRAM_SPAWNED"
    echo "execute if entity @e[type=minecraft:item_display] run say CI_HOLOGRAM_ICON_SPAWNED"
    sleep 4

    echo "stop"
    sleep 10
} | timeout "$BOOT_TIMEOUT" java -Xms1G -Xmx2G -jar paper.jar --nogui 2>&1 | tee server.log || true

# --- assertions ---------------------------------------------------------------------------------

log_step "Checking the run"

[ -s server.log ] || fail "the server produced no output at all"

require() {
    grep -qF "$1" server.log || fail "$2"
    echo "  ok: $2"
}

refuse() {
    if grep -qE "$1" server.log; then
        echo "--- offending lines ---" >&2
        grep -nE "$1" server.log | head -20 >&2
        fail "$2"
    fi
    echo "  ok: $2"
}

require 'Done (' 'the server finished starting up'
require 'MineBlocks enabled successfully!' 'the plugin enabled'
require 'Loaded blocks from the config:' 'the plugin loaded its blocks'

for block in stone gold color ore mithcoin; do
    grep -q "Loaded blocks from the config:.*\b${block}\b" server.log \
        || fail "block '${block}' from the shipped config did not load"
    echo "  ok: block '${block}' loaded"
done

require 'to be force loaded' 'the probe chunks were force loaded'
require 'CI_BLOCK_PLACED' 'the first block was placed in the world'
require 'CI_LAST_BLOCK_PLACED' 'the last block was placed in the world'
require 'CI_HOLOGRAM_SPAWNED' 'a text hologram entity was respawned on chunk load'
require 'CI_HOLOGRAM_ICON_SPAWNED' 'a hologram icon entity was respawned on chunk load'
require 'List of blocks:' 'the /mb list command answered'
require 'MineBlocks version' 'the /mb version command answered'
require 'MineBlocks disabled successfully!' 'the plugin shut down cleanly'

refuse 'could not be loaded' 'no block failed to load'
refuse "Could not load 'plugins" 'the server accepted the plugin jar'
refuse 'Error occurred while enabling MineBlocks' 'the plugin enabled without errors'
refuse 'Could not pass event' 'no listener threw'
refuse 'Unhandled exception|java\.lang\.[A-Za-z]*(Exception|Error)' 'no exception was logged'
refuse 'Unknown or incomplete command|Incorrect argument for command' 'every console command was accepted'
# A silent "not loaded" is how this test previously fooled itself into reading an empty world.
refuse 'That position is not loaded' 'every probe read a loaded chunk'
refuse 'Could not save config' 'the configuration was written'

log_step "Checking what the plugin wrote back to disk"

WRITTEN_CONFIG="plugins/MineBlocks/config.yml"
[ -f "$WRITTEN_CONFIG" ] || fail "the plugin never wrote its configuration"

grep -qF 'CI_PERSISTED_LINE' "$WRITTEN_CONFIG" \
    || fail "an edit made through the plugin was not persisted to config.yml"
echo "  ok: an edit made in game survives to config.yml"

# Saving a block rewrites its whole section from the loaded object, so every field the loader
# reads has to be written back or it is lost. break-limit was not, and disappeared from the config
# the first time an admin touched a block. Only "stone" is edited above, so only its section proves
# anything - the others still hold the values they were loaded with.
stone_section() { awk '/^  stone:/{f=1;next} /^  [a-z_-]+:/{f=0} f' "$WRITTEN_CONFIG"; }

for field in break-limit damage-tools; do
    stone_section | grep -qE "^ +${field}:" \
        || fail "saving a block dropped '${field}' from its config section"
    echo "  ok: '${field}' survives a block save"
done

# Bukkit keeps comments when it rewrites a configuration. If that ever stops being true, every
# admin loses the documentation in their own config the first time they touch the editor.
grep -q '^# MineBlocks' "$WRITTEN_CONFIG" \
    || fail "rewriting the config stripped its comments"
echo "  ok: rewriting the config keeps its comments"

log_step "Server test passed"
