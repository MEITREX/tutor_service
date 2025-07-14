package de.unistuttgart.iste.meitrex.tutor_service.controller;

import de.unistuttgart.iste.meitrex.tutor_service.persistence.models.LectureQuestionResponse;
import de.unistuttgart.iste.meitrex.tutor_service.service.TutorService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TutorControllerTest {

    @InjectMocks
    private TutorController tutorController;

    @Mock
    private TutorService tutorService;

    @Test
    void testSendMessage_withEmptyInput() {
        LectureQuestionResponse answer = tutorController.sendMessage("", new UUID(0, 0));
        assertEquals("Eine leere Nachricht kann nicht beantwortet werden", answer.getAnswer());
    }

    @Test
    void testSendMessage_withValidInput() {
        String question = "How do i upload my assignments?";
        LectureQuestionResponse expectedResponse = new LectureQuestionResponse("Response for the question");
        when(tutorService.handleUserQuestion(question, new UUID(0,0))).thenReturn(expectedResponse);

        LectureQuestionResponse answer = tutorController.sendMessage(question, new UUID(0, 0));

        assertEquals(expectedResponse, answer);
        verify(tutorService).handleUserQuestion(question, new UUID(0,0));
    }

}
