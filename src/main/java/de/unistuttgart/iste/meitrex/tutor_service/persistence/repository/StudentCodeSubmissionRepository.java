package de.unistuttgart.iste.meitrex.tutor_service.persistence.repository;

import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.StudentCodeSubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for StudentCodeSubmissionEntity.
 * Provides database access for student code submission information.
 */
@Repository
public interface StudentCodeSubmissionRepository extends JpaRepository<StudentCodeSubmissionEntity, StudentCodeSubmissionEntity.PrimaryKey> {

    /**
     * Finds all code submissions for a specific student.
     * 
     * @param studentId the student's ID
     * @return list of code submissions for the student
     */
    List<StudentCodeSubmissionEntity> findByPrimaryKey_StudentId(UUID studentId);

    /**
     * Finds all code submissions for a specific assignment.
     * 
     * @param assignmentId the assignment's ID
     * @return list of code submissions for the assignment
     */
    List<StudentCodeSubmissionEntity> findByPrimaryKey_AssignmentId(UUID assignmentId);

    /**
     * Finds all code submissions for a specific course.
     * 
     * @param courseId the course's ID
     * @return list of code submissions in the course
     */
    List<StudentCodeSubmissionEntity> findByCourseId(UUID courseId);

    /**
     * Finds a specific code submission for a student and assignment.
     * 
     * @param studentId the student's ID
     * @param assignmentId the assignment's ID
     * @return Optional containing the code submission if found
     */
    Optional<StudentCodeSubmissionEntity> findByPrimaryKey_StudentIdAndPrimaryKey_AssignmentId(UUID studentId, UUID assignmentId);
}
