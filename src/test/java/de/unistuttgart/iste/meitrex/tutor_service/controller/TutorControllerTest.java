package de.unistuttgart.iste.meitrex.tutor_service.controller;

import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.ProactiveFeedbackEntity;
import de.unistuttgart.iste.meitrex.tutor_service.service.HintService;
import de.unistuttgart.iste.meitrex.tutor_service.service.ProactiveFeedbackService;
import de.unistuttgart.iste.meitrex.tutor_service.service.TutorService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipInCourseWithId;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TutorControllerTest {

    @InjectMocks
    private TutorController tutorController;

    @Mock
    private TutorService tutorService;

    @Mock
    private HintService hintService;

    @Mock
    private ProactiveFeedbackService proactiveFeedbackService;

    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, LoggedInUser.UserRoleInCourse.STUDENT);

    @Test
    void testSendMessage_withEmptyInput() {
        LectureQuestionResponse answer = tutorController.sendMessage("", courseId, loggedInUser);
        assertEquals("An empty message cannot be answered.", answer.getAnswer());
    }

    @Test
    void testSendMessage_withValidInput() {
        String question = "How do i upload my assignments?";
        LectureQuestionResponse expectedResponse = new LectureQuestionResponse(
                "Response for the question", List.of());
        when(tutorService.handleUserQuestion(question, courseId, loggedInUser)).thenReturn(expectedResponse);

        LectureQuestionResponse answer = tutorController.sendMessage(question, courseId, loggedInUser);

        assertEquals(expectedResponse, answer);
        verify(tutorService).handleUserQuestion(question, courseId, loggedInUser);
    }

    @Test
    void testGenerateHint_withMultipleChoice() {
        HintMultipleChoiceInput multipleChoiceInput = HintMultipleChoiceInput.builder()
                .setText("What is Java?")
                .setAnswers(List.of("A programming language", "A type of coffee", "An island"))
                .build();
        
        HintGenerationInput input = HintGenerationInput.builder()
                .setType(HintQuestionType.MULTIPLE_CHOICE)
                .setMultipleChoice(multipleChoiceInput)
                .build();

        HintResponse expectedResponse = new HintResponse("Java is a something a programmer uses...");
        when(hintService.generateHintWithQuestion(input, courseId, loggedInUser))
                .thenReturn(expectedResponse);

        HintResponse response = tutorController.generateHint(input, courseId, loggedInUser);

        assertEquals(expectedResponse, response);
        verify(hintService).generateHintWithQuestion(input, courseId, loggedInUser);
    }

    @Test
    void testGenerateHint_withCloze() {
        HintClozeInput clozeInput = HintClozeInput.builder()
                .setText("Java is a ____ language")
                .setBlanks(List.of("programming", "scripting", "markup"))
                .build();
        
        HintGenerationInput input = HintGenerationInput.builder()
                .setType(HintQuestionType.CLOZE)
                .setCloze(clozeInput)
                .build();

        HintResponse expectedResponse = new HintResponse("Think about what Java is commonly used for...");
        when(hintService.generateHintWithQuestion(input, courseId, loggedInUser))
                .thenReturn(expectedResponse);

        HintResponse response = tutorController.generateHint(input, courseId, loggedInUser);

        assertEquals(expectedResponse, response);
        verify(hintService).generateHintWithQuestion(input, courseId, loggedInUser);
    }

    @Test
    void testGenerateHint_withAssociation() {
        HintAssociationInput associationInput = HintAssociationInput.builder()
                .setText("Match the concepts")
                .setPairs(List.of(
                        new AssociationPairInput("JVM", "Java Virtual Machine"),
                        new AssociationPairInput("JDK", "Java Development Kit")
                ))
                .build();
        
        HintGenerationInput input = HintGenerationInput.builder()
                .setType(HintQuestionType.ASSOCIATION)
                .setAssociation(associationInput)
                .build();

        HintResponse expectedResponse = new HintResponse("Consider what each acronym stands for...");
        when(hintService.generateHintWithQuestion(input, courseId, loggedInUser))
                .thenReturn(expectedResponse);

        HintResponse response = tutorController.generateHint(input, courseId, loggedInUser);

        assertEquals(expectedResponse, response);
        verify(hintService).generateHintWithQuestion(input, courseId, loggedInUser);
    }

    @Test
    void testProactiveFeedback_withExistingFeedback() {
        UUID assessmentId = UUID.randomUUID();
        ProactiveFeedbackEntity entity = ProactiveFeedbackEntity.builder()
                .id(UUID.randomUUID())
                .userId(loggedInUser.getId())
                .assessmentId(assessmentId)
                .feedbackText("Great work!")
                .correctness(0.85)
                .success(true)
                .createdAt(OffsetDateTime.now())
                .build();

        when(proactiveFeedbackService.getFeedbackForAssignment(loggedInUser.getId(), assessmentId))
                .thenReturn(Optional.of(entity));

        ProactiveFeedback result = tutorController.proactiveFeedback(assessmentId, loggedInUser);

        assertNotNull(result);
        assertEquals(entity.getId(), result.getId());
        assertEquals(entity.getFeedbackText(), result.getFeedbackText());
        assertEquals(entity.getCorrectness(), result.getCorrectness());
    }

    @Test
    void testProactiveFeedback_withNoFeedback() {
        UUID assessmentId = UUID.randomUUID();
        when(proactiveFeedbackService.getFeedbackForAssignment(loggedInUser.getId(), assessmentId))
                .thenReturn(Optional.empty());

        ProactiveFeedback result = tutorController.proactiveFeedback(assessmentId, loggedInUser);

        assertNull(result);
    }

    @Test
    void testAllProactiveFeedback() {
        ProactiveFeedbackEntity entity1 = ProactiveFeedbackEntity.builder()
                .id(UUID.randomUUID())
                .userId(loggedInUser.getId())
                .assessmentId(UUID.randomUUID())
                .feedbackText("Feedback 1")
                .correctness(0.85)
                .success(true)
                .createdAt(OffsetDateTime.now())
                .build();

        ProactiveFeedbackEntity entity2 = ProactiveFeedbackEntity.builder()
                .id(UUID.randomUUID())
                .userId(loggedInUser.getId())
                .assessmentId(UUID.randomUUID())
                .feedbackText("Feedback 2")
                .correctness(0.75)
                .success(true)
                .createdAt(OffsetDateTime.now().minusHours(1))
                .build();

        when(proactiveFeedbackService.getAllFeedbackForUser(loggedInUser.getId()))
                .thenReturn(List.of(entity1, entity2));

        List<ProactiveFeedback> result = tutorController.allProactiveFeedback(loggedInUser);

        assertEquals(2, result.size());
        assertEquals(entity1.getFeedbackText(), result.get(0).getFeedbackText());
        assertEquals(entity2.getFeedbackText(), result.get(1).getFeedbackText());
    }

    @Test
    void testAllProactiveFeedback_empty() {
        when(proactiveFeedbackService.getAllFeedbackForUser(loggedInUser.getId()))
                .thenReturn(List.of());

        List<ProactiveFeedback> result = tutorController.allProactiveFeedback(loggedInUser);

        assertTrue(result.isEmpty());
    }

    @Test
    void testProactiveFeedbackAdded_subscription() {
        UUID userId = UUID.randomUUID();
        Flux<ProactiveFeedback> mockFlux = Flux.empty();
        
        when(proactiveFeedbackService.proactiveFeedbackStream(userId))
                .thenReturn(mockFlux);

        var publisher = tutorController.proactiveFeedbackAdded(userId);

        assertNotNull(publisher);
        verify(proactiveFeedbackService).proactiveFeedbackStream(userId);
    }

    @Test
    void testEmptyQuery_shouldThrowException() {
        assertThrows(UnsupportedOperationException.class, () -> {
            tutorController._empty();
        });
    }

}

