package de.unistuttgart.iste.meitrex.tutor_service.service;

import de.unistuttgart.iste.meitrex.common.event.HexadPlayerType;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.UserPlayerTypeEntity;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.repository.UserPlayerTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing user player type information.
 * Handles saving and retrieving player type data from events.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserPlayerTypeService {

    private final UserPlayerTypeRepository userPlayerTypeRepository;

    /**
     * Saves or updates user player type information from an event.
     * 
     * @param userId the ID of the user
     * @param primaryPlayerType the user's primary player type
     * @param playerTypePercentages map of all player type percentages
     */
    @Transactional
    public void saveUserPlayerType(UUID userId, HexadPlayerType primaryPlayerType, Map<HexadPlayerType, Double> playerTypePercentages) {
        log.info("Saving player type for user {}: primaryType={}", userId, primaryPlayerType);
        
        UserPlayerTypeEntity entity = userPlayerTypeRepository.findById(userId)
                .orElse(UserPlayerTypeEntity.builder()
                        .userId(userId)
                        .build());
        
        entity.setPrimaryPlayerType(primaryPlayerType);
        entity.setPlayerTypePercentagesFromMap(playerTypePercentages);
        
        userPlayerTypeRepository.save(entity);
    }

    /**
     * Retrieves user player type information by user ID.
     * 
     * @param userId the ID of the user
     * @return Optional containing the user's player type entity if found
     */
    public Optional<UserPlayerTypeEntity> getUserPlayerType(UUID userId) {
        return userPlayerTypeRepository.findById(userId);
    }

    /**
     * Gets the primary player type for a user.
     * 
     * @param userId the ID of the user
     * @return Optional containing the primary player type if found
     */
    public Optional<HexadPlayerType> getPrimaryPlayerType(UUID userId) {
        return userPlayerTypeRepository.findById(userId)
                .map(UserPlayerTypeEntity::getPrimaryPlayerType);
    }
}
