package de.unistuttgart.iste.meitrex.tutor_service.controller;

import de.unistuttgart.iste.meitrex.common.event.HexadPlayerType;
import de.unistuttgart.iste.meitrex.common.event.UserHexadPlayerTypeSetEvent;
import de.unistuttgart.iste.meitrex.tutor_service.service.UserPlayerTypeService;
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
}
