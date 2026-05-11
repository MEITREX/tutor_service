package de.unistuttgart.iste.meitrex.tutor_service.service;


import de.unistuttgart.iste.meitrex.common.dapr.TopicPublisher;
import de.unistuttgart.iste.meitrex.common.event.TutorCategory;
import de.unistuttgart.iste.meitrex.common.ollama.OllamaClient;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.LectureQuestionResponse;
import de.unistuttgart.iste.meitrex.tutor_service.service.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipInCourseWithId;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TutorServiceTest {

        private final OllamaClient ollamaClient = Mockito.mock(OllamaClient.class);
    private final SemanticSearchService semanticSearchService = Mockito.mock(SemanticSearchService.class);
    private final TopicPublisher topicPublisher = Mockito.mock(TopicPublisher.class);
    private final UserPlayerTypeService userPlayerTypeService = Mockito.mock(UserPlayerTypeService.class);
    private final UserSkillLevelService userSkillLevelService = Mockito.mock(UserSkillLevelService.class);
    private final ProactiveFeedbackService proactiveFeedbackService = Mockito.mock(ProactiveFeedbackService.class);
    private final ConversationHistoryService conversationHistoryService = Mockito.mock(ConversationHistoryService.class);
    private final StudentCodeSubmissionService studentCodeSubmissionService = Mockito.mock(StudentCodeSubmissionService.class);
    private TutorService tutorService;

    @BeforeEach
    void setUp() {
                tutorService = new TutorService(ollamaClient, semanticSearchService, topicPublisher, 
                userPlayerTypeService, userSkillLevelService, proactiveFeedbackService, 
                conversationHistoryService, studentCodeSubmissionService);
        ReflectionTestUtils.setField(tutorService, "scoreThreshold", 0.4);
    }
    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, LoggedInUser.UserRoleInCourse.STUDENT);

    @Test
    void testHandleUserQuestion_withUnrecognizableCategory() {
        String question = "jchbjshbcjhsdbc";
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question, TutorCategory.UNRECOGNIZABLE);
        when(ollamaClient.startQuery(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(categorizedQuestion);

        LectureQuestionResponse response = tutorService.handleUserQuestion(question, null, loggedInUser);
        assertEquals("Unfortunately, I couldn't understand your question. " +
                        "Please rephrase it and ask again. Thank you :)",
                response.getAnswer());
    }

    @Test
    void testHandleUserQuestion_withOtherCategory() {
        String question = "Gib mir ein Rezept für Schokokuchen";
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question,TutorCategory.OTHER);
        when(ollamaClient.startQuery(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(categorizedQuestion);
        when(conversationHistoryService.formatHistoryForPrompt(Mockito.any(), Mockito.any())).thenReturn("");
        when(studentCodeSubmissionService.getCodeSubmissionsForStudent(Mockito.any())).thenReturn(List.of());

        LectureQuestionResponse response = tutorService.handleUserQuestion(question, courseId, loggedInUser);
        assertEquals("I'm currently unable to answer this type of message. " +
                        "However, I can still help you with questions about lecture materials or the MEITREX system :)",
                response.getAnswer());
    }

    @Test
    void testHandleUserQuestion_withLectureCategoryNoCourseId() {
        String question = "What is the difference between supervised and unsupervised training?";
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question,TutorCategory.LECTURE);
        when(ollamaClient.startQuery(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(categorizedQuestion);

        String expectedAnswer = "Something went wrong! If your question is about lecture materials, " +
                "please navigate to the course it relates to. Thank you! :)";

        LectureQuestionResponse response = tutorService.handleUserQuestion(question, null, loggedInUser);
        assertEquals(expectedAnswer, response.getAnswer());
    }

    @Test
    void testHandleUserQuestion_withLectureCategoryValidCourseId() {
        String question = "What is the difference between supervised and unsupervised training?";
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question,TutorCategory.LECTURE);
        List<SemanticSearchResult> dummyResults = List.of(
                SemanticSearchResult.builder()
                        .score(0.15)
                        .typename("DocumentRecordSegment")
                        .mediaRecordSegment(DocumentRecordSegment.builder().page(2).text("Dummy content").build())
                        .build(),
                SemanticSearchResult.builder()
                        .score(0.28)
                        .typename("DocumentRecordSegment")
                        .mediaRecordSegment(DocumentRecordSegment.builder().page(3).text("Dummy content").build())
                        .build()
        );
        String expectedAnswer = dummyResults.size() + " relevant segments were found. " +
                "At the moment, I’m not yet able to answer questions about the course material :(";

        when(ollamaClient.startQuery(Mockito.eq(CategorizedQuestion.class), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(categorizedQuestion);
        when(semanticSearchService.semanticSearch(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(dummyResults);
        when(semanticSearchService.formatIntoNumberedListForPrompt(Mockito.any())).thenReturn("Mocked content");
        when(ollamaClient.startQuery(Mockito.eq(TutorAnswer.class), Mockito.any(), Mockito.any(),
                Mockito.any())).thenReturn(new TutorAnswer(expectedAnswer));
        when(userSkillLevelService.getAllSkillLevelsForUser(Mockito.any())).thenReturn(List.of());
        when(conversationHistoryService.formatHistoryForPrompt(Mockito.any(), Mockito.any())).thenReturn("");


        LectureQuestionResponse response = tutorService.handleUserQuestion(question, courseId, loggedInUser);
        assertEquals(expectedAnswer, response.getAnswer());
    }

    @Test
    void testHandleUserQuestion_withSystemCategory() {
        String question = "Where do i upload my assignment?";
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question,TutorCategory.SYSTEM);
        when(ollamaClient.startQuery(Mockito.eq(CategorizedQuestion.class), Mockito.any(), Mockito.any(),
                Mockito.any())).thenReturn(categorizedQuestion);

        LectureQuestionResponse response = tutorService.handleUserQuestion(question, null, loggedInUser);
        assertEquals("At the moment, I can't answer any questions about the MEITREX system :(",
                response.getAnswer());
    }

    @Test
    void testAnswerLectureQuestion_withoutRelevantSegments() {
        String question = "Mock question";
        String expectedAnswer = "No answer was found in the lecture.";

        // Mock PreProcess to be a Lecture Question
        when(ollamaClient.startQuery(Mockito.eq(CategorizedQuestion.class), Mockito.any(), Mockito.any(),
                Mockito.any())).thenReturn(new CategorizedQuestion(question,TutorCategory.LECTURE));

        when(semanticSearchService.semanticSearch(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(List.of());
        LectureQuestionResponse response = tutorService.handleUserQuestion(question, courseId, loggedInUser);
        assertEquals(expectedAnswer, response.getAnswer());
    }

    @Test
    void testAnswerLectureQuestion_withoutDocumentSegments() {
        String question = "Mock question";
        String expectedAnswer = "No answer was found in the documents of the lecture.";
        List<SemanticSearchResult> dummyResults = List.of(
                SemanticSearchResult.builder()
                        .score(0.15)
                        .typename("VideoRecordSegment")
                        .mediaRecordSegment(VideoRecordSegment.builder().startTime(2).build())
                        .build(),
                SemanticSearchResult.builder()
                        .score(0.28)
                        .typename("VideoRecordSegment")
                        .mediaRecordSegment(VideoRecordSegment.builder().startTime(3).build())
                        .build()
        );

        // Mock PreProcess to be a Lecture Question
        when(ollamaClient.startQuery(Mockito.eq(CategorizedQuestion.class), Mockito.any(), Mockito.any(),
                Mockito.any())).thenReturn(new CategorizedQuestion(question,TutorCategory.LECTURE));
        when(semanticSearchService.semanticSearch(Mockito.any(), Mockito.any(),
                Mockito.any())).thenReturn(dummyResults);

        LectureQuestionResponse response = tutorService.handleUserQuestion(question, courseId, loggedInUser);
        assertEquals(expectedAnswer, response.getAnswer());
    }

    @Test
    void testHandleUserQuestion_withCodeFeedbackCategoryNoCourseId() {
        String question = "Can you review my code?";
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question, TutorCategory.CODE_FEEDBACK);
        when(ollamaClient.startQuery(Mockito.eq(CategorizedQuestion.class), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(categorizedQuestion);

        String expectedAnswer = "Something went wrong! If your question is about code for an assignment, " +
                "please navigate to the course it relates to. Thank you! :)";

        LectureQuestionResponse response = tutorService.handleUserQuestion(question, null, loggedInUser);
        assertEquals(expectedAnswer, response.getAnswer());
    }

    @Test
    void testHandleUserQuestion_withCodeFeedbackNoSubmissions() {
        String question = "Can you review my code?";
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question, TutorCategory.CODE_FEEDBACK);
        
        when(ollamaClient.startQuery(Mockito.eq(CategorizedQuestion.class), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(categorizedQuestion);
        when(studentCodeSubmissionService.getCodeSubmissionsForStudent(Mockito.any())).thenReturn(List.of());

        String expectedAnswer = "I couldn't find any code submission from you. " +
                "Please make sure you've committed your code to your assignment repository. " +
                "Open this respository and try again. Make sure to commit and push your code first!";

        LectureQuestionResponse response = tutorService.handleUserQuestion(question, courseId, loggedInUser);
        assertEquals(expectedAnswer, response.getAnswer());
    }

    @Test
    void testGetLatestProactiveFeedback_withFeedback() {
        String feedback = "Great job on your assignment!";
        when(proactiveFeedbackService.getAndDeleteLatestFeedback(loggedInUser.getId()))
                .thenReturn(Optional.of(feedback));

        Optional<String> response = tutorService.getLatestProactiveFeedback(loggedInUser);
        assertTrue(response.isPresent());
        assertEquals(feedback, response.get());
        Mockito.verify(proactiveFeedbackService).getAndDeleteLatestFeedback(loggedInUser.getId());
    }

    @Test
    void testGetLatestProactiveFeedback_withNoFeedback() {
        when(proactiveFeedbackService.getAndDeleteLatestFeedback(loggedInUser.getId()))
                .thenReturn(Optional.empty());

        Optional<String> response = tutorService.getLatestProactiveFeedback(loggedInUser);
        assertFalse(response.isPresent());
    }

    @Test
    void testHandleUserQuestion_withFollowUpQuestionWithCodeContext() {
        String question = "Can you explain that better?";
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question, TutorCategory.OTHER);
        TutorAnswer tutorAnswer = new TutorAnswer("Here is a more detailed explanation based on your code.");
        
        de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.StudentCodeSubmissionEntity submission = 
                Mockito.mock(de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.StudentCodeSubmissionEntity.class);
        de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.StudentCodeSubmissionEntity.PrimaryKey primaryKey = 
                Mockito.mock(de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.StudentCodeSubmissionEntity.PrimaryKey.class);
        
        when(primaryKey.getAssignmentId()).thenReturn(UUID.randomUUID());
        when(submission.getPrimaryKey()).thenReturn(primaryKey);
        when(submission.getLastUpdated()).thenReturn(java.time.OffsetDateTime.now());
        
        List<SemanticSearchResult> dummyResults = List.of(
                SemanticSearchResult.builder()
                        .score(0.15)
                        .typename("DocumentRecordSegment")
                        .mediaRecordSegment(DocumentRecordSegment.builder().page(2).text("Dummy content").build())
                        .build()
        );
        
        when(ollamaClient.startQuery(Mockito.eq(CategorizedQuestion.class), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(categorizedQuestion);
        when(ollamaClient.startQuery(Mockito.eq(TutorAnswer.class), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(tutorAnswer);
        when(conversationHistoryService.formatHistoryForPrompt(Mockito.any(), Mockito.any()))
                .thenReturn("Previous conversation history");
        when(studentCodeSubmissionService.getCodeSubmissionsForStudent(Mockito.any()))
                .thenReturn(List.of(submission));
        when(studentCodeSubmissionService.getCodeSubmissionContextForTutor(Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of("public class Example { }"));
        when(semanticSearchService.semanticSearch(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(dummyResults);
        when(userSkillLevelService.getAllSkillLevelsForUser(Mockito.any())).thenReturn(List.of());

        LectureQuestionResponse response = tutorService.handleUserQuestion(question, courseId, loggedInUser);
        assertEquals("Here is a more detailed explanation based on your code.", response.getAnswer());
    }

    @Test
    void testHandleUserQuestion_withFollowUpQuestionNoCodeContext() {
        String question = "Can you clarify that?";
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question, TutorCategory.OTHER);
        TutorAnswer tutorAnswer = new TutorAnswer("Here is a clarification.");
        
        List<SemanticSearchResult> dummyResults = List.of(
                SemanticSearchResult.builder()
                        .score(0.15)
                        .typename("DocumentRecordSegment")
                        .mediaRecordSegment(DocumentRecordSegment.builder().page(2).text("Dummy content").build())
                        .build()
        );
        
        when(ollamaClient.startQuery(Mockito.eq(CategorizedQuestion.class), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(categorizedQuestion);
        when(ollamaClient.startQuery(Mockito.eq(TutorAnswer.class), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(tutorAnswer);
        when(conversationHistoryService.formatHistoryForPrompt(Mockito.any(), Mockito.any()))
                .thenReturn("Previous conversation history");
        when(studentCodeSubmissionService.getCodeSubmissionsForStudent(Mockito.any()))
                .thenReturn(List.of());
        when(semanticSearchService.semanticSearch(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(dummyResults);
        when(userSkillLevelService.getAllSkillLevelsForUser(Mockito.any())).thenReturn(List.of());

        LectureQuestionResponse response = tutorService.handleUserQuestion(question, courseId, loggedInUser);
        assertEquals("Here is a clarification.", response.getAnswer());
    }
}
