package de.unistuttgart.iste.meitrex.tutor_service.service;

import de.unistuttgart.iste.meitrex.common.event.ContentProgressedEvent;
import de.unistuttgart.iste.meitrex.common.event.HexadPlayerType;
import de.unistuttgart.iste.meitrex.generated.dto.ProactiveFeedback;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.ProactiveFeedbackEntity;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.UserSkillLevelEntity;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.repository.ProactiveFeedbackRepository;
import de.unistuttgart.iste.meitrex.tutor_service.service.models.TemplateArgs;
import de.unistuttgart.iste.meitrex.tutor_service.service.models.TutorAnswer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service for generating personalized tutor feedback after assignment completion.
 * Supports real-time streaming of feedback via GraphQL subscriptions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProactiveFeedbackService {

    private final OllamaService ollamaService;
    private final UserPlayerTypeService userPlayerTypeService;
    private final UserSkillLevelService userSkillLevelService;
    private final ProactiveFeedbackRepository proactiveFeedbackRepository;
    
    /**
     * Per-user reactive sinks for streaming feedback to subscribers.
     * Emits ProactiveFeedback DTOs for querying feedback history.
     */
    private final ConcurrentMap<UUID, Sinks.Many<ProactiveFeedback>> feedbackSinks = new ConcurrentHashMap<>();
    
    @Value("${skill.level.threshold.low:0.3}")
    private double skillLevelLowThreshold;
    
    @Value("${skill.level.threshold.high:0.7}")
    private double skillLevelHighThreshold;

    @Value("${correctness.level.high:0.8}")
    private double correctnessLevelHigh;

    @Value("${correctness.level.max:0.99}")
    private double correctnessLevelMax;


    private static final String FEEDBACK_PROMPT_TEMPLATE = "proactive_feedback_prompt.txt";

    /**
     * Returns a per-user stream for GraphQL subscription to proactive feedback.
     * Frontend can subscribe to this to receive feedback DTOs for history/display.
     *
     * @param userId user id
     * @return publisher emitting ProactiveFeedback for this user
     */
    public Publisher<ProactiveFeedback> proactiveFeedbackStream(final UUID userId) {
        return feedbackSinks.computeIfAbsent(userId, k -> Sinks.many().multicast().onBackpressureBuffer())
                .asFlux();
    }

    /**
     * Publishes feedback to a specific user's feedback stream.
     *
     * @param userId user id
     * @param feedback
     */
    private void publishFeedbackToUser(final UUID userId, final ProactiveFeedback feedback) {
        final var sink = feedbackSinks.get(userId);
        if (sink != null) {
            sink.tryEmitNext(feedback);
            log.debug("Published feedback to user feedback stream: userId={}", userId);
        }
    }

    /**
     * Generates personalized feedback for the user based on their assignment performance.
     * Takes into account the user's player type and skill level to tailor the response.
     * Automatically pushes feedback to subscribed clients via GraphQL subscription AND
     * sends the message to the user's chat window.
     *
     * @param event the content progressed event containing assignment completion data
     * @return the generated feedback message, or null if feedback generation fails
     */
    public String generateFeedback(ContentProgressedEvent event) {
        try {
            log.info("Generating feedback for user {} on assignment {}", 
                    event.getUserId(), event.getContentId());

            String individualizedPrompt = getIndivualizedPromotProactiveTestDone(event.getUserId(), event.getCorrectness());

            String performanceContext = getPerformanceContext(event);

            String prompt = ollamaService.getTemplate(FEEDBACK_PROMPT_TEMPLATE);
            List<TemplateArgs> promptArgs = List.of(
                    TemplateArgs.builder()
                            .argumentName("correctness")
                            .argumentValue(String.format("%.2f", event.getCorrectness()))
                            .build(),
                    TemplateArgs.builder()
                            .argumentName("performance")
                            .argumentValue(performanceContext)
                            .build(),
                    TemplateArgs.builder()
                            .argumentName("individualizedPrompt")
                            .argumentValue(individualizedPrompt)
                            .build()
            );

            String error = "Oops, something went wrong generating proactive feedback! Your correctness was " +
                    String.format("%.2f", event.getCorrectness()) +
                    ". Please try again later.";

            TutorAnswer feedback = ollamaService.startQuery(
                    TutorAnswer.class, 
                    prompt, 
                    promptArgs, 
                    new TutorAnswer(error)
            );

            ProactiveFeedbackEntity feedbackEntity = ProactiveFeedbackEntity.builder()
                    .userId(event.getUserId())
                    .assessmentId(event.getContentId())
                    .feedbackText(feedback.getAnswer())
                    .correctness(event.getCorrectness())
                    .success(event.isSuccess())
                    .createdAt(OffsetDateTime.now())
                    .build();

            ProactiveFeedbackEntity savedEntity = proactiveFeedbackRepository.save(feedbackEntity);

            // Convert to DTO and publish to subscribed clients
            ProactiveFeedback feedbackDto = ProactiveFeedback.builder()
                    .setId(savedEntity.getId())
                    .setAssessmentId(savedEntity.getAssessmentId())
                    .setFeedbackText(savedEntity.getFeedbackText())
                    .setCorrectness(savedEntity.getCorrectness())
                    .setSuccess(savedEntity.getSuccess())
                    .setCreatedAt(savedEntity.getCreatedAt())
                    .build();
            publishFeedbackToUser(event.getUserId(), feedbackDto);

            log.info("Generated and saved feedback for user {}: {}", event.getUserId(), feedback.getAnswer());
            return feedback.getAnswer();

        } catch (Exception e) {
            log.error("Failed to generate feedback for user {}: {}", 
                    event.getUserId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Retrieves the most recent feedback for a user on a specific assessment.
     *
     * @param userId the user ID
     * @param assessmentId the assessment ID
     * @return optional feedback entity
     */
    public Optional<ProactiveFeedbackEntity> getFeedbackForAssignment(UUID userId, UUID assessmentId) {
        return proactiveFeedbackRepository.findFirstByUserIdAndAssessmentIdOrderByCreatedAtDesc(userId, assessmentId);
    }

    /**
     * Retrieves all feedback for a user, ordered by most recent first.
     *
     * @param userId the user ID
     * @return list of feedback entities
     */
    public List<ProactiveFeedbackEntity> getAllFeedbackForUser(UUID userId) {
        return proactiveFeedbackRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Retrieves the most recent feedback for a user and deletes it.
     * This is used when the frontend requests proactive feedback.
     *
     * @param userId the user ID
     * @return optional feedback text, or empty if no feedback exists
     */
    public Optional<String> getAndDeleteLatestFeedback(UUID userId) {
        List<ProactiveFeedbackEntity> feedbackList = proactiveFeedbackRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        if (feedbackList.isEmpty()) {
            return Optional.empty();
        }
        
        ProactiveFeedbackEntity latestFeedback = feedbackList.get(0);
        String feedbackText = latestFeedback.getFeedbackText();
        
        proactiveFeedbackRepository.delete(latestFeedback);
        log.info("Retrieved and deleted latest feedback for user {}: feedbackId={}", userId, latestFeedback.getId());
        
        return Optional.of(feedbackText);
    }

    /**
     * Gets player specific guidance for feedback personalization.
     */
    private String getIndivualizedPromotProactiveTestDone(UUID userId, double correctness) {
        try {
            Optional<HexadPlayerType> playerType = userPlayerTypeService.getPrimaryPlayerType(userId);
            if (playerType.isEmpty()) {
                log.debug("No player type found for user {}, using generic feedback", userId);
                return "";
            }

            HexadPlayerType type = playerType.get();
            
            if (type == HexadPlayerType.ACHIEVER || type == HexadPlayerType.PHILANTHROPIST || 
                type == HexadPlayerType.SOCIALISER || type == HexadPlayerType.PLAYER) {
                return getIndivualizedPromotProactiveTestDoneLogic(type, correctness);
            }
            
            // use heighest player type for anything but DISRUPTOR or FREE_SPIRIT
            if (type == HexadPlayerType.DISRUPTOR || type == HexadPlayerType.FREE_SPIRIT) {
                Optional<HexadPlayerType> alternativeType = userPlayerTypeService.getUserPlayerType(userId)
                    .map(entity -> {
                        Map<HexadPlayerType, Double> scores = entity.getPlayerTypePercentagesAsMap();
                        Map<Double, HexadPlayerType> map = new HashMap<>();
                        map.put(scores.get(HexadPlayerType.ACHIEVER), HexadPlayerType.ACHIEVER);
                        map.put(scores.get(HexadPlayerType.SOCIALISER), HexadPlayerType.SOCIALISER);
                        map.put(scores.get(HexadPlayerType.PHILANTHROPIST), HexadPlayerType.PHILANTHROPIST);
                        map.put(scores.get(HexadPlayerType.PLAYER), HexadPlayerType.PLAYER);

                        Map.Entry<Double, HexadPlayerType> highestPlayerTypeWithHandling = 
                            Collections.max(map.entrySet(), Map.Entry.comparingByKey());
                        return highestPlayerTypeWithHandling.getValue();
                    });
                
                if (alternativeType.isPresent()) {
                    return getIndivualizedPromotProactiveTestDoneLogic(alternativeType.get(), correctness);
                }
            }
            
            return "";
        } catch (Exception e) {
            log.warn("Failed to get individualized prompt for user {}: {}, using generic feedback", 
                    userId, e.getMessage());
            return "";
        }
    }

    /**
     * YOU YIELD NOT PASS! *insert gandalf throwing a keyboard*
     * 
     * @param playerType the user's primary player type. 
     * @requires to be ACHIEVER, PHILANTHROPIST, SOCIALISER or PLAYER
    */
    private String getIndivualizedPromotProactiveTestDoneLogic(HexadPlayerType playerType, double correctness) {
        return switch (playerType) {
            case ACHIEVER -> {
                if (correctness >= correctnessLevelMax) {
                    yield "Congratulate them on this perfect test and suggest the next challenge to tackle.";
                } else {
                    yield "Suggest the test to be repeated to achieve perfection.";
                }
            }
            case PHILANTHROPIST, SOCIALISER -> {
                if (correctness >= correctnessLevelHigh) {
                    yield "Congratulate them on their great performance and suggest helping classmates who might struggle with the material.";
                } else {
                    yield "Suggest reviewing the material together with classmates to improve understanding.";
                }
            }
            case PLAYER -> {
                if (correctness >= correctnessLevelMax) {
                    yield "Congratulate them on this perfect test and suggest the next challenge to unlock more rewards.";
                } else {
                    yield "Suggest the test to be repeated to unlock full rewards.";
                }
            }
            case DISRUPTOR, FREE_SPIRIT ->  {
                throw new IllegalArgumentException("DISRUPTOR and FREE_SPIRIT should not reach this logic branch");
            }
        };
    }

    /**
     * Gets skill level-specific guidance for feedback complexity.
     */
    private String getSkillLevelGuidance(double averageSkillLevel) {
        if (averageSkillLevel <= skillLevelLowThreshold) {
            return "Use encouraging and simple language. Focus on building confidence.";
        } else if (averageSkillLevel < skillLevelHighThreshold) {
            return "Use balanced feedback that reinforces understanding and suggests next steps.";
        } else {
            return "Use advanced terminology. Challenge them with deeper insights and connections.";
        }
    }

    /**
     * Determines performance context based on correctness and success.
     */
    private String getPerformanceContext(ContentProgressedEvent event) {
        double correctness = event.getCorrectness();
        boolean success = event.isSuccess();

        if (success && (correctness >= correctnessLevelMax)) {
            return "nearly perfect performance";
        } else if (success && (correctness >= correctnessLevelHigh)) {
            return "solid understanding demonstrated";
        } else {
            return "significant gaps in understanding";
        }
    }
}
