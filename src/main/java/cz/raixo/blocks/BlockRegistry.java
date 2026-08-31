package cz.raixo.blocks;

import cz.raixo.blocks.block.MineBlock;
import cz.raixo.blocks.config.blocks.BlocksConfig;
import lombok.SneakyThrows;
import org.bukkit.Location;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class BlockRegistry {

    private final Map<String, MineBlock> blockMap = new ConcurrentHashMap<>();
    private final Map<Location, MineBlock> blockByLocation = new ConcurrentHashMap<>();
    /** Permission nodes this registry published, so it only ever removes its own. */
    private final Set<String> ownedPermissions = ConcurrentHashMap.newKeySet();

    public void changeId(MineBlock mineBlock, String id) {
        BlocksConfig config = mineBlock.getPlugin().getConfiguration().getBlocksConfig();
        config.removeBlock(mineBlock.getId());
        mineBlock.hide();
        blockMap.remove(mineBlock.getId(), mineBlock);
        mineBlock.setId(id);
        blockMap.put(mineBlock.getId(), mineBlock);
        mineBlock.show();
        mineBlock.getPlugin().saveConfiguration();
    }

    public void changeLocation(MineBlock mineBlock, Location location) {
        mineBlock.hide();
        blockByLocation.remove(mineBlock.getLocation(), mineBlock);
        mineBlock.teleport(location);
        blockByLocation.put(mineBlock.getLocation(), mineBlock);
        mineBlock.show();
    }

    public void register(MineBlock mineBlock) {
        blockMap.put(mineBlock.getId(), mineBlock);
        blockByLocation.put(mineBlock.getLocation(), mineBlock);
        registerPermission(mineBlock);
        mineBlock.show();
    }

    public void unregister(MineBlock mineBlock) {
        blockMap.remove(mineBlock.getId(), mineBlock);
        blockByLocation.remove(mineBlock.getLocation(), mineBlock);
        unregisterPermission(mineBlock);
        mineBlock.destroy();
    }

    /**
     * Publishes the node a block gates on to the server.
     *
     * <p>The nodes cannot be declared in plugin.yml because they come from the config, and an
     * unregistered node is invisible to permission plugins: it will not tab-complete in LuckPerms
     * and does not show up in a permission listing, which makes a typo hard to spot.</p>
     */
    private void registerPermission(MineBlock mineBlock) {
        if (!mineBlock.hasPermission()) return;
        PluginManager pluginManager = mineBlock.getPlugin().getServer().getPluginManager();
        if (pluginManager.getPermission(mineBlock.getPermission()) != null) return;
        pluginManager.addPermission(new Permission(
                mineBlock.getPermission(),
                "Allows mining the MineBlocks block '" + mineBlock.getId() + "'",
                PermissionDefault.FALSE
        ));
        ownedPermissions.add(mineBlock.getPermission());
    }

    private void unregisterPermission(MineBlock mineBlock) {
        String permission = mineBlock.getPermission();
        if (permission == null || !ownedPermissions.contains(permission)) return;
        // Several blocks may share a node; only drop it once the last one is gone.
        if (blockMap.values().stream().anyMatch(block -> permission.equals(block.getPermission()))) return;
        mineBlock.getPlugin().getServer().getPluginManager().removePermission(permission);
        ownedPermissions.remove(permission);
    }

    public List<MineBlock> unregisterAll(Consumer<MineBlock> beforeUnload) {
        List<MineBlock> blocks = new LinkedList<>(blockMap.values());
        for (MineBlock block : blocks) {
            beforeUnload.accept(block);
            unregister(block);
        }
        return blocks;
    }

    @SneakyThrows
    public void delete(MineBlock block) {
        MineBlocksPlugin plugin = block.getPlugin();
        plugin.getConfiguration().getBlocksConfig().removeBlock(block.getId());
        plugin.saveConfiguration();
        Files.deleteIfExists(MineBlock.getStoragePath(plugin, block).toPath());
        unregister(block);
    }

    public MineBlock get(String id) {
        return blockMap.get(id);
    }

    public MineBlock get(Location location) {
        return blockByLocation.get(location);
    }

    public Collection<MineBlock> getBlocks() {
        return blockMap.values();
    }

}
