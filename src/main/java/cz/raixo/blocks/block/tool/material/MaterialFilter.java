package cz.raixo.blocks.block.tool.material;

import cz.raixo.blocks.util.ConfigUtil;
import cz.raixo.blocks.block.tool.Result;
import org.bukkit.Material;

import java.util.List;
import java.util.function.Predicate;

public interface MaterialFilter extends Predicate<Material> {

    static MaterialFilter parse(String value, Result result) {
        return ConfigUtil.getMaterialOpt(value)
                .<MaterialFilter>map(material -> new SingleMaterialFilter(material, result))
                .orElseGet(() -> new PatternMaterialFilter(value, result));
    }

    /** Later filters win, so a broad deny can be narrowed by a specific allow below it. */
    static Result matches(Material type, List<MaterialFilter> filters, Result defaultResult) {
        Result result = defaultResult;
        for (MaterialFilter filter : filters) {
            if (filter.test(type)) {
                result = filter.getResult();
            }
        }
        return result;
    }

    Result getResult();

    void setResult(Result result);

}
