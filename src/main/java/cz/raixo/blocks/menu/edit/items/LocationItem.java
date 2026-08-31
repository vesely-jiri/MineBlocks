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
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.concurrent.TimeoutException;

public class LocationItem extends BlockMenuItem {

    public LocationItem(BlockMenu<?> editMenu) {
        super(editMenu);
    }

    @Override
    public void click(ItemClickEvent<MineBlock> itemClickEvent) {
        Player player = itemClickEvent.getPlayer();
        player.closeInventory();
        Colors.send(player, "#2C74B3Click on block to teleport mineblock there!");
        MineBlock block = getState();
        block.getPlugin().getEditValuesListener().awaitLocationSelection(player)
                .exceptionally(throwable -> {
                    if (throwable instanceof TimeoutException) {
                        Colors.send(player, "#DF2E38You took too long to select the location!");
                    } else {
                        Colors.send(player, "#DF2E38An error occurred");
                        throwable.printStackTrace();
                    }
                    return null;
                }).thenAccept((loc) -> Gui.runSync(() -> {
                    getMenu().open(player);
                    if (loc == null) return;
                    block.getPlugin().getBlockRegistry().changeLocation(block, loc);
                    getMenu().update();
                    getMenu().save();
                }));
    }

    @Override
    public ItemStack render(MineBlock state) {
        return ItemStackBuilder.create(Material.COMPASS)
                .withName(Colors.itemComponent("&#205295&&lLocation"))
                .withLore(
                        Component.empty(),
                        Colors.itemComponent("&7Current location: &#2C74B3&" + Optional.ofNullable(state.getLocation())
                                .map(loc ->
                                        Optional.ofNullable(loc.getWorld())
                                                .map(World::getName).orElse("unknown world") + ", " +
                                                loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ())
                                .orElse("&cUnknown location")),
                        Component.empty(),
                        Colors.itemComponent("&7Click to change")
                ).build();
    }

}
