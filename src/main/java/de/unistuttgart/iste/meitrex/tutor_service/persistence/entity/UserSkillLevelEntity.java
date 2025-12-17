package de.unistuttgart.iste.meitrex.tutor_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * Entity representing a user's skill level for a specific skill.
 * Stores the userId, skillId, and the skill level value.
 */
@Entity
@Table(name = "user_skill_level", indexes = {
    @Index(name = "idx_user_skill_level_user_id", columnList = "user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(UserSkillLevelEntity.UserSkillLevelId.class)
public class UserSkillLevelEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "skill_id", nullable = false)
    private UUID skillId;

    @Column(name = "skill_level_value", nullable = false)
    private Float skillLevelValue;

    /**
     * Composite primary key for UserSkillLevelEntity.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSkillLevelId implements Serializable {
        private UUID userId;
        private UUID skillId;
    }
}
