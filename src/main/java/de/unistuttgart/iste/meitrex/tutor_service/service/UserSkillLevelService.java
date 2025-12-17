package de.unistuttgart.iste.meitrex.tutor_service.service;

import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.UserSkillLevelEntity;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.mapper.UserSkillLevelMapper;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.repository.UserSkillLevelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing user skill level information.
 * Handles saving and retrieving skill level data from events.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserSkillLevelService {

    private final UserSkillLevelRepository userSkillLevelRepository;
    private final UserSkillLevelMapper userSkillLevelMapper;

    /**
     * Saves or updates user skill level information from an event.
     * 
     * @param userId the ID of the user
     * @param skillId the ID of the skill
     * @param skillLevelValue the new skill level value
     */
    @Transactional
    public void saveUserSkillLevel(UUID userId, UUID skillId, Float skillLevelValue) {
        log.info("Saving skill level for user {} and skill {}: value={}", userId, skillId, skillLevelValue);
        
        UserSkillLevelEntity.UserSkillLevelId id = new UserSkillLevelEntity.UserSkillLevelId(userId, skillId);
        Optional<UserSkillLevelEntity> existingEntity = userSkillLevelRepository.findById(id);
        
        UserSkillLevelEntity entity;
        if (existingEntity.isPresent()) {
            entity = userSkillLevelMapper.updateEntity(existingEntity.get(), skillLevelValue);
        } else {
            entity = userSkillLevelMapper.createEntity(userId, skillId, skillLevelValue);
        }
        
        userSkillLevelRepository.save(entity);
    }

    /**
     * Retrieves a specific skill level for a user.
     * 
     * @param userId the ID of the user
     * @param skillId the ID of the skill
     * @return Optional containing the skill level value if found
     */
    public Optional<Float> getSkillLevel(UUID userId, UUID skillId) {
        UserSkillLevelEntity.UserSkillLevelId id = new UserSkillLevelEntity.UserSkillLevelId(userId, skillId);
        return userSkillLevelRepository.findById(id)
                .map(UserSkillLevelEntity::getSkillLevelValue);
    }

    /**
     * Retrieves all skill levels for a user.
     * 
     * @param userId the ID of the user
     * @return List of all skill level entities for the user
     */
    public List<UserSkillLevelEntity> getAllSkillLevelsForUser(UUID userId) {
        return userSkillLevelRepository.findByUserId(userId);
    }
}
