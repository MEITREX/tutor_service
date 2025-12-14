package de.unistuttgart.iste.meitrex.tutor_service.controller;

import de.unistuttgart.iste.meitrex.common.event.ContentProgressedEvent;
import de.unistuttgart.iste.meitrex.common.event.HexadPlayerType;
import de.unistuttgart.iste.meitrex.common.event.UserHexadPlayerTypeSetEvent;
import de.unistuttgart.iste.meitrex.common.event.skilllevels.UserSkillLevelChangedEvent;
import de.unistuttgart.iste.meitrex.tutor_service.service.ProactiveFeedbackService;
import de.unistuttgart.iste.meitrex.tutor_service.service.UserPlayerTypeService;
import de.unistuttgart.iste.meitrex.tutor_service.service.UserSkillLevelService;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for subscribing to Dapr pub/sub events.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SubscriptionController {

    private final UserPlayerTypeService userPlayerTypeService;
    private final UserSkillLevelService userSkillLevelService;
    private final ProactiveFeedbackService proactiveFeedbackService;

    /**
     * Handles the user-hexad-player-type-set event.
     * Saves the user's player type information when received.
     * 
     * @param cloudEvent the cloud event containing the user hexad player type data
     * @param headers request headers from Dapr
     * @return Mono<Void> for reactive processing
     */
    @Topic(name = "user-hexad-player-type-set", pubsubName = "meitrex")
    @PostMapping(path = "/user-hexad-player-type-set-pubsub")
    public Mono<Void> onUserHexadPlayerTypeSetEvent(@RequestBody CloudEvent<UserHexadPlayerTypeSetEvent> cloudEvent,
                                                      @RequestHeader Map<String, String> headers) {
        return Mono.fromRunnable(() -> {
            UserHexadPlayerTypeSetEvent event = cloudEvent.getData();
            
            log.info("Received UserHexadPlayerTypeSetEvent for user: {}, primaryType: {}", 
                    event.getUserId(), 
                    event.getPrimaryPlayerType());
            
            // Convert Float playerTypePercentages to Double
            Map<HexadPlayerType, Double> playerTypePercentages = new HashMap<>();
            event.getPlayerTypePercentages().forEach((type, percentage) -> 
                playerTypePercentages.put(type, percentage.doubleValue())
            );
            
            // Save the player type information
            userPlayerTypeService.saveUserPlayerType(
                    event.getUserId(),
                    event.getPrimaryPlayerType(),
                    playerTypePercentages
            );
        });
    }

    /**
     * Handles the user-skill-level-changed event.
     * Saves the user's skill level information when received.
     * 
     * @param cloudEvent the cloud event containing the user skill level data
     * @param headers request headers from Dapr
     * @return Mono<Void> for reactive processing
     */
    @Topic(name = "user-skill-level-changed", pubsubName = "meitrex")
    @PostMapping(path = "/user-skill-level-changed-pubsub")
    public Mono<Void> onUserSkillLevelChangedEvent(@RequestBody CloudEvent<UserSkillLevelChangedEvent> cloudEvent,
                                                     @RequestHeader Map<String, String> headers) {
        return Mono.fromRunnable(() -> {
            UserSkillLevelChangedEvent event = cloudEvent.getData();

            userSkillLevelService.saveUserSkillLevel(
                    event.getUserId(),
                    event.getSkillId(),
                    event.getNewValue()
            );
        });
    }

    /**
     * Handles the content-progressed event.
     * Generates proactive tutor feedback when a user completes an assignment.
     * Currently supports proactive feedback for assignments and quizzes.
     * 
     * @param cloudEvent the cloud event containing the content progressed data
     * @param headers request headers from Dapr
     * @return Mono<Void> for reactive processing
     */
    @Topic(name = "content-progressed", pubsubName = "meitrex")
    @PostMapping(path = "/content-progressed-pubsub")
    public Mono<Void> onContentProgressedEvent(@RequestBody CloudEvent<ContentProgressedEvent> cloudEvent,
                                                 @RequestHeader Map<String, String> headers) {                         
        return Mono.fromRunnable(() -> {
            try {
                ContentProgressedEvent event = cloudEvent.getData();
                
                log.info("Received ContentProgressedEvent for user: {}, content: {}, type: {}", 
                        event.getUserId(), 
                        event.getContentId(),
                        event.getContentType());
                
                if (event.getContentType() == ContentProgressedEvent.ContentType.ASSIGNMENT ||
                    event.getContentType() == ContentProgressedEvent.ContentType.QUIZ) {
                    try {
                        proactiveFeedbackService.generateFeedback(event);
                    } catch (Exception e) {
                        log.error("Failed to generate feedback for user {} on content {}: {}", 
                                event.getUserId(), event.getContentId(), e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                log.error("Error processing ContentProgressedEvent: {}", e.getMessage(), e);
            }
        });
    }
}
