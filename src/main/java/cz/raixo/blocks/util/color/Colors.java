package cz.raixo.blocks.util.color;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Turns the legacy colour strings used throughout the configuration into Adventure components.
 *
 * <p>Supported input: {@code &7} codes, {@code #RRGGBB} hex, {@code <GRADIENT:..>} / {@code <RAINBOW..>}
 * patterns and the MineDown style {@code &#RRGGBB&} that older configs and menu code used.</p>
 */
public final class Colors {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .build();

    /** MineDown wrote hex as {@code &#205295&}; normalise it to the {@code #205295} form. */
    private static final Pattern MINEDOWN_HEX = Pattern.compile("&(#[0-9a-fA-F]{6})&");

    /**
     * Colourises a string into the legacy section format. Kept for the places that still hand raw
     * strings to the server (command output of other plugins, item NBT written by third parties).
     */
    public static String colorize(String string) {
        if (string == null) return null;
        return ColorProcessor.process(MINEDOWN_HEX.matcher(string).replaceAll("$1"));
    }

    public static List<String> colorize(List<String> list) {
        return list.stream().map(Colors::colorize).collect(Collectors.toList());
    }

    /** Colourises and parses into a component. This is the preferred entry point on modern Paper. */
    public static Component component(String string) {
        return LEGACY.deserialize(colorize(string));
    }

    /** Same as {@link #component(String)} but without the italics the vanilla item renderer adds. */
    public static Component itemComponent(String string) {
        return component(string).decoration(TextDecoration.ITALIC, false);
    }

    /** Strips every colour code, used to detect hologram lines that render as blank. */
    public static String strip(String string) {
        return ColorProcessor.strip(colorize(string));
    }

    public static void send(CommandSender sender, String... messages) {
        for (String message : messages) {
            sender.sendMessage(component(message));
        }
    }

    public static void send(CommandSender sender, List<String> messages) {
        send(sender, messages.toArray(String[]::new));
    }

    /** Splits a multi-line message (stored newline separated) and sends every line. */
    public static void sendMultiLine(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        Arrays.stream(message.split("\n")).forEach(line -> sender.sendMessage(component(line)));
    }

    private Colors() {}

}
