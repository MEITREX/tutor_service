package de.unistuttgart.iste.meitrex.tutor_service.service;


import de.unistuttgart.iste.meitrex.tutor_service.persistence.models.CategorizedQuestion;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.models.Category;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.models.OllamaResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TutorServiceTest {

    private final OllamaService ollamaService = Mockito.mock(OllamaService.class) ;
    private final TutorService tutorService = new TutorService(ollamaService);

    @Test
    void testHandleUserQuestion_withUnrecognizableCategory() throws IOException, InterruptedException {
        String question = "jchbjshbcjhsdbc";
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question, Category.UNRECOGNIZABLE);
        when(ollamaService.queryLLM(Mockito.any())).thenReturn(new OllamaResponse());
        when(ollamaService.parseResponse(Mockito.any(), Mockito.eq(CategorizedQuestion.class)))
                .thenReturn(Optional.of(categorizedQuestion));

        String response = tutorService.handleUserQuestion(question);
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

        String response = tutorService.handleUserQuestion(question);
        assertEquals("So eine Art von Nachricht kann ich derzeit nicht beantworten. Bei Fragen über"
                + " Vorlesungsmaterialien oder das MEITREX System kann ich Ihnen dennoch behilflich sein :)", response);
    }

    @Test
    void testHandleUserQuestion_withLectureCategory() throws IOException, InterruptedException {
        String question = "What is the difference between supervised and unsupervised training?";
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question, Category.LECTURE);
        when(ollamaService.queryLLM(Mockito.any())).thenReturn(new OllamaResponse());
        when(ollamaService.parseResponse(Mockito.any(), Mockito.eq(CategorizedQuestion.class)))
                .thenReturn(Optional.of(categorizedQuestion));

        String response = tutorService.handleUserQuestion(question);
        assertEquals("Aktuell kann ich noch keine Fragen zum Lehrmaterial beantworten :(", response);
    }

    @Test
    void testHandleUserQuestion_withSystemCategory() throws IOException, InterruptedException {
        String question = "Where do i upload my assignment?";
        CategorizedQuestion categorizedQuestion = new CategorizedQuestion(question, Category.SYSTEM);
        when(ollamaService.queryLLM(Mockito.any())).thenReturn(new OllamaResponse());
        when(ollamaService.parseResponse(Mockito.any(), Mockito.eq(CategorizedQuestion.class)))
                .thenReturn(Optional.of(categorizedQuestion));

        String response = tutorService.handleUserQuestion(question);
        assertEquals("Aktuell kann ich noch keine Fragen zum MEITREX System beantworten :(", response);
    }

    @Test
    void testHandleUserQuestion_errorInOllama() throws IOException, InterruptedException {
        String question = "What is the difference between supervised and unsupervised training?";
        when(ollamaService.queryLLM(Mockito.any())).thenThrow(new RuntimeException());

        String response = tutorService.handleUserQuestion(question);
        assertEquals("Ups etwas ist schiefgegangen!"
                + "Die Anfrage kann nicht verarbeitet werden. Bitte versuchen Sie es nocheinmal", response);
    }

}
