package de.unistuttgart.iste.meitrex.tutor_service.service;

import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.StudentCodeSubmissionEntity;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.repository.StudentCodeSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StudentCodeSubmissionService.
 */
@ExtendWith(MockitoExtension.class)
class StudentCodeSubmissionServiceTest {

    @Mock
    private StudentCodeSubmissionRepository repository;

    @InjectMocks
    private StudentCodeSubmissionService service;

    private UUID studentId;
    private UUID assignmentId;
    private UUID courseId;
    private String repositoryUrl;
    private String commitSha;
    private OffsetDateTime commitTimestamp;
    private Map<String, String> files;
    private String branch;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        assignmentId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        repositoryUrl = "https://github.com/student/assignment-repo";
        commitSha = "abc123def456";
        commitTimestamp = OffsetDateTime.now();
        branch = "main";
        files = Map.of(
                "src/Main.java", "public class Main { public static void main(String[] args) {} }",
                "src/Utils.java", "public class Utils { }"
        );
    }

    /**
     * Test saving a new code submission.
     * The repository should save a new entity.
     */
    @Test
    void testSaveCodeSubmission_NewSubmission() {
        when(repository.findById(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.saveCodeSubmission(studentId, assignmentId, courseId, repositoryUrl, 
                commitSha, commitTimestamp, files, branch);

        verify(repository, times(1)).findById(any());
        verify(repository, times(1)).save(any());
    }

    /**
     * Test updating an existing code submission.
     * The repository should update the existing entity.
     */
    @Test
    void testSaveCodeSubmission_UpdateExisting() {
        StudentCodeSubmissionEntity existingEntity = StudentCodeSubmissionEntity.builder()
                .primaryKey(new StudentCodeSubmissionEntity.PrimaryKey(studentId, assignmentId))
                .courseId(courseId)
                .repositoryUrl(repositoryUrl)
                .commitSha("oldCommitSha")
                .commitTimestamp(commitTimestamp.minusDays(1))
                .files(Map.of("old/file.java", "old content"))
                .branch("old-branch")
                .lastUpdated(OffsetDateTime.now().minusDays(1))
                .build();

        when(repository.findById(any())).thenReturn(Optional.of(existingEntity));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.saveCodeSubmission(studentId, assignmentId, courseId, repositoryUrl, 
                commitSha, commitTimestamp, files, branch);

        verify(repository, times(1)).findById(any());
        verify(repository, times(1)).save(argThat(entity -> 
                entity.getCommitSha().equals(commitSha) &&
                entity.getFiles().size() == 2 &&
                entity.getBranch().equals(branch)
        ));
    }

    /**
     * Test retrieving a code submission by student and assignment IDs.
     */
    @Test
    void testGetCodeSubmission() {
        StudentCodeSubmissionEntity entity = StudentCodeSubmissionEntity.builder()
                .primaryKey(new StudentCodeSubmissionEntity.PrimaryKey(studentId, assignmentId))
                .courseId(courseId)
                .repositoryUrl(repositoryUrl)
                .commitSha(commitSha)
                .commitTimestamp(commitTimestamp)
                .files(files)
                .branch(branch)
                .lastUpdated(OffsetDateTime.now())
                .build();

        when(repository.findById(any())).thenReturn(Optional.of(entity));

        Optional<StudentCodeSubmissionEntity> result = service.getCodeSubmission(studentId, assignmentId);

        assertTrue(result.isPresent());
        assertEquals(commitSha, result.get().getCommitSha());
        assertEquals(2, result.get().getFiles().size());
        verify(repository, times(1)).findById(any());
    }

    /**
     * Test getting code submission context for a tutor.
     */
    @Test
    void testGetCodeSubmissionContextForTutor() {
        StudentCodeSubmissionEntity entity = StudentCodeSubmissionEntity.builder()
                .primaryKey(new StudentCodeSubmissionEntity.PrimaryKey(studentId, assignmentId))
                .courseId(courseId)
                .repositoryUrl(repositoryUrl)
                .commitSha(commitSha)
                .commitTimestamp(commitTimestamp)
                .files(files)
                .branch(branch)
                .lastUpdated(OffsetDateTime.now())
                .build();

        when(repository.findById(any())).thenReturn(Optional.of(entity));

        Optional<String> context = service.getCodeSubmissionContextForTutor(studentId, assignmentId);

        assertTrue(context.isPresent());
        String contextString = context.get();
        assertTrue(contextString.contains(repositoryUrl));
        assertTrue(contextString.contains(commitSha));
        assertTrue(contextString.contains(branch));
        assertTrue(contextString.contains("src/Main.java"));
        assertTrue(contextString.contains("src/Utils.java"));
        assertTrue(contextString.contains("public class Main"));
        verify(repository, times(1)).findById(any());
    }

    /**
     * Test getting code submission context for a tutor when no submission exists.
     */
    @Test
    void testGetCodeSubmissionContextForTutor_NotFound() {
        when(repository.findById(any())).thenReturn(Optional.empty());

        Optional<String> context = service.getCodeSubmissionContextForTutor(studentId, assignmentId);

        assertFalse(context.isPresent());
        verify(repository, times(1)).findById(any());
    }
}
