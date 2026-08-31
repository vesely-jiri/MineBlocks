package cz.raixo.blocks.block.placeholder;

import cz.raixo.blocks.block.MineBlock;
import cz.raixo.blocks.block.cooldown.BlockCoolDown;
import cz.raixo.blocks.block.playerdata.PlayerData;
import cz.raixo.blocks.block.tool.RequiredTool;
import cz.raixo.blocks.block.top.BlockTop;
import cz.raixo.blocks.config.MineBlocksConfig;
import cz.raixo.blocks.integration.models.prefix.PrefixProvider;
import cz.raixo.blocks.util.placeholders.PlaceholderSet;

import java.util.Optional;

/**
 * Placeholders describing a block: used by hologram lines, block messages and reward messages.
 */
public class BlockPlaceholderSet extends PlaceholderSet {

    public BlockPlaceholderSet(MineBlock block) {
        MineBlocksConfig config = block.getPlugin().getConfiguration();
        PrefixProvider prefixProvider = block.getPlugin().getIntegrationManager().getPrefixProvider();

        for (int i = 0; i < BlockTop.MAX_TOP_SIZE; i++) {
            int pos = i;
            String posStr = "player_" + (pos + 1);
            addPlaceholder(posStr, () -> block.getTop()
                    .getPlayer(pos)
                    .map(PlayerData::getDisplayName)
                    .orElse(config.getLangConfig().getNobodyName())
            );
            addPlaceholder(posStr + "_breaks", () -> block.getTop()
                    .getPlayer(pos)
                    .map(PlayerData::getBreaks)
                    .map(String::valueOf)
                    .orElse(config.getLangConfig().getNobodyBreaks())
            );
            if (prefixProvider != null) {
                addPlaceholder(posStr + "_prefix", () -> block.getTop()
                        .getPlayer(pos)
                        .map(PlayerData::getUuid)
                        .map(prefixProvider::provide)
                        .orElse("")
                );
            }
        }
        addPlaceholder("id", block::getId);
        addPlaceholder("name", block::getDisplayName);
        addPlaceholder("type", () -> String.valueOf(block.getType().getType()));
        addPlaceholder("health", () -> String.valueOf(block.getHealth().getHealth()));
        addPlaceholder("max_health", () -> String.valueOf(block.getHealth().getMaxHealth()));
        // How many breaks the block has taken this cycle, the counterpart of %health%.
        addPlaceholder("broken", () -> String.valueOf(block.getBrokenCount()));
        addPlaceholder("reward_info", () -> Optional.ofNullable(block.getRewardInfo()).orElse(""));
        addPlaceholder("required_tool", () -> Optional.ofNullable(block.getRequiredTool())
                .map(RequiredTool::describe)
                .orElse(""));
        addPlaceholder("timeout", () -> {
            BlockCoolDown coolDown = block.getCoolDown();
            if (!coolDown.isActive()) return "";
            return config.getLangConfig().getTimeoutFormatted(coolDown.getActive().getEnd());
        });
    }

}
