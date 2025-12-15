package de.unistuttgart.iste.meitrex.tutor_service.service;

import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.StudentCodeSubmissionEntity;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.repository.StudentCodeSubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing student code submissions.
 * Handles saving and retrieving code submission data from events.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StudentCodeSubmissionService {

    private final StudentCodeSubmissionRepository studentCodeSubmissionRepository;

    /**
     * Saves or updates a student's code submission for an assignment.
     * Only keeps the latest submission per student per assignment.
     * 
     * @param studentId the student's ID
     * @param assignmentId the assignment's ID
     * @param courseId the course's ID
     * @param repositoryUrl the GitHub repository URL
     * @param commitSha the commit SHA
     * @param commitTimestamp the timestamp of the commit
     * @param files map of file paths to file contents
     * @param branch the branch name
     */
    @Transactional
    public void saveCodeSubmission(
            UUID studentId,
            UUID assignmentId,
            UUID courseId,
            String repositoryUrl,
            String commitSha,
            OffsetDateTime commitTimestamp,
            Map<String, String> files,
            String branch) {
        
        StudentCodeSubmissionEntity.PrimaryKey primaryKey = 
            new StudentCodeSubmissionEntity.PrimaryKey(studentId, assignmentId);
        
        Optional<StudentCodeSubmissionEntity> existingEntity = studentCodeSubmissionRepository.findById(primaryKey);
        
        StudentCodeSubmissionEntity entity;
        if (existingEntity.isPresent()) {
            entity = existingEntity.get();
        } else {
            entity = new StudentCodeSubmissionEntity();
            entity.setPrimaryKey(primaryKey);
        }
        
        entity.setCourseId(courseId);
        entity.setRepositoryUrl(repositoryUrl);
        entity.setCommitSha(commitSha);
        entity.setCommitTimestamp(commitTimestamp);
        entity.setFiles(files);
        entity.setBranch(branch);
        entity.setLastUpdated(OffsetDateTime.now());
        
        studentCodeSubmissionRepository.save(entity);
        
        log.info("Successfully saved code submission for student {} on assignment {}", studentId, assignmentId);
    }

    /**
     * Retrieves a student's code submission for a specific assignment.
     * 
     * @param studentId the student's ID
     * @param assignmentId the assignment's ID
     * @return Optional containing the code submission if found
     */
    public Optional<StudentCodeSubmissionEntity> getCodeSubmission(UUID studentId, UUID assignmentId) {
        StudentCodeSubmissionEntity.PrimaryKey primaryKey = 
            new StudentCodeSubmissionEntity.PrimaryKey(studentId, assignmentId);
        return studentCodeSubmissionRepository.findById(primaryKey);
    }

    /**
     * Retrieves all code submissions for a specific student.
     * 
     * @param studentId the student's ID
     * @return list of code submissions for the student
     */
    public java.util.List<StudentCodeSubmissionEntity> getCodeSubmissionsForStudent(UUID studentId) {
        return studentCodeSubmissionRepository.findByPrimaryKey_StudentId(studentId);
    }

    /**
     * Retrieves all code submissions for a specific assignment.
     * 
     * @param assignmentId the assignment's ID
     * @return list of code submissions for the assignment
     */
    public java.util.List<StudentCodeSubmissionEntity> getCodeSubmissionsForAssignment(UUID assignmentId) {
        return studentCodeSubmissionRepository.findByPrimaryKey_AssignmentId(assignmentId);
    }

    /**
     * Retrieves all code submissions for a specific course.
     * 
     * @param courseId the course's ID
     * @return list of code submissions in the course
     */
    public java.util.List<StudentCodeSubmissionEntity> getCodeSubmissionsForCourse(UUID courseId) {
        return studentCodeSubmissionRepository.findByCourseId(courseId);
    }

    /**
     * Retrieves the latest code submission for a student on an assignment as a formatted string.
     * This method is designed to be used by the TutorService to provide context about student code.
     * 
     * The returned string includes:
     * - Repository information
     * - Commit details
     * - All source files with their contents
     * 
     * @param studentId the student's ID
     * @param assignmentId the assignment's ID
     * @return Optional containing a formatted string with code submission details, or empty if not found
     */
    public Optional<String> getCodeSubmissionContextForTutor(UUID studentId, UUID assignmentId) {
        return getCodeSubmission(studentId, assignmentId)
                .map(this::formatCodeSubmissionForTutor);
    }

    /**
     * Formats a code submission entity into a human-readable string for the AI tutor.
     * 
     * @param submission the code submission entity
     * @return formatted string containing all relevant code submission information
     */
    private String formatCodeSubmissionForTutor(StudentCodeSubmissionEntity submission) {
        StringBuilder context = new StringBuilder();
        
        context.append("Repository: ").append(submission.getRepositoryUrl()).append("\n");
        context.append("Branch: ").append(submission.getBranch()).append("\n");
        context.append("Commit: ").append(submission.getCommitSha()).append("\n");
        
        context.append("Source Code Files:\n");
        context.append("==================\n\n");
        
        submission.getFiles().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String filePath = entry.getKey();
                    String fileContent = entry.getValue();
                    
                    context.append("File: ").append(filePath).append("\n");
                    context.append("---\n");
                    context.append(fileContent);
                    context.append("\n\n");
                });
        
        return context.toString();
    }
}
