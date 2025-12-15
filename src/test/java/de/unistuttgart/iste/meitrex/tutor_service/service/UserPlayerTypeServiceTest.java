package de.unistuttgart.iste.meitrex.tutor_service.service;

import de.unistuttgart.iste.meitrex.common.event.HexadPlayerType;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.UserPlayerTypeEntity;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.mapper.UserPlayerTypeMapper;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.repository.UserPlayerTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPlayerTypeServiceTest {

    @Mock
    private UserPlayerTypeRepository userPlayerTypeRepository;

    @Mock
    private UserPlayerTypeMapper userPlayerTypeMapper;

    @InjectMocks
    private UserPlayerTypeService userPlayerTypeService;

    private UUID userId;
    private HexadPlayerType primaryPlayerType;
    private Map<HexadPlayerType, Double> playerTypePercentages;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        primaryPlayerType = HexadPlayerType.ACHIEVER;
        
        playerTypePercentages = new HashMap<>();
        playerTypePercentages.put(HexadPlayerType.ACHIEVER, 0.8);
        playerTypePercentages.put(HexadPlayerType.PLAYER, 0.6);
        playerTypePercentages.put(HexadPlayerType.SOCIALISER, 0.4);
        playerTypePercentages.put(HexadPlayerType.FREE_SPIRIT, 0.5);
        playerTypePercentages.put(HexadPlayerType.PHILANTHROPIST, 0.7);
        playerTypePercentages.put(HexadPlayerType.DISRUPTOR, 0.3);
    }

    @Test
    void testSaveUserPlayerType_CreatesNewEntityWhenNotExists() {
        UserPlayerTypeEntity newEntity = mock(UserPlayerTypeEntity.class);
        
        when(userPlayerTypeRepository.findById(userId)).thenReturn(Optional.empty());
        when(userPlayerTypeMapper.createEntity(userId, primaryPlayerType, playerTypePercentages))
                .thenReturn(newEntity);
        
        userPlayerTypeService.saveUserPlayerType(userId, primaryPlayerType, playerTypePercentages);
        
        verify(userPlayerTypeMapper).createEntity(userId, primaryPlayerType, playerTypePercentages);
        verify(userPlayerTypeRepository).save(newEntity);
        verify(userPlayerTypeMapper, never()).updateEntity(any(), any(), any());
    }

    @Test
    void testSaveUserPlayerType_UpdatesExistingEntity() {
        UserPlayerTypeEntity existingEntity = mock(UserPlayerTypeEntity.class);
        UserPlayerTypeEntity updatedEntity = mock(UserPlayerTypeEntity.class);
        
        when(userPlayerTypeRepository.findById(userId)).thenReturn(Optional.of(existingEntity));
        when(userPlayerTypeMapper.updateEntity(existingEntity, primaryPlayerType, playerTypePercentages))
                .thenReturn(updatedEntity);
        
        userPlayerTypeService.saveUserPlayerType(userId, primaryPlayerType, playerTypePercentages);
        
        verify(userPlayerTypeMapper).updateEntity(existingEntity, primaryPlayerType, playerTypePercentages);
        verify(userPlayerTypeRepository).save(updatedEntity);
        verify(userPlayerTypeMapper, never()).createEntity(any(), any(), any());
    }

    @Test
    void testGetUserPlayerType_ReturnsEntityWhenExists() {
        UserPlayerTypeEntity entity = mock(UserPlayerTypeEntity.class);
        when(userPlayerTypeRepository.findById(userId)).thenReturn(Optional.of(entity));
        
        Optional<UserPlayerTypeEntity> result = userPlayerTypeService.getUserPlayerType(userId);
        
        assertTrue(result.isPresent());
        assertEquals(entity, result.get());
    }

    @Test
    void testGetUserPlayerType_ReturnsEmptyWhenNotExists() {
        when(userPlayerTypeRepository.findById(userId)).thenReturn(Optional.empty());
        
        Optional<UserPlayerTypeEntity> result = userPlayerTypeService.getUserPlayerType(userId);
        
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetPrimaryPlayerType_ReturnsTypeWhenExists() {
        UserPlayerTypeEntity entity = mock(UserPlayerTypeEntity.class);
        when(entity.getPrimaryPlayerType()).thenReturn(primaryPlayerType);
        when(userPlayerTypeRepository.findById(userId)).thenReturn(Optional.of(entity));
        
        Optional<HexadPlayerType> result = userPlayerTypeService.getPrimaryPlayerType(userId);
        
        assertTrue(result.isPresent());
        assertEquals(primaryPlayerType, result.get());
    }

    @Test
    void testGetPrimaryPlayerType_ReturnsEmptyWhenNotExists() {
        when(userPlayerTypeRepository.findById(userId)).thenReturn(Optional.empty());
        
        Optional<HexadPlayerType> result = userPlayerTypeService.getPrimaryPlayerType(userId);
        
        assertTrue(result.isEmpty());
    }

    @Test
    void testSaveUserPlayerType_HandlesAllPlayerTypes() {
        UserPlayerTypeEntity newEntity = mock(UserPlayerTypeEntity.class);
        
        Map<HexadPlayerType, Double> allTypes = new HashMap<>();
        for (HexadPlayerType type : HexadPlayerType.values()) {
            allTypes.put(type, 0.5);
        }
        
        when(userPlayerTypeRepository.findById(userId)).thenReturn(Optional.empty());
        when(userPlayerTypeMapper.createEntity(userId, HexadPlayerType.PHILANTHROPIST, allTypes))
                .thenReturn(newEntity);
        
        userPlayerTypeService.saveUserPlayerType(userId, HexadPlayerType.PHILANTHROPIST, allTypes);
        
        verify(userPlayerTypeMapper).createEntity(userId, HexadPlayerType.PHILANTHROPIST, allTypes);
        verify(userPlayerTypeRepository).save(newEntity);
    }
}
