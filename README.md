# MineBlocks

Server "nexus" blocks for Paper: a block has health, every allowed hit counts, and when it runs out
the block turns to bedrock for a cooldown. Players earn weighted rewards while mining, and the
blocks hand out the permissions that unlock the next block in the ladder.

This is a fork of [RAIXOCZ/MineBlocks](https://github.com/RAIXOCZ/MineBlocks), rebuilt for
**Minecraft 26.2** and maintained independently. Upstream is not modified.

## Requirements

| | |
|---|---|
| Server | Paper 26.2 (or a fork of it) |
| Java | 25 |
| Optional | PlaceholderAPI, LuckPerms |

Holograms use vanilla display entities, so no hologram plugin is needed. AFK detection uses the
server's own idle timer, so no AFK plugin is needed either.

## Build

```bash
./gradlew build
```

The jar lands in `build/libs/MineBlocks-<version>.jar`. Gradle downloads the Java 25 toolchain by
itself if it is not installed.

CI builds every push and pull request; pushing a `v<version>` tag publishes a GitHub release with
the jar attached (the tag must match `version` in `build.gradle`).

## How a block works

1. A player hits the block with a tool the block accepts.
2. The hit takes one health and adds one to that player's score for the current cycle.
3. Every *n*-th hit the player receives a reward, picked by weight from a list.
4. When health reaches zero the block announces itself, becomes bedrock, and starts its cooldown.
   Final rewards (top places, last hit) are paid out here.
5. When the cooldown ends the block announces that it is back and health is restored.

## The nexus ladder

The shipped configuration defines five tiers. Each is gated by its own permission and unlocks the
next one through a reward:

```
mineblocks.blocks.stone -> .gold -> .color -> .ore -> .mithcoin
```

Give your default group `mineblocks.blocks.stone`; the blocks hand out the rest. Permissions are
granted by a plain console command, so any permission plugin works — the defaults use LuckPerms
syntax.

```
lp group default permission set mineblocks.blocks.stone true
```

### Permission naming

A block's `permission` is free-form — any node works. The shipped blocks follow the plugin's own
namespace so the nodes are recognisable and can be granted as a set:

```
mineblocks.blocks.<block id>
```

That means `mineblocks.blocks.*` unlocks every block at once, which is handy for staff ranks and
for testing. Every node a block gates on is registered with the server at load, so it tab-completes
in LuckPerms instead of having to be typed from memory.

If you are coming from a setup that used bare `<tier>.nexus` nodes, migrate the grants once:

```
lp group default permission set mineblocks.blocks.stone true
lp bulk update users permissions set mineblocks.blocks.gold true where permission == gold.nexus
```

Repeat the second line for each tier, then delete the old nodes.

## Configuration

Everything lives in `config.yml`, and every option is also editable in-game with `/mb edit <block>`.

### Block

```yaml
blocks:
  gold:
    name: "&6Gold Nexus"            # %name%, shown in holograms and messages
    reward-info: "&7gold every &f5 &7breaks"   # %reward_info%
    location: { world: world, x: 10, y: 100, z: 0 }
    type: GOLD_BLOCK
    health: 400
    permission: "mineblocks.blocks.gold"   # empty means everyone may mine it
    break-limit: 20                 # minimum ms between two counted hits
    timeout:
      time: 600                     # cooldown in seconds
      type: BEDROCK                 # material shown during the cooldown
      respawn: "&a[Nexus] &7%name% &7is back online!"
    messages:
      break:
        - "&a[Nexus] &7%name% &7was mined out, back in &f%timeout%&7."
        - "&7Your breaks: &f%breaks%"
    reset:
      inactive:
        time: -1                    # auto-reset after this many idle seconds, -1 disables
        message: ""
      onrestart: false              # reset progress when the server restarts
```

### Tools

```yaml
    tool:
      display: "&firon pickaxe or better"   # what %required_tool% says
      types:
        - "default: DENIED"
        - "(IRON|GOLDEN|DIAMOND|NETHERITE)_PICKAXE: ALLOWED"
      enchantments:
        default: DENIED
        efficiency:
          level: 4-5
          type: ALLOWED
      names:
        default: ALLOWED
```

Material entries are exact material names or regular expressions. Later entries win, so a broad
`default: DENIED` can be narrowed by specific allows below it. If `display` is not set,
`%required_tool%` is generated from the allowed materials.

### Rewards

```yaml
    rewards:
      every_five:
        type: break
        interval: 5        # every 5th hit by that player
        mode: random       # weighted pick of one entry; "all" runs every entry
        commands:
          - "60;give %player% gold_ingot 2"    # weight;command
          - "30;give %player% gold_block 1"
          - "10;give %player% emerald 2"

      unlock_color:
        type: break
        interval: 150
        commands:
          - command: "lp user %player% permission set mineblocks.blocks.color true"
            chance: 100
            message: "&a[Nexus] &7You unlocked the &dColor Nexus&7!"
            broadcast: false

      top_1:
        type: top          # paid out when the block is mined out
        place: 1           # also accepts "1-3" or "1,3,5"
        mode: all
        commands:
          - command: "give %player% diamond 8"
            message: "&a[Nexus] &7You finished &f1st &7on %name%!"
```

Reward types:

| type | when it pays out |
|---|---|
| `break` + `interval: n` | every *n*-th hit by that player |
| `break` + `condition: last` | to the player who lands the final hit |
| `break` + `condition: less than 5` | while the player's count matches (`less than`, `more than`, `equal to`) |
| `top` + `place:` | to the players in those top positions when the block is mined out |
| `break_count` + `from:`/`to:` | to players whose count falls in that range when the block is mined out |

Weights are relative, not percentages: `60` and `30` means twice as likely, not 60%.

Entries are silent by default — add `message` when the player should be told (broadcast to everyone
with `broadcast: true`). Both entry forms are accepted, so configs written for the original plugin
keep working.

### Placeholders

Available in hologram lines, block messages and reward messages:

`%name%` `%id%` `%type%` `%health%` `%max_health%` `%broken%` `%timeout%` `%required_tool%`
`%reward_info%` `%player%` `%breaks%` `%uuid%` `%player_1%`…`%player_10%` `%player_1_breaks%`…
`%player_1_prefix%` (needs LuckPerms)

With PlaceholderAPI installed, other plugins can read block state:
`%mb_<block>_hp%`, `%mb_<block>_max_hp%`, `%mb_<block>_breaks%`, `%mb_<block>_timeout%`,
`%mb_<block>_rank%`, `%mb_<block>_top_<n>%`, `%mb_<block>_top_breaks_<n>%`.

### Colours

`&7` codes, `#RRGGBB` hex, `{#FF0000}gradient{/#0000FF}` and `<RAINBOW80>text</RAINBOW>` are
supported everywhere.

## Commands

All under `/mineblocks` (alias `/mb`), permission `mineblocks.admin`:

| command | what it does |
|---|---|
| `/mb help` | command list |
| `/mb reload` | reload the config and every block |
| `/mb list` | list every block with its location |
| `/mb create <block>` | create a block at your position |
| `/mb edit <block>` | open the in-game editor |
| `/mb remove <block>` | delete a block (console only) |
| `/mb reset <block>` | reset health, progress and cooldown |
| `/mb sethealth <block> <health>` | set the remaining health |
| `/mb teleport <block>` | teleport to a block |
| `/mb hologram show\|addline\|removeline\|setline` | edit hologram lines |
| `/mb version` | show the installed version |

Each subcommand also has its own permission, `mineblocks.admin.<subcommand>`. The old
`mb.admin` node is kept as an alias, so setups that already granted it keep working.

## Licence

Mozilla Public License 2.0, see [LICENSE](LICENSE).
