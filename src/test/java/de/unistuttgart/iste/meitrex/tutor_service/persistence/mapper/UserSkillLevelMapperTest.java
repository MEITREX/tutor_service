package de.unistuttgart.iste.meitrex.tutor_service.persistence.mapper;

import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.UserSkillLevelEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserSkillLevelMapperTest {

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private UserSkillLevelMapper mapper;

    private UUID userId;
    private UUID skillId;
    private Float skillLevelValue;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        skillId = UUID.randomUUID();
        skillLevelValue = 0.75f;
    }

    @Test
    void testCreateEntity_CreatesNewEntityWithAllFields() {
        UserSkillLevelEntity entity = mapper.createEntity(userId, skillId, skillLevelValue);

        assertNotNull(entity);
        assertEquals(userId, entity.getUserId());
        assertEquals(skillId, entity.getSkillId());
        assertEquals(skillLevelValue, entity.getSkillLevelValue());
    }

    @Test
    void testCreateEntity_WithDifferentSkillLevels() {
        UserSkillLevelEntity beginnerEntity = mapper.createEntity(userId, skillId, 0.1f);
        assertEquals(0.1f, beginnerEntity.getSkillLevelValue());

        UserSkillLevelEntity intermediateEntity = mapper.createEntity(userId, skillId, 0.5f);
        assertEquals(0.5f, intermediateEntity.getSkillLevelValue());

        UserSkillLevelEntity expertEntity = mapper.createEntity(userId, skillId, 0.9f);
        assertEquals(0.9f, expertEntity.getSkillLevelValue());

        UserSkillLevelEntity maxEntity = mapper.createEntity(userId, skillId, 1.0f);
        assertEquals(1.0f, maxEntity.getSkillLevelValue());
    }

    @Test
    void testCreateEntity_WithZeroSkillLevel() {
        UserSkillLevelEntity entity = mapper.createEntity(userId, skillId, 0.0f);

        assertNotNull(entity);
        assertEquals(0.0f, entity.getSkillLevelValue());
    }

    @Test
    void testCreateEntity_WithMultipleSkills() {
        UUID skill1 = UUID.randomUUID();
        UUID skill2 = UUID.randomUUID();
        UUID skill3 = UUID.randomUUID();

        UserSkillLevelEntity entity1 = mapper.createEntity(userId, skill1, 0.3f);
        UserSkillLevelEntity entity2 = mapper.createEntity(userId, skill2, 0.6f);
        UserSkillLevelEntity entity3 = mapper.createEntity(userId, skill3, 0.9f);

        assertEquals(skill1, entity1.getSkillId());
        assertEquals(0.3f, entity1.getSkillLevelValue());
        
        assertEquals(skill2, entity2.getSkillId());
        assertEquals(0.6f, entity2.getSkillLevelValue());
        
        assertEquals(skill3, entity3.getSkillId());
        assertEquals(0.9f, entity3.getSkillLevelValue());
    }

    @Test
    void testUpdateEntity_UpdatesSkillLevelValue() {
        UserSkillLevelEntity existingEntity = UserSkillLevelEntity.builder()
                .userId(userId)
                .skillId(skillId)
                .skillLevelValue(0.4f)
                .build();

        Float newSkillLevel = 0.8f;

        UserSkillLevelEntity updatedEntity = mapper.updateEntity(existingEntity, newSkillLevel);

        assertSame(existingEntity, updatedEntity);
        assertEquals(newSkillLevel, updatedEntity.getSkillLevelValue());
    }

    @Test
    void testUpdateEntity_ReturnsSameInstance() {
        UserSkillLevelEntity existingEntity = UserSkillLevelEntity.builder()
                .userId(userId)
                .skillId(skillId)
                .skillLevelValue(0.5f)
                .build();

        UserSkillLevelEntity result = mapper.updateEntity(existingEntity, 0.7f);

        assertSame(existingEntity, result, "updateEntity should return the same instance");
    }

    @Test
    void testUpdateEntity_CanIncreaseSkillLevel() {
        UserSkillLevelEntity existingEntity = UserSkillLevelEntity.builder()
                .userId(userId)
                .skillId(skillId)
                .skillLevelValue(0.3f)
                .build();

        mapper.updateEntity(existingEntity, 0.7f);

        assertEquals(0.7f, existingEntity.getSkillLevelValue());
    }

    @Test
    void testUpdateEntity_CanDecreaseSkillLevel() {
        UserSkillLevelEntity existingEntity = UserSkillLevelEntity.builder()
                .userId(userId)
                .skillId(skillId)
                .skillLevelValue(0.9f)
                .build();

        mapper.updateEntity(existingEntity, 0.4f);

        assertEquals(0.4f, existingEntity.getSkillLevelValue());
    }

    @Test
    void testUpdateEntity_CanSetToZero() {
        UserSkillLevelEntity existingEntity = UserSkillLevelEntity.builder()
                .userId(userId)
                .skillId(skillId)
                .skillLevelValue(0.5f)
                .build();

        mapper.updateEntity(existingEntity, 0.0f);

        assertEquals(0.0f, existingEntity.getSkillLevelValue());
    }

    @Test
    void testUpdateEntity_CanSetToMaximum() {
        UserSkillLevelEntity existingEntity = UserSkillLevelEntity.builder()
                .userId(userId)
                .skillId(skillId)
                .skillLevelValue(0.5f)
                .build();

        mapper.updateEntity(existingEntity, 1.0f);

        assertEquals(1.0f, existingEntity.getSkillLevelValue());
    }

    @Test
    void testCreateEntity_PreservesUserId() {
        UUID specificUserId = UUID.randomUUID();

        UserSkillLevelEntity entity = mapper.createEntity(specificUserId, skillId, 0.5f);

        assertEquals(specificUserId, entity.getUserId());
    }

    @Test
    void testCreateEntity_PreservesSkillId() {
        UUID specificSkillId = UUID.randomUUID();

        UserSkillLevelEntity entity = mapper.createEntity(userId, specificSkillId, 0.5f);

        assertEquals(specificSkillId, entity.getSkillId());
    }
}
