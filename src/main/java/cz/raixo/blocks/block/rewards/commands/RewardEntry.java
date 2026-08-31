package cz.raixo.blocks.block.rewards.commands;

import org.jetbrains.annotations.Nullable;

/**
 * A single thing a reward can hand out: one console command, optionally announced to the player.
 *
 * <p>Silence is the default so bulk rewards stay quiet (their own {@code -s} style command flags
 * handle feedback); rewards worth shouting about — unlocking the permission for the next block —
 * set a message.</p>
 */
public interface RewardEntry {

    String getCommand();

    /** Message sent when this entry is granted, or {@code null} to stay silent. */
    @Nullable
    String getMessage();

    /** Whether {@link #getMessage()} goes to the whole server instead of just the winner. */
    boolean isBroadcast();

    /** Config representation: a plain {@code chance;command} string, or a map when it has a message. */
    Object serialize();

}
