package cz.raixo.blocks.util.color;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorProcessorTest {

    @Test
    void translatesLegacyCodes() {
        assertEquals("§7Hello §lworld", ColorProcessor.process("&7Hello &lworld"));
    }

    @Test
    void translatesHexColours() {
        assertEquals("§x§2§c§7§4§b§3Blue", ColorProcessor.process("#2C74B3Blue"));
        assertEquals("§x§2§c§7§4§b§3Blue", ColorProcessor.process("<#2C74B3>Blue"));
    }

    @Test
    void hexColoursAreNotDowngradedOnModernVersions() {
        // The old implementation parsed "1.x" out of the server version string and fell back to
        // legacy colours when it failed, which is exactly what happens on 26.x.
        assertTrue(ColorProcessor.process("#FF0000red").startsWith("§x"));
    }

    @Test
    void gradientColoursEveryCharacterAndKeepsDecorations() {
        String processed = ColorProcessor.process("{#FF0000}&lab{/#0000FF}");
        assertEquals("§x§f§f§0§0§0§0§la§x§0§0§0§0§f§f§lb", processed);
    }

    @Test
    void rainbowColoursEveryCharacter() {
        String processed = ColorProcessor.process("<RAINBOW80>abc</RAINBOW>");
        assertEquals(3, processed.split("§x", -1).length - 1);
    }

    @Test
    void stripRemovesEverySupportedCode() {
        assertTrue(ColorProcessor.strip("&7Hello #2C74B3world").equals("Hello world"));
        assertTrue(ColorProcessor.strip(ColorProcessor.process("&7Hello #2C74B3world")).equals("Hello world"));
        assertTrue(ColorProcessor.strip("<RAINBOW80>abc</RAINBOW>").equals("abc"));
    }

    @Test
    void strippedEmptyLineIsDetectedAsBlank() {
        // Hologram lines that only contain colour codes must be recognised as blank and dropped.
        assertTrue(ColorProcessor.strip("&c").isBlank());
        assertFalse(ColorProcessor.strip("&c5m").isBlank());
    }

}
