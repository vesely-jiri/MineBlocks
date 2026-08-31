package cz.raixo.blocks.block.rewards.commands;

import cz.raixo.blocks.block.playerdata.PlayerData;
import cz.raixo.blocks.block.rewards.commands.batch.BatchRewardCommands;
import cz.raixo.blocks.block.rewards.commands.random.RandomRewardCommands;
import cz.raixo.blocks.block.rewards.context.RewardContext;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * The entries of one reward and the rule that decides which of them a player actually receives.
 *
 * @param <T> concrete entry type this mode stores
 */
public interface RewardCommands<T extends RewardEntry> {

    static RewardCommands<? extends RewardEntry> parse(@Nullable String mode, List<?> commands) {
        List<RawEntry> entries = RawEntry.parseAll(commands);
        return "all".equalsIgnoreCase(Optional.ofNullable(mode).orElse(""))
                ? new BatchRewardCommands(entries)
                : new RandomRewardCommands(entries);
    }

    List<T> asList();

    List<Object> saveToList();

    void addCommand(T command);

    void removeCommand(RewardEntry command);

    /** Picks the entries this player receives right now. */
    List<? extends RewardEntry> rewardPlayer(PlayerData player, RewardContext context);

    String getModeName();

}
