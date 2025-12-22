package de.unistuttgart.iste.meitrex.tutor_service.controller;

import de.unistuttgart.iste.meitrex.common.event.ContentProgressedEvent;
import de.unistuttgart.iste.meitrex.common.event.HexadPlayerType;
import de.unistuttgart.iste.meitrex.common.event.StudentCodeSubmittedEvent;
import de.unistuttgart.iste.meitrex.common.event.UserHexadPlayerTypeSetEvent;
import de.unistuttgart.iste.meitrex.common.event.skilllevels.UserSkillLevelChangedEvent;
import de.unistuttgart.iste.meitrex.tutor_service.config.StudentCodeSubmissionConfig;
import de.unistuttgart.iste.meitrex.tutor_service.service.ProactiveFeedbackService;
import de.unistuttgart.iste.meitrex.tutor_service.service.StudentCodeSubmissionService;
import de.unistuttgart.iste.meitrex.tutor_service.service.UserPlayerTypeService;
import de.unistuttgart.iste.meitrex.tutor_service.service.UserSkillLevelService;
import io.dapr.client.domain.CloudEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SubscriptionController.
 * Tests all Dapr pub/sub event handlers.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock
    private UserPlayerTypeService userPlayerTypeService;

    @Mock
    private UserSkillLevelService userSkillLevelService;

    @Mock
    private ProactiveFeedbackService proactiveFeedbackService;

    @Mock
    private StudentCodeSubmissionService studentCodeSubmissionService;

    @Mock
    private StudentCodeSubmissionConfig studentCodeSubmissionConfig;

    @InjectMocks
    private SubscriptionController subscriptionController;

    private Map<String, String> headers;
    private UUID userId;
    private UUID skillId;
    private UUID contentId;
    private UUID assignmentId;
    private UUID courseId;

    @BeforeEach
    void setUp() {
        headers = new HashMap<>();
        headers.put("Content-Type", "application/cloudevents+json");
        
        userId = UUID.randomUUID();
        skillId = UUID.randomUUID();
        contentId = UUID.randomUUID();
        assignmentId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        
        lenient().when(studentCodeSubmissionConfig.getFileEndings())
                .thenReturn(List.of(".java", ".kt", ".py"));
    }

    @Test
    void testOnUserHexadPlayerTypeSetEvent_SavesPlayerType() {
        Map<HexadPlayerType, Double> playerTypePercentages = new HashMap<>();
        playerTypePercentages.put(HexadPlayerType.ACHIEVER, 0.8);
        playerTypePercentages.put(HexadPlayerType.PLAYER, 0.6);
        playerTypePercentages.put(HexadPlayerType.SOCIALISER, 0.4);
        playerTypePercentages.put(HexadPlayerType.FREE_SPIRIT, 0.5);
        playerTypePercentages.put(HexadPlayerType.PHILANTHROPIST, 0.7);
        playerTypePercentages.put(HexadPlayerType.DISRUPTOR, 0.3);

        UserHexadPlayerTypeSetEvent event = mock(UserHexadPlayerTypeSetEvent.class);
        when(event.getUserId()).thenReturn(userId);
        when(event.getPrimaryPlayerType()).thenReturn(HexadPlayerType.ACHIEVER);
        when(event.getPlayerTypePercentages()).thenReturn(playerTypePercentages);

        CloudEvent<UserHexadPlayerTypeSetEvent> cloudEvent = mock(CloudEvent.class);
        when(cloudEvent.getData()).thenReturn(event);

        doNothing().when(userPlayerTypeService).saveUserPlayerType(any(), any(), any());

        assertDoesNotThrow(() -> subscriptionController.onUserHexadPlayerTypeSetEvent(cloudEvent, headers).block());

        verify(userPlayerTypeService, times(1)).saveUserPlayerType(
                eq(userId),
                eq(HexadPlayerType.ACHIEVER),
                argThat(percentages -> 
                        percentages.get(HexadPlayerType.ACHIEVER) == 0.8 &&
                        percentages.get(HexadPlayerType.PLAYER) == 0.6)
        );
    }

    @Test
    void testOnUserSkillLevelChangedEvent_SavesSkillLevel() {
        Float skillLevelValue = 0.75f;

        UserSkillLevelChangedEvent event = UserSkillLevelChangedEvent.builder()
                .userId(userId)
                .skillId(skillId)
                .newValue(skillLevelValue)
                .build();

        CloudEvent<UserSkillLevelChangedEvent> cloudEvent = mock(CloudEvent.class);
        when(cloudEvent.getData()).thenReturn(event);

        doNothing().when(userSkillLevelService).saveUserSkillLevel(any(), any(), any());

        assertDoesNotThrow(() -> subscriptionController.onUserSkillLevelChangedEvent(cloudEvent, headers).block());

        verify(userSkillLevelService, times(1)).saveUserSkillLevel(
                eq(userId),
                eq(skillId),
                eq(skillLevelValue)
        );
    }

    @Test
    void testOnContentProgressedEvent_Assignment_GeneratesFeedback() {
        ContentProgressedEvent event = ContentProgressedEvent.builder()
                .userId(userId)
                .contentId(contentId)
                .contentType(ContentProgressedEvent.ContentType.ASSIGNMENT)
                .correctness(0.85)
                .success(true)
                .build();

        CloudEvent<ContentProgressedEvent> cloudEvent = mock(CloudEvent.class);
        when(cloudEvent.getData()).thenReturn(event);

        when(proactiveFeedbackService.generateFeedback(any())).thenReturn("Feedback generated");

        assertDoesNotThrow(() -> subscriptionController.onContentProgressedEvent(cloudEvent, headers).block());

        verify(proactiveFeedbackService, times(1)).generateFeedback(event);
    }

    @Test
    void testOnContentProgressedEvent_Quiz_GeneratesFeedback() {
        ContentProgressedEvent event = ContentProgressedEvent.builder()
                .userId(userId)
                .contentId(contentId)
                .contentType(ContentProgressedEvent.ContentType.QUIZ)
                .correctness(0.90)
                .success(true)
                .build();

        CloudEvent<ContentProgressedEvent> cloudEvent = mock(CloudEvent.class);
        when(cloudEvent.getData()).thenReturn(event);

        when(proactiveFeedbackService.generateFeedback(any())).thenReturn("Feedback generated");

        assertDoesNotThrow(() -> subscriptionController.onContentProgressedEvent(cloudEvent, headers).block());

        verify(proactiveFeedbackService, times(1)).generateFeedback(event);
    }

    @Test
    void testOnContentProgressedEvent_NonAssignmentOrQuiz_NoFeedback() {
        ContentProgressedEvent event = mock(ContentProgressedEvent.class);
        when(event.getUserId()).thenReturn(userId);
        when(event.getContentId()).thenReturn(contentId);
        when(event.getContentType()).thenReturn(null); // Will cause NPE internally, caught by exception handler

        CloudEvent<ContentProgressedEvent> cloudEvent = mock(CloudEvent.class);
        when(cloudEvent.getData()).thenReturn(event);

        assertDoesNotThrow(() -> subscriptionController.onContentProgressedEvent(cloudEvent, headers).block());

        verify(proactiveFeedbackService, never()).generateFeedback(any());
    }

    @Test
    void testOnContentProgressedEvent_FeedbackGenerationThrowsException_HandlesGracefully() {
        ContentProgressedEvent event = ContentProgressedEvent.builder()
                .userId(userId)
                .contentId(contentId)
                .contentType(ContentProgressedEvent.ContentType.ASSIGNMENT)
                .correctness(0.85)
                .success(true)
                .build();

        CloudEvent<ContentProgressedEvent> cloudEvent = mock(CloudEvent.class);
        when(cloudEvent.getData()).thenReturn(event);

        when(proactiveFeedbackService.generateFeedback(any()))
                .thenThrow(new RuntimeException("AI service unavailable"));

        assertDoesNotThrow(() -> subscriptionController.onContentProgressedEvent(cloudEvent, headers).block());

        verify(proactiveFeedbackService, times(1)).generateFeedback(event);
    }

    @Test
    void testOnStudentCodeSubmittedEvent_SavesCodeSubmission() {
        Map<String, String> files = new HashMap<>();
        files.put("src/Main.java", "public class Main {}");
        files.put("src/Helper.java", "public class Helper {}");

        OffsetDateTime commitTimestamp = OffsetDateTime.now();
        String commitSha = "abc123def456";
        String repositoryUrl = "https://github.com/student/repo";
        String branch = "main";

        StudentCodeSubmittedEvent event = StudentCodeSubmittedEvent.builder()
                .studentId(userId)
                .assignmentId(assignmentId)
                .courseId(courseId)
                .repositoryUrl(repositoryUrl)
                .commitSha(commitSha)
                .commitTimestamp(commitTimestamp)
                .files(files)
                .branch(branch)
                .build();

        CloudEvent<StudentCodeSubmittedEvent> cloudEvent = mock(CloudEvent.class);
        when(cloudEvent.getData()).thenReturn(event);

        doNothing().when(studentCodeSubmissionService).saveCodeSubmission(
                any(), any(), any(), any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> subscriptionController.onStudentCodeSubmittedEvent(cloudEvent, headers).block());

        verify(studentCodeSubmissionService, times(1)).saveCodeSubmission(
                eq(userId),
                eq(assignmentId),
                eq(courseId),
                eq(repositoryUrl),
                eq(commitSha),
                eq(commitTimestamp),
                argThat(filteredFiles -> filteredFiles.size() == 2 
                        && filteredFiles.containsKey("src/Main.java")
                        && filteredFiles.containsKey("src/Helper.java")),
                eq(branch)
        );
    }

    @Test
    void testOnStudentCodeSubmittedEvent_SaveThrowsException_HandlesGracefully() {
        Map<String, String> files = new HashMap<>();
        files.put("src/Main.java", "public class Main {}");

        StudentCodeSubmittedEvent event = StudentCodeSubmittedEvent.builder()
                .studentId(userId)
                .assignmentId(assignmentId)
                .courseId(courseId)
                .repositoryUrl("https://github.com/student/repo")
                .commitSha("abc123")
                .commitTimestamp(OffsetDateTime.now())
                .files(files)
                .branch("main")
                .build();

        CloudEvent<StudentCodeSubmittedEvent> cloudEvent = mock(CloudEvent.class);
        when(cloudEvent.getData()).thenReturn(event);

        doThrow(new RuntimeException("Database error"))
                .when(studentCodeSubmissionService).saveCodeSubmission(
                        any(), any(), any(), any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> subscriptionController.onStudentCodeSubmittedEvent(cloudEvent, headers).block());

        verify(studentCodeSubmissionService, times(1)).saveCodeSubmission(
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testMultiplePlayerTypes_ConvertsFloatToDouble() {
        Map<HexadPlayerType, Double> playerTypePercentages = new HashMap<>();
        playerTypePercentages.put(HexadPlayerType.ACHIEVER, 0.3);
        playerTypePercentages.put(HexadPlayerType.PLAYER, 0.7);
        playerTypePercentages.put(HexadPlayerType.SOCIALISER, 0.5);
        playerTypePercentages.put(HexadPlayerType.FREE_SPIRIT, 0.6);
        playerTypePercentages.put(HexadPlayerType.PHILANTHROPIST, 0.8);
        playerTypePercentages.put(HexadPlayerType.DISRUPTOR, 0.4);

        UserHexadPlayerTypeSetEvent event = mock(UserHexadPlayerTypeSetEvent.class);
        when(event.getUserId()).thenReturn(userId);
        when(event.getPrimaryPlayerType()).thenReturn(HexadPlayerType.PHILANTHROPIST);
        when(event.getPlayerTypePercentages()).thenReturn(playerTypePercentages);

        CloudEvent<UserHexadPlayerTypeSetEvent> cloudEvent = mock(CloudEvent.class);
        when(cloudEvent.getData()).thenReturn(event);

        assertDoesNotThrow(() -> subscriptionController.onUserHexadPlayerTypeSetEvent(cloudEvent, headers).block());

        verify(userPlayerTypeService, times(1)).saveUserPlayerType(
                eq(userId),
                eq(HexadPlayerType.PHILANTHROPIST),
                argThat(percentages -> 
                        percentages.size() == 6 &&
                        percentages.get(HexadPlayerType.PHILANTHROPIST) == 0.8 &&
                        percentages.get(HexadPlayerType.DISRUPTOR) == 0.4)
        );
    }

    @Test
    void testOnContentProgressedEvent_NullEventData_HandlesGracefully() {
        CloudEvent<ContentProgressedEvent> cloudEvent = mock(CloudEvent.class);
        when(cloudEvent.getData()).thenReturn(null);

        assertDoesNotThrow(() -> subscriptionController.onContentProgressedEvent(cloudEvent, headers).block());

        verify(proactiveFeedbackService, never()).generateFeedback(any());
    }
    
    @Test
    void testFileFiltering_OnlyJavaFiles_AllSaved() {
        Map<String, String> files = new HashMap<>();
        files.put("src/Main.java", "public class Main {}");
        files.put("src/Helper.java", "public class Helper {}");
        files.put("src/Utils.java", "public class Utils {}");

        StudentCodeSubmittedEvent event = createCodeSubmissionEvent(files);
        CloudEvent<StudentCodeSubmittedEvent> cloudEvent = mock(CloudEvent.class);
        when(cloudEvent.getData()).thenReturn(event);

        assertDoesNotThrow(() -> subscriptionController.onStudentCodeSubmittedEvent(cloudEvent, headers).block());

        verify(studentCodeSubmissionService, times(1)).saveCodeSubmission(
                any(), any(), any(), any(), any(), any(),
                argThat(filteredFiles -> filteredFiles.size() == 3),
                any()
        );
    }

    @Test
    void testFileFiltering_MixedFileTypes_OnlyAllowedSaved() {
        Map<String, String> files = new HashMap<>();
        files.put("src/Main.java", "public class Main {}");
        files.put("src/Helper.kt", "class Helper {}");
        files.put("script.py", "print('hello')");
        files.put("README.md", "# Documentation");
        files.put("package.json", "{}");
        files.put(".gitignore", "*.class");

        StudentCodeSubmittedEvent event = createCodeSubmissionEvent(files);
        CloudEvent<StudentCodeSubmittedEvent> cloudEvent = mock(CloudEvent.class);
        when(cloudEvent.getData()).thenReturn(event);

        assertDoesNotThrow(() -> subscriptionController.onStudentCodeSubmittedEvent(cloudEvent, headers).block());

        verify(studentCodeSubmissionService, times(1)).saveCodeSubmission(
                any(), any(), any(), any(), any(), any(),
                argThat(filteredFiles -> 
                        filteredFiles.size() == 3 
                        && filteredFiles.containsKey("src/Main.java")
                        && filteredFiles.containsKey("src/Helper.kt")
                        && filteredFiles.containsKey("script.py")
                        && !filteredFiles.containsKey("README.md")
                        && !filteredFiles.containsKey("package.json")),
                any()
        );
    }

    @Test
    void testFileFiltering_NullFilename_Filtered() {
        Map<String, String> files = new HashMap<>();
        files.put("src/Main.java", "public class Main {}");
        files.put(null, "some content");

        StudentCodeSubmittedEvent event = createCodeSubmissionEvent(files);
        CloudEvent<StudentCodeSubmittedEvent> cloudEvent = mock(CloudEvent.class);
        when(cloudEvent.getData()).thenReturn(event);

        assertDoesNotThrow(() -> subscriptionController.onStudentCodeSubmittedEvent(cloudEvent, headers).block());

        verify(studentCodeSubmissionService, times(1)).saveCodeSubmission(
                any(), any(), any(), any(), any(), any(),
                argThat(filteredFiles -> filteredFiles.size() == 1 && filteredFiles.containsKey("src/Main.java")),
                any()
        );
    }

    @Test
    void testFileFiltering_NullContent_Filtered() {
        Map<String, String> files = new HashMap<>();
        files.put("src/Main.java", "public class Main {}");
        files.put("src/Empty.java", null);

        StudentCodeSubmittedEvent event = createCodeSubmissionEvent(files);
        CloudEvent<StudentCodeSubmittedEvent> cloudEvent = mock(CloudEvent.class);
        when(cloudEvent.getData()).thenReturn(event);

        assertDoesNotThrow(() -> subscriptionController.onStudentCodeSubmittedEvent(cloudEvent, headers).block());

        verify(studentCodeSubmissionService, times(1)).saveCodeSubmission(
                any(), any(), any(), any(), any(), any(),
                argThat(filteredFiles -> filteredFiles.size() == 1 && filteredFiles.containsKey("src/Main.java")),
                any()
        );
    }

    @Test
    void testFileFiltering_NoMatchingFileEndings_EmptyMap() {
        Map<String, String> files = new HashMap<>();
        files.put("README.md", "# Documentation");
        files.put("package.json", "{}");
        files.put("Dockerfile", "FROM openjdk");

        StudentCodeSubmittedEvent event = createCodeSubmissionEvent(files);
        CloudEvent<StudentCodeSubmittedEvent> cloudEvent = mock(CloudEvent.class);
        when(cloudEvent.getData()).thenReturn(event);

        assertDoesNotThrow(() -> subscriptionController.onStudentCodeSubmittedEvent(cloudEvent, headers).block());

        verify(studentCodeSubmissionService, times(1)).saveCodeSubmission(
                any(), any(), any(), any(), any(), any(),
                argThat(Map::isEmpty),
                any()
        );
    }

    @Test
    void testFileFiltering_NullFilesMap_EmptyMap() {
        StudentCodeSubmittedEvent event = createCodeSubmissionEvent(null);
        CloudEvent<StudentCodeSubmittedEvent> cloudEvent = mock(CloudEvent.class);
        when(cloudEvent.getData()).thenReturn(event);

        assertDoesNotThrow(() -> subscriptionController.onStudentCodeSubmittedEvent(cloudEvent, headers).block());

        verify(studentCodeSubmissionService, times(1)).saveCodeSubmission(
                any(), any(), any(), any(), any(), any(),
                argThat(Map::isEmpty),
                any()
        );
    }

    @Test
    void testFileFiltering_EmptyFilesMap_EmptyMap() {
        Map<String, String> files = new HashMap<>();

        StudentCodeSubmittedEvent event = createCodeSubmissionEvent(files);
        CloudEvent<StudentCodeSubmittedEvent> cloudEvent = mock(CloudEvent.class);
        when(cloudEvent.getData()).thenReturn(event);

        assertDoesNotThrow(() -> subscriptionController.onStudentCodeSubmittedEvent(cloudEvent, headers).block());

        verify(studentCodeSubmissionService, times(1)).saveCodeSubmission(
                any(), any(), any(), any(), any(), any(),
                argThat(Map::isEmpty),
                any()
        );
    }

    @Test
    void testFileFiltering_SingleFileEndingConfiguration_OnlyThatTypeSaved() {
        // Configure to only accept .java files
        when(studentCodeSubmissionConfig.getFileEndings()).thenReturn(List.of(".java"));

        Map<String, String> files = new HashMap<>();
        files.put("src/Main.java", "public class Main {}");
        files.put("src/Helper.kt", "class Helper {}");
        files.put("script.py", "print('hello')");

        StudentCodeSubmittedEvent event = createCodeSubmissionEvent(files);
        CloudEvent<StudentCodeSubmittedEvent> cloudEvent = mock(CloudEvent.class);
        when(cloudEvent.getData()).thenReturn(event);

        assertDoesNotThrow(() -> subscriptionController.onStudentCodeSubmittedEvent(cloudEvent, headers).block());

        verify(studentCodeSubmissionService, times(1)).saveCodeSubmission(
                any(), any(), any(), any(), any(), any(),
                argThat(filteredFiles -> 
                        filteredFiles.size() == 1 
                        && filteredFiles.containsKey("src/Main.java")),
                any()
        );
    }

    @Test
    void testFileFiltering_CaseSensitiveFileEndings() {
        Map<String, String> files = new HashMap<>();
        files.put("src/Main.java", "public class Main {}");
        files.put("src/Helper.JAVA", "public class Helper {}"); // uppercase extension

        StudentCodeSubmittedEvent event = createCodeSubmissionEvent(files);
        CloudEvent<StudentCodeSubmittedEvent> cloudEvent = mock(CloudEvent.class);
        when(cloudEvent.getData()).thenReturn(event);

        assertDoesNotThrow(() -> subscriptionController.onStudentCodeSubmittedEvent(cloudEvent, headers).block());

        verify(studentCodeSubmissionService, times(1)).saveCodeSubmission(
                any(), any(), any(), any(), any(), any(),
                argThat(filteredFiles -> 
                        filteredFiles.size() == 1 
                        && filteredFiles.containsKey("src/Main.java")
                        && !filteredFiles.containsKey("src/Helper.JAVA")),
                any()
        );
    }

    @Test
    void testFileFiltering_MultipleDotsInFilename() {
        Map<String, String> files = new HashMap<>();
        files.put("src/Main.test.java", "public class MainTest {}");
        files.put("src/config.prod.py", "config = {}");
        files.put("src/data.backup.json", "{}");

        StudentCodeSubmittedEvent event = createCodeSubmissionEvent(files);
        CloudEvent<StudentCodeSubmittedEvent> cloudEvent = mock(CloudEvent.class);
        when(cloudEvent.getData()).thenReturn(event);

        assertDoesNotThrow(() -> subscriptionController.onStudentCodeSubmittedEvent(cloudEvent, headers).block());

        verify(studentCodeSubmissionService, times(1)).saveCodeSubmission(
                any(), any(), any(), any(), any(), any(),
                argThat(filteredFiles -> 
                        filteredFiles.size() == 2 
                        && filteredFiles.containsKey("src/Main.test.java")
                        && filteredFiles.containsKey("src/config.prod.py")
                        && !filteredFiles.containsKey("src/data.backup.json")),
                any()
        );
    }

    /**
     * Helper method to create a StudentCodeSubmittedEvent with the given files.
     */
    private StudentCodeSubmittedEvent createCodeSubmissionEvent(Map<String, String> files) {
        return StudentCodeSubmittedEvent.builder()
                .studentId(userId)
                .assignmentId(assignmentId)
                .courseId(courseId)
                .repositoryUrl("https://github.com/student/repo")
                .commitSha("abc123")
                .commitTimestamp(OffsetDateTime.now())
                .files(files)
                .branch("main")
                .build();
    }
}
