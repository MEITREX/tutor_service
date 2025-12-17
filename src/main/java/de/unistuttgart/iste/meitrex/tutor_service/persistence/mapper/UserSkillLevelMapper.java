package de.unistuttgart.iste.meitrex.tutor_service.persistence.mapper;

import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.UserSkillLevelEntity;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mapper for UserSkillLevelEntity.
 * Handles conversion between entity and domain models.
 */
@Component
@RequiredArgsConstructor
public class UserSkillLevelMapper {

    private final ModelMapper modelMapper;

    /**
     * Creates a UserSkillLevelEntity from event data.
     *
     * @param userId          the user ID
     * @param skillId         the skill ID
     * @param skillLevelValue the skill level value
     * @return the entity
     */
    public UserSkillLevelEntity createEntity(UUID userId, UUID skillId, Float skillLevelValue) {
        return UserSkillLevelEntity.builder()
                .userId(userId)
                .skillId(skillId)
                .skillLevelValue(skillLevelValue)
                .build();
    }

    /**
     * Updates an existing entity with a new skill level value.
     *
     * @param entity          the entity to update
     * @param skillLevelValue the new skill level value
     * @return the updated entity
     */
    public UserSkillLevelEntity updateEntity(UserSkillLevelEntity entity, Float skillLevelValue) {
        entity.setSkillLevelValue(skillLevelValue);
        return entity;
    }
}
