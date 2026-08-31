package cz.raixo.blocks.block.rewards.commands;

import cz.raixo.blocks.util.NumberUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parsed form of one configured reward entry, before it becomes a {@link RewardEntry}.
 *
 * <p>Two shapes are accepted, so configs written for the original plugin keep working:</p>
 * <pre>
 * commands:
 *   - "100;give %player% iron_nugget"          # legacy: weight;command
 *   - command: "lp user %player% permission set mineblocks.blocks.gold true"
 *     chance: 5
 *     message: "&amp;aYou unlocked the gold block!"
 *     broadcast: true
 * </pre>
 */
public record RawEntry(String command, int chance, String message, boolean broadcast) {

    private static final int DEFAULT_CHANCE = 100;

    public static List<RawEntry> parseAll(List<?> raw) {
        return raw.stream().map(RawEntry::parse).toList();
    }

    public static RawEntry parse(Object raw) {
        if (raw instanceof Map<?, ?> map) return fromMap(map);
        String value = String.valueOf(raw);
        String[] parts = value.split(";", 2);
        if (parts.length < 2) {
            // No weight given: treat the whole line as the command with the default weight.
            return new RawEntry(value, DEFAULT_CHANCE, null, false);
        }
        int chance = NumberUtil.parseInt(parts[0].trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid chance on command " + value));
        return new RawEntry(parts[1], chance, null, false);
    }

    private static RawEntry fromMap(Map<?, ?> map) {
        Object command = map.get("command");
        if (command == null) throw new IllegalArgumentException("Reward entry " + map + " is missing a command");
        Object chance = map.get("chance");
        Object message = map.get("message");
        return new RawEntry(
                String.valueOf(command),
                chance == null ? DEFAULT_CHANCE : NumberUtil.parseInt(String.valueOf(chance))
                        .orElseThrow(() -> new IllegalArgumentException("Invalid chance on command " + command)),
                message == null ? null : String.valueOf(message),
                Boolean.parseBoolean(String.valueOf(map.get("broadcast")))
        );
    }

    /** Serialises back, preferring the compact legacy string when nothing extra is configured. */
    public Object serialize(boolean withChance) {
        if (message == null && !broadcast) {
            return withChance ? chance + ";" + command : command;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("command", command);
        if (withChance) map.put("chance", chance);
        if (message != null) map.put("message", message);
        if (broadcast) map.put("broadcast", true);
        return map;
    }

}
