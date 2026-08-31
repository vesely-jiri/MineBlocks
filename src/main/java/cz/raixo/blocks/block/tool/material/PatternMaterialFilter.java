package cz.raixo.blocks.block.tool.material;

import cz.raixo.blocks.block.tool.Result;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Matches a material by name pattern, e.g. {@code .*_PICKAXE} or {@code STONE_.*}.
 *
 * <p>The original plugin only did a substring check here, which meant the documented
 * {@code STONE_.*: ALLOWED} syntax never matched anything and silently denied every tool. Patterns
 * are compiled as regular expressions and fall back to a substring check when the string is not a
 * valid one, so both spellings behave the way an admin would expect.</p>
 */
@Getter
@Setter
public class PatternMaterialFilter implements MaterialFilter {

    private final String value;
    private final Pattern pattern;
    private Result result;

    public PatternMaterialFilter(String value, Result result) {
        this.value = value;
        this.result = result;
        this.pattern = compile(value);
    }

    private static Pattern compile(String value) {
        try {
            return Pattern.compile(value, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            return null;
        }
    }

    @Override
    public boolean test(Material material) {
        String name = material.name();
        if (pattern == null) return name.contains(value.toUpperCase());
        return pattern.matcher(name).matches();
    }

    @Override
    public String toString() {
        return value;
    }

}
