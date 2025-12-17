package de.unistuttgart.iste.meitrex.tutor_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing a conversation exchange between user and tutor.
 * Stores question-answer pairs for context in subsequent interactions.
 */
@Entity
@Table(name = "conversation_history", indexes = {
    @Index(name = "idx_conversation_history_user_id", columnList = "user_id"),
    @Index(name = "idx_conversation_history_course_id", columnList = "course_id"),
    @Index(name = "idx_conversation_history_timestamp", columnList = "timestamp"),
    @Index(name = "idx_conversation_history_user_course", columnList = "user_id,course_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "user_message", nullable = false, columnDefinition = "TEXT")
    private String userMessage;

    @Column(name = "tutor_response", nullable = false, columnDefinition = "TEXT")
    private String tutorResponse;

    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;
}
