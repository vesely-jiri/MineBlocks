package cz.raixo.blocks.integration;

import cz.raixo.blocks.MineBlocksPlugin;
import cz.raixo.blocks.integration.luckperms.LPIntegration;
import cz.raixo.blocks.integration.models.placeholder.PlaceholderProvider;
import cz.raixo.blocks.integration.models.prefix.PrefixProvider;
import cz.raixo.blocks.integration.papi.PAPIIntegration;
import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.PluginManager;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Wires up the optional plugins MineBlocks can cooperate with.
 *
 * <p>Every integration here is genuinely optional: holograms are rendered with vanilla display
 * entities and AFK is read from the server itself, so the plugin starts and works on a bare Paper
 * install.</p>
 */
@Getter
public class IntegrationManager implements PlaceholderProvider {

    private static final Map<String, Function<MineBlocksPlugin, Integration>> INTEGRATION_REGISTRY = new LinkedHashMap<>();

    static {
        INTEGRATION_REGISTRY.put(LPIntegration.PLUGIN_NAME, LPIntegration::new);
        INTEGRATION_REGISTRY.put(PAPIIntegration.PLUGIN_NAME, PAPIIntegration::new);
    }

    private final List<Integration> integrations = new LinkedList<>();
    private final PrefixProvider prefixProvider;
    private final List<PlaceholderProvider> placeholderProviders;

    public IntegrationManager(MineBlocksPlugin plugin) {
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        for (Map.Entry<String, Function<MineBlocksPlugin, Integration>> entry : INTEGRATION_REGISTRY.entrySet()) {
            if (!pluginManager.isPluginEnabled(entry.getKey())) continue;
            try {
                integrations.add(entry.getValue().apply(plugin));
                plugin.logInfo("Integration with plugin {0} successfully enabled!", entry.getKey());
            } catch (Exception e) {
                // A broken optional integration must never stop the plugin from loading.
                plugin.logWarn("Integration with plugin " + entry.getKey() + " failed to load: " + e.getMessage());
            }
        }
        prefixProvider = integrations.stream()
                .filter(PrefixProvider.class::isInstance)
                .max(Comparator.comparingInt(Integration::getPriority))
                .map(PrefixProvider.class::cast)
                .orElse(null);
        placeholderProviders = integrations.stream()
                .filter(PlaceholderProvider.class::isInstance)
                .map(PlaceholderProvider.class::cast)
                .toList();
    }

    public void disable() {
        integrations.forEach(Integration::disable);
        integrations.clear();
    }

    @Override
    public String setPlaceholders(OfflinePlayer player, String text) {
        String result = text;
        for (PlaceholderProvider placeholderProvider : placeholderProviders) {
            result = placeholderProvider.setPlaceholders(player, result);
        }
        return result;
    }

}
