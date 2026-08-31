package cz.raixo.blocks.menu.edit.items;

import cz.raixo.blocks.block.MineBlock;
import cz.raixo.blocks.block.rewards.BlockRewards;
import cz.raixo.blocks.gui.item.click.ItemClickEvent;
import cz.raixo.blocks.gui.itemstack.ItemStackBuilder;
import cz.raixo.blocks.menu.BlockMenu;
import cz.raixo.blocks.menu.BlockMenuItem;
import cz.raixo.blocks.menu.edit.rewards.RewardsEditMenu;
import cz.raixo.blocks.util.color.Colors;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class RewardsItem  extends BlockMenuItem {
    public RewardsItem(BlockMenu<?> editMenu) {
        super(editMenu);
    }

    @Override
    public void click(ItemClickEvent<MineBlock> itemClickEvent) {
        new RewardsEditMenu(getState()).open(itemClickEvent.getPlayer());
    }

    @Override
    public ItemStack render(MineBlock state) {
        BlockRewards rewards = getState().getRewards();
        int totalRewards = rewards.getRewards().size() + rewards.getLastRewards().size();
        return ItemStackBuilder.create(Material.GOLD_NUGGET)
                .withName(Colors.itemComponent("&#205295&&lRewards"))
                .withLore(List.of(
                        Component.empty(),
                        Colors.itemComponent("&7There are &#2C74B3&" + totalRewards + " &7rewards"),
                        Colors.itemComponent("&7in total"),
                        Component.empty(),
                        Colors.itemComponent("&7Click to edit")
                ))
                .addItemFlags(ItemFlag.values())
                .build();
    }
}
