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
import java.util.Optional;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipInCourseWithId;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TutorServiceTest {

    private final OllamaService ollamaService = Mockito.mock(OllamaService.class) ;
    private final DocProcAIServiceClient docProcAIService = Mockito.mock(DocProcAIServiceClient.class);
    private final ContentServiceClient contentService = Mockito.mock(ContentServiceClient.class);
    private final TutorService tutorService = new TutorService(docProcAIService, contentService, ollamaService);

    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, LoggedInUser.UserRoleInCourse.STUDENT);

    @Test
    void testHandleUserQuestion_withUnrecognizableCategory() throws IOException, InterruptedException {
        String question = "jchbjshbcjhsdbc";
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question, Category.UNRECOGNIZABLE);
        when(ollamaService.queryLLM(Mockito.any())).thenReturn(new OllamaResponse());
        when(ollamaService.parseResponse(Mockito.any(), Mockito.eq(CategorizedQuestion.class)))
                .thenReturn(Optional.of(categorizedQuestion));

        LectureQuestionResponse response = tutorService.handleUserQuestion(question, null, loggedInUser);
        assertEquals("Unfortunately, I couldn't understand your question. " +
                        "Please rephrase it and ask again. Thank you :)",
                response.getAnswer());
    }

    @Test
    void testHandleUserQuestion_withOtherCategory() throws IOException, InterruptedException {
        String question = "Gib mir ein Rezept für Schokokuchen";
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question, Category.OTHER);
        when(ollamaService.queryLLM(Mockito.any())).thenReturn(new OllamaResponse());
        when(ollamaService.parseResponse(Mockito.any(), Mockito.eq(CategorizedQuestion.class)))
                .thenReturn(Optional.of(categorizedQuestion));

        LectureQuestionResponse response = tutorService.handleUserQuestion(question, null, loggedInUser);
        assertEquals("I'm currently unable to answer this type of message. " +
                        "However, I can still help you with questions about lecture materials or the MEITREX system :)",
                response.getAnswer());
    }

    @Test
    void testHandleUserQuestion_withLectureCategoryNoCourseId() throws IOException, InterruptedException {
        String question = "What is the difference between supervised and unsupervised training?";
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question, Category.LECTURE);
        when(ollamaService.queryLLM(Mockito.any())).thenReturn(new OllamaResponse());
        when(ollamaService.parseResponse(Mockito.any(), Mockito.eq(CategorizedQuestion.class)))
                .thenReturn(Optional.of(categorizedQuestion));

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
                        .typename("VideoSegment")
                        .mediaRecordSegment(DocumentRecordSegment.builder().page(2).text("Dummy content").build())
                        .build(),
                SemanticSearchResult.builder()
                        .score(0.88)
                        .typename("VideoSegment")
                        .mediaRecordSegment(DocumentRecordSegment.builder().page(3).text("Dummy content").build())
                        .build()
        );
        String expectedAnswer = dummyResults.size() + " relevant segments were found. " +
                "At the moment, I’m not yet able to answer questions about the course material :(";

        when(ollamaService.queryLLM(Mockito.any())).thenReturn(new OllamaResponse());
        when(ollamaService.parseResponse(Mockito.any(), Mockito.eq(CategorizedQuestion.class)))
                .thenReturn(Optional.of(categorizedQuestion));
        when(ollamaService.parseResponse(Mockito.any(), Mockito.eq(LectureQuestionResponse.class)))
                .thenReturn(Optional.of(new LectureQuestionResponse(expectedAnswer)));
        when(contentService.queryContentIdsOfCourse(courseId)).thenReturn(mockIds);
        when(docProcAIService.semanticSearch(Mockito.any(), Mockito.any())).thenReturn(dummyResults);


        LectureQuestionResponse response = tutorService.handleUserQuestion(question, courseId, loggedInUser);
        assertEquals(expectedAnswer, response.getAnswer());
    }

    @Test
    void testHandleUserQuestion_withSystemCategory() throws IOException, InterruptedException {
        String question = "Where do i upload my assignment?";
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question, Category.SYSTEM);
        when(ollamaService.queryLLM(Mockito.any())).thenReturn(new OllamaResponse());
        when(ollamaService.parseResponse(Mockito.any(), Mockito.eq(CategorizedQuestion.class)))
                .thenReturn(Optional.of(categorizedQuestion));

        LectureQuestionResponse response = tutorService.handleUserQuestion(question, null, loggedInUser);
        assertEquals("At the moment, I can't answer any questions about the MEITREX system :(",
                response.getAnswer());
    }

    @Test
    void testHandleUserQuestion_errorInOllama() throws IOException, InterruptedException {
        String question = "What is the difference between supervised and unsupervised training?";
        when(ollamaService.queryLLM(Mockito.any())).thenThrow(new RuntimeException());

        LectureQuestionResponse response = tutorService.handleUserQuestion(question, null, loggedInUser);
        assertEquals("Oops, something went wrong! " +
                "The request could not be processed. Please try again.", response.getAnswer());
    }

}
