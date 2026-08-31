package cz.raixo.blocks.menu.edit.items;

import cz.raixo.blocks.block.MineBlock;
import cz.raixo.blocks.gui.Gui;
import cz.raixo.blocks.gui.item.click.ItemClickEvent;
import cz.raixo.blocks.gui.itemstack.ItemStackBuilder;
import cz.raixo.blocks.menu.BlockMenu;
import cz.raixo.blocks.menu.BlockMenuItem;
import cz.raixo.blocks.menu.edit.EditMenu;
import cz.raixo.blocks.util.color.Colors;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.TimeoutException;

public class RenameItem extends BlockMenuItem {

    public RenameItem(BlockMenu<?> editMenu) {
        super(editMenu);
    }

    @Override
    public void click(ItemClickEvent<MineBlock> itemClickEvent) {
        Player player = itemClickEvent.getPlayer();
        player.closeInventory();
        MineBlock block = getState();
        Colors.send(player, "#2C74B3Enter new name into chat. Name can't contain spaces!");
        block.getPlugin().getEditValuesListener().awaitChatInput(player)
                .exceptionally(throwable -> {
                    if (throwable instanceof TimeoutException) {
                        Colors.send(player, "#DF2E38You took too long to enter new name!");
                    } else {
                        Colors.send(player, "#DF2E38An error occurred");
                        throwable.printStackTrace();
                    }
                    return null;
                })
                .thenAccept(s -> Gui.runSync(() -> {
                    if (s == null) {
                        getMenu().open(player);
                        return;
                    }
                    if (s.contains(" ")) {
                        Colors.send(player, "#DF2E38Block name can't contain spaces!");
                        getMenu().open(player);
                    } else if (!block.getPlugin().getBlockRegistry().changeId(block, s)) {
                        Colors.send(player, "#DF2E38A block named " + s + " already exists!");
                        getMenu().open(player);
                    } else {
                        new EditMenu(block).open(player);
                    }
                }));
    }

    @Override
    public ItemStack render(MineBlock state) {
        return ItemStackBuilder.create(Material.NAME_TAG)
                .withName(Colors.itemComponent("&#205295&&lRename"))
                .withLore(
                        Component.empty(),
                        Colors.itemComponent("&7Current name: &#2C74B3&" + state.getId()),
                        Component.empty(),
                        Colors.itemComponent("&7Click to change")
                ).build();
    }
}
