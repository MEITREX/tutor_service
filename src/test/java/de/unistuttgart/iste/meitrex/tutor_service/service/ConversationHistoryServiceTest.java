package de.unistuttgart.iste.meitrex.tutor_service.service;

import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.ConversationHistoryEntity;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.repository.ConversationHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationHistoryServiceTest {

    @Mock
    private ConversationHistoryRepository conversationHistoryRepository;

    @InjectMocks
    private ConversationHistoryService conversationHistoryService;

    private UUID userId;
    private UUID courseId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        
        ReflectionTestUtils.setField(conversationHistoryService, "maxHistoryPairs", 3);
        ReflectionTestUtils.setField(conversationHistoryService, "maxAgeMinutes", 30);
    }

    @Test
    void testGetRecentHistory_ReturnsOnlyRecentEntries() {
        OffsetDateTime now = OffsetDateTime.now();
        
        ConversationHistoryEntity recent1 = createHistoryEntity(now.minusMinutes(5));
        ConversationHistoryEntity recent2 = createHistoryEntity(now.minusMinutes(10));
        ConversationHistoryEntity old = createHistoryEntity(now.minusMinutes(60));
        
        when(conversationHistoryRepository.findByUserIdAndCourseIdOrderByTimestampDesc(userId, courseId))
                .thenReturn(Arrays.asList(recent1, recent2, old));
        
        List<ConversationHistoryEntity> result = conversationHistoryService.getRecentHistory(userId, courseId);
        
        assertEquals(2, result.size());
        assertTrue(result.contains(recent1));
        assertTrue(result.contains(recent2));
        assertFalse(result.contains(old));
    }

    @Test
    void testGetRecentHistory_LimitsToMaxHistoryPairs() {
        OffsetDateTime now = OffsetDateTime.now();
        
        ConversationHistoryEntity entry1 = createHistoryEntity(now.minusMinutes(1));
        ConversationHistoryEntity entry2 = createHistoryEntity(now.minusMinutes(2));
        ConversationHistoryEntity entry3 = createHistoryEntity(now.minusMinutes(3));
        ConversationHistoryEntity entry4 = createHistoryEntity(now.minusMinutes(4));
        
        when(conversationHistoryRepository.findByUserIdAndCourseIdOrderByTimestampDesc(userId, courseId))
                .thenReturn(Arrays.asList(entry1, entry2, entry3, entry4));
        
        List<ConversationHistoryEntity> result = conversationHistoryService.getRecentHistory(userId, courseId);
        
        assertEquals(3, result.size());
        assertTrue(result.contains(entry1));
        assertTrue(result.contains(entry2));
        assertTrue(result.contains(entry3));
        assertFalse(result.contains(entry4));
    }

    @Test
    void testGetRecentHistory_EmptyWhenNoHistory() {
        when(conversationHistoryRepository.findByUserIdAndCourseIdOrderByTimestampDesc(userId, courseId))
                .thenReturn(Collections.emptyList());
        
        List<ConversationHistoryEntity> result = conversationHistoryService.getRecentHistory(userId, courseId);
        
        assertTrue(result.isEmpty());
    }

    @Test
    void testAddConversationExchange_SavesNewEntry() {
        String userMessage = "What do you get if you multiply six by nine?";
        String tutorResponse = "42";
        
        when(conversationHistoryRepository.findByUserIdAndCourseIdOrderByTimestampDesc(userId, courseId))
                .thenReturn(Collections.emptyList());
        
        conversationHistoryService.addConversationExchange(userId, courseId, userMessage, tutorResponse);
        
        verify(conversationHistoryRepository).save(argThat(entity ->
                entity.getUserId().equals(userId) &&
                entity.getCourseId().equals(courseId) &&
                entity.getUserMessage().equals(userMessage) &&
                entity.getTutorResponse().equals(tutorResponse)
        ));
    }

    @Test
    void testAddConversationExchange_DeletesOldEntriesBeforeSaving() {
        String userMessage = "What is the meaning of life?";
        String tutorResponse = "42";
        OffsetDateTime cutoffTime = OffsetDateTime.now().minusMinutes(30);
        
        when(conversationHistoryRepository.findByUserIdAndCourseIdOrderByTimestampDesc(userId, courseId))
                .thenReturn(Collections.emptyList());
        
        conversationHistoryService.addConversationExchange(userId, courseId, userMessage, tutorResponse);
        
        verify(conversationHistoryRepository).deleteByUserIdAndCourseIdAndTimestampBefore(
                eq(userId), 
                eq(courseId), 
                any(OffsetDateTime.class)
        );
    }

    @Test
    void testAddConversationExchange_RemovesOldestWhenAtMaxCapacity() {
        String userMessage = "New question";
        String tutorResponse = "New response";
        
        ConversationHistoryEntity oldest = createHistoryEntity(OffsetDateTime.now().minusMinutes(20));
        ConversationHistoryEntity middle = createHistoryEntity(OffsetDateTime.now().minusMinutes(10));
        ConversationHistoryEntity newest = createHistoryEntity(OffsetDateTime.now().minusMinutes(5));
        
        when(conversationHistoryRepository.findByUserIdAndCourseIdOrderByTimestampDesc(userId, courseId))
                .thenReturn(Arrays.asList(newest, middle, oldest));
        
        conversationHistoryService.addConversationExchange(userId, courseId, userMessage, tutorResponse);
        
        verify(conversationHistoryRepository).delete(oldest);
        verify(conversationHistoryRepository).save(any(ConversationHistoryEntity.class));
    }

    @Test
    void testFormatHistoryForPrompt_ReturnsFormattedString() {
        OffsetDateTime now = OffsetDateTime.now();
        String userMessage1 = "pi = e = 3";
        String tutorResponse1 = "And sqrt(10) is also pi";
        String userMessage2 = "Hello there";
        String tutorResponse2 = "General Kenobi";
        
        ConversationHistoryEntity entry1 = ConversationHistoryEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .courseId(courseId)
                .userMessage(userMessage1)
                .tutorResponse(tutorResponse1)
                .timestamp(now.minusMinutes(10))
                .build();
        
        ConversationHistoryEntity entry2 = ConversationHistoryEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .courseId(courseId)
                .userMessage(userMessage2)
                .tutorResponse(tutorResponse2)
                .timestamp(now.minusMinutes(5))
                .build();
        
        when(conversationHistoryRepository.findByUserIdAndCourseIdOrderByTimestampDesc(userId, courseId))
                .thenReturn(Arrays.asList(entry2, entry1)); // Newest first
        
        String result = conversationHistoryService.formatHistoryForPrompt(userId, courseId);
        
        assertTrue(result.contains("Previous Conversation History"));
        assertTrue(result.contains("Exchange 1"));
        assertTrue(result.contains("Exchange 2"));
        assertTrue(result.contains(userMessage1));
        assertTrue(result.contains(tutorResponse1));
        assertTrue(result.contains(userMessage2));
        assertTrue(result.contains(tutorResponse2));
        
        int inheritanceIndex = result.indexOf(userMessage1);
        int exampleIndex = result.indexOf(userMessage2);
        assertTrue(inheritanceIndex < exampleIndex);
    }

    @Test
    void testFormatHistoryForPrompt_ReturnsEmptyWhenNoHistory() {
        when(conversationHistoryRepository.findByUserIdAndCourseIdOrderByTimestampDesc(userId, courseId))
                .thenReturn(Collections.emptyList());
        
        String result = conversationHistoryService.formatHistoryForPrompt(userId, courseId);
        
        assertEquals("", result);
    }

    @Test
    void testClearHistory_DeletesAllHistoryForUser() {
        ConversationHistoryEntity entry1 = createHistoryEntity(OffsetDateTime.now().minusMinutes(5));
        ConversationHistoryEntity entry2 = createHistoryEntity(OffsetDateTime.now().minusMinutes(10));
        
        when(conversationHistoryRepository.findByUserIdAndCourseIdOrderByTimestampDesc(userId, courseId))
                .thenReturn(Arrays.asList(entry1, entry2));
        
        conversationHistoryService.clearHistory(userId, courseId);
        
        verify(conversationHistoryRepository).deleteAll(Arrays.asList(entry1, entry2));
    }

    @Test
    void testClearHistory_HandlesEmptyHistory() {
        when(conversationHistoryRepository.findByUserIdAndCourseIdOrderByTimestampDesc(userId, courseId))
                .thenReturn(Collections.emptyList());
        
        conversationHistoryService.clearHistory(userId, courseId);
        
        verify(conversationHistoryRepository).deleteAll(Collections.emptyList());
    }

    /**
     * Creates a ConversationHistoryEntity with the given timestamp.
     */
    private ConversationHistoryEntity createHistoryEntity(OffsetDateTime timestamp) {
        return ConversationHistoryEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .courseId(courseId)
                .userMessage("Test question at " + timestamp)
                .tutorResponse("Test response at " + timestamp)
                .timestamp(timestamp)
                .build();
    }
}
