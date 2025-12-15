package de.unistuttgart.iste.meitrex.tutor_service.service;

import de.unistuttgart.iste.meitrex.common.event.ContentProgressedEvent;
import de.unistuttgart.iste.meitrex.common.event.HexadPlayerType;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.ProactiveFeedbackEntity;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.StudentCodeSubmissionEntity;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.UserPlayerTypeEntity;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.repository.ProactiveFeedbackRepository;
import de.unistuttgart.iste.meitrex.tutor_service.service.models.TutorAnswer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProactiveFeedbackService.
 */
@ExtendWith(MockitoExtension.class)
class ProactiveFeedbackServiceTest {

    @Mock
    private OllamaService ollamaService;

    @Mock
    private UserPlayerTypeService userPlayerTypeService;

    @Mock
    private UserSkillLevelService userSkillLevelService;

    @Mock
    private ProactiveFeedbackRepository proactiveFeedbackRepository;

    @Mock
    private StudentCodeSubmissionService studentCodeSubmissionService;

    @InjectMocks
    private ProactiveFeedbackService proactiveFeedbackService;

    private UUID userId;
    private UUID assignmentId;
    private UUID quizId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        assignmentId = UUID.randomUUID();
        quizId = UUID.randomUUID();
        
        ReflectionTestUtils.setField(proactiveFeedbackService, "skillLevelLowThreshold", 0.3);
        ReflectionTestUtils.setField(proactiveFeedbackService, "skillLevelHighThreshold", 0.7);
        ReflectionTestUtils.setField(proactiveFeedbackService, "correctnessLevelHigh", 0.8);
        ReflectionTestUtils.setField(proactiveFeedbackService, "correctnessLevelMax", 0.99);
    }

    @Test
    void testGenerateFeedback_forAssignmentWithCode() {
        ContentProgressedEvent event = ContentProgressedEvent.builder()
                .userId(userId)
                .contentId(assignmentId)
                .contentType(ContentProgressedEvent.ContentType.ASSIGNMENT)
                .correctness(0.85)
                .success(true)
                .build();

        String codeContext = "Repository: https://github.com/test/repo\nFile: Main.java\npublic class Main {}";
        String expectedFeedback = "Great job on your assignment! Your code shows good structure.";

        when(ollamaService.getTemplate(anyString())).thenReturn("Mock template");
        when(ollamaService.startQuery(eq(TutorAnswer.class), anyString(), anyList(), any()))
                .thenReturn(new TutorAnswer(expectedFeedback));
        when(userPlayerTypeService.getPrimaryPlayerType(userId)).thenReturn(Optional.empty());
        when(studentCodeSubmissionService.getCodeSubmissionContextForTutor(userId, assignmentId))
                .thenReturn(Optional.of(codeContext));
        when(proactiveFeedbackRepository.save(any())).thenAnswer(invocation -> {
            ProactiveFeedbackEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        String result = proactiveFeedbackService.generateFeedback(event);

        assertNotNull(result);
        assertEquals(expectedFeedback, result);
        verify(studentCodeSubmissionService, times(1))
                .getCodeSubmissionContextForTutor(userId, assignmentId);
        verify(proactiveFeedbackRepository, times(1)).save(any(ProactiveFeedbackEntity.class));
    }

    @Test
    void testGenerateFeedback_forQuizWithoutCode() {
        ContentProgressedEvent event = ContentProgressedEvent.builder()
                .userId(userId)
                .contentId(quizId)
                .contentType(ContentProgressedEvent.ContentType.QUIZ)
                .correctness(0.75)
                .success(true)
                .build();

        String expectedFeedback = "Good work on the quiz! You demonstrated solid understanding.";

        when(ollamaService.getTemplate(anyString())).thenReturn("Mock template");
        when(ollamaService.startQuery(eq(TutorAnswer.class), anyString(), anyList(), any()))
                .thenReturn(new TutorAnswer(expectedFeedback));
        when(userPlayerTypeService.getPrimaryPlayerType(userId)).thenReturn(Optional.empty());
        when(proactiveFeedbackRepository.save(any())).thenAnswer(invocation -> {
            ProactiveFeedbackEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        String result = proactiveFeedbackService.generateFeedback(event);

        assertNotNull(result);
        assertEquals(expectedFeedback, result);

        verify(studentCodeSubmissionService, never())
                .getCodeSubmissionContextForTutor(any(), any());
        verify(proactiveFeedbackRepository, times(1)).save(any(ProactiveFeedbackEntity.class));
    }

    @Test
    void testGenerateFeedback_forAssignmentWithoutCodeSubmission() {
        ContentProgressedEvent event = ContentProgressedEvent.builder()
                .userId(userId)
                .contentId(assignmentId)
                .contentType(ContentProgressedEvent.ContentType.ASSIGNMENT)
                .correctness(0.90)
                .success(true)
                .build();

        String expectedFeedback = "Excellent work on your assignment!";

        when(ollamaService.getTemplate(anyString())).thenReturn("Mock template");
        when(ollamaService.startQuery(eq(TutorAnswer.class), anyString(), anyList(), any()))
                .thenReturn(new TutorAnswer(expectedFeedback));
        when(userPlayerTypeService.getPrimaryPlayerType(userId)).thenReturn(Optional.empty());
        when(studentCodeSubmissionService.getCodeSubmissionContextForTutor(userId, assignmentId))
                .thenReturn(Optional.empty());
        when(proactiveFeedbackRepository.save(any())).thenAnswer(invocation -> {
            ProactiveFeedbackEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        String result = proactiveFeedbackService.generateFeedback(event);

        assertNotNull(result);
        assertEquals(expectedFeedback, result);
        verify(studentCodeSubmissionService, times(1))
                .getCodeSubmissionContextForTutor(userId, assignmentId);
        verify(proactiveFeedbackRepository, times(1)).save(any(ProactiveFeedbackEntity.class));
    }

    @Test
    void testGenerateFeedback_withPlayerType() {
        ContentProgressedEvent event = ContentProgressedEvent.builder()
                .userId(userId)
                .contentId(assignmentId)
                .contentType(ContentProgressedEvent.ContentType.ASSIGNMENT)
                .correctness(1.0)
                .success(true)
                .build();

        String expectedFeedback = "Perfect score! You're an achiever!";

        UserPlayerTypeEntity playerTypeEntity = UserPlayerTypeEntity.builder()
                .userId(userId)
                .primaryPlayerType(HexadPlayerType.ACHIEVER)
                .achieverScore(0.9)
                .playerScore(0.5)
                .socialiserScore(0.4)
                .freeSpiritScore(0.3)
                .philanthropistScore(0.6)
                .disruptorScore(0.2)
                .build();

        when(ollamaService.getTemplate(anyString())).thenReturn("Mock template");
        when(ollamaService.startQuery(eq(TutorAnswer.class), anyString(), anyList(), any()))
                .thenReturn(new TutorAnswer(expectedFeedback));
        when(userPlayerTypeService.getPrimaryPlayerType(userId))
                .thenReturn(Optional.of(HexadPlayerType.ACHIEVER));
        when(studentCodeSubmissionService.getCodeSubmissionContextForTutor(userId, assignmentId))
                .thenReturn(Optional.empty());
        when(proactiveFeedbackRepository.save(any())).thenAnswer(invocation -> {
            ProactiveFeedbackEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        String result = proactiveFeedbackService.generateFeedback(event);

        assertNotNull(result);
        assertEquals(expectedFeedback, result);
        verify(proactiveFeedbackRepository, times(1)).save(any(ProactiveFeedbackEntity.class));
    }
}
