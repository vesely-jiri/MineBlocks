package cz.raixo.blocks.block.rewards;

import cz.raixo.blocks.block.MineBlock;
import cz.raixo.blocks.block.playerdata.PlayerData;
import cz.raixo.blocks.block.playerdata.placeholder.PlayerDataPlaceholderSet;
import cz.raixo.blocks.block.rewards.commands.RewardEntry;
import cz.raixo.blocks.block.rewards.context.RewardContext;
import cz.raixo.blocks.block.rewards.offline.OfflineRewardsStorage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@RequiredArgsConstructor
public class BlockRewards {

    private final MineBlock block;
    /** Rewards evaluated on every break. */
    private final List<Reward> rewards;
    /** Rewards evaluated once, when the block runs out of health. */
    private final List<Reward> lastRewards;

    /**
     * Builds the actions for the rewards handed out when the block is mined out.
     *
     * <p>Nothing is executed here: the caller runs the returned task once the block state has
     * settled, so a reward command cannot observe a half-updated block.</p>
     */
    public Runnable giveLastRewards(UUID lastBreaker) {
        Map<UUID, Integer> positions = new HashMap<>();
        List<UUID> sorted = block.getPlayerDataMap().values().stream()
                .sorted(Comparator.comparingInt(PlayerData::getBreaks).reversed())
                .map(PlayerData::getUuid)
                .toList();
        for (int i = 0; i < sorted.size(); i++) {
            positions.put(sorted.get(i), i + 1);
        }
        RewardContext context = new RewardContext(block, ThreadLocalRandom.current(), positions, lastBreaker);

        boolean offlineRewards = block.getPlugin().getConfiguration().getOptionsConfig().hasOfflineRewards();
        OfflineRewardsStorage offlineStorage = block.getPlugin().getOfflineRewards();

        List<Runnable> toExecute = new LinkedList<>();
        for (PlayerData player : block.getPlayerDataMap().values()) {
            for (Reward reward : lastRewards) {
                if (!reward.canGet(player, context)) continue;
                for (RewardEntry entry : reward.getCommands().rewardPlayer(player, context)) {
                    String command = parsePlaceholders(player, entry.getCommand());
                    if (offlineRewards && !player.isOnline()) {
                        storeForLater(offlineStorage, player, command);
                        continue;
                    }
                    toExecute.add(() -> grant(player, entry, command));
                }
            }
        }
        return () -> toExecute.forEach(Runnable::run);
    }

    /** Builds the actions for the rewards a single break earns, if any. */
    public Runnable giveRewards(PlayerData player) {
        RewardContext context = new RewardContext(block, ThreadLocalRandom.current(), null, null);
        List<Runnable> toExecute = new LinkedList<>();
        for (Reward reward : rewards) {
            if (!reward.canGet(player, context)) continue;
            for (RewardEntry entry : reward.getCommands().rewardPlayer(player, context)) {
                String command = parsePlaceholders(player, entry.getCommand());
                toExecute.add(() -> grant(player, entry, command));
            }
        }
        return () -> toExecute.forEach(Runnable::run);
    }

    private void grant(PlayerData player, RewardEntry entry, String command) {
        dispatchCommand(command);
        announce(player, entry);
    }

    /**
     * Runs a reward command and complains loudly when the server did not accept it.
     *
     * <p>Without this a typo or a missing permission plugin fails in silence: the player still
     * reads "you unlocked gold.nexus" while nothing was granted.</p>
     */
    private void dispatchCommand(String command) {
        if (command == null || command.isBlank()) return;
        CommandSender sender = block.getPlugin().getServer().getConsoleSender();
        if (!block.getPlugin().getServer().dispatchCommand(sender, command)) {
            block.getPlugin().logWarn(
                    "Reward command of block " + block.getId() + " was rejected by the server: /" + command
                            + " - is the plugin that owns this command installed?"
            );
        }
    }

    /**
     * Tells the player what they got. Most rewards stay silent and let the granting plugin speak;
     * the ones that unlock access to the next block announce themselves.
     */
    private void announce(PlayerData player, RewardEntry entry) {
        String message = entry.getMessage();
        if (message == null || message.isBlank()) return;
        if (entry.isBroadcast()) {
            block.broadcast(message);
            return;
        }
        Player online = block.getPlugin().getServer().getPlayer(player.getUuid());
        if (online != null) block.message(online, message);
    }

    private void storeForLater(OfflineRewardsStorage storage, PlayerData player, String command) {
        try {
            storage.addCommand(player.getUuid(), command);
        } catch (IOException e) {
            block.getPlugin().getLogger().warning(
                    "Can't store offline reward '" + command + "' for player " + player.getDisplayName() + ": " + e.getMessage()
            );
        }
    }

    private String parsePlaceholders(PlayerData playerData, String value) {
        OfflinePlayer offlinePlayer = block.getPlugin().getServer().getOfflinePlayer(playerData.getUuid());
        String parsed = new PlayerDataPlaceholderSet(playerData).parse(value);
        return block.getPlugin().getIntegrationManager().setPlaceholders(offlinePlayer, parsed);
    }

}
