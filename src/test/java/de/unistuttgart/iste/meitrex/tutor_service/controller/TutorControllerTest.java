package de.unistuttgart.iste.meitrex.tutor_service.controller;

import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.LectureQuestionResponse;
import de.unistuttgart.iste.meitrex.tutor_service.service.TutorService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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

}
