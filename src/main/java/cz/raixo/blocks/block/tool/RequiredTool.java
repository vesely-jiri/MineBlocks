package cz.raixo.blocks.block.tool;

import cz.raixo.blocks.block.tool.enchantment.ToolEnchantment;
import cz.raixo.blocks.block.tool.material.MaterialFilter;
import cz.raixo.blocks.block.tool.name.NameFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Decides whether the item a player is holding may break the block.
 *
 * <p>Three independent filters must all allow the item: its material, its enchantments and its
 * display name. Each has its own default for items no filter mentions.</p>
 */
@Getter
@Setter
@AllArgsConstructor
public class RequiredTool implements Predicate<ItemStack> {

    /** How many materials {@link #describe()} lists before it gives up and says "and more". */
    private static final int MAX_DESCRIBED = 3;

    private final List<MaterialFilter> materialFilters;
    private Result materialDefault;
    private final Map<Enchantment, ToolEnchantment> enchantmentFilters;
    private Result enchantmentDefault;
    private final List<NameFilter> nameFilters;
    private Result nameDefault;
    /** Optional admin-written label shown to players instead of the generated one. */
    private String displayName;

    public RequiredTool(List<MaterialFilter> materialFilters, Result materialDefault,
                        Map<Enchantment, ToolEnchantment> enchantmentFilters, Result enchantmentDefault,
                        List<NameFilter> nameFilters, Result nameDefault) {
        this(materialFilters, materialDefault, enchantmentFilters, enchantmentDefault, nameFilters, nameDefault, null);
    }

    @Override
    public boolean test(ItemStack itemStack) {
        if (itemStack == null) return false;
        ItemMeta meta = itemStack.getItemMeta();
        if (!NameFilter.matches(displayNameOf(meta), nameFilters, nameDefault).getBooleanValue()) return false;
        if (!ToolEnchantment.matches(
                Optional.ofNullable(meta).map(ItemMeta::getEnchants).orElseGet(Map::of),
                enchantmentFilters,
                enchantmentDefault
        ).getBooleanValue()) return false;
        return MaterialFilter.matches(itemStack.getType(), materialFilters, materialDefault).getBooleanValue();
    }

    /**
     * A short, player facing description of what may be used, for {@code %required_tool%} and the
     * "wrong tool" message.
     */
    public String describe() {
        if (displayName != null && !displayName.isBlank()) return displayName;

        List<String> allowed = materialFilters.stream()
                .filter(filter -> filter.getResult() == Result.ALLOWED)
                .flatMap(filter -> describeFilter(filter).stream())
                .distinct()
                .toList();

        if (allowed.isEmpty()) return materialDefault == Result.ALLOWED ? "any tool" : "nothing";
        if (allowed.size() <= MAX_DESCRIBED) return String.join(", ", allowed);
        return String.join(", ", allowed.subList(0, MAX_DESCRIBED)) + " and more";
    }

    /** Expands a filter into the readable material names it accepts. */
    private List<String> describeFilter(MaterialFilter filter) {
        List<String> matched = Arrays.stream(Material.values())
                .filter(Material::isItem)
                .filter(filter)
                .map(RequiredTool::readable)
                .limit(MAX_DESCRIBED + 1L)
                .collect(Collectors.toList());
        return matched.isEmpty() ? List.of(filter.toString()) : matched;
    }

    private static String readable(Material material) {
        return Arrays.stream(material.name().split("_"))
                .map(word -> word.charAt(0) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private static String displayNameOf(ItemMeta meta) {
        if (meta == null) return "";
        return Optional.ofNullable(meta.displayName())
                .map(component -> LegacyComponentSerializer.legacySection().serialize(component))
                .orElse("");
    }

}
