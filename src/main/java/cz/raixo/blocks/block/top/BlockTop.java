package cz.raixo.blocks.block.top;

import cz.raixo.blocks.block.playerdata.PlayerData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The best players of the current block cycle, highest break count first.
 *
 * <p>The previous hand-rolled insertion walked the list looking for the first smaller entry and
 * inserted before the last element when it found none, so a player who belonged at the bottom
 * landed one slot too high and the order drifted as soon as the list was full. The list holds at
 * most {@link #MAX_TOP_SIZE} entries, so keeping it sorted outright is both correct and cheap.</p>
 */
public class BlockTop {

    public static final int MAX_TOP_SIZE = 10;

    private static final Comparator<PlayerData> BY_BREAKS_DESC =
            Comparator.comparingInt(PlayerData::getBreaks).reversed();

    private final List<PlayerData> players = new ArrayList<>();

    public void update(PlayerData player) {
        players.removeIf(p -> p.getUuid().equals(player.getUuid()));
        players.add(player);
        players.sort(BY_BREAKS_DESC);
        while (players.size() > MAX_TOP_SIZE) {
            players.remove(players.size() - 1);
        }
    }

    public Optional<PlayerData> getPlayer(int pos) {
        if (pos < 0 || pos >= players.size()) return Optional.empty();
        return Optional.ofNullable(players.get(pos));
    }

    public void clear() {
        players.clear();
    }

    public List<PlayerData> getPlayers() {
        return Collections.unmodifiableList(players);
    }

}
