package cz.raixo.blocks.block.top;

import cz.raixo.blocks.block.playerdata.PlayerData;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockTopTest {

    @Test
    void keepsTheTenBestPlayersInOrder() {
        BlockTop top = new BlockTop();
        List<PlayerData> players = new LinkedList<>();
        // Seeded so a failure is reproducible instead of showing up on one run in ten.
        Random random = new Random(20260831L);

        for (int i = 0; i < 100; i++) {
            PlayerData playerData = new PlayerData(UUID.randomUUID(), "player" + (i + 1), random.nextInt(1000));
            players.add(playerData);
            top.update(playerData);
        }

        assertEquals(
                players.stream()
                        .sorted(Comparator.comparingInt(PlayerData::getBreaks).reversed())
                        .limit(10)
                        .collect(Collectors.toList()),
                top.getPlayers()
        );
    }

}