package cz.raixo.blocks.listener;

import cz.raixo.blocks.MineBlocksPlugin;
import cz.raixo.blocks.block.MineBlock;
import cz.raixo.blocks.block.placeholder.BlockPlaceholderSet;
import cz.raixo.blocks.block.tool.RequiredTool;
import cz.raixo.blocks.config.options.NotificationType;
import cz.raixo.blocks.config.options.OptionsConfig;
import cz.raixo.blocks.util.color.Colors;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BlocksListener implements Listener {

    private final MineBlocksPlugin plugin;
    private final Map<UUID, Long> lastBreak = new HashMap<>();

    public BlocksListener(MineBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        MineBlock block = plugin.getBlockRegistry().get(e.getBlock().getLocation());
        if (block == null) return;

        // A mine block is never actually broken; every hit is a counted interaction.
        e.setCancelled(true);

        if (isRateLimited(block, player)) return;

        String statusMessage = checkStatus(block, player);
        if (statusMessage != null) {
            NotificationType type = options().getNotificationType();
            type.send(player, Colors.component(new BlockPlaceholderSet(block).parse(statusMessage)));
            player.playSound(e.getBlock().getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 1f);
            return;
        }

        block.onBreak(player).run();
    }

    /** Returns the message explaining why the player may not mine, or {@code null} when they may. */
    private String checkStatus(MineBlock block, Player player) {
        var lang = plugin.getConfiguration().getLangConfig();
        if (block.hasPermission() && !player.hasPermission(block.getPermission())) return lang.getStatusNoPermission();
        if (isAfk(player)) return lang.getStatusAFK();
        if (block.getCoolDown().isActive()) return lang.getStatusTimeout();
        if (!hasValidTool(block, player)) return lang.getStatusInvalidTool();
        return null;
    }

    /**
     * Guards against auto-clickers and against a single dig registering many times. A block may
     * override the global limit; a negative block value means "use the global one".
     */
    private boolean isRateLimited(MineBlock block, Player player) {
        int breakLimit = block.getBreakLimit();
        if (breakLimit < 0) breakLimit = options().getBlockBreakLimit();
        if (breakLimit <= 0) return false;

        long now = System.currentTimeMillis();
        long last = lastBreak.getOrDefault(player.getUniqueId(), 0L);
        if (last + breakLimit > now) return true;
        lastBreak.put(player.getUniqueId(), now);
        return false;
    }

    /**
     * Uses the server's own idle timer instead of an AFK plugin, so no third-party dependency is
     * needed for this check.
     */
    private boolean isAfk(Player player) {
        OptionsConfig options = options();
        if (!options.isAfkEnabled()) return false;
        Duration idle = player.getIdleDuration();
        return idle.getSeconds() >= options.getAfkIdleSeconds();
    }

    private boolean hasValidTool(MineBlock block, Player player) {
        RequiredTool requiredTool = block.getRequiredTool();
        if (requiredTool == null) return true;
        return requiredTool.test(player.getInventory().getItemInMainHand());
    }

    private OptionsConfig options() {
        return plugin.getConfiguration().getOptionsConfig();
    }

    /**
     * Hologram entities are spawned non-persistent, so they disappear with their chunk. Bring them
     * back when the chunk returns.
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        Chunk chunk = e.getChunk();
        for (MineBlock block : plugin.getBlockRegistry().getBlocks()) {
            Location location = block.getLocation();
            if (location == null || !chunk.getWorld().equals(location.getWorld())) continue;
            if (location.getBlockX() >> 4 != chunk.getX() || location.getBlockZ() >> 4 != chunk.getZ()) continue;
            block.show();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lastBreak.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!options().hasOfflineRewards()) return;
        UUID uuid = e.getPlayer().getUniqueId();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<String> rewards = plugin.getOfflineRewards().getAndRemoveCommands(uuid);
                if (rewards.isEmpty()) return;
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    CommandSender sender = plugin.getServer().getConsoleSender();
                    rewards.forEach(reward -> plugin.getServer().dispatchCommand(sender, reward));
                });
            } catch (IOException ex) {
                plugin.logWarn("Could not deliver offline rewards for " + uuid + ": " + ex.getMessage());
            }
        });
    }

}
