package de.unistuttgart.iste.meitrex.tutor_service.service;


import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.content_service.client.ContentServiceClient;
import de.unistuttgart.iste.meitrex.content_service.exception.ContentServiceConnectionException;
import de.unistuttgart.iste.meitrex.tutor_service.client.DocProcAIServiceClient;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.models.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipInCourseWithId;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class TutorServiceTest {

    private final OllamaService ollamaService = Mockito.mock(OllamaService.class);
    private final DocProcAIServiceClient docProcAIService = Mockito.mock(DocProcAIServiceClient.class);
    private final ContentServiceClient contentService = Mockito.mock(ContentServiceClient.class);
    private final SemanticSearchService semanticSearchService = Mockito.mock(SemanticSearchService.class);
    private final TutorService tutorService = new TutorService(ollamaService, semanticSearchService);

    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, LoggedInUser.UserRoleInCourse.STUDENT);

    @Test
    void testHandleUserQuestion_withUnrecognizableCategory() throws IOException, InterruptedException {
        String question = "jchbjshbcjhsdbc";
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question, Category.UNRECOGNIZABLE);
        when(ollamaService.startQuery(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(categorizedQuestion);
        when(ollamaService.getTemplate(Mockito.any())).thenReturn("Mocked Prompt");

        LectureQuestionResponse response = tutorService.handleUserQuestion(question, null, loggedInUser);
        assertEquals("Unfortunately, I couldn't understand your question. " +
                        "Please rephrase it and ask again. Thank you :)",
                response.getAnswer());
    }

    @Test
    void testHandleUserQuestion_withOtherCategory() throws IOException, InterruptedException {
        String question = "Gib mir ein Rezept für Schokokuchen";
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question, Category.OTHER);
        when(ollamaService.startQuery(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(categorizedQuestion);
        when(ollamaService.getTemplate(Mockito.any())).thenReturn("Mocked Prompt");

        LectureQuestionResponse response = tutorService.handleUserQuestion(question, null, loggedInUser);
        assertEquals("I'm currently unable to answer this type of message. " +
                        "However, I can still help you with questions about lecture materials or the MEITREX system :)",
                response.getAnswer());
    }

    @Test
    void testHandleUserQuestion_withLectureCategoryNoCourseId() throws IOException, InterruptedException {
        String question = "What is the difference between supervised and unsupervised training?";
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question, Category.LECTURE);
        when(ollamaService.startQuery(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(categorizedQuestion);
        when(ollamaService.getTemplate(Mockito.any())).thenReturn("Mocked Prompt");

        String expectedAnswer = "Something went wrong! If your question is about lecture materials, " +
                "please navigate to the course it relates to. Thank you! :)";

        LectureQuestionResponse response = tutorService.handleUserQuestion(question, null, loggedInUser);
        assertEquals(expectedAnswer, response.getAnswer());
    }

    @Test
    void testHandleUserQuestion_withLectureCategoryValidCourseId() throws IOException, InterruptedException, ContentServiceConnectionException {
        String question = "What is the difference between supervised and unsupervised training?";
        List<UUID> mockIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question, Category.LECTURE);
        List<SemanticSearchResult> dummyResults = List.of(
                SemanticSearchResult.builder()
                        .score(0.95)
                        .typename("DocumentRecordSegment")
                        .mediaRecordSegment(DocumentRecordSegment.builder().page(2).text("Dummy content").build())
                        .build(),
                SemanticSearchResult.builder()
                        .score(0.88)
                        .typename("DocumentRecordSegment")
                        .mediaRecordSegment(DocumentRecordSegment.builder().page(3).text("Dummy content").build())
                        .build()
        );
        String expectedAnswer = dummyResults.size() + " relevant segments were found. " +
                "At the moment, I’m not yet able to answer questions about the course material :(";

        when(ollamaService.startQuery(Mockito.eq(CategorizedQuestion.class), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(categorizedQuestion);
        when(ollamaService.getTemplate(Mockito.any())).thenReturn("Mocked Prompt");
        when(semanticSearchService.semanticSearch(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(dummyResults);
        when(semanticSearchService.formatDocumentSegmentsForPrompt(Mockito.any())).thenReturn("Mocked content");
        when(ollamaService.startQuery(Mockito.eq(LectureQuestionResponse.class), Mockito.any(), Mockito.any(),
                Mockito.any())).thenReturn(new LectureQuestionResponse(expectedAnswer));


        LectureQuestionResponse response = tutorService.handleUserQuestion(question, courseId, loggedInUser);
        assertEquals(expectedAnswer, response.getAnswer());
    }

    @Test
    void testHandleUserQuestion_withSystemCategory() throws IOException, InterruptedException {
        String question = "Where do i upload my assignment?";
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question, Category.SYSTEM);
        when(ollamaService.startQuery(Mockito.eq(CategorizedQuestion.class), Mockito.any(), Mockito.any(),
                Mockito.any())).thenReturn(categorizedQuestion);
        when(ollamaService.getTemplate(Mockito.any())).thenReturn("Mocked Prompt");

        LectureQuestionResponse response = tutorService.handleUserQuestion(question, null, loggedInUser);
        assertEquals("At the moment, I can't answer any questions about the MEITREX system :(",
                response.getAnswer());
    }

    @Test
    void testAnswerLectureQuestion_withoutRelevantSegments() {
        String question = "Mock question";
        String expectedAnswer = "No answer was found in the lecture.";

        // Mock PreProcess to be a Lecture Question
        when(ollamaService.startQuery(Mockito.eq(CategorizedQuestion.class), Mockito.any(), Mockito.any(),
                Mockito.any())).thenReturn(new CategorizedQuestion(question, Category.LECTURE));
        when(ollamaService.getTemplate(Mockito.any())).thenReturn("Mocked Prompt");

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
                        .score(0.95)
                        .typename("VideoRecordSegment")
                        .mediaRecordSegment(VideoRecordSegment.builder().startTime(2).build())
                        .build(),
                SemanticSearchResult.builder()
                        .score(0.88)
                        .typename("VideoRecordSegment")
                        .mediaRecordSegment(VideoRecordSegment.builder().startTime(3).build())
                        .build()
        );

        // Mock PreProcess to be a Lecture Question
        when(ollamaService.startQuery(Mockito.eq(CategorizedQuestion.class), Mockito.any(), Mockito.any(),
                Mockito.any())).thenReturn(new CategorizedQuestion(question, Category.LECTURE));
        when(ollamaService.getTemplate(Mockito.any())).thenReturn("Mocked Prompt");
        when(semanticSearchService.semanticSearch(Mockito.any(), Mockito.any(),
                Mockito.any())).thenReturn(dummyResults);

        LectureQuestionResponse response = tutorService.handleUserQuestion(question, courseId, loggedInUser);
        assertEquals(expectedAnswer, response.getAnswer());
    }

    @Test
    void testAnswerLectureQuestion_hasLinksInAnswer() {
        String question = "Mock question";
        UUID firstContentId = UUID.randomUUID();
        UUID secondContentId = UUID.randomUUID();
        List<SemanticSearchResult> dummyResults = List.of(
            SemanticSearchResult.builder()
                .score(0.95)
                .typename("DocumentRecordSegment")
                .mediaRecordSegment(
                    DocumentRecordSegment.builder()
                        .typename("DocumentRecordSegment")
                        .page(2)
                        .text("Dummy Text")
                        .mediaRecord(
                            MediaRecord.builder()
                                .id(firstContentId)
                                .contents(List.of(new Content(secondContentId)))
                                .build())
                        .build())
                .build(),
            SemanticSearchResult.builder()
                .score(0.88)
                .typename("DocumentRecordSegment")
                .mediaRecordSegment(
                    DocumentRecordSegment.builder()
                        .page(3)
                        .text("Dummy Text")
                        .mediaRecord(
                            MediaRecord.builder()
                                .id(secondContentId)
                                .contents(List.of(new Content(firstContentId)))
                                .build())
                        .build())
                .build()
        );

        // Mock PreProcess to be a Lecture Question
        when(ollamaService.startQuery(Mockito.eq(CategorizedQuestion.class), Mockito.any(), Mockito.any(),
                Mockito.any())).thenReturn(new CategorizedQuestion(question, Category.LECTURE));
        when(ollamaService.getTemplate(Mockito.any())).thenReturn("Mocked Prompt");
        when(semanticSearchService.semanticSearch(Mockito.any(), Mockito.any(),
                Mockito.any())).thenReturn(dummyResults);
        when(ollamaService.startQuery(Mockito.eq(LectureQuestionResponse.class), Mockito.any(), Mockito.any(),
                Mockito.any())).thenReturn(new LectureQuestionResponse("Mock Answer"));

        String correctURLForFirst = "/courses/" + courseId + "/media/" + secondContentId
                + "?selectedDocument=" + firstContentId + "&page=3";
        String correctURLForSecond = "/courses/" + courseId + "/media/" + firstContentId
                + "?selectedDocument=" + secondContentId + "&page=4";


        LectureQuestionResponse response = tutorService.handleUserQuestion(question, courseId, loggedInUser);
        assertThat(response.getLinks(), hasSize(2));
        assertEquals(correctURLForFirst, response.getLinks().getFirst());
        assertEquals(correctURLForSecond, response.getLinks().getLast());
    }

}
