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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

/**
 * Edits the short reward description shown on the hologram as {@code %reward_info%}.
 *
 * <p>The reward commands themselves live behind the rewards item; this is only the line players
 * read before they start mining.</p>
 */
public class RewardInfoItem extends BlockMenuItem {

    public RewardInfoItem(BlockMenu<?> editMenu) {
        super(editMenu);
    }

    @Override
    public void click(ItemClickEvent<MineBlock> itemClickEvent) {
        Player player = itemClickEvent.getPlayer();
        MineBlock block = getState();

        if (itemClickEvent.getType() == ClickType.RIGHT) {
            block.setRewardInfo(null);
            getMenu().saveAndUpdate();
            block.getHologram().update();
            return;
        }

        player.closeInventory();
        Colors.send(player, "#2C74B3Enter the reward description into chat. Colour codes are allowed.");
        block.getPlugin().getEditValuesListener().awaitChatInput(player)
                .exceptionally(throwable -> {
                    if (throwable instanceof TimeoutException) {
                        Colors.send(player, "#DF2E38You took too long to enter the description!");
                    } else {
                        Colors.send(player, "#DF2E38An error occurred: " + throwable.getMessage());
                    }
                    return null;
                })
                .thenAccept(input -> Gui.runSync(() -> {
                    if (input != null) {
                        block.setRewardInfo(input);
                        getMenu().save();
                        block.getHologram().update();
                    }
                    getMenu().open(player);
                }));
    }

    @Override
    public ItemStack render(MineBlock state) {
        List<Component> lore = new ArrayList<>(List.of(
                Component.empty(),
                Colors.itemComponent("&7Shown as &#2C74B3&%reward_info%&7 in"),
                Colors.itemComponent("&7the hologram"),
                Component.empty()
        ));
        String current = Optional.ofNullable(state.getRewardInfo()).filter(s -> !s.isBlank()).orElse(null);
        if (current == null) {
            lore.add(Colors.itemComponent("&7Current: &8not set"));
        } else {
            lore.add(Colors.itemComponent("&7Current: ").append(Colors.itemComponent(current)));
        }
        lore.add(Component.empty());
        lore.add(Colors.itemComponent("&7Left click to change"));
        lore.add(Colors.itemComponent("&7Right click to &#DF2E38&clear"));

        return ItemStackBuilder.create(Material.CHEST)
                .withName(Colors.itemComponent("&#205295&&lReward info"))
                .withLore(lore)
                .build();
    }

}
