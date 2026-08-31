package cz.raixo.blocks.menu.edit.items;

import cz.raixo.blocks.block.MineBlock;
import cz.raixo.blocks.gui.Gui;
import cz.raixo.blocks.gui.item.click.ItemClickEvent;
import cz.raixo.blocks.gui.itemstack.ItemStackBuilder;
import cz.raixo.blocks.menu.BlockMenu;
import cz.raixo.blocks.menu.BlockMenuItem;
import cz.raixo.blocks.util.color.Colors;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.TimeoutException;

/**
 * Edits the player facing block name, rendered as {@code %name%} in holograms and messages. This
 * is separate from the block id, which is the config key and must stay stable.
 */
public class DisplayNameItem extends BlockMenuItem {

    public DisplayNameItem(BlockMenu<?> editMenu) {
        super(editMenu);
    }

    @Override
    public void click(ItemClickEvent<MineBlock> itemClickEvent) {
        Player player = itemClickEvent.getPlayer();
        player.closeInventory();
        MineBlock block = getState();
        Colors.send(player, "#2C74B3Enter the new display name into chat. Colour codes are allowed.");
        block.getPlugin().getEditValuesListener().awaitChatInput(player)
                .exceptionally(throwable -> {
                    if (throwable instanceof TimeoutException) {
                        Colors.send(player, "#DF2E38You took too long to enter the name!");
                    } else {
                        Colors.send(player, "#DF2E38An error occurred: " + throwable.getMessage());
                    }
                    return null;
                })
                .thenAccept(input -> Gui.runSync(() -> {
                    if (input != null) {
                        block.setDisplayName(input);
                        getMenu().save();
                        block.getHologram().update();
                    }
                    getMenu().open(player);
                }));
    }

    @Override
    public ItemStack render(MineBlock state) {
        return ItemStackBuilder.create(Material.OAK_HANGING_SIGN)
                .withName(Colors.itemComponent("&#205295&&lDisplay name"))
                .withLore(
                        Component.empty(),
                        Colors.itemComponent("&7Shown as &#2C74B3&%name%&7 in"),
                        Colors.itemComponent("&7holograms and messages"),
                        Component.empty(),
                        Colors.itemComponent("&7Current: ").append(Colors.itemComponent(state.getDisplayName())),
                        Component.empty(),
                        Colors.itemComponent("&7Click to change")
                ).build();
    }

}
