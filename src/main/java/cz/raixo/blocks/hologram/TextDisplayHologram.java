package cz.raixo.blocks.hologram;

import cz.raixo.blocks.util.ConfigUtil;
import cz.raixo.blocks.util.color.Colors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Hologram backed by vanilla display entities, so no external hologram plugin is required.
 *
 * <p>All text lines are rendered by a single {@link TextDisplay} (it supports multi-line text
 * natively, which also gives one shared background instead of one per line). A line starting with
 * {@link #ICON_PREFIX} spawns an {@link ItemDisplay} floating above the text.</p>
 *
 * <p>The entities are spawned non-persistent so they are never written to the region files; that
 * makes a crashed or force-killed server impossible to litter with orphans. They are respawned when
 * the chunk loads again. Any entity that does survive is still tagged with {@link #ID_KEY} and gets
 * cleaned up before a new one is spawned.</p>
 */
public class TextDisplayHologram implements Hologram {

    /** Vertical distance between two rendered text lines, in blocks. */
    private static final float LINE_HEIGHT = 0.26f;
    /** Gap between the top of the text block and the floating icon. */
    private static final float ICON_GAP = 0.45f;

    /** Marks the display entities as ours, so orphans can be recognised and cleaned up. */
    public static NamespacedKey idKey(Plugin plugin) {
        return new NamespacedKey(plugin, "hologram_id");
    }

    private final String id;
    private final NamespacedKey idKey;

    private Location location;
    private List<String> lines = List.of();
    private boolean visible;

    private TextDisplay text;
    private ItemDisplay icon;

    public TextDisplayHologram(Plugin plugin, String id, Location location) {
        this.id = id;
        this.idKey = idKey(plugin);
        this.location = location;
        removeOrphans(location);
    }

    @Override
    public void setLocation(Location location) {
        this.location = location;
        if (visible) {
            despawn();
            spawn();
        }
    }

    @Override
    public void setLines(List<String> lines) {
        this.lines = List.copyOf(lines);
        if (visible) render();
    }

    @Override
    public void setVisible(boolean visible) {
        if (this.visible == visible) {
            if (visible) render();
            return;
        }
        this.visible = visible;
        if (visible) {
            spawn();
        } else {
            despawn();
        }
    }

    @Override
    public void delete() {
        setVisible(false);
        removeOrphans(location);
    }

    /**
     * Re-creates the entities if the server removed them, for example after the chunk was unloaded.
     * Cheap enough to call on a timer.
     */
    public void ensureSpawned() {
        if (!visible) return;
        if (text == null || !text.isValid()) {
            despawn();
            spawn();
        }
    }

    private void spawn() {
        World world = location.getWorld();
        if (world == null) return;
        if (!world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return;
        removeOrphans(location);
        render();
    }

    private void despawn() {
        if (text != null) {
            text.remove();
            text = null;
        }
        if (icon != null) {
            icon.remove();
            icon = null;
        }
    }

    private void render() {
        World world = location.getWorld();
        if (world == null) return;

        List<String> textLines = new ArrayList<>();
        Material iconMaterial = null;
        for (String line : lines) {
            if (line.startsWith(ICON_PREFIX)) {
                iconMaterial = ConfigUtil.getMaterial(
                        Colors.strip(line.substring(ICON_PREFIX.length())).trim().toUpperCase()
                );
            } else {
                textLines.add(line);
            }
        }

        renderText(world, textLines);
        renderIcon(world, iconMaterial, textLines.size());
    }

    private void renderText(World world, List<String> textLines) {
        if (textLines.isEmpty()) {
            if (text != null) {
                text.remove();
                text = null;
            }
            return;
        }
        Component content = Component.join(
                JoinConfiguration.newlines(),
                textLines.stream().map(Colors::component).toList()
        );
        if (text == null || !text.isValid()) {
            text = world.spawn(textLocation(textLines.size()), TextDisplay.class, display -> {
                applyCommon(display);
                display.setBillboard(Display.Billboard.CENTER);
                display.setAlignment(TextDisplay.TextAlignment.CENTER);
                display.setSeeThrough(false);
                // Drop shadow keeps the text readable once the background is transparent.
                display.setShadowed(true);
                display.setDefaultBackground(false);
                display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                display.text(content);
            });
        } else {
            text.text(content);
            text.teleport(textLocation(textLines.size()));
        }
    }

    private void renderIcon(World world, Material material, int textLineCount) {
        if (material == null || material.isAir()) {
            if (icon != null) {
                icon.remove();
                icon = null;
            }
            return;
        }
        ItemStack itemStack = new ItemStack(material);
        Location iconLocation = iconLocation(textLineCount);
        if (icon == null || !icon.isValid()) {
            icon = world.spawn(iconLocation, ItemDisplay.class, display -> {
                applyCommon(display);
                display.setBillboard(Display.Billboard.VERTICAL);
                display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
                display.setItemStack(itemStack);
            });
        } else {
            icon.setItemStack(itemStack);
            icon.teleport(iconLocation);
        }
    }

    private void applyCommon(Display display) {
        display.setPersistent(false);
        display.setViewRange(1.0f);
        display.setShadowRadius(0);
        display.setShadowStrength(0);
        display.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);
    }

    /** The text block is centred on the entity, so lift it by half its own height. */
    private Location textLocation(int lineCount) {
        return location.clone().add(0, (lineCount - 1) * LINE_HEIGHT / 2f, 0);
    }

    private Location iconLocation(int textLineCount) {
        return location.clone().add(0, textLineCount * LINE_HEIGHT + ICON_GAP, 0);
    }

    /** Removes leftovers from a previous server run or a crashed hologram with the same id. */
    private void removeOrphans(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        if (!world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return;
        for (Entity entity : world.getNearbyEntities(location, 4, 6, 4)) {
            if (entity.equals(text) || entity.equals(icon)) continue;
            Optional.ofNullable(entity.getPersistentDataContainer().get(idKey, PersistentDataType.STRING))
                    .filter(id::equals)
                    .ifPresent(ignored -> entity.remove());
        }
    }

}
