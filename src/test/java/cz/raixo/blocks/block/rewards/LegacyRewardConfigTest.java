package cz.raixo.blocks.block.rewards;

import cz.raixo.blocks.block.playerdata.PlayerData;
import cz.raixo.blocks.block.rewards.commands.RewardEntry;
import cz.raixo.blocks.block.rewards.commands.random.RandomCommandEntry;
import cz.raixo.blocks.block.rewards.context.RewardContext;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rewards written by the original plugin must keep working unchanged.
 *
 * <p>Servers upgrading to this fork carry their existing configuration over, so the old reward
 * shapes - "weight;command" strings, {@code interval}, {@code condition}, {@code place} ranges -
 * are part of the contract, not legacy to be broken quietly.</p>
 */
class LegacyRewardConfigTest {

    private static final UUID PLAYER = UUID.randomUUID();
    private static final UUID SOMEONE_ELSE = UUID.randomUUID();

    private static ConfigurationSection rewards;

    @BeforeAll
    static void loadFixture() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File("src/test/resources/legacy-rewards.yml"));
        rewards = Objects.requireNonNull(config.getConfigurationSection("rewards"), "fixture has no rewards section");
    }

    private Reward reward(String name) {
        return Reward.parse(Objects.requireNonNull(rewards.getConfigurationSection(name), "no reward " + name));
    }

    private PlayerData player(int breaks) {
        return new PlayerData(PLAYER, "Tester", breaks);
    }

    /** Context for a block that has just been mined out, with this player in the given position. */
    private RewardContext finished(int position, UUID lastBreaker) {
        return new RewardContext(null, new Random(42), Map.of(PLAYER, position), lastBreaker);
    }

    /** Context for a single hit on a block that is still standing. */
    private RewardContext midCycle() {
        return new RewardContext(null, new Random(42), null, null);
    }

    @Test
    void everyLegacyRewardStillParses() {
        for (String name : rewards.getKeys(false)) {
            Reward parsed = reward(name);
            assertEquals(name, parsed.getName());
        }
    }

    @Test
    void intervalRewardFiresOnEveryNthBreak() {
        Reward reward = reward("reward2");

        assertEquals(RewardType.BREAK, reward.getType());
        assertFalse(reward.isLast(), "an interval reward is paid during mining, not at the end");

        for (int breaks = 1; breaks <= 4; breaks++) {
            assertFalse(reward.canGet(player(breaks), midCycle()), "should not fire on break " + breaks);
        }
        assertTrue(reward.canGet(player(5), midCycle()), "should fire on break 5");
        assertTrue(reward.canGet(player(10), midCycle()), "should fire on break 10");
        assertFalse(reward.canGet(player(11), midCycle()), "should not fire on break 11");
    }

    @Test
    void comparatorConditionStillReads() {
        Reward reward = reward("reward3");

        assertTrue(reward.canGet(player(4), midCycle()));
        assertFalse(reward.canGet(player(5), midCycle()));
    }

    @Test
    void lastConditionOnlyPaysTheFinalHit() {
        Reward reward = reward("reward4");

        assertTrue(reward.isLast(), "the last-hit reward is paid when the block is mined out");
        assertTrue(reward.canGet(player(1), finished(1, PLAYER)));
        assertFalse(reward.canGet(player(1), finished(1, SOMEONE_ELSE)));
    }

    @Test
    void topRewardsUseThePlaceRange() {
        Reward first = reward("reward_name");
        assertTrue(first.isLast());
        assertTrue(first.canGet(player(10), finished(1, PLAYER)));
        assertFalse(first.canGet(player(10), finished(2, PLAYER)));

        Reward podium = reward("reward1");
        assertTrue(podium.canGet(player(10), finished(3, PLAYER)));
        assertFalse(podium.canGet(player(10), finished(4, PLAYER)));
    }

    @Test
    void breakCountRewardUsesTheRange() {
        Reward reward = reward("reward5");

        assertEquals(RewardType.BREAK_COUNT, reward.getType());
        assertTrue(reward.isLast());
        assertFalse(reward.canGet(player(9), finished(1, PLAYER)));
        assertTrue(reward.canGet(player(10), finished(1, PLAYER)));
        assertTrue(reward.canGet(player(50), finished(1, PLAYER)));
        assertFalse(reward.canGet(player(51), finished(1, PLAYER)));
    }

    @Test
    void weightAndCommandSurviveTheOldStringForm() {
        List<? extends RewardEntry> entries = reward("reward_name").getCommands().asList();

        assertEquals(2, entries.size());
        RandomCommandEntry diamond = assertInstanceOf(RandomCommandEntry.class, entries.get(0));
        assertEquals(10, diamond.getChance());
        assertEquals("give %player% diamond", diamond.getCommand());
        assertEquals(100, ((RandomCommandEntry) entries.get(1)).getChance());

        // Old entries carry no message, so they stay silent - the behaviour they had before.
        assertFalse(entries.stream().anyMatch(entry -> entry.getMessage() != null));
        assertFalse(entries.stream().anyMatch(RewardEntry::isBroadcast));
    }

    @Test
    void randomModeHandsOutExactlyOneEntry() {
        Reward reward = reward("reward_name");
        List<? extends RewardEntry> granted = reward.getCommands().rewardPlayer(player(1), midCycle());

        assertEquals(1, granted.size());
        assertEquals("random", reward.getCommands().getModeName());
    }

    @Test
    void allModeHandsOutEveryEntry() {
        Reward reward = reward("reward6");
        List<? extends RewardEntry> granted = reward.getCommands().rewardPlayer(player(3), midCycle());

        assertEquals("all", reward.getCommands().getModeName());
        assertEquals(2, granted.size());
        assertEquals(List.of("give %player% stone", "give %player% dirt"),
                granted.stream().map(RewardEntry::getCommand).toList());
    }

    @Test
    void rewardsRoundTripBackToTheLegacyStringForm() {
        // Saving from the GUI must not rewrite an untouched config into the verbose map form.
        assertEquals(
                List.of("10;give %player% diamond", "100;give %player% iron_ingot"),
                reward("reward_name").getCommands().saveToList()
        );
    }

}
