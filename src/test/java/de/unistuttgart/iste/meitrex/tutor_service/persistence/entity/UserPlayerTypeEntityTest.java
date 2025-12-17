package de.unistuttgart.iste.meitrex.tutor_service.persistence.entity;

import de.unistuttgart.iste.meitrex.common.event.HexadPlayerType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserPlayerTypeEntityTest {

    @Test
    void testGetPlayerTypePercentagesAsMap_ReturnsAllTypes() {
        UUID userId = UUID.randomUUID();
        UserPlayerTypeEntity entity = UserPlayerTypeEntity.builder()
                .userId(userId)
                .primaryPlayerType(HexadPlayerType.ACHIEVER)
                .achieverScore(0.45)
                .playerScore(0.20)
                .socialiserScore(0.15)
                .freeSpiritScore(0.10)
                .philanthropistScore(0.05)
                .disruptorScore(0.05)
                .build();

        Map<HexadPlayerType, Double> percentages = entity.getPlayerTypePercentagesAsMap();

        assertNotNull(percentages);
        assertEquals(6, percentages.size());
        assertEquals(0.45, percentages.get(HexadPlayerType.ACHIEVER));
        assertEquals(0.20, percentages.get(HexadPlayerType.PLAYER));
        assertEquals(0.15, percentages.get(HexadPlayerType.SOCIALISER));
        assertEquals(0.10, percentages.get(HexadPlayerType.FREE_SPIRIT));
        assertEquals(0.05, percentages.get(HexadPlayerType.PHILANTHROPIST));
        assertEquals(0.05, percentages.get(HexadPlayerType.DISRUPTOR));
    }

    @Test
    void testSetPlayerTypePercentagesFromMap_SetsAllFields() {
        UUID userId = UUID.randomUUID();
        UserPlayerTypeEntity entity = UserPlayerTypeEntity.builder()
                .userId(userId)
                .primaryPlayerType(HexadPlayerType.ACHIEVER)
                .build();

        Map<HexadPlayerType, Double> percentages = Map.of(
                HexadPlayerType.ACHIEVER, 0.30,
                HexadPlayerType.PLAYER, 0.25,
                HexadPlayerType.SOCIALISER, 0.20,
                HexadPlayerType.FREE_SPIRIT, 0.10,
                HexadPlayerType.PHILANTHROPIST, 0.10,
                HexadPlayerType.DISRUPTOR, 0.05
        );

        entity.setPlayerTypePercentagesFromMap(percentages);

        assertEquals(0.30, entity.getAchieverScore());
        assertEquals(0.25, entity.getPlayerScore());
        assertEquals(0.20, entity.getSocialiserScore());
        assertEquals(0.10, entity.getFreeSpiritScore());
        assertEquals(0.10, entity.getPhilanthropistScore());
        assertEquals(0.05, entity.getDisruptorScore());
    }

    @Test
    void testRoundTrip_MapToEntityToMap() {
        Map<HexadPlayerType, Double> originalPercentages = Map.of(
                HexadPlayerType.ACHIEVER, 0.40,
                HexadPlayerType.PLAYER, 0.30,
                HexadPlayerType.SOCIALISER, 0.10,
                HexadPlayerType.FREE_SPIRIT, 0.10,
                HexadPlayerType.PHILANTHROPIST, 0.05,
                HexadPlayerType.DISRUPTOR, 0.05
        );

        UserPlayerTypeEntity entity = UserPlayerTypeEntity.builder()
                .userId(UUID.randomUUID())
                .primaryPlayerType(HexadPlayerType.ACHIEVER)
                .build();

        entity.setPlayerTypePercentagesFromMap(originalPercentages);
        Map<HexadPlayerType, Double> retrievedPercentages = entity.getPlayerTypePercentagesAsMap();

        assertEquals(originalPercentages, retrievedPercentages);
    }

    @Test
    void testBuilder_CreatesEntityWithAllFields() {
        UUID userId = UUID.randomUUID();

        UserPlayerTypeEntity entity = UserPlayerTypeEntity.builder()
                .userId(userId)
                .primaryPlayerType(HexadPlayerType.FREE_SPIRIT)
                .achieverScore(0.10)
                .playerScore(0.15)
                .socialiserScore(0.20)
                .freeSpiritScore(0.35)
                .philanthropistScore(0.10)
                .disruptorScore(0.10)
                .build();

        assertNotNull(entity);
        assertEquals(userId, entity.getUserId());
        assertEquals(HexadPlayerType.FREE_SPIRIT, entity.getPrimaryPlayerType());
        assertEquals(0.10, entity.getAchieverScore());
        assertEquals(0.15, entity.getPlayerScore());
        assertEquals(0.20, entity.getSocialiserScore());
        assertEquals(0.35, entity.getFreeSpiritScore());
        assertEquals(0.10, entity.getPhilanthropistScore());
        assertEquals(0.10, entity.getDisruptorScore());
    }

    @Test
    void testSettersAndGetters_WorkCorrectly() {
        UserPlayerTypeEntity entity = new UserPlayerTypeEntity();
        UUID userId = UUID.randomUUID();

        entity.setUserId(userId);
        entity.setPrimaryPlayerType(HexadPlayerType.PHILANTHROPIST);
        entity.setAchieverScore(0.15);
        entity.setPlayerScore(0.10);
        entity.setSocialiserScore(0.20);
        entity.setFreeSpiritScore(0.15);
        entity.setPhilanthropistScore(0.30);
        entity.setDisruptorScore(0.10);

        assertEquals(userId, entity.getUserId());
        assertEquals(HexadPlayerType.PHILANTHROPIST, entity.getPrimaryPlayerType());
        assertEquals(0.15, entity.getAchieverScore());
        assertEquals(0.10, entity.getPlayerScore());
        assertEquals(0.20, entity.getSocialiserScore());
        assertEquals(0.15, entity.getFreeSpiritScore());
        assertEquals(0.30, entity.getPhilanthropistScore());
        assertEquals(0.10, entity.getDisruptorScore());
    }

    @Test
    void testSetPlayerTypePercentagesFromMap_HandlesNullScores() {
        UserPlayerTypeEntity entity = new UserPlayerTypeEntity();
        Map<HexadPlayerType, Double> percentages = Map.of(
                HexadPlayerType.ACHIEVER, 1.0,
                HexadPlayerType.PLAYER, 0.0,
                HexadPlayerType.SOCIALISER, 0.0,
                HexadPlayerType.FREE_SPIRIT, 0.0,
                HexadPlayerType.PHILANTHROPIST, 0.0,
                HexadPlayerType.DISRUPTOR, 0.0
        );

        entity.setPlayerTypePercentagesFromMap(percentages);

        assertEquals(1.0, entity.getAchieverScore());
        assertEquals(0.0, entity.getPlayerScore());
        assertEquals(0.0, entity.getSocialiserScore());
    }
}
