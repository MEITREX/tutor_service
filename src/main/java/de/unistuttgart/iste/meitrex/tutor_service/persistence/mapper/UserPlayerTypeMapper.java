package de.unistuttgart.iste.meitrex.tutor_service.persistence.mapper;

import de.unistuttgart.iste.meitrex.common.event.HexadPlayerType;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.UserPlayerTypeEntity;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Mapper for UserPlayerTypeEntity.
 * Handles conversion between entity and domain models.
 */
@Component
@RequiredArgsConstructor
public class UserPlayerTypeMapper {

    private final ModelMapper modelMapper;

    /**
     * Creates a UserPlayerTypeEntity from event data.
     *
     * @param userId               the user ID
     * @param primaryPlayerType    the primary player type
     * @param playerTypePercentages map of player type percentages
     * @return the entity
     */
    public UserPlayerTypeEntity createEntity(UUID userId, 
                                            HexadPlayerType primaryPlayerType, 
                                            Map<HexadPlayerType, Double> playerTypePercentages) {
        UserPlayerTypeEntity entity = UserPlayerTypeEntity.builder()
                .userId(userId)
                .primaryPlayerType(primaryPlayerType)
                .build();
        entity.setPlayerTypePercentagesFromMap(playerTypePercentages);
        return entity;
    }

    /**
     * Updates an existing entity with new data.
     *
     * @param entity               the entity to update
     * @param primaryPlayerType    the new primary player type
     * @param playerTypePercentages the new percentages
     * @return the updated entity
     */
    public UserPlayerTypeEntity updateEntity(UserPlayerTypeEntity entity,
                            HexadPlayerType primaryPlayerType,
                            Map<HexadPlayerType, Double> playerTypePercentages) {
        entity.setPrimaryPlayerType(primaryPlayerType);
        entity.setPlayerTypePercentagesFromMap(playerTypePercentages);
        return entity;
    }
}
