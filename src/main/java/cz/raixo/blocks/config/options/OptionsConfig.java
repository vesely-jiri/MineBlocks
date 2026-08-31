package cz.raixo.blocks.config.options;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Optional;

public class OptionsConfig {

    private final ConfigurationSection config;

    public OptionsConfig(ConfigurationSection config) {
        this.config = config;
    }

    public boolean isAfkEnabled() {
        return config.getBoolean("afk.enabled", false);
    }

    /** How long a player has to be idle before their hits stop counting. */
    public int getAfkIdleSeconds() {
        return config.getInt("afk.idle-seconds", 300);
    }

    public NotificationType getNotificationType() {
        return Optional.ofNullable(config.getString("notification-type"))
                .flatMap(NotificationType::getByName)
                .orElse(NotificationType.ACTIONBAR);
    }

    /** Minimum milliseconds between two counted hits from the same player. */
    public int getBlockBreakLimit() {
        return config.getInt("block-break-limit", 0);
    }

    /** Hologram refresh interval in ticks; {@code -1} refreshes immediately on every change. */
    public int getUpdateInterval() {
        return config.getInt("hologram-update-interval", -1);
    }

    public boolean hasOfflineRewards() {
        return config.getBoolean("offline-rewards", false);
    }

}
