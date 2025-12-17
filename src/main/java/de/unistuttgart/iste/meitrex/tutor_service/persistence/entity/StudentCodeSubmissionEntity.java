package de.unistuttgart.iste.meitrex.tutor_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a student's code submission for an assignment.
 * Stores the latest code submission per student per assignment.
 */
@Entity
@Table(name = "student_code_submission", indexes = {
    @Index(name = "idx_student_code_submission_student", columnList = "student_id"),
    @Index(name = "idx_student_code_submission_assignment", columnList = "assignment_id"),
    @Index(name = "idx_student_code_submission_course", columnList = "course_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentCodeSubmissionEntity {

    @EmbeddedId
    private PrimaryKey primaryKey;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "repository_url", nullable = false, length = 500)
    private String repositoryUrl;

    @Column(name = "commit_sha", nullable = false, length = 100)
    private String commitSha;

    @Column(name = "commit_timestamp", nullable = false)
    private OffsetDateTime commitTimestamp;

    @Column(name = "branch", length = 200)
    private String branch;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "code_submission_files",
        joinColumns = {
            @JoinColumn(name = "student_id", referencedColumnName = "student_id"),
            @JoinColumn(name = "assignment_id", referencedColumnName = "assignment_id")
        }
    )
    @MapKeyColumn(name = "file_path", length = 500)
    @Column(name = "file_content", columnDefinition = "TEXT")
    private Map<String, String> files;

    @Column(name = "last_updated", nullable = false)
    private OffsetDateTime lastUpdated;

    /**
     * Composite primary key for student code submission.
     * Consists of student ID and assignment ID to ensure only one submission per student per assignment.
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrimaryKey implements Serializable {
        
        @Column(name = "student_id", nullable = false)
        private UUID studentId;

        @Column(name = "assignment_id", nullable = false)
        private UUID assignmentId;
    }
}
