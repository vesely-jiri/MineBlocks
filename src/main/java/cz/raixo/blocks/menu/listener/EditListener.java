package cz.raixo.blocks.menu.listener;

import cz.raixo.blocks.MineBlocksPlugin;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class EditListener implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final MineBlocksPlugin plugin;
    private final Map<UUID, CompletableFuture<String>> chatInputFutures = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<Location>> locationFutures = new ConcurrentHashMap<>();

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        UUID player = e.getPlayer().getUniqueId();
        CompletableFuture<Location> future = locationFutures.remove(player);
        if (future != null && !future.isDone()) {
            e.setCancelled(true);
            future.complete(e.getBlock().getLocation());
        }
    }

    /**
     * Captures the next chat line from a player who is editing a value.
     *
     * <p>Uses Paper's component based chat event; the legacy {@code AsyncPlayerChatEvent} is
     * deprecated and no longer sees every message.</p>
     */
    @EventHandler
    public void onChat(AsyncChatEvent e) {
        UUID player = e.getPlayer().getUniqueId();
        CompletableFuture<String> future = chatInputFutures.remove(player);
        if (future == null || future.isDone()) return;
        e.setCancelled(true);
        // Back to the '&' notation the config and the editor use everywhere else.
        future.complete(LEGACY.serialize(e.message()).replace('§', '&'));
    }

    public CompletableFuture<String> awaitChatInput(Player player) {
        return chatInputFutures.compute(player.getUniqueId(), (uuid, future) -> {
            if (future != null && !future.isDone()) return future;
            return new CompletableFuture<String>()
                    .orTimeout(30, TimeUnit.SECONDS);
        });
    }

    public CompletableFuture<Location> awaitLocationSelection(Player player) {
        return locationFutures.compute(player.getUniqueId(), (uuid, future) -> {
            if (future != null && !future.isDone()) return future;
            return new CompletableFuture<Location>()
                    .orTimeout(30, TimeUnit.SECONDS);
        });
    }

    public void removeChat(Player player) {
        chatInputFutures.remove(player.getUniqueId());
    }

    public void removeLocation(Player player) {
        locationFutures.remove(player.getUniqueId());
    }

}
