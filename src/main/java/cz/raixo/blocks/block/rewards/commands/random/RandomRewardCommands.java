package cz.raixo.blocks.block.rewards.commands.random;

import cz.raixo.blocks.block.playerdata.PlayerData;
import cz.raixo.blocks.block.rewards.commands.RawEntry;
import cz.raixo.blocks.block.rewards.commands.RewardCommands;
import cz.raixo.blocks.block.rewards.commands.RewardEntry;
import cz.raixo.blocks.block.rewards.context.RewardContext;
import cz.raixo.blocks.util.SimpleRandom;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Hands out exactly one entry per grant, chosen by weight: an entry with {@code chance: 30} is
 * picked three times as often as one with {@code chance: 10}.
 */
public class RandomRewardCommands implements RewardCommands<RandomCommandEntry> {

    static final String MODE_NAME = "random";

    private final List<RandomCommandEntry> entries = new LinkedList<>();
    private final SimpleRandom<RandomCommandEntry> pool = new SimpleRandom<>();

    public RandomRewardCommands(List<RawEntry> commands) {
        for (RawEntry raw : commands) {
            entries.add(RandomCommandEntry.of(raw));
        }
        refresh();
    }

    @Override
    public List<RandomCommandEntry> asList() {
        return Collections.unmodifiableList(entries);
    }

    @Override
    public List<Object> saveToList() {
        return entries.stream().map(RewardEntry::serialize).toList();
    }

    @Override
    public void addCommand(RandomCommandEntry entry) {
        entries.add(entry);
        refresh();
    }

    @Override
    public void removeCommand(RewardEntry entry) {
        if (entries.remove(entry)) refresh();
    }

    public RandomCommandEntry getRandom(Random random) {
        return pool.next(random);
    }

    public void refresh() {
        pool.clear();
        for (RandomCommandEntry entry : entries) {
            pool.add(entry.getChance(), entry);
        }
    }

    @Override
    public List<? extends RewardEntry> rewardPlayer(PlayerData player, RewardContext context) {
        RandomCommandEntry entry = getRandom(context.getRandom());
        return entry == null ? List.of() : List.of(entry);
    }

    @Override
    public String getModeName() {
        return MODE_NAME;
    }

}
