package de.unistuttgart.iste.meitrex.tutor_service.persistence.repository;

import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.ConversationHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for ConversationHistoryEntity.
 */
@Repository
public interface ConversationHistoryRepository extends JpaRepository<ConversationHistoryEntity, UUID> {

    /**
     * Finds all conversation history entries for a specific user and course, ordered by timestamp descending.
     *
     * @param userId the user ID
     * @param courseId the course ID
     * @return list of conversation history entities
     */
    List<ConversationHistoryEntity> findByUserIdAndCourseIdOrderByTimestampDesc(UUID userId, UUID courseId);

    /**
     * Deletes conversation history entries older than the specified timestamp.
     *
     * @param userId the user ID
     * @param courseId the course ID
     * @param timestamp the cutoff timestamp
     */
    void deleteByUserIdAndCourseIdAndTimestampBefore(UUID userId, UUID courseId, OffsetDateTime timestamp);

    /**
     * Counts the number of conversation history entries for a user and course.
     *
     * @param userId the user ID
     * @param courseId the course ID
     * @return count of entries
     */
    long countByUserIdAndCourseId(UUID userId, UUID courseId);
}
