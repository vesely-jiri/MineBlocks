package cz.raixo.blocks.menu.general;

import cz.raixo.blocks.gui.filler.GuiFiller;
import cz.raixo.blocks.gui.item.AbstractItem;
import cz.raixo.blocks.gui.item.click.ItemClickEvent;
import cz.raixo.blocks.gui.itemstack.ItemStackBuilder;
import cz.raixo.blocks.util.color.Colors;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class LearnMoreItem extends AbstractItem<Void> {

    public LearnMoreItem(GuiFiller<?> parent) {
        super(parent, null);
    }

    @Override
    public void click(ItemClickEvent<Void> itemClickEvent) {
        Player player = itemClickEvent.getPlayer();
        player.closeInventory();
        Colors.send(player,
                "&7Every option in this editor is also a key in &#2C74B3&config.yml&7.",
                "&7Command overview: &#2C74B3&/mb help",
                "&7Documentation: &#2C74B3&https://github.com/vesely-jiri/MineBlocks"
        );
    }

    @Override
    public ItemStack render(Void state) {
        return ItemStackBuilder.create(Material.KNOWLEDGE_BOOK)
                .withName(Colors.itemComponent("&#205295&&lAre you lost?"))
                .withLore(List.of(
                        Component.empty(),
                        Colors.itemComponent("&7Everything here maps to a"),
                        Colors.itemComponent("&7key in &#2C74B3&config.yml&7."),
                        Component.empty(),
                        Colors.itemComponent("&7Click for the command list")
                ))
                .build();
    }

}
