package cz.raixo.blocks.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleRandomTest {

    @Test
    void picksProportionallyToWeight() {
        SimpleRandom<String> random = new SimpleRandom<>();
        random.add(90, "common");
        random.add(10, "rare");

        Map<String, Integer> counts = roll(random, 20_000);

        assertTrue(counts.get("common") > counts.get("rare") * 5,
                "expected the 90 weight entry to dominate, got " + counts);
        assertTrue(counts.getOrDefault("rare", 0) > 0, "the 10 weight entry should still show up");
    }

    @Test
    void clearResetsTheWeightTable() {
        SimpleRandom<String> random = new SimpleRandom<>();
        random.add(10, "old");
        random.clear();
        random.add(10, "new");

        // Before the fix the running total survived clear(), so half of the rolls landed in the
        // range of the removed entry and returned null.
        Map<String, Integer> counts = roll(random, 1_000);
        assertEquals(1, counts.size(), "only the re-added entry should be reachable, got " + counts);
        assertEquals(1_000, counts.get("new"));
    }

    @Test
    void zeroWeightEntriesAreNeverPicked() {
        SimpleRandom<String> random = new SimpleRandom<>();
        random.add(0, "never");
        random.add(5, "always");

        Map<String, Integer> counts = roll(random, 500);
        assertEquals(500, counts.get("always"));
    }

    private Map<String, Integer> roll(SimpleRandom<String> random, int times) {
        Random source = new Random(1234);
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < times; i++) {
            String value = random.next(source);
            assertNotNull(value, "weighted pick returned null");
            counts.merge(value, 1, Integer::sum);
        }
        return counts;
    }

}
