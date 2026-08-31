package cz.raixo.blocks.menu.edit.items;

import cz.raixo.blocks.block.MineBlock;
import cz.raixo.blocks.gui.Gui;
import cz.raixo.blocks.gui.item.click.ItemClickEvent;
import cz.raixo.blocks.gui.itemstack.ItemStackBuilder;
import cz.raixo.blocks.menu.BlockMenu;
import cz.raixo.blocks.menu.BlockMenuItem;
import cz.raixo.blocks.util.color.Colors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.TimeoutException;

public class PermissionItem extends BlockMenuItem {
    public PermissionItem(BlockMenu<?> editMenu) {
        super(editMenu);
    }

    @Override
    public void click(ItemClickEvent<MineBlock> itemClickEvent) {
        Player player = itemClickEvent.getPlayer();
        player.closeInventory();
        MineBlock block = getState();
        Colors.send(player, "#2C74B3Enter new permission into chat. Enter 'none' to remove permission");
        block.getPlugin().getEditValuesListener().awaitChatInput(player)
                .exceptionally(throwable -> {
                    if (throwable instanceof TimeoutException) {
                        Colors.send(player, "#DF2E38You took too long to enter new permission!");
                    } else {
                        Colors.send(player, "#DF2E38An error occurred");
                        throwable.printStackTrace();
                    }
                    return null;
                })
                .thenAccept(s -> Gui.runSync(() -> {
                    if (s != null) apply(player, block, s);
                    getMenu().open(player);
                }));
    }

    /**
     * A permission node is an identifier, not a message: it carries no colour and no spaces.
     * Anything the chat capture picked up beyond the typed text is dropped here rather than being
     * written into the config, where it would silently gate the block on a node nobody holds.
     */
    private void apply(Player player, MineBlock block, String input) {
        String value = Colors.strip(input).trim();

        if (value.isEmpty() || value.equalsIgnoreCase("none")) {
            block.setPermission(null);
            getMenu().saveAndUpdate();
            Colors.send(player, "#2C74B3Permission removed, anyone can mine this block now.");
            return;
        }
        if (value.contains(" ")) {
            Colors.send(player, "#DF2E38A permission can't contain spaces!");
            return;
        }

        block.setPermission(value);
        getMenu().saveAndUpdate();
        // Echoed back so a typo is visible immediately instead of at the next player complaint.
        Colors.send(player, "#2C74B3Permission set to &f" + value);
    }

    @Override
    public ItemStack render(MineBlock state) {
        Component current = state.hasPermission()
                // Rendered literally: the node is data, so it must never be read as colour codes.
                ? Component.text(state.getPermission(), TextColor.color(0x2C74B3)).decoration(TextDecoration.ITALIC, false)
                : Colors.itemComponent("&#DF2E38&No permission");

        return ItemStackBuilder.create(Material.IRON_BARS)
                .withName(Colors.itemComponent("&#205295&&lPermission"))
                .withLore(
                        Component.empty(),
                        Colors.itemComponent("&7Current permission: ").append(current),
                        Component.empty(),
                        Colors.itemComponent("&7Click to change")
                ).build();
    }
}
