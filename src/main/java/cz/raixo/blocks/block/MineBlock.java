package cz.raixo.blocks.block;

import cz.raixo.blocks.MineBlocksPlugin;
import cz.raixo.blocks.block.cooldown.BlockCoolDown;
import cz.raixo.blocks.block.health.BlockHealth;
import cz.raixo.blocks.block.hologram.BlockHologram;
import cz.raixo.blocks.block.messages.BlockMessages;
import cz.raixo.blocks.block.placeholder.BlockPlaceholderSet;
import cz.raixo.blocks.block.playerdata.PlayerData;
import cz.raixo.blocks.block.playerdata.placeholder.PlayerDataPlaceholderSet;
import cz.raixo.blocks.block.reset.ResetOptions;
import cz.raixo.blocks.block.rewards.BlockRewards;
import cz.raixo.blocks.block.tool.RequiredTool;
import cz.raixo.blocks.block.tool.Result;
import cz.raixo.blocks.block.top.BlockTop;
import cz.raixo.blocks.block.type.BlockType;
import cz.raixo.blocks.util.color.Colors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
@RequiredArgsConstructor
public class MineBlock {

    public static File getStoragePath(MineBlocksPlugin plugin, MineBlock block) {
        return new File(plugin.getStorageFolder(), block.id + ".mb");
    }

    /** The block {@code /mb create} starts from, so the command and the config defaults cannot drift. */
    public static MineBlock createDefault(MineBlocksPlugin plugin, String id, Location location) {
        MineBlock block = new MineBlock(plugin);
        block.setId(id);
        block.setDisplayName(id);
        block.setLocation(location);
        block.setType(new BlockType(block, Material.DIAMOND_BLOCK));
        block.setHealth(new BlockHealth(block, 100));
        block.setHologram(new BlockHologram(block, null, List.of(
                "#ICON: %type%",
                "#2C74B3&l%name%",
                "&7%broken%&8/&7%max_health% &8| &7%required_tool%",
                "&7%reward_info%",
                "#2C74B3&lTOP",
                "&7%player_1% &8- #2C74B3%player_1_breaks%",
                "&7%player_2% &8- #2C74B3%player_2_breaks%",
                "&7%player_3% &8- #2C74B3%player_3_breaks%",
                "&c%timeout%"
        )));
        block.setCoolDown(new BlockCoolDown(block, -1, null, ""));
        block.setResetOptions(new ResetOptions(block, false, -1, ""));
        block.setMessages(new BlockMessages("&7Block was destroyed\n&7Your breaks: %breaks%"));
        block.setRewards(new BlockRewards(block, new LinkedList<>(), new LinkedList<>()));
        block.setRequiredTool(new RequiredTool(
                new LinkedList<>(), Result.ALLOWED,
                new LinkedHashMap<>(), Result.ALLOWED,
                new LinkedList<>(), Result.ALLOWED
        ));
        return block;
    }

    private final MineBlocksPlugin plugin;
    private String id;
    /** Human readable name shown in the hologram and in messages; falls back to the id. */
    private String displayName;
    /** Free-form description of what the block hands out, rendered as {@code %reward_info%}. */
    private String rewardInfo;
    private BlockHologram hologram;
    private BlockHealth health;
    private Location location;
    private BlockType type;
    private BlockCoolDown coolDown;
    private ResetOptions resetOptions;
    private BlockMessages messages;
    private BlockRewards rewards;
    private String permission;
    private RequiredTool requiredTool;
    private BlockTop top = new BlockTop();
    private int breakLimit = 0;
    private Map<UUID, PlayerData> playerDataMap = new HashMap<>();

    public String getDisplayName() {
        return displayName == null || displayName.isBlank() ? id : displayName;
    }

    public Runnable onBreak(Player player) {
        health.decrement();
        resetOptions.resetInactive();

        List<Runnable> runnables = new LinkedList<>();

        PlayerData playerData = playerDataMap.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerData(uuid, player.getName()));
        playerData.incrementBreaks();
        top.update(playerData);
        runnables.add(rewards.giveRewards(playerData));

        if (health.getHealth() <= 0) runnables.add(onLastBreak(player));

        hologram.update();

