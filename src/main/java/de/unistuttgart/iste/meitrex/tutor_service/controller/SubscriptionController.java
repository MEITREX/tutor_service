package de.unistuttgart.iste.meitrex.tutor_service.controller;

import de.unistuttgart.iste.meitrex.common.event.ContentProgressedEvent;
import de.unistuttgart.iste.meitrex.common.event.HexadPlayerType;
import de.unistuttgart.iste.meitrex.common.event.StudentCodeSubmittedEvent;
import de.unistuttgart.iste.meitrex.common.event.UserHexadPlayerTypeSetEvent;
import de.unistuttgart.iste.meitrex.common.event.skilllevels.UserSkillLevelChangedEvent;
import de.unistuttgart.iste.meitrex.tutor_service.service.ProactiveFeedbackService;
import de.unistuttgart.iste.meitrex.tutor_service.service.StudentCodeSubmissionService;
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
    private final StudentCodeSubmissionService studentCodeSubmissionService;

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
            event.getPlayerTypePercentages().forEach((type, percentage) -> {
                if (percentage != null) {
                    playerTypePercentages.put(type, (double) percentage);
                }
            });
            
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
            ContentProgressedEvent event = cloudEvent.getData();
            
            if (event == null) {
                log.warn("Received ContentProgressedEvent with null data");
                return;
            }
            
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
        });
    }

    /**
     * Handles the student-code-submitted event.
     * Saves the student's code submission when received.
     * Only keeps the latest submission per student per assignment.
     * 
     * Currently, only .java files with valid content and filenames are stored.
     * 
     * @param cloudEvent the cloud event containing the student code submission data
     * @param headers request headers from Dapr
     * @return Mono<Void> for reactive processing
     */
    @Topic(name = "student-code-submitted", pubsubName = "meitrex")
    @PostMapping(path = "/student-code-submitted-pubsub")
    public Mono<Void> onStudentCodeSubmittedEvent(@RequestBody CloudEvent<StudentCodeSubmittedEvent> cloudEvent,
                                                    @RequestHeader Map<String, String> headers) {
        return Mono.fromRunnable(() -> {
            StudentCodeSubmittedEvent event = cloudEvent.getData();
            
            if (event == null) {
                log.warn("Received StudentCodeSubmittedEvent with null data");
                return;
            }
            
            log.info("Received StudentCodeSubmittedEvent for student: {}, assignment: {}, commit: {}", 
                    event.getStudentId(), 
                    event.getAssignmentId(),
                    event.getCommitSha());
            
            try {
                Map<String, String> filteredFiles = filterJavaFiles(event.getFiles());
                
                log.info("Filtered {} files down to {} Java files for student {} on assignment {}", 
                        event.getFiles() != null ? event.getFiles().size() : 0,
                        filteredFiles.size(),
                        event.getStudentId(),
                        event.getAssignmentId());
                
                studentCodeSubmissionService.saveCodeSubmission(
                        event.getStudentId(),
                        event.getAssignmentId(),
                        event.getCourseId(),
                        event.getRepositoryUrl(),
                        event.getCommitSha(),
                        event.getCommitTimestamp(),
                        filteredFiles,
                        event.getBranch()
                );
            } catch (Exception e) {
                log.error("Error processing StudentCodeSubmittedEvent for student {} on assignment {}: {}", 
                        event.getStudentId(), event.getAssignmentId(), e.getMessage(), e);
            }
        });
    }
    
    /**
     * Filters files to only include .java files with valid content and filename.
     * 
     * @param files map of file paths to file contents
     * @return filtered map containing only valid Java files
     */
    private Map<String, String> filterJavaFiles(Map<String, String> files) {
        if (files == null) {
            return new HashMap<>();
        }
        
        Map<String, String> filteredFiles = new HashMap<>();
        
        files.forEach((filename, content) -> {
            if (filename != null && 
                content != null && 
                filename.endsWith(".java")) {
                filteredFiles.put(filename, content);
            }
        });
        
        return filteredFiles;
    }
}
