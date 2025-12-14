package de.unistuttgart.iste.meitrex.tutor_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing feedback generated for a user after completing an assignment.
 */
@Entity
@Table(name = "proactive_feedback", indexes = {
    @Index(name = "idx_proactive_feedback_user_id", columnList = "user_id"),
    @Index(name = "idx_proactive_feedback_assessment_id", columnList = "assessment_id"),
    @Index(name = "idx_proactive_feedback_user_assessment", columnList = "user_id,assessment_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProactiveFeedbackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "assessment_id", nullable = false)
    private UUID assessmentId;

    @Column(name = "feedback_text", nullable = false, columnDefinition = "TEXT")
    private String feedbackText;

    @Column(name = "correctness", nullable = false)
    private Double correctness;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
