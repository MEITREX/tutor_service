package de.unistuttgart.iste.meitrex.tutor_service.controller;

import de.unistuttgart.iste.meitrex.tutor_service.persistence.models.HintResponse;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.models.LectureQuestionResponse;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.tutor_service.service.HintService;
import de.unistuttgart.iste.meitrex.tutor_service.service.TutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TutorController {

    private final TutorService tutorService;
    private final HintService hintService;

    @MutationMapping
    public LectureQuestionResponse sendMessage(
            @Argument final String userInput,
            @Argument final UUID courseId,
            @ContextValue final LoggedInUser currentUser
    ) {
        if (userInput.isEmpty()){
            return new LectureQuestionResponse("An empty message cannot be answered.");
        }

        return tutorService.handleUserQuestion(userInput, courseId, currentUser);
    }

    @QueryMapping
    public String _empty(){
        throw new UnsupportedOperationException("This service supports only mutations but needs a query.");
    }

    @MutationMapping
    public HintResponse generateHint(
            @Argument final String questionText,
            @Argument final UUID courseId,
            @ContextValue final LoggedInUser currentUser
    ){
        if (questionText.isEmpty()){
            return new HintResponse("No Hint can be generated without a question in the quiz.");
        }
        return hintService.generateHintWithQuestion(questionText, courseId, currentUser);
    }

}
