package cz.raixo.blocks.block.hologram;

import cz.raixo.blocks.MineBlocksPlugin;
import cz.raixo.blocks.block.MineBlock;
import cz.raixo.blocks.block.placeholder.BlockPlaceholderSet;
import cz.raixo.blocks.hologram.Hologram;
import cz.raixo.blocks.hologram.TextDisplayHologram;
import cz.raixo.blocks.util.color.Colors;
import cz.raixo.blocks.util.placeholders.PlaceholderSet;
import lombok.AccessLevel;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Getter
public class BlockHologram {

    @Getter(AccessLevel.NONE)
    private final MineBlock block;
    @Getter(AccessLevel.NONE)
    private final TextDisplayHologram hologram;
    private final HologramOffset offset;
    private final List<String> lines;
    @Getter(AccessLevel.NONE)
    private final PlaceholderSet placeholders;
    @Getter(AccessLevel.NONE)
    private boolean shouldUpdate;
    @Getter(AccessLevel.NONE)
    private BukkitTask updateTask;
    private final int updateInterval;

    public BlockHologram(MineBlock block, HologramOffset offset, List<String> lines) {
        this.block = block;
        this.offset = offset;
        this.lines = new LinkedList<>(lines);
        this.placeholders = new BlockPlaceholderSet(block);
        this.hologram = new TextDisplayHologram(block.getPlugin(), "mineblock-" + block.getId(), getLocation());
        this.updateInterval = block.getPlugin().getConfiguration().getOptionsConfig().getUpdateInterval();
    }

    public HologramOffset getOffset() {
        return Optional.ofNullable(offset).orElseGet(() -> new HologramOffset(0, 0, 0));
    }

    public Location getLocation() {
        HologramOffset hologramOffset = getOffset();
        return block.getLocation().clone()
                .add(.5, 1.5, .5)
                .add(hologramOffset.getX(), hologramOffset.getY(), hologramOffset.getZ());
    }

    public void updateLines() {
        List<String> rendered = new ArrayList<>(lines.size());
        for (String line : lines) {
            String parsed = placeholders.parse(line);
            // Drop lines that collapse to nothing once placeholders resolve (an inactive cooldown,
            // an empty top slot), but keep lines that were intentionally left blank as spacing.
            if (!line.isBlank() && Colors.strip(parsed).isBlank()) continue;
            rendered.add(parsed);
        }
        hologram.setLines(rendered);
    }

    /** Renders the configured lines as components for the in-game editor preview. */
    public List<Component> getPreview() {
        return lines.stream().map(Colors::component).toList();
    }

    public void updateLocation() {
        hologram.setLocation(getLocation());
    }

    public void update() {
        if (updateInterval > 0) {
            shouldUpdate = true;
        } else if (block.getPlugin().getServer().isPrimaryThread()) {
            updateLines();
        } else {
            MineBlocksPlugin plugin = block.getPlugin();
            plugin.getServer().getScheduler().runTask(plugin, this::updateLines);
        }
    }

    public void show() {
        changeVisibility(true);
    }

    public void hide() {
        changeVisibility(false);
    }

    public void delete() {
        changeVisibility(false);
        hologram.delete();
    }

    private synchronized void changeVisibility(boolean visible) {
        if (visible) {
            hologram.setVisible(true);
            updateLines();
            if (updateTask == null) {
                // Doubles as the keep-alive tick: display entities are not persisted, so they have
                // to be respawned after their chunk cycles.
                int interval = updateInterval > 0 ? updateInterval : 20;
                updateTask = block.getPlugin().getServer().getScheduler().runTaskTimer(block.getPlugin(), () -> {
                    hologram.ensureSpawned();
                    if (updateInterval > 0 && shouldUpdate) {
                        shouldUpdate = false;
                        updateLines();
                    }
                }, interval, interval);
            }
        } else {
            if (updateTask != null) {
                updateTask.cancel();
                updateTask = null;
            }
            hologram.setVisible(false);
        }
    }

    public void setLine(int line, String value) {
        lines.set(line, value);
        update();
    }

    public void removeLine(int line) {
        lines.remove(line);
        update();
    }

    public void addLine(String value) {
        lines.add(value);
        update();
    }

}
