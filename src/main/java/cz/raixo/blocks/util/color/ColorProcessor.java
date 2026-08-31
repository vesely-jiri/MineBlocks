package cz.raixo.blocks.util.color;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts the colour syntax used in the configuration into legacy section-sign text.
 *
 * <p>Replaces the old bundled IridiumColorAPI: that copy decided whether hex colours were supported
 * by parsing {@code "1.x"} out of {@link org.bukkit.Bukkit#getVersion()}, which no longer matches
 * the {@code 26.2} version scheme and silently downgraded every hex colour to the nearest legacy
 * one. Modern servers always support hex, so there is nothing to detect. Having no server calls at
 * all also makes the whole colour pipeline unit testable.</p>
 *
 * <p>Supported syntax, unchanged from the original plugin:</p>
 * <ul>
 *     <li>{@code &a} / {@code &l} legacy codes</li>
 *     <li>{@code #RRGGBB}, {@code &#RRGGBB}, {@code <#RRGGBB>} or <code>{#RRGGBB}</code> solid colours</li>
 *     <li>{@code <#RRGGBB>text</#RRGGBB>} gradients</li>
 *     <li>{@code <RAINBOW80>text</RAINBOW>} rainbows</li>
 * </ul>
 */
public final class ColorProcessor {

    public static final char SECTION = '§';

    private static final Pattern GRADIENT = Pattern.compile("[<{]#([A-Fa-f0-9]{6})[>}](.*?)[<{]/#([A-Fa-f0-9]{6})[>}]");
    private static final Pattern RAINBOW = Pattern.compile("<RAINBOW([0-9]{1,3})>(.*?)</RAINBOW>");
    private static final Pattern SOLID = Pattern.compile("[<{]#([A-Fa-f0-9]{6})[}>]|&?#([A-Fa-f0-9]{6})");
    private static final Pattern LEGACY_CODE = Pattern.compile("&([0-9a-fk-orA-FK-OR])");
    private static final Pattern STRIP = Pattern.compile(
            "[<{]#[A-Fa-f0-9]{6}[}>]|&?#[A-Fa-f0-9]{6}|[&" + SECTION + "]x(?:[&" + SECTION + "][0-9a-fA-F]){6}"
                    + "|[&" + SECTION + "][0-9a-fk-orA-FK-OR]|</?RAINBOW[0-9]{0,3}>|[<{]/#[A-Fa-f0-9]{6}[}>]"
    );

    /** Formatting codes that must be re-applied to every character of a gradient. */
    private static final String[] DECORATIONS = {"&l", "&n", "&o", "&k", "&m"};

    public static String process(String string) {
        if (string == null || string.isEmpty()) return string;
        String result = gradients(string);
        result = rainbows(result);
        result = solids(result);
        return legacyCodes(result);
    }

    /** Removes every colour and formatting code, so blank-looking lines can be detected. */
    public static String strip(String string) {
        if (string == null) return null;
        return STRIP.matcher(string).replaceAll("");
    }

    /** Renders a single hex colour as the {@code §x§R§R§G§G§B§B} sequence Adventure understands. */
    public static String hex(int rgb) {
        String hex = String.format("%06x", rgb & 0xFFFFFF);
        StringBuilder builder = new StringBuilder(14).append(SECTION).append('x');
        for (int i = 0; i < 6; i++) {
            builder.append(SECTION).append(hex.charAt(i));
        }
        return builder.toString();
    }

    private static String gradients(String string) {
        Matcher matcher = GRADIENT.matcher(string);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            Color start = new Color(Integer.parseInt(matcher.group(1), 16));
            Color end = new Color(Integer.parseInt(matcher.group(3), 16));
            matcher.appendReplacement(out, Matcher.quoteReplacement(gradient(matcher.group(2), start, end)));
        }
        return matcher.appendTail(out).toString();
    }

    private static String rainbows(String string) {
        Matcher matcher = RAINBOW.matcher(string);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            float saturation = Math.min(100f, Float.parseFloat(matcher.group(1))) / 100f;
            matcher.appendReplacement(out, Matcher.quoteReplacement(rainbow(matcher.group(2), saturation)));
        }
        return matcher.appendTail(out).toString();
    }

    private static String solids(String string) {
        Matcher matcher = SOLID.matcher(string);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String color = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            matcher.appendReplacement(out, Matcher.quoteReplacement(hex(Integer.parseInt(color, 16))));
        }
        return matcher.appendTail(out).toString();
    }

    private static String legacyCodes(String string) {
        return LEGACY_CODE.matcher(string).replaceAll(match -> SECTION + match.group(1).toLowerCase());
    }

    private static String gradient(String content, Color start, Color end) {
        Extracted extracted = Extracted.of(content);
        String text = extracted.text();
        StringBuilder builder = new StringBuilder();
        int length = text.length();
        for (int i = 0; i < length; i++) {
            double ratio = length == 1 ? 0 : (double) i / (length - 1);
            builder.append(hex(blend(start, end, ratio)))
                    .append(extracted.decorations())
                    .append(text.charAt(i));
        }
        return builder.toString();
    }

    private static String rainbow(String content, float saturation) {
        Extracted extracted = Extracted.of(content);
        String text = extracted.text();
        StringBuilder builder = new StringBuilder();
        int length = text.length();
        for (int i = 0; i < length; i++) {
            Color color = Color.getHSBColor((float) i / Math.max(1, length), saturation, saturation);
            builder.append(hex(color.getRGB()))
                    .append(extracted.decorations())
                    .append(text.charAt(i));
        }
        return builder.toString();
    }

    private static int blend(Color start, Color end, double ratio) {
        int red = (int) Math.round(start.getRed() + ratio * (end.getRed() - start.getRed()));
        int green = (int) Math.round(start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
        int blue = (int) Math.round(start.getBlue() + ratio * (end.getBlue() - start.getBlue()));
        return (red << 16) | (green << 8) | blue;
    }

    /**
     * A gradient colours every character individually, which resets bold/italic in the process.
     * Pull those codes out of the content so they can be re-applied per character.
     */
    private record Extracted(String text, String decorations) {

        static Extracted of(String content) {
            StringBuilder decorations = new StringBuilder();
            String text = content;
            for (String decoration : DECORATIONS) {
                if (text.contains(decoration)) {
                    decorations.append(SECTION).append(decoration.charAt(1));
                    text = text.replace(decoration, "");
                }
            }
            return new Extracted(text, decorations.toString());
        }

    }

    private ColorProcessor() {}

}
