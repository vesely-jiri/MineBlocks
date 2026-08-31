package cz.raixo.blocks.hologram;

import org.bukkit.Location;

import java.util.List;

/**
 * A floating text display attached to a mine block.
 *
 * <p>Lines are handed over as already colourised legacy strings; the implementation decides how to
 * render them.</p>
 */
public interface Hologram {

    /** Prefix marking a line that should render as a floating item instead of text. */
    String ICON_PREFIX = "#ICON:";

    void setLocation(Location location);

    void setLines(List<String> lines);

    void setVisible(boolean visible);

    /** Removes the hologram and every entity backing it. */
    void delete();

}
