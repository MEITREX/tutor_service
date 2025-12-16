package de.unistuttgart.iste.meitrex.tutor_service.persistence.mapper;

import de.unistuttgart.iste.meitrex.common.event.HexadPlayerType;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.UserPlayerTypeEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserPlayerTypeMapperTest {

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private UserPlayerTypeMapper mapper;

    private UUID userId;
    private HexadPlayerType primaryPlayerType;
    private Map<HexadPlayerType, Double> playerTypePercentages;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        primaryPlayerType = HexadPlayerType.ACHIEVER;
        playerTypePercentages = Map.of(
                HexadPlayerType.ACHIEVER, 0.40,
                HexadPlayerType.PLAYER, 0.25,
                HexadPlayerType.SOCIALISER, 0.15,
                HexadPlayerType.FREE_SPIRIT, 0.10,
                HexadPlayerType.PHILANTHROPIST, 0.05,
                HexadPlayerType.DISRUPTOR, 0.05
        );
    }

    @Test
    void testCreateEntity_CreatesNewEntityWithAllFields() {
        UserPlayerTypeEntity entity = mapper.createEntity(userId, primaryPlayerType, playerTypePercentages);

        assertNotNull(entity);
        assertEquals(userId, entity.getUserId());
        assertEquals(primaryPlayerType, entity.getPrimaryPlayerType());
        assertEquals(0.40, entity.getAchieverScore());
        assertEquals(0.25, entity.getPlayerScore());
        assertEquals(0.15, entity.getSocialiserScore());
        assertEquals(0.10, entity.getFreeSpiritScore());
        assertEquals(0.05, entity.getPhilanthropistScore());
        assertEquals(0.05, entity.getDisruptorScore());
    }

    @Test
    void testCreateEntity_WithDifferentPrimaryType() {
        HexadPlayerType philanthropist = HexadPlayerType.PHILANTHROPIST;
        Map<HexadPlayerType, Double> percentages = Map.of(
                HexadPlayerType.ACHIEVER, 0.10,
                HexadPlayerType.PLAYER, 0.10,
                HexadPlayerType.SOCIALISER, 0.20,
                HexadPlayerType.FREE_SPIRIT, 0.15,
                HexadPlayerType.PHILANTHROPIST, 0.35,
                HexadPlayerType.DISRUPTOR, 0.10
        );

        UserPlayerTypeEntity entity = mapper.createEntity(userId, philanthropist, percentages);

        assertNotNull(entity);
        assertEquals(userId, entity.getUserId());
        assertEquals(philanthropist, entity.getPrimaryPlayerType());
        assertEquals(0.35, entity.getPhilanthropistScore());
    }

    @Test
    void testUpdateEntity_UpdatesExistingEntity() {
        UserPlayerTypeEntity existingEntity = UserPlayerTypeEntity.builder()
                .userId(userId)
                .primaryPlayerType(HexadPlayerType.PLAYER)
                .achieverScore(0.20)
                .playerScore(0.50)
                .socialiserScore(0.10)
                .freeSpiritScore(0.10)
                .philanthropistScore(0.05)
                .disruptorScore(0.05)
                .build();

        HexadPlayerType newPrimaryType = HexadPlayerType.ACHIEVER;
        Map<HexadPlayerType, Double> newPercentages = Map.of(
                HexadPlayerType.ACHIEVER, 0.45,
                HexadPlayerType.PLAYER, 0.20,
                HexadPlayerType.SOCIALISER, 0.15,
                HexadPlayerType.FREE_SPIRIT, 0.10,
                HexadPlayerType.PHILANTHROPIST, 0.05,
                HexadPlayerType.DISRUPTOR, 0.05
        );

        UserPlayerTypeEntity updatedEntity = mapper.updateEntity(existingEntity, newPrimaryType, newPercentages);

        assertSame(existingEntity, updatedEntity);
        assertEquals(newPrimaryType, updatedEntity.getPrimaryPlayerType());
        assertEquals(0.45, updatedEntity.getAchieverScore());
        assertEquals(0.20, updatedEntity.getPlayerScore());
        assertEquals(0.15, updatedEntity.getSocialiserScore());
        assertEquals(0.10, updatedEntity.getFreeSpiritScore());
        assertEquals(0.05, updatedEntity.getPhilanthropistScore());
        assertEquals(0.05, updatedEntity.getDisruptorScore());
    }

    @Test
    void testUpdateEntity_ChangesAllPlayerTypeScores() {
        UserPlayerTypeEntity existingEntity = UserPlayerTypeEntity.builder()
                .userId(userId)
                .primaryPlayerType(HexadPlayerType.ACHIEVER)
                .achieverScore(0.10)
                .playerScore(0.10)
                .socialiserScore(0.10)
                .freeSpiritScore(0.10)
                .philanthropistScore(0.10)
                .disruptorScore(0.50)
                .build();

        HexadPlayerType newPrimaryType = HexadPlayerType.FREE_SPIRIT;
        Map<HexadPlayerType, Double> newPercentages = Map.of(
                HexadPlayerType.ACHIEVER, 0.15,
                HexadPlayerType.PLAYER, 0.15,
                HexadPlayerType.SOCIALISER, 0.15,
                HexadPlayerType.FREE_SPIRIT, 0.35,
                HexadPlayerType.PHILANTHROPIST, 0.10,
                HexadPlayerType.DISRUPTOR, 0.10
        );

        UserPlayerTypeEntity updatedEntity = mapper.updateEntity(existingEntity, newPrimaryType, newPercentages);

        assertEquals(newPrimaryType, updatedEntity.getPrimaryPlayerType());
        assertNotEquals(0.10, updatedEntity.getAchieverScore());
        assertNotEquals(0.50, updatedEntity.getDisruptorScore());
        assertEquals(0.35, updatedEntity.getFreeSpiritScore());
    }

    @Test
    void testCreateEntity_WithAllSixPlayerTypes() {
        for (HexadPlayerType type : HexadPlayerType.values()) {
            Map<HexadPlayerType, Double> percentages = Map.of(
                    HexadPlayerType.ACHIEVER, 0.16,
                    HexadPlayerType.PLAYER, 0.17,
                    HexadPlayerType.SOCIALISER, 0.17,
                    HexadPlayerType.FREE_SPIRIT, 0.17,
                    HexadPlayerType.PHILANTHROPIST, 0.17,
                    HexadPlayerType.DISRUPTOR, 0.16
            );

            UserPlayerTypeEntity entity = mapper.createEntity(UUID.randomUUID(), type, percentages);

            assertNotNull(entity);
            assertEquals(type, entity.getPrimaryPlayerType());
        }
    }

    @Test
    void testCreateEntity_WithZeroPercentages() {
        Map<HexadPlayerType, Double> zeroPercentages = Map.of(
                HexadPlayerType.ACHIEVER, 1.0,
                HexadPlayerType.PLAYER, 0.0,
                HexadPlayerType.SOCIALISER, 0.0,
                HexadPlayerType.FREE_SPIRIT, 0.0,
                HexadPlayerType.PHILANTHROPIST, 0.0,
                HexadPlayerType.DISRUPTOR, 0.0
        );

        UserPlayerTypeEntity entity = mapper.createEntity(userId, HexadPlayerType.ACHIEVER, zeroPercentages);

        assertNotNull(entity);
        assertEquals(1.0, entity.getAchieverScore());
        assertEquals(0.0, entity.getPlayerScore());
    }

    @Test
    void testUpdateEntity_ReturnsSameInstance() {
        UserPlayerTypeEntity existingEntity = UserPlayerTypeEntity.builder()
                .userId(userId)
                .primaryPlayerType(HexadPlayerType.PLAYER)
                .build();

        UserPlayerTypeEntity result = mapper.updateEntity(existingEntity, primaryPlayerType, playerTypePercentages);

        assertSame(existingEntity, result, "updateEntity should return the same instance");
    }
}
