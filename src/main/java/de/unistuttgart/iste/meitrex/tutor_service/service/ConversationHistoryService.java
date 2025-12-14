package de.unistuttgart.iste.meitrex.tutor_service.service;

import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.ConversationHistoryEntity;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.repository.ConversationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing conversation history between users and the AI tutor.
 * Maintains a sliding window of recent conversation exchanges for context.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationHistoryService {

    private final ConversationHistoryRepository conversationHistoryRepository;

    @Value("${tutor.conversation.history.max-pairs:3}")
    private int maxHistoryPairs;

    @Value("${tutor.conversation.history.max-age-minutes:30}")
    private int maxAgeMinutes;

    /**
     * Retrieves the recent conversation history for a user in a specific course.
     * Only returns entries that are not older than the configured max age.
     *
     * @param userId the user ID
     * @param courseId the course ID
     * @return list of recent conversation history entries, ordered by timestamp descending
     */
    @Transactional(readOnly = true)
    public List<ConversationHistoryEntity> getRecentHistory(UUID userId, UUID courseId) {
        OffsetDateTime cutoffTime = OffsetDateTime.now().minusMinutes(maxAgeMinutes);
        
        List<ConversationHistoryEntity> allHistory = conversationHistoryRepository
                .findByUserIdAndCourseIdOrderByTimestampDesc(userId, courseId);
        
        List<ConversationHistoryEntity> recentHistory = allHistory.stream()
                .filter(entry -> entry.getTimestamp().isAfter(cutoffTime))
                .limit(maxHistoryPairs)
                .toList();
        
        log.info("Retrieved {} recent history entries for user {} in course {} (filtered from {} total)", 
                recentHistory.size(), userId, courseId, allHistory.size());
        
        return recentHistory;
    }

    /**
     * Adds a new conversation exchange to the history.
     * Automatically manages the sliding window by removing old entries.
     *
     * @param userId the user ID
     * @param courseId the course ID
     * @param userMessage the user's question
     * @param tutorResponse the tutor's response
     */
    @Transactional
    public void addConversationExchange(UUID userId, UUID courseId, String userMessage, String tutorResponse) {
        OffsetDateTime cutoffTime = OffsetDateTime.now().minusMinutes(maxAgeMinutes);
        conversationHistoryRepository.deleteByUserIdAndCourseIdAndTimestampBefore(userId, courseId, cutoffTime);
        
        List<ConversationHistoryEntity> currentHistory = conversationHistoryRepository
                .findByUserIdAndCourseIdOrderByTimestampDesc(userId, courseId);
        
        if (currentHistory.size() >= maxHistoryPairs) {
            ConversationHistoryEntity oldest = currentHistory.get(currentHistory.size() - 1);
            conversationHistoryRepository.delete(oldest);
            log.info("Removed oldest conversation entry {} for user {} in course {}", 
                    oldest.getId(), userId, courseId);
        }
        
        ConversationHistoryEntity newEntry = ConversationHistoryEntity.builder()
                .userId(userId)
                .courseId(courseId)
                .userMessage(userMessage)
                .tutorResponse(tutorResponse)
                .timestamp(OffsetDateTime.now())
                .build();
        
        conversationHistoryRepository.save(newEntry);
        log.info("Added new conversation entry for user {} in course {}", userId, courseId);
    }

    /**
     * Formats the conversation history as a string for inclusion in prompts.
     * Oldest exchanges appear first.
     * Returns an empty string if there is no valid history.
     *
     * @param userId the user ID
     * @param courseId the course ID
     * @return formatted conversation history
     */
    public String formatHistoryForPrompt(UUID userId, UUID courseId) {
        List<ConversationHistoryEntity> history = getRecentHistory(userId, courseId);
        
        if (history.isEmpty()) {
            return "";
        }
        
        StringBuilder formatted = new StringBuilder();
        formatted.append("\n\n---\n\nPrevious Conversation History:\n");
        
        List<ConversationHistoryEntity> chronological = history.reversed();
        
        for (int i = 0; i < chronological.size(); i++) {
            ConversationHistoryEntity entry = chronological.get(i);
            formatted.append("\nExchange ").append(i + 1).append(":\n");
            formatted.append("Student: ").append(entry.getUserMessage()).append("\n");
            formatted.append("Tutor: ").append(entry.getTutorResponse()).append("\n");
        }
        
        return formatted.toString();
    }

    /**
     * Clears all conversation history for a user in a specific course.
     *
     * @param userId the user ID
     * @param courseId the course ID
     */
    @Transactional
    public void clearHistory(UUID userId, UUID courseId) {
        List<ConversationHistoryEntity> history = conversationHistoryRepository
                .findByUserIdAndCourseIdOrderByTimestampDesc(userId, courseId);
        conversationHistoryRepository.deleteAll(history);
        log.info("Cleared conversation history for user {} in course {}", userId, courseId);
    }
}
