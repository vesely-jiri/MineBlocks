package cz.raixo.blocks.block.rewards.commands;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RawEntryTest {

    @Test
    void parsesTheLegacyWeightCommandForm() {
        RawEntry entry = RawEntry.parse("30;give %player% diamond");

        assertEquals(30, entry.chance());
        assertEquals("give %player% diamond", entry.command());
        assertNull(entry.message());
        assertFalse(entry.broadcast());
    }

    @Test
    void keepsSemicolonsInsideTheCommand() {
        RawEntry entry = RawEntry.parse("10;tellraw %player% {\"text\":\"a;b\"}");
        assertEquals("tellraw %player% {\"text\":\"a;b\"}", entry.command());
    }

    @Test
    void parsesTheMapFormWithAMessage() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("command", "lp user %player% permission set gold.nexus true");
        map.put("chance", 5);
        map.put("message", "&aUnlocked!");
        map.put("broadcast", true);

        RawEntry entry = RawEntry.parse(map);

        assertEquals("lp user %player% permission set gold.nexus true", entry.command());
        assertEquals(5, entry.chance());
        assertEquals("&aUnlocked!", entry.message());
        assertTrue(entry.broadcast());
    }

    @Test
    void defaultsToFullWeightWhenNoneIsGiven() {
        assertEquals(100, RawEntry.parse("say hello").chance());
        assertEquals("say hello", RawEntry.parse("say hello").command());
    }

    @Test
    void rejectsAnEntryWithoutACommand() {
        assertThrows(IllegalArgumentException.class, () -> RawEntry.parse(Map.of("chance", 5)));
    }

    @Test
    void roundTripsBackToTheCompactFormWhenNothingExtraIsSet() {
        RawEntry entry = RawEntry.parse("30;give %player% diamond");
        assertEquals("30;give %player% diamond", entry.serialize(true));
    }

    @Test
    void roundTripsToAMapWhenAMessageIsSet() {
        RawEntry entry = new RawEntry("say hi", 5, "&amessage", false);
        Object serialized = entry.serialize(true);

        assertInstanceOfMap(serialized);
        Map<?, ?> map = (Map<?, ?>) serialized;
        assertEquals("say hi", map.get("command"));
        assertEquals(5, map.get("chance"));
        assertEquals("&amessage", map.get("message"));
        assertFalse(map.containsKey("broadcast"));
    }

    @Test
    void parsesAMixedList() {
        List<RawEntry> entries = RawEntry.parseAll(List.of(
                "60;give %player% coal",
                Map.of("command", "say hi", "message", "&ahi")
        ));

        assertEquals(2, entries.size());
        assertEquals(60, entries.get(0).chance());
        assertEquals("&ahi", entries.get(1).message());
    }

    private void assertInstanceOfMap(Object value) {
        assertTrue(value instanceof Map, "expected a map, got " + value.getClass());
    }

}
