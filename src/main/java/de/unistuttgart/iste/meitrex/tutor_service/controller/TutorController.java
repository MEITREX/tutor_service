package de.unistuttgart.iste.meitrex.tutor_service.controller;

import de.unistuttgart.iste.meitrex.generated.dto.ProactiveFeedback;
import de.unistuttgart.iste.meitrex.generated.dto.HintGenerationInput;
import de.unistuttgart.iste.meitrex.generated.dto.HintResponse;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.LectureQuestionResponse;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.ProactiveFeedbackEntity;
import de.unistuttgart.iste.meitrex.tutor_service.service.ProactiveFeedbackService;
import de.unistuttgart.iste.meitrex.tutor_service.service.HintService;
import de.unistuttgart.iste.meitrex.tutor_service.service.TutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TutorController {

    private final TutorService tutorService;
    private final HintService hintService;
    private final ProactiveFeedbackService proactiveFeedbackService;

    @MutationMapping
    public LectureQuestionResponse sendMessage(
            @Argument final String userInput,
            @Argument final UUID courseId,
            @ContextValue final LoggedInUser currentUser
    ) {
        if (userInput.isEmpty()){
            return new LectureQuestionResponse("An empty message cannot be answered.", List.of());
        }

        return tutorService.handleUserQuestion(userInput, courseId, currentUser);
    }

    @QueryMapping
    public String _empty(){
        throw new UnsupportedOperationException("This service supports only mutations but needs a query.");
    }

    @MutationMapping
    public HintResponse generateHint(
            @Argument final HintGenerationInput questionInput,
            @Argument final UUID courseId,
            @ContextValue final LoggedInUser currentUser
    ){
        return hintService.generateHintWithQuestion(questionInput, courseId, currentUser);
    }

    @QueryMapping
    public ProactiveFeedback proactiveFeedback(
            @Argument final UUID assessmentId,
            @ContextValue final LoggedInUser currentUser
    ) {
        return proactiveFeedbackService.getFeedbackForAssignment(currentUser.getId(), assessmentId)
                .map(this::entityToDto)
                .orElse(null);
    }

    @QueryMapping
    public List<ProactiveFeedback> allProactiveFeedback(
            @ContextValue final LoggedInUser currentUser
    ) {
        return proactiveFeedbackService.getAllFeedbackForUser(currentUser.getId())
                .stream()
                .map(this::entityToDto)
                .toList();
    }

    /**
     * GraphQL subscription for real-time proactive feedback.
     * Frontend subscribes to this and receives feedback automatically when generated.
     *
     * @param userId the user to receive feedback for
     * @return publisher emitting ProactiveFeedback events
     */
    @SubscriptionMapping
    public Publisher<ProactiveFeedback> proactiveFeedbackAdded(@Argument final UUID userId) {
        log.info("User {} subscribed to proactive feedback", userId);
        return proactiveFeedbackService.proactiveFeedbackStream(userId);
    }

    private ProactiveFeedback entityToDto(ProactiveFeedbackEntity entity) {
        return ProactiveFeedback.builder()
                .setId(entity.getId())
                .setAssessmentId(entity.getAssessmentId())
                .setFeedbackText(entity.getFeedbackText())
                .setCorrectness(entity.getCorrectness())
                .setSuccess(entity.getSuccess())
                .setCreatedAt(entity.getCreatedAt())
                .build();
    }

}
