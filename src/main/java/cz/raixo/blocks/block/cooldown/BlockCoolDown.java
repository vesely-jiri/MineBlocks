package cz.raixo.blocks.block.cooldown;

import cz.raixo.blocks.block.MineBlock;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * The period after a block has been mined out, during which it is replaced by {@code typeOverride}
 * (bedrock by default) and cannot be mined.
 */
@Getter
@Setter
public class BlockCoolDown {

    /** How often the hologram countdown is refreshed while the cooldown runs, in ticks. */
    private static final long COUNTDOWN_INTERVAL = 10L;

    @Getter(AccessLevel.NONE)
    private final MineBlock block;
    private int time;
    private Material typeOverride;
    private String respawnMessage;
    @Setter(AccessLevel.NONE)
    private ActiveCoolDown active;

    public BlockCoolDown(MineBlock block, int time, Material typeOverride, String respawnMessage) {
        this.block = block;
        this.time = time;
        this.typeOverride = typeOverride;
        this.respawnMessage = respawnMessage;
    }

    public ActiveCoolDown activate() {
        if (time <= 0) return null;
        return activate(new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(time)));
    }

    public ActiveCoolDown activate(Date end) {
        cancel();
        long remainingMillis = end.getTime() - System.currentTimeMillis();
        if (remainingMillis <= 0) return null;
        block.getType().setOverride(typeOverride);
        this.active = new ActiveCoolDown(
                end,
                new CompletableFuture<>(),
                block.getPlugin().getServer().getScheduler().runTaskLater(
                        block.getPlugin(), this::expire, Math.max(1L, remainingMillis / 50L)),
                block.getPlugin().getServer().getScheduler().runTaskTimer(
                        block.getPlugin(), () -> block.getHologram().update(), 0L, COUNTDOWN_INTERVAL)
        );
        block.getHologram().update();
        return active;
    }

    /**
     * Ends the cooldown because the timer ran out: the block becomes mineable again and the respawn
     * message is announced.
     */
    public boolean expire() {
        if (!cancel()) return false;
        block.broadcast(respawnMessage);
        return true;
    }

    /**
     * Stops the cooldown without announcing anything. Used on reload, teleport and manual resets,
     * where telling players the block "was restored" would be wrong.
     */
    public boolean cancel() {
        if (!isActive()) return false;
        active.getTask().cancel();
        active.getUpdateTask().cancel();
        active.getFuture().complete(null);
        this.active = null;
        block.getType().setOverride(null);
        block.getHologram().update();
        return true;
    }

    public boolean isActive() {
        return active != null;
    }

    public void setTypeOverride(Material typeOverride) {
        this.typeOverride = typeOverride;
        if (isActive()) {
            block.getType().setOverride(typeOverride);
        }
    }

}