        return () -> runnables.forEach(Runnable::run);
    }

    private Runnable onLastBreak(Player player) {
        Runnable runnable = rewards.giveLastRewards(player.getUniqueId());
        // Start the cooldown before announcing, so %timeout% in the break message already resolves,
        // and clear the progress afterwards, so %breaks% still holds each player's own count.
        coolDown.activate();
        broadcast(messages.getBreakMessage());
        resetProgress();
        hologram.update();
        return runnable;
    }

    public void show() {
        hologram.show();
        type.update();
        hologram.update();
    }

    public void hide() {
        hologram.hide();
        getLocation().getBlock().setType(Material.AIR, false);
        coolDown.cancel();
    }

    public void destroy() {
        hide();
        hologram.delete();
    }

    /** Restores the block to a fully mineable state and cancels a running cooldown. */
    public void reset() {
        resetProgress();
        coolDown.cancel();
        hologram.update();
    }

    /** Restores health and per-player progress without touching the cooldown. */
    public void resetProgress() {
        health.reset();
        playerDataMap.clear();
        resetOptions.cancelInactive();
        top.clear();
    }

    /** Sends a message to every online player, with block and per-player placeholders resolved. */
    public void broadcast(String message) {
        if (message == null || message.isEmpty()) return;
        String parsed = new BlockPlaceholderSet(this).parse(message);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            PlayerData playerData = playerDataMap.getOrDefault(
                    player.getUniqueId(),
                    new PlayerData(player.getUniqueId(), player.getName())
            );
            Colors.sendMultiLine(player, new PlayerDataPlaceholderSet(playerData).parse(parsed));
        }
    }

    /** Sends a message to a single player, with block and that player's placeholders resolved. */
    public void message(Player player, String message) {
        if (message == null || message.isEmpty()) return;
        PlayerData playerData = playerDataMap.getOrDefault(
                player.getUniqueId(),
                new PlayerData(player.getUniqueId(), player.getName())
        );
        String parsed = new BlockPlaceholderSet(this).parse(message);
        Colors.sendMultiLine(player, new PlayerDataPlaceholderSet(playerData).parse(parsed));
    }

    public void saveData(DataOutput output) throws IOException {
        output.writeInt(health.getHealth());
        boolean isCoolDownActive = coolDown.isActive();
        output.writeBoolean(isCoolDownActive);
        if (isCoolDownActive) output.writeLong(coolDown.getActive().getEnd().getTime());
        List<PlayerData> playerData = new LinkedList<>(playerDataMap.values());
        output.writeInt(playerData.size());
        for (PlayerData player : playerData) {
            player.serialize(output);
        }
    }

    public void saveData(File file) throws IOException {
        if (!file.exists() && !file.createNewFile()) return;
        try (DataOutputStream fos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            saveData(fos);
        }
    }

    public void loadData(DataInput input) throws IOException {
        if (resetOptions.isOnRestart()) return;
        health.setHealth(input.readInt());
        if (input.readBoolean()) {
            coolDown.activate(new Date(input.readLong()));
        } else coolDown.cancel();
        playerDataMap.clear();
        top.clear();
        int players = input.readInt();
        for (int i = 0; i < players; i++) {
            PlayerData playerData = PlayerData.deserialize(input);
            playerDataMap.put(playerData.getUuid(), playerData);
            top.update(playerData);
        }
        if (health.getHealth() != health.getMaxHealth()) resetOptions.resetInactive();
    }

    public void loadData(File file) throws IOException {
        if (!file.exists()) return;
        try (DataInputStream fis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            loadData(fis);
        }
    }

    public void setCoolDown(BlockCoolDown coolDown) {
        if (this.coolDown != null) this.coolDown.cancel();
        this.coolDown = coolDown;
    }

    public void teleport(Location location) {
        hide();
        this.location = location;
        hologram.updateLocation();
        show();
    }

    public boolean hasPermission() {
        return permission != null && !permission.isBlank();
    }

    /** Number of times the block has been mined in the current cycle. */
    public int getBrokenCount() {
        return Optional.ofNullable(health)
                .map(h -> h.getMaxHealth() - h.getHealth())
                .orElse(0);
    }

}
