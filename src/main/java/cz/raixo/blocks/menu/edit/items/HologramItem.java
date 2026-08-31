package cz.raixo.blocks.menu.edit.items;

import cz.raixo.blocks.block.MineBlock;
import cz.raixo.blocks.commands.MineBlocksCommand;
import cz.raixo.blocks.gui.item.click.ItemClickEvent;
import cz.raixo.blocks.gui.itemstack.ItemStackBuilder;
import cz.raixo.blocks.menu.BlockMenu;
import cz.raixo.blocks.menu.BlockMenuItem;
import cz.raixo.blocks.util.color.Colors;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedList;
import java.util.List;

public class HologramItem extends BlockMenuItem {

    public HologramItem(BlockMenu<?> editMenu) {
        super(editMenu);
    }

    @Override
    public void click(ItemClickEvent<MineBlock> itemClickEvent) {
        itemClickEvent.getPlayer().closeInventory();
        MineBlocksCommand.showHologram(itemClickEvent.getPlayer(), getState());
    }

    @Override
    public ItemStack render(MineBlock state) {
        List<Component> lore = new LinkedList<>();
        lore.add(Component.empty());

        List<Component> preview = getState().getHologram().getPreview();

        if (preview != null) {
            for (Component component : preview) {
                lore.add(Colors.itemComponent("&8- ").append(component));
            }
            lore.add(Component.empty());
        }

        lore.add(Colors.itemComponent("&7Click to edit"));
        return ItemStackBuilder.create(Material.OAK_SIGN)
                .withName(Colors.itemComponent("&#205295&&lHologram"))
                .withLore(lore)
                .build();
    }

}
