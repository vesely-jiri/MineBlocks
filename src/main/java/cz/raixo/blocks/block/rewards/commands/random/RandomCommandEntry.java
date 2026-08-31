package cz.raixo.blocks.block.rewards.commands.random;

import cz.raixo.blocks.block.rewards.commands.RawEntry;
import cz.raixo.blocks.block.rewards.commands.RewardEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
@Getter
@Setter
public class RandomCommandEntry implements RewardEntry {

    private final String command;
    /** Relative weight against the other entries of the same reward, not a percentage. */
    private int chance;
    @Nullable
    private final String message;
    private final boolean broadcast;

    public RandomCommandEntry(String command, int chance) {
        this(command, chance, null, false);
    }

    public static RandomCommandEntry of(RawEntry raw) {
        return new RandomCommandEntry(raw.command(), raw.chance(), raw.message(), raw.broadcast());
    }

    @Override
    public Object serialize() {
        return new RawEntry(command, chance, message, broadcast).serialize(true);
    }

}
