package cz.raixo.blocks.gui.itemstack;

import cz.raixo.blocks.gui.Gui;
import net.kyori.adventure.text.Component;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class ItemStackBuilder {

    public static ItemStackBuilder create(Material material) {
        return new ItemStackBuilder(new ItemStack(material));
    }

    /**
     * Builds a decorative player head from a base64 texture value.
     *
     * <p>Uses the profile API instead of {@code Bukkit.getUnsafe().modifyItemStack}, which is
     * deprecated and parses raw NBT that no longer matches the item component format.</p>
     */
    public static ItemStackBuilder create(String headValue) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (item.getItemMeta() instanceof SkullMeta skullMeta) {
            // The uuid only has to be stable and unique per texture; the texture does the work.
            PlayerProfile profile = Bukkit.createProfile(new UUID(headValue.hashCode(), headValue.hashCode()));
            profile.setProperty(new ProfileProperty("textures", headValue));
            skullMeta.setPlayerProfile(profile);
            item.setItemMeta(skullMeta);
        }
        return new ItemStackBuilder(item);
    }

    private final ItemStack itemStack;

    public ItemStackBuilder(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public ItemStackBuilder withName(Component name) {
        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;
        meta.displayName(name);
        itemStack.setItemMeta(meta);
        return this;
    }

    public ItemStackBuilder withLore(Component... lore) {
        return withLore(Arrays.asList(lore));
    }

    public ItemStackBuilder withLore(List<Component> lore) {
        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;
        meta.lore(lore);
        itemStack.setItemMeta(meta);
        return this;
    }

    public ItemStackBuilder withCount(int count) {
        itemStack.setAmount(count);
        return this;
    }

    public ItemStackBuilder withItemFlags(ItemFlag... itemFlags) {
        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;
        meta.removeItemFlags(meta.getItemFlags().toArray(ItemFlag[]::new));
        meta.addItemFlags(itemFlags);
        itemStack.setItemMeta(meta);
        return this;
    }

    public ItemStackBuilder addItemFlags(ItemFlag... itemFlags) {
        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;
        meta.addItemFlags(itemFlags);
        itemStack.setItemMeta(meta);
        return this;
    }

    public ItemStackBuilder withEnchantment(Enchantment enchantment, int level) {
        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;
        meta.addEnchant(enchantment, level, true);
        itemStack.setItemMeta(meta);
        return this;
    }

    public ItemStackBuilder withMeta(UnaryOperator<ItemMeta> updater) {
        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;
        itemStack.setItemMeta(updater.apply(meta));
        return this;
    }

    public ItemStackBuilder withItemStack(UnaryOperator<ItemStack> updater) {
        return new ItemStackBuilder(updater.apply(itemStack));
    }

    public ItemStackBuilder shiny() {
        return
                addItemFlags(ItemFlag.HIDE_ENCHANTS)
                        .withEnchantment(Enchantment.UNBREAKING, 1);

    }

    public ItemStack build() {
        return itemStack;
    }

}
