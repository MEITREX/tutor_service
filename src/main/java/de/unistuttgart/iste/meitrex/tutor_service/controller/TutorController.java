package de.unistuttgart.iste.meitrex.tutor_service.controller;

import de.unistuttgart.iste.meitrex.tutor_service.persistence.models.LectureQuestionResponse;
import de.unistuttgart.iste.meitrex.tutor_service.service.TutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TutorController {

    private final TutorService tutorService;

    @MutationMapping
    public LectureQuestionResponse sendMessage(@Argument final String userInput, @Argument final UUID courseId) {

        if (userInput.isEmpty()){
            return new LectureQuestionResponse("Eine leere Nachricht kann nicht beantwortet werden");
        }

        return tutorService.handleUserQuestion(userInput, courseId);
    }

    @QueryMapping
    public String _empty(){
        throw new UnsupportedOperationException("This service supports only mutations but needs a query.");
    }

}
