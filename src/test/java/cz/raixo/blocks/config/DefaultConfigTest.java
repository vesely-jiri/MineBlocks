package cz.raixo.blocks.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the shipped config.yml: a typo there only shows up as a warning in the server log on a
 * live server, which is a slow and easy thing to miss.
 */
class DefaultConfigTest {

    /** The nexus ladder, in order. Each tier unlocks the next one. */
    private static final List<String> TIERS = List.of("stone", "gold", "color", "ore", "mithcoin");

    /** Block permissions live under the plugin's own namespace, keyed by block id. */
    private static final String PERMISSION_PREFIX = "mineblocks.blocks.";

    private static YamlConfiguration config;

    @BeforeAll
    static void loadConfig() {
        config = YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));
    }

    @Test
    void definesEveryTierOfTheLadder() {
        ConfigurationSection blocks = blocks();
        assertEquals(TIERS, List.copyOf(blocks.getKeys(false)));
    }

    @Test
    void everyBlockHasTheSectionsTheLoaderNeeds() {
        for (String id : TIERS) {
            ConfigurationSection block = block(id);
            assertNotNull(block.getConfigurationSection("location"), id + " has no location");
            assertNotNull(block.getString("type"), id + " has no material");
            assertTrue(block.getInt("health") > 0, id + " has no health");
            assertNotNull(block.getConfigurationSection("hologram"), id + " has no hologram");
            assertFalse(block.getStringList("hologram.lines").isEmpty(), id + " has no hologram lines");
            assertTrue(block.getInt("timeout.time") > 0, id + " has no cooldown");
            assertEquals("BEDROCK", block.getString("timeout.type"), id + " should turn into bedrock");
            assertFalse(Objects.requireNonNull(block.getString("timeout.respawn")).isBlank(),
                    id + " has no respawn announcement");
            assertNotNull(block.getConfigurationSection("rewards"), id + " has no rewards");
        }
    }

    @Test
    void everyHologramShowsTheInformationPlayersNeed() {
        for (String id : TIERS) {
            String lines = String.join("\n", block(id).getStringList("hologram.lines"));
            assertTrue(lines.contains("%broken%") && lines.contains("%max_health%"),
                    id + " hologram does not show progress");
            assertTrue(lines.contains("%name%"), id + " hologram does not show the block name");
            assertTrue(lines.contains("%required_tool%"), id + " hologram does not show the tool");
            assertTrue(lines.contains("%reward_info%"), id + " hologram does not show the reward");
            assertTrue(lines.contains("%timeout%"), id + " hologram does not show the cooldown");
            assertTrue(lines.contains("%player_1%") && lines.contains("%player_1_breaks%"),
                    id + " hologram does not show the top players");
        }
    }

    @Test
    void everyBlockRestrictsTheToolAndExplainsWhich() {
        for (String id : TIERS) {
            ConfigurationSection block = block(id);
            List<String> types = block.getStringList("tool.types");
            assertTrue(types.contains("default: DENIED"), id + " does not deny unknown tools by default");
            assertTrue(types.size() > 1, id + " does not allow any tool");
            assertFalse(Objects.requireNonNull(block.getString("tool.display", "")).isBlank(),
                    id + " does not describe its required tool");
        }
    }

    @Test
    void everyBlockRewardsRegularlyDuringMining() {
        for (String id : TIERS) {
            ConfigurationSection rewards = Objects.requireNonNull(block(id).getConfigurationSection("rewards"));
            boolean hasInterval = rewards.getKeys(false).stream()
                    .map(rewards::getConfigurationSection)
                    .filter(Objects::nonNull)
                    .anyMatch(reward -> "break".equals(reward.getString("type")) && reward.getInt("interval") > 0);
            assertTrue(hasInterval, id + " never rewards a player mid-cycle");
        }
    }

    /** The convention every gating node follows, so a wildcard can grant the whole set. */
    private static String permissionOf(String id) {
        return PERMISSION_PREFIX + id;
    }

    @Test
    void everyBlockPermissionFollowsThePluginNamespace() {
        for (String id : TIERS) {
            String permission = block(id).getString("permission");
            assertEquals(permissionOf(id), permission, id + " is gated by the wrong permission");
        }
    }

    @Test
    void eachTierUnlocksTheNextOne() {
        for (int i = 0; i + 1 < TIERS.size(); i++) {
            String id = TIERS.get(i);
            String next = permissionOf(TIERS.get(i + 1));
            assertTrue(rewardCommandsOf(id).contains(next),
                    id + " never hands out " + next + ", the ladder is broken");
        }
    }

    @Test
    void thePermissionRewardTellsThePlayerAboutIt() {
        for (int i = 0; i + 1 < TIERS.size(); i++) {
            String id = TIERS.get(i);
            ConfigurationSection rewards = Objects.requireNonNull(block(id).getConfigurationSection("rewards"));
            boolean announced = rewards.getKeys(false).stream()
                    .map(rewards::getConfigurationSection)
                    .filter(Objects::nonNull)
                    .flatMap(reward -> reward.getMapList("commands").stream())
                    .filter(entry -> String.valueOf(entry.get("command")).contains(PERMISSION_PREFIX))
                    .anyMatch(entry -> entry.get("message") != null);
            assertTrue(announced, id + " unlocks the next block silently");
        }
    }

    @Test
    void theUnlockMessageNamesTheBlockRatherThanThePermissionNode() {
        // Players should read "you unlocked the Gold Nexus", not a permission node.
        for (String id : TIERS) {
            ConfigurationSection rewards = Objects.requireNonNull(block(id).getConfigurationSection("rewards"));
            rewards.getKeys(false).stream()
                    .map(rewards::getConfigurationSection)
                    .filter(Objects::nonNull)
                    .flatMap(reward -> reward.getMapList("commands").stream())
                    .map(entry -> String.valueOf(entry.get("message")))
                    .filter(message -> !"null".equals(message))
                    .forEach(message -> assertFalse(message.contains(PERMISSION_PREFIX),
                            id + " shows a raw permission node to the player: " + message));
        }
    }

    private String rewardCommandsOf(String id) {
        ConfigurationSection rewards = Objects.requireNonNull(block(id).getConfigurationSection("rewards"));
        StringBuilder builder = new StringBuilder();
        for (String name : rewards.getKeys(false)) {
            ConfigurationSection reward = rewards.getConfigurationSection(name);
            if (reward == null) continue;
            reward.getList("commands", List.of()).forEach(entry -> builder.append(entry).append('\n'));
        }
        return builder.toString();
    }

    private ConfigurationSection blocks() {
        return Objects.requireNonNull(config.getConfigurationSection("blocks"), "config.yml has no blocks section");
    }

    private ConfigurationSection block(String id) {
        return Objects.requireNonNull(blocks().getConfigurationSection(id), "config.yml has no block " + id);
    }

}
