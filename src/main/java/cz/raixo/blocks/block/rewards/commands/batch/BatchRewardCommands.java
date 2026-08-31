package cz.raixo.blocks.block.rewards.commands.batch;

import cz.raixo.blocks.block.playerdata.PlayerData;
import cz.raixo.blocks.block.rewards.commands.RawEntry;
import cz.raixo.blocks.block.rewards.commands.RewardCommands;
import cz.raixo.blocks.block.rewards.commands.RewardEntry;
import cz.raixo.blocks.block.rewards.context.RewardContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Hands out every configured entry, used by rewards where nothing should be left to chance. */
public class BatchRewardCommands implements RewardCommands<BatchCommandEntry> {

    static final String MODE_NAME = "all";

    private final List<BatchCommandEntry> commands;

    public BatchRewardCommands(List<RawEntry> commands) {
        this.commands = new ArrayList<>(commands.stream().map(BatchCommandEntry::of).toList());
    }

    @Override
    public List<BatchCommandEntry> asList() {
        return Collections.unmodifiableList(commands);
    }

    @Override
    public List<Object> saveToList() {
        return commands.stream().map(RewardEntry::serialize).toList();
    }

    @Override
    public void addCommand(BatchCommandEntry command) {
        commands.add(command);
    }

    @Override
    public void removeCommand(RewardEntry command) {
        commands.remove(command);
    }

    @Override
    public List<? extends RewardEntry> rewardPlayer(PlayerData player, RewardContext context) {
        return asList();
    }

    @Override
    public String getModeName() {
        return MODE_NAME;
    }

}
