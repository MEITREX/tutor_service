package de.unistuttgart.iste.meitrex.tutor_service.persistence.repository;

import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.UserSkillLevelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for UserSkillLevelEntity.
 * Provides database access for user skill level information.
 */
@Repository
public interface UserSkillLevelRepository extends JpaRepository<UserSkillLevelEntity, UserSkillLevelEntity.UserSkillLevelId> {
    
    /**
     * Find all skill levels for a specific user.
     * @param userId the ID of the user
     * @return list of all skill levels for the user
     */
    List<UserSkillLevelEntity> findByUserId(UUID userId);
}
