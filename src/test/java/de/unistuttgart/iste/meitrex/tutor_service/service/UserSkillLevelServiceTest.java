package de.unistuttgart.iste.meitrex.tutor_service.service;

import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.UserSkillLevelEntity;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.mapper.UserSkillLevelMapper;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.repository.UserSkillLevelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSkillLevelServiceTest {

    @Mock
    private UserSkillLevelRepository userSkillLevelRepository;

    @Mock
    private UserSkillLevelMapper userSkillLevelMapper;

    @InjectMocks
    private UserSkillLevelService userSkillLevelService;

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
    void testSaveUserSkillLevel_CreatesNewEntityWhenNotExists() {
        UserSkillLevelEntity.UserSkillLevelId id = new UserSkillLevelEntity.UserSkillLevelId(userId, skillId);
        UserSkillLevelEntity newEntity = mock(UserSkillLevelEntity.class);
        
        when(userSkillLevelRepository.findById(id)).thenReturn(Optional.empty());
        when(userSkillLevelMapper.createEntity(userId, skillId, skillLevelValue)).thenReturn(newEntity);
        
        userSkillLevelService.saveUserSkillLevel(userId, skillId, skillLevelValue);
        
        verify(userSkillLevelMapper).createEntity(userId, skillId, skillLevelValue);
        verify(userSkillLevelRepository).save(newEntity);
        verify(userSkillLevelMapper, never()).updateEntity(any(), any());
    }

    @Test
    void testSaveUserSkillLevel_UpdatesExistingEntity() {
        UserSkillLevelEntity.UserSkillLevelId id = new UserSkillLevelEntity.UserSkillLevelId(userId, skillId);
        UserSkillLevelEntity existingEntity = mock(UserSkillLevelEntity.class);
        UserSkillLevelEntity updatedEntity = mock(UserSkillLevelEntity.class);
        
        when(userSkillLevelRepository.findById(id)).thenReturn(Optional.of(existingEntity));
        when(userSkillLevelMapper.updateEntity(existingEntity, skillLevelValue)).thenReturn(updatedEntity);
        
        userSkillLevelService.saveUserSkillLevel(userId, skillId, skillLevelValue);
        
        verify(userSkillLevelMapper).updateEntity(existingEntity, skillLevelValue);
        verify(userSkillLevelRepository).save(updatedEntity);
        verify(userSkillLevelMapper, never()).createEntity(any(), any(), any());
    }

    @Test
    void testGetSkillLevel_ReturnsValueWhenExists() {
        UserSkillLevelEntity.UserSkillLevelId id = new UserSkillLevelEntity.UserSkillLevelId(userId, skillId);
        UserSkillLevelEntity entity = mock(UserSkillLevelEntity.class);
        when(entity.getSkillLevelValue()).thenReturn(skillLevelValue);
        when(userSkillLevelRepository.findById(id)).thenReturn(Optional.of(entity));
        
        Optional<Float> result = userSkillLevelService.getSkillLevel(userId, skillId);
        
        assertTrue(result.isPresent());
        assertEquals(skillLevelValue, result.get());
    }

    @Test
    void testGetSkillLevel_ReturnsEmptyWhenNotExists() {
        UserSkillLevelEntity.UserSkillLevelId id = new UserSkillLevelEntity.UserSkillLevelId(userId, skillId);
        when(userSkillLevelRepository.findById(id)).thenReturn(Optional.empty());
        
        Optional<Float> result = userSkillLevelService.getSkillLevel(userId, skillId);
        
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAllSkillLevelsForUser_ReturnsAllSkills() {
        UserSkillLevelEntity skill1 = mock(UserSkillLevelEntity.class);
        UserSkillLevelEntity skill2 = mock(UserSkillLevelEntity.class);
        UserSkillLevelEntity skill3 = mock(UserSkillLevelEntity.class);
        
        List<UserSkillLevelEntity> skills = Arrays.asList(skill1, skill2, skill3);
        when(userSkillLevelRepository.findByUserId(userId)).thenReturn(skills);
        
        List<UserSkillLevelEntity> result = userSkillLevelService.getAllSkillLevelsForUser(userId);
        
        assertEquals(3, result.size());
        assertTrue(result.contains(skill1));
        assertTrue(result.contains(skill2));
        assertTrue(result.contains(skill3));
    }

    @Test
    void testGetAllSkillLevelsForUser_ReturnsEmptyListWhenNoSkills() {
        when(userSkillLevelRepository.findByUserId(userId)).thenReturn(List.of());
        
        List<UserSkillLevelEntity> result = userSkillLevelService.getAllSkillLevelsForUser(userId);
        
        assertTrue(result.isEmpty());
    }

    @Test
    void testSaveUserSkillLevel_HandlesMultipleSkillsForSameUser() {
        UUID skill1Id = UUID.randomUUID();
        UUID skill2Id = UUID.randomUUID();
        Float value1 = 0.6f;
        Float value2 = 0.9f;
        
        UserSkillLevelEntity.UserSkillLevelId id1 = new UserSkillLevelEntity.UserSkillLevelId(userId, skill1Id);
        UserSkillLevelEntity.UserSkillLevelId id2 = new UserSkillLevelEntity.UserSkillLevelId(userId, skill2Id);
        
        UserSkillLevelEntity entity1 = mock(UserSkillLevelEntity.class);
        UserSkillLevelEntity entity2 = mock(UserSkillLevelEntity.class);
        
        when(userSkillLevelRepository.findById(id1)).thenReturn(Optional.empty());
        when(userSkillLevelRepository.findById(id2)).thenReturn(Optional.empty());
        when(userSkillLevelMapper.createEntity(userId, skill1Id, value1)).thenReturn(entity1);
        when(userSkillLevelMapper.createEntity(userId, skill2Id, value2)).thenReturn(entity2);
        
        userSkillLevelService.saveUserSkillLevel(userId, skill1Id, value1);
        userSkillLevelService.saveUserSkillLevel(userId, skill2Id, value2);
        
        verify(userSkillLevelRepository).save(entity1);
        verify(userSkillLevelRepository).save(entity2);
    }
}
