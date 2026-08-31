package cz.raixo.blocks.util;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Registry lookups for enchantments.
 *
 * <p>{@code Enchantment.values()} and {@code Enchantment.getByKey(..)} are deprecated: enchantments
 * are registry entries now and a server can add its own, so the static list is no longer the truth.</p>
 */
public final class Enchantments {

    /** Every enchantment the server knows, in a stable order so menus do not shuffle between opens. */
    public static List<Enchantment> all() {
        return RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT)
                .stream()
                .sorted(Comparator.comparing(enchantment -> enchantment.getKey().getKey()))
                .toList();
    }

    /** Looks up a vanilla enchantment by its config key, e.g. {@code efficiency}. */
    public static Optional<Enchantment> byName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        return Optional.ofNullable(RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT)
                .get(NamespacedKey.minecraft(name.toLowerCase())));
    }

    private Enchantments() {}

}
