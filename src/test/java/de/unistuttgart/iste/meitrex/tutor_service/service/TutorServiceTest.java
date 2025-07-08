package de.unistuttgart.iste.meitrex.tutor_service.service;


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

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TutorServiceTest {

    private final OllamaService ollamaService = Mockito.mock(OllamaService.class) ;
    private final DocProcAIServiceClient docProcAIService = Mockito.mock(DocProcAIServiceClient.class);
    private final ContentServiceClient contentService = Mockito.mock(ContentServiceClient.class);
    private final TutorService tutorService = new TutorService(docProcAIService, contentService, ollamaService);

    @Test
    void testHandleUserQuestion_withUnrecognizableCategory() throws IOException, InterruptedException {
        String question = "jchbjshbcjhsdbc";
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question, Category.UNRECOGNIZABLE);
        when(ollamaService.queryLLM(Mockito.any())).thenReturn(new OllamaResponse());
        when(ollamaService.parseResponse(Mockito.any(), Mockito.eq(CategorizedQuestion.class)))
                .thenReturn(Optional.of(categorizedQuestion));

        String response = tutorService.handleUserQuestion(question, null);
        assertEquals("Ich konnte Ihre Frage leider nicht verstehen."
                + "Formulieren Sie die Frage bitte anders und stellen Sie diese erneut. Vielen Dank :)", response);
    }

    @Test
    void testHandleUserQuestion_withOtherCategory() throws IOException, InterruptedException {
        String question = "Gib mir ein Rezept für Schokokuchen";
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question, Category.OTHER);
        when(ollamaService.queryLLM(Mockito.any())).thenReturn(new OllamaResponse());
        when(ollamaService.parseResponse(Mockito.any(), Mockito.eq(CategorizedQuestion.class)))
                .thenReturn(Optional.of(categorizedQuestion));

        String response = tutorService.handleUserQuestion(question, null);
        assertEquals("So eine Art von Nachricht kann ich derzeit nicht beantworten. Bei Fragen über"
                + " Vorlesungsmaterialien oder das MEITREX System kann ich Ihnen dennoch behilflich sein :)", response);
    }

    @Test
    void testHandleUserQuestion_withLectureCategoryNoCourseId() throws IOException, InterruptedException {
        String question = "What is the difference between supervised and unsupervised training?";
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question, Category.LECTURE);
        when(ollamaService.queryLLM(Mockito.any())).thenReturn(new OllamaResponse());
        when(ollamaService.parseResponse(Mockito.any(), Mockito.eq(CategorizedQuestion.class)))
                .thenReturn(Optional.of(categorizedQuestion));

        String expectedAnswer = "Es ist etwas schiefgegangen! Sollte es sich um eine Frage über "
                + "Vorlesungsmaterialien handeln, gehen Sie bitte in den Kurs auf den sich diese Frage bezieht. "
                + "Vielen Dank! :)";

        String response = tutorService.handleUserQuestion(question, null);
        assertEquals(expectedAnswer, response);
    }

    @Test
    void testHandleUserQuestion_withLectureCategoryValidCourseId() throws IOException, InterruptedException, ContentServiceConnectionException {
        String question = "What is the difference between supervised and unsupervised training?";
        UUID courseID = UUID.randomUUID();
        List<UUID> mockIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question, Category.LECTURE);
        List<SemanticSearchResult> dummyResults = List.of(
                SemanticSearchResult.builder()
                        .score(0.95)
                        .__typename("VideoSegment")
                        .mediaRecordSegment(MediaRecordSegment.builder().id(UUID.randomUUID()).build())
                        .build(),
                SemanticSearchResult.builder()
                        .score(0.88)
                        .__typename("VideoSegment")
                        .mediaRecordSegment(MediaRecordSegment.builder().id(UUID.randomUUID()).build())
                        .build()
        );

        when(ollamaService.queryLLM(Mockito.any())).thenReturn(new OllamaResponse());
        when(ollamaService.parseResponse(Mockito.any(), Mockito.eq(CategorizedQuestion.class)))
                .thenReturn(Optional.of(categorizedQuestion));
        when(contentService.queryContentIdsOfCourse(courseID)).thenReturn(mockIds);
        when(docProcAIService.semanticSearch(Mockito.any(), Mockito.any())).thenReturn(dummyResults);

        String expectedAnswer = "Es wurden " + dummyResults.size() + " relevante Segmente gefunden. "
                + "Aktuell kann ich noch keine Fragen zum Lehrmaterial beantworten :(";

        String response = tutorService.handleUserQuestion(question, courseID);
        assertEquals(expectedAnswer, response);
    }

    @Test
    void testHandleUserQuestion_withSystemCategory() throws IOException, InterruptedException {
        String question = "Where do i upload my assignment?";
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question, Category.SYSTEM);
        when(ollamaService.queryLLM(Mockito.any())).thenReturn(new OllamaResponse());
        when(ollamaService.parseResponse(Mockito.any(), Mockito.eq(CategorizedQuestion.class)))
                .thenReturn(Optional.of(categorizedQuestion));

        String response = tutorService.handleUserQuestion(question, null);
        assertEquals("Aktuell kann ich noch keine Fragen zum MEITREX System beantworten :(", response);
    }

    @Test
    void testHandleUserQuestion_errorInOllama() throws IOException, InterruptedException {
        String question = "What is the difference between supervised and unsupervised training?";
        when(ollamaService.queryLLM(Mockito.any())).thenThrow(new RuntimeException());

        String response = tutorService.handleUserQuestion(question, null);
        assertEquals("Ups etwas ist schiefgegangen!"
                + "Die Anfrage kann nicht verarbeitet werden. Bitte versuchen Sie es nocheinmal", response);
    }

}
