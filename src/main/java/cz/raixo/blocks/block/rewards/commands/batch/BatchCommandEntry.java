package cz.raixo.blocks.block.rewards.commands.batch;

import cz.raixo.blocks.block.rewards.commands.RawEntry;
import cz.raixo.blocks.block.rewards.commands.RewardEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
@Getter
public class BatchCommandEntry implements RewardEntry {

    private final String command;
    @Nullable
    private final String message;
    private final boolean broadcast;

    public BatchCommandEntry(String command) {
        this(command, null, false);
    }

    public static BatchCommandEntry of(RawEntry raw) {
        return new BatchCommandEntry(raw.command(), raw.message(), raw.broadcast());
    }

    @Override
    public Object serialize() {
        return new RawEntry(command, 0, message, broadcast).serialize(false);
    }

}
