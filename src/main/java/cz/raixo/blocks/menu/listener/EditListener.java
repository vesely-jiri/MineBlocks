package cz.raixo.blocks.menu.listener;

import cz.raixo.blocks.MineBlocksPlugin;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class EditListener implements Listener {

    private final MineBlocksPlugin plugin;
    private final Map<UUID, CompletableFuture<String>> chatInputFutures = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<Location>> locationFutures = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent e) {
        UUID player = e.getPlayer().getUniqueId();
        CompletableFuture<Location> future = locationFutures.remove(player);
        if (future != null && !future.isDone()) {
            e.setCancelled(true);
            future.complete(e.getBlock().getLocation());
        }
    }

    /**
     * Reads exactly the characters a player typed out of a chat message.
     *
     * <p>Serialising to the legacy format instead would turn the <em>styling</em> of the message
     * component into {@code §} codes and bake them into the value, so a plain permission came back
     * carrying colour codes it was never given. Only the typed text is wanted here; if the admin
     * wants colour they type {@code &7} themselves and it arrives as those two characters.</p>
     */
    static String readChatInput(Component message) {
        // A section sign cannot be typed, so anything carrying one would bypass the colour
        // pipeline further down; normalise it to the '&' notation used everywhere else.
        return PlainTextComponentSerializer.plainText().serialize(message).replace('§', '&').trim();
    }

    /**
     * Captures the next chat line from a player who is editing a value.
     *
     * <p>Uses Paper's component based chat event; the legacy {@code AsyncPlayerChatEvent} is
     * deprecated and no longer sees every message.</p>
     */
    // LOWEST so the value is read before any chat plugin rewrites the message. A permission like
    // "mineblocks.blocks.gold" looks like a domain to a link detector, and a plugin that swaps it
    // for a styled link placeholder would otherwise be what gets stored.
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        UUID player = e.getPlayer().getUniqueId();
        CompletableFuture<String> future = chatInputFutures.remove(player);
        if (future == null || future.isDone()) return;
        e.setCancelled(true);
        future.complete(readChatInput(e.message()));
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
