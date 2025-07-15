package de.unistuttgart.iste.meitrex.tutor_service.controller;

import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.tutor_service.service.TutorService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipInCourseWithId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TutorControllerTest {

    @InjectMocks
    private TutorController tutorController;

    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, LoggedInUser.UserRoleInCourse.STUDENT);

    @Mock
    private TutorService tutorService;

    @Test
    void testSendMessage_withEmptyInput() {
        String answer = tutorController.sendMessage("", courseId, loggedInUser);
        assertEquals("Eine leere Nachricht kann nicht beantwortet werden", answer);
    }

    @Test
    void testSendMessage_withValidInput() {
        String question = "How do i upload my assignments?";
        String expectedResponse = "Response for the question";
        when(tutorService.handleUserQuestion(question, courseId, loggedInUser)).thenReturn(expectedResponse);

        String answer = tutorController.sendMessage(question, courseId, loggedInUser);

        assertEquals(expectedResponse, answer);
        verify(tutorService).handleUserQuestion(question, courseId, loggedInUser);
    }

}
