package cz.raixo.blocks.menu.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The editor asks for a value in chat and stores what comes back verbatim, so this conversion is
 * the boundary between "what the admin typed" and "what ends up in config.yml".
 */
class EditListenerTest {

    @Test
    void readsPlainTypedText() {
        assertEquals("mineblocks.blocks.gold",
                EditListener.readChatInput(Component.text("mineblocks.blocks.gold")));
    }

    @Test
    void styleOnTheMessageNeverLeaksIntoTheValue() {
        // A chat message arrives as a styled component. Serialising it to the legacy format turned
        // that styling into colour codes and wrote them into the value, so a plain permission came
        // back as something like "&fmineblocks.blocks.gold" and gated the block on a node that
        // does not exist.
        Component styled = Component.text("mineblocks.blocks.gold", NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD);

        assertEquals("mineblocks.blocks.gold", EditListener.readChatInput(styled));
    }

    @Test
    void styleSpreadOverSeveralPartsIsStillJustTheText() {
        Component split = Component.text("mineblocks.", NamedTextColor.GRAY)
                .append(Component.text("blocks.", TextColor.color(0x2C74B3)))
                .append(Component.text("gold", NamedTextColor.GOLD));

        assertEquals("mineblocks.blocks.gold", EditListener.readChatInput(split));
    }

    @Test
    void typedColourCodesAreKeptAsTheCharactersTheyAre() {
        // Hologram lines and messages are typed the same way, and there "&7" is meant literally.
        assertEquals("&7Stone Nexus", EditListener.readChatInput(Component.text("&7Stone Nexus")));
    }

    @Test
    void sectionSignsAreNormalisedToTheAmpersandNotation() {
        assertEquals("&7text", EditListener.readChatInput(Component.text("§7text")));
    }

    @Test
    void surroundingWhitespaceIsDropped() {
        assertEquals("mineblocks.blocks.ore",
                EditListener.readChatInput(Component.text("  mineblocks.blocks.ore  ")));
    }

}
