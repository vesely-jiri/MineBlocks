package cz.raixo.blocks.menu.edit.tool.items;

import cz.raixo.blocks.block.MineBlock;
import cz.raixo.blocks.gui.item.click.ItemClickEvent;
import cz.raixo.blocks.gui.itemstack.ItemStackBuilder;
import cz.raixo.blocks.menu.BlockMenu;
import cz.raixo.blocks.menu.BlockMenuItem;
import cz.raixo.blocks.util.color.Colors;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

/**
 * Toggles whether mining this block wears the player's tool down.
 *
 * <p>Mine blocks cancel the break event, so the server never applies durability by itself. Off
 * means tools last forever on this block, which is what every config did before the option
 * existed.</p>
 */
public class ToolDurabilityItem extends BlockMenuItem {

    public ToolDurabilityItem(BlockMenu<?> editMenu) {
        super(editMenu);
    }

    @Override
    public void click(ItemClickEvent<MineBlock> itemClickEvent) {
        MineBlock block = getState();
        block.setDamageTools(!block.isDamageTools());
        getMenu().saveAndUpdate();
    }

    @Override
    public ItemStack render(MineBlock state) {
        boolean damages = state.isDamageTools();
        return ItemStackBuilder.create(damages ? Material.DAMAGED_ANVIL : Material.ANVIL)
                .withName(Colors.itemComponent("&#205295&&lTool durability"))
                .withLore(
                        Component.empty(),
                        Colors.itemComponent("&7Tools take damage: " +
                                (damages ? "&#539165&Yes" : "&#DF2E38&No")),
                        Component.empty(),
                        damages
                                ? Colors.itemComponent("&7Each break costs &#2C74B3&1 &7durability,")
                                : Colors.itemComponent("&7Tools never wear down"),
                        damages
                                ? Colors.itemComponent("&7Unbreaking still applies.")
                                : Colors.itemComponent("&7while mining this block."),
                        Component.empty(),
                        Colors.itemComponent("&7Click to toggle")
                )
                .addItemFlags(ItemFlag.values())
                .build();
    }
}
