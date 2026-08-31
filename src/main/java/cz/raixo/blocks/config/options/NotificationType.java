package cz.raixo.blocks.config.options;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Where short status feedback ("wrong tool", "on cooldown") is shown to the player. */
public enum NotificationType {
    ACTIONBAR {
        @Override
        public void send(Player player, Component message) {
            player.sendActionBar(message);
        }
    },
    CHAT {
        @Override
        public void send(Player player, Component message) {
            player.sendMessage(message);
        }
    },
    /** Shows nothing, for servers that consider the feedback spam. */
    NONE {
        @Override
        public void send(Player player, Component message) {
            // intentionally silent
        }
    };

    private static final Map<String, NotificationType> TYPES = new HashMap<>();

    static {
        for (NotificationType value : values()) {
            TYPES.put(value.name(), value);
        }
    }

    public static Optional<NotificationType> getByName(String name) {
        return Optional.ofNullable(TYPES.get(name.toUpperCase()));
    }

    public abstract void send(Player player, Component message);

}
