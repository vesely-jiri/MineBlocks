package cz.raixo.blocks;

import cz.raixo.blocks.block.MineBlock;
import cz.raixo.blocks.block.rewards.offline.OfflineRewardsStorage;
import cz.raixo.blocks.commands.MineBlocksCommand;
import cz.raixo.blocks.config.MineBlocksConfig;
import cz.raixo.blocks.gui.Gui;
import cz.raixo.blocks.integration.IntegrationManager;
import cz.raixo.blocks.listener.BlocksListener;
import cz.raixo.blocks.menu.BlockMenu;
import cz.raixo.blocks.menu.listener.EditListener;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Getter
public class MineBlocksPlugin extends JavaPlugin {

    private FileConfiguration config;
    private MineBlocksConfig configuration;
    private IntegrationManager integrationManager;
    private final BlockRegistry blockRegistry = new BlockRegistry();
    private EditListener editValuesListener;
    private File storageFolder;
    private OfflineRewardsStorage offlineRewards;

    @Override
    public void onEnable() {
        storageFolder = new File(getDataFolder(), "storage");
        createFolders();
        offlineRewards = new OfflineRewardsStorage(storageFolder);
        Gui.enable(this);
        saveDefaultConfig();

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            commands.register(
                    MineBlocksCommand.build(this),
                    "Manage MineBlocks blocks",
                    List.of("mb")
            );
        });

        // Blocks need their worlds, and the hologram entities need loaded chunks, so the actual
        // load runs a tick after every plugin has enabled.
        getServer().getScheduler().runTaskLater(this, () -> {
            editValuesListener = new EditListener(this);
            getServer().getPluginManager().registerEvents(editValuesListener, this);
            getServer().getPluginManager().registerEvents(new BlocksListener(this), this);
            load();
        }, 1L);
    }

    private void load() {
        configuration = new MineBlocksConfig(getConfig());
        integrationManager = new IntegrationManager(this);
        for (MineBlock block : configuration.getBlocksConfig().getBlocks(this)) {
            blockRegistry.register(block);
        }
        logInfo("Loaded blocks from the config: {0}",
                blockRegistry.getBlocks().stream()
                        .map(MineBlock::getId)
                        .collect(Collectors.joining(", "))
        );
        logInfo("MineBlocks enabled successfully!");
    }

    @Override
    public void onDisable() {
        unload();
        Gui.disable();
    }

    private void unload() {
        if (integrationManager != null) integrationManager.disable();
        if (!storageFolder.exists()) createFolders();
        closeAllGuis();
        blockRegistry.unregisterAll(block -> {
            try {
                block.saveData(MineBlock.getStoragePath(this, block));
            } catch (IOException e) {
                logWarn("Could not save data of block " + block.getId() + ": " + e.getMessage());
            }
        });
        logInfo("MineBlocks disabled successfully!");
    }

    public void reload() {
        closeAllGuis();
        unload();
        reloadConfig();
        load();
    }

    public void logInfo(String msg, Object... args) {
        getLogger().log(Level.INFO, msg, args);
    }

    public void logWarn(String msg, Object... args) {
        getLogger().log(Level.WARNING, msg, args);
    }

    @Override
    public File getFile() {
        return super.getFile();
    }

    public void saveConfiguration() {
        getServer().getScheduler().runTaskAsynchronously(this, this::saveConfig);
    }

    public void closeAllGuis() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getOpenInventory().getTopInventory().getHolder() instanceof BlockMenu<?>) {
                onlinePlayer.closeInventory();
            }
        }
    }

    @NotNull
    @Override
    public FileConfiguration getConfig() {
        if (config == null) reloadConfig();
        return config;
    }

    @Override
    public void reloadConfig() {
        File file = getConfigFile();
        if (!file.exists()) {
            createFolders();
            saveDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public void saveConfig() {
        if (config == null) return;
        try {
            config.save(getConfigFile());
        } catch (IOException e) {
            logWarn("Could not save config.yml: " + e.getMessage());
        }
    }

    private File getConfigFile() {
        return new File(getDataFolder(), "config.yml");
    }

    private void createFolders() {
        getDataFolder().mkdirs();
        storageFolder.mkdirs();
    }

}
