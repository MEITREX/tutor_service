package de.unistuttgart.iste.meitrex.tutor_service.persistence.repository;

import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.ProactiveFeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ProactiveFeedbackEntity.
 */
@Repository
public interface ProactiveFeedbackRepository extends JpaRepository<ProactiveFeedbackEntity, UUID> {

    /**
     * Finds all feedback entries for a specific user.
     *
     * @param userId the user ID
     * @return list of feedback entities
     */
    List<ProactiveFeedbackEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Finds the most recent feedback for a user on a specific assessment.
     *
     * @param userId the user ID
     * @param assessmentId the assessment ID
     * @return optional feedback entity
     */
    Optional<ProactiveFeedbackEntity> findFirstByUserIdAndAssessmentIdOrderByCreatedAtDesc(UUID userId, UUID assessmentId);

    /**
     * Finds all feedback for a specific assessment.
     *
     * @param assessmentId the assessment ID
     * @return list of feedback entities
     */
    List<ProactiveFeedbackEntity> findByAssessmentIdOrderByCreatedAtDesc(UUID assessmentId);
}
