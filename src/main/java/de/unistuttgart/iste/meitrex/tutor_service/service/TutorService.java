package de.unistuttgart.iste.meitrex.tutor_service.service;

import de.unistuttgart.iste.meitrex.common.event.AskedTutorAQuestionEvent;
import de.unistuttgart.iste.meitrex.common.event.HexadPlayerType;
import de.unistuttgart.iste.meitrex.common.event.RequestUserSkillLevelEvent;
import de.unistuttgart.iste.meitrex.common.event.TutorCategory;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.common.dapr.TopicPublisher;
import de.unistuttgart.iste.meitrex.generated.dto.DocumentSource;
import de.unistuttgart.iste.meitrex.generated.dto.LectureQuestionResponse;
import de.unistuttgart.iste.meitrex.generated.dto.Source;
import de.unistuttgart.iste.meitrex.generated.dto.VideoSource;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.UserSkillLevelEntity;
import de.unistuttgart.iste.meitrex.tutor_service.service.models.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TutorService {

    private final OllamaService ollamaService;
    private final SemanticSearchService semanticSearchService;
    private final TopicPublisher topicPublisher;
    private final UserPlayerTypeService userPlayerTypeService;
    private final UserSkillLevelService userSkillLevelService;
    private final ProactiveFeedbackService proactiveFeedbackService;
    private final ConversationHistoryService conversationHistoryService;
    private final StudentCodeSubmissionService studentCodeSubmissionService;
    @Value("${semantic.search.threshold.tutor:0.4}")
    private double scoreThreshold;
    @Value("${semantic.search.topN.tutor:5}")
    private long topSourceCount;
    @Value("${skill.level.threshold.low:0.3}")
    private double skillLevelLowThreshold;
    @Value("${skill.level.threshold.high:0.7}")
    private double skillLevelHighThreshold;

    private final String ERROR_MESSAGE = ("Oops, something went wrong! " +
            "The request could not be processed. Please try again.");

    private final String CODE_FEEDBACK_NO_SUBMISSION_MESSAGE = ("I couldn't find any code submission from you. " +
            "Please make sure you've committed your code to your assignment repository. " +
            "Open this respository and try again. Make sure to commit and push your code first!");

    private static final List<String> PROMPT_TEMPLATES = List.of(
            "categorize_message_prompt.txt",
            "answer_lecture_question_prompt.txt",
            "answer_code_feedback_prompt.txt",
            "answer_followup_question_prompt.txt"
    );
    private static final List<String> SKILL_LEVEL_PROMPT_TEMPLATES = List.of(
            "Provide a clear and simple hint that gently guides the user toward the next step without overwhelming them.",
            "Give the user a balanced hint that assists their reasoning while still allowing them to work out the solution independently. " +
            "Assume general familiarity with the topic",
            "Offer a sophisticated and subtle hint that challenges the users understanding without giving away the solution. " +
            "Use precise, domain-specific language and encourage deeper analysis or alternative approaches"
    );

    /**
     * Handles a user’s question by categorizing it and returning an appropriate response.
     * Lecture questions are further processed, while other categories currently return default answers.
     * Special handling: if the user input is "proactivefeedback", retrieves and deletes the latest saved feedback for the user.
     *
     * @param userQuestion the question asked by the user
     * @param courseId     the ID of the course, required for lecture-related questions
     * @param currentUser  the currently logged-in user
     * @return a response object containing the answer or a default message
     */
    public LectureQuestionResponse handleUserQuestion(String userQuestion, UUID courseId, LoggedInUser currentUser){

        // Special handling for proactive feedback retrieval. Will be removed once proactive feedback is integrated into the main flow and graphql works correctly.
        if ("proactivefeedback".equalsIgnoreCase(userQuestion.trim())) {
            Optional<String> feedback = proactiveFeedbackService.getAndDeleteLatestFeedback(currentUser.getId());
            if (feedback.isPresent()) {
                return new LectureQuestionResponse(feedback.get(), List.of());
            } else {
                return new LectureQuestionResponse("No proactive feedback available at the moment.", List.of());
            }
        }

        log.info("[TUTOR] User {} asked question: {}", currentUser.getId(), userQuestion);

        CategorizedQuestion categorizedQuestion = preprocessQuestion(userQuestion);

        TutorCategory category = categorizedQuestion.getCategory();

        //publish that the tutor was asked a question
        topicPublisher.notifyTutorQuestionAsked(new AskedTutorAQuestionEvent(
                currentUser.getId(),
                courseId,
                userQuestion,
                category
        ));
        
        //Return Answers for user-input that cannot be handled right now
        if(category == TutorCategory.UNRECOGNIZABLE){
            String unrecognizable = ("Unfortunately, I couldn't understand your question. " +
                    "Please rephrase it and ask again. Thank you :)");
            return new LectureQuestionResponse(unrecognizable, List.of());
        }
        if(category == TutorCategory.OTHER){
            return handleFollowUpQuestion(userQuestion, courseId, currentUser);
        }
        
        //Further process the question for the remaining categories 
        if (category == TutorCategory.LECTURE){
            return answerLectureQuestion(userQuestion, courseId, currentUser);
        } else if (category == TutorCategory.CODE_FEEDBACK) {
            return answerCodeFeedbackQuestion(userQuestion, courseId, currentUser);
        } else if (category == TutorCategory.SYSTEM) {
            return new LectureQuestionResponse(
                    "At the moment, I can't answer any questions about the MEITREX system :(", List.of());
        }
        return new LectureQuestionResponse(ERROR_MESSAGE, List.of());
    }

    private LectureQuestionResponse answerLectureQuestion(String question, UUID courseId, LoggedInUser currentUser){        
        Optional<HexadPlayerType> playerType = userPlayerTypeService.getPrimaryPlayerType(currentUser.getId());

        if(courseId == null){
            String response =
                "Something went wrong! If your question is about lecture materials, " +
                        "please navigate to the course it relates to. Thank you! :)";
            return new LectureQuestionResponse(response, List.of());
        }
        List<SemanticSearchResult> searchResults = semanticSearchService.semanticSearch(question, courseId, currentUser);

        List<SemanticSearchResult> segmentSearchResults = searchResults.stream()
                .filter(result -> result.getMediaRecordSegment() != null)
                .toList();

        if(segmentSearchResults.isEmpty()){
            return new LectureQuestionResponse("No answer was found in the lecture.", List.of());
        }

        List<DocumentRecordSegment> documentSegments = segmentSearchResults.stream()
                .filter(result -> result.getScore() <= scoreThreshold)
                .sorted(Comparator.comparingDouble(SemanticSearchResult::getScore).reversed())
                .map(SemanticSearchResult::getMediaRecordSegment)
                .filter(segment -> segment instanceof DocumentRecordSegment)
                .map(segment -> (DocumentRecordSegment) segment)
                .toList();

        if(documentSegments.isEmpty()){
            return new LectureQuestionResponse("No answer was found in the documents of the lecture.", List.of());
        }

        double averageSkillLevel = getAverageSkillLevel(currentUser.getId());
        log.info("User {} average skill level: {}", currentUser.getId(), averageSkillLevel);
        
        String skillLevelPromotContent = getSkillBasedFeedbackStyle(averageSkillLevel);

        LectureQuestionResponse errorResponse = new LectureQuestionResponse(ERROR_MESSAGE, List.of());
        String prompt = ollamaService.getTemplate(PROMPT_TEMPLATES.get(1));
        String contentString = semanticSearchService.formatIntoNumberedListForPrompt(
                documentSegments.stream().map(DocumentRecordSegment::getText).toList());
        
        String conversationHistory = conversationHistoryService.formatHistoryForPrompt(
                currentUser.getId(), courseId);
        
        log.info("Conversation history for user {} in course {}: {}", 
                currentUser.getId(), courseId, conversationHistory.isEmpty() ? "No history" : "Has history");

        List<TemplateArgs> promptArgs = List.of(
            TemplateArgs.builder().argumentName("question").argumentValue(question).build(),
            TemplateArgs.builder().argumentName("content").argumentValue(contentString).build(),
            TemplateArgs.builder().argumentName("skill").argumentValue(skillLevelPromotContent).build(),
            TemplateArgs.builder().argumentName("conversationHistory").argumentValue(conversationHistory).build()
        );

        TutorAnswer response = ollamaService.startQuery(
                TutorAnswer.class, prompt, promptArgs, new TutorAnswer(ERROR_MESSAGE));

        conversationHistoryService.addConversationExchange(
                currentUser.getId(), courseId, question, response.getAnswer());

        List<Source> sources = segmentSearchResults.stream()
                .filter(result -> result.getScore() <= scoreThreshold)
                .filter(result -> result.getMediaRecordSegment() instanceof DocumentRecordSegment)
                .sorted(Comparator.comparingDouble(SemanticSearchResult::getScore).reversed())
                .limit(topSourceCount)
                .map(this::generateSource)
                .filter(Objects::nonNull)
                .toList();
        return new LectureQuestionResponse(response.getAnswer(), sources);
    }

    /**
     * Handles follow-up questions by performing semantic search on conversation history and the user's prompt.
     * This method is used for questions categorized as OTHER, typically follow-ups to previous questions.
     * 
     * @param question the follow-up question asked by the user
     * @param courseId the ID of the course
     * @param currentUser the currently logged-in user
     * @return a response containing the answer based on conversation history
     */
    private LectureQuestionResponse handleFollowUpQuestion(String question, UUID courseId, LoggedInUser currentUser) {
        if (courseId == null) {
            String response = "Something went wrong! If your question is a follow-up to previous questions, " +
                    "please navigate to the course it relates to. Thank you! :)";
            return new LectureQuestionResponse(response, List.of());
        }

        String conversationHistory = conversationHistoryService.formatHistoryForPrompt(
                currentUser.getId(), courseId);

        if (conversationHistory.isEmpty()) {
            String response = ("I'm currently unable to answer this type of message. " +
                    "However, I can still help you with questions about lecture materials or the MEITREX system :)");
            return new LectureQuestionResponse(response, List.of());
        }

        String codeContext = "";
        List<de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.StudentCodeSubmissionEntity> submissions =
                studentCodeSubmissionService.getCodeSubmissionsForStudent(currentUser.getId());

        if (!submissions.isEmpty()) {
            de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.StudentCodeSubmissionEntity mostRecentSubmission =
                    submissions.stream()
                            .max(Comparator.comparing(de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.StudentCodeSubmissionEntity::getLastUpdated))
                            .orElse(null);

            if (mostRecentSubmission != null) {
                Optional<String> codeContextOpt = studentCodeSubmissionService.getCodeSubmissionContextForTutor(
                        currentUser.getId(),
                        mostRecentSubmission.getPrimaryKey().getAssignmentId());
                
                if (codeContextOpt.isPresent()) {
                    codeContext = codeContextOpt.get();
                }
            }
        }

        StringBuilder searchQueryBuilder = new StringBuilder(question)
                .append(" ")
                .append(conversationHistory);
        if (!codeContext.isEmpty()) {
            searchQueryBuilder.append(" ").append(codeContext);
        }
        String searchQuery = searchQueryBuilder.toString();

        List<SemanticSearchResult> searchResults = semanticSearchService.semanticSearch(
                searchQuery, courseId, currentUser);

        List<SemanticSearchResult> segmentSearchResults = searchResults.stream()
                .filter(result -> result.getMediaRecordSegment() != null)
                .toList();

        List<DocumentRecordSegment> documentSegments = segmentSearchResults.stream()
                .filter(result -> result.getScore() <= scoreThreshold)
                .sorted(Comparator.comparingDouble(SemanticSearchResult::getScore).reversed())
                .map(SemanticSearchResult::getMediaRecordSegment)
                .filter(segment -> segment instanceof DocumentRecordSegment)
                .map(segment -> (DocumentRecordSegment) segment)
                .toList();

        double averageSkillLevel = getAverageSkillLevel(currentUser.getId());
        log.info("User {} average skill level: {}", currentUser.getId(), averageSkillLevel);

        String skillLevelPromotContent = getSkillBasedFeedbackStyle(averageSkillLevel);

        String prompt = ollamaService.getTemplate(PROMPT_TEMPLATES.get(3));
        String contentString = semanticSearchService.formatIntoNumberedListForPrompt(
                documentSegments.stream().map(DocumentRecordSegment::getText).toList());

        log.info("Processing follow-up question for user {} in course {}",
                currentUser.getId(), courseId);

        List<TemplateArgs> promptArgs = List.of(
                TemplateArgs.builder().argumentName("question").argumentValue(question).build(),
                TemplateArgs.builder().argumentName("content").argumentValue(contentString).build(),
                TemplateArgs.builder().argumentName("skill").argumentValue(skillLevelPromotContent).build(),
                TemplateArgs.builder().argumentName("conversationHistory").argumentValue(conversationHistory).build(),
                TemplateArgs.builder().argumentName("codeContext").argumentValue(codeContext).build()
        );

        TutorAnswer response = ollamaService.startQuery(
                TutorAnswer.class, prompt, promptArgs, new TutorAnswer(ERROR_MESSAGE));

        conversationHistoryService.addConversationExchange(
                currentUser.getId(), courseId, question, response.getAnswer());

        List<Source> sources = segmentSearchResults.stream()
                .filter(result -> result.getScore() <= scoreThreshold)
                .filter(result -> result.getMediaRecordSegment() instanceof DocumentRecordSegment)
                .sorted(Comparator.comparingDouble(SemanticSearchResult::getScore).reversed())
                .limit(topSourceCount)
                .map(this::generateSource)
                .filter(Objects::nonNull)
                .toList();

        return new LectureQuestionResponse(response.getAnswer(), sources);
    }

    /**
     * preprocesses the question send by the user to be categorized into material or system question
     * @param userQuestion The question the user asked the AI Tutor
     * @return categorized question
     */
    private CategorizedQuestion preprocessQuestion(final String userQuestion){
        CategorizedQuestion error = new CategorizedQuestion("", TutorCategory.ERROR);
        String templateName = PROMPT_TEMPLATES.get(0);
        List<TemplateArgs> preprocessArgs = List.of(TemplateArgs.builder()
                .argumentName("question")
                .argumentValue(userQuestion)
                .build());
        String prompt = ollamaService.getTemplate(templateName);
        return ollamaService.startQuery(CategorizedQuestion.class, prompt, preprocessArgs, error);
    }

    private Source generateSource(SemanticSearchResult result){
        MediaRecordSegment segment = result.getMediaRecordSegment();

        if (segment instanceof DocumentRecordSegment docSegment) {
            DocumentSource docSource = new DocumentSource();
            docSource.setMediaRecordId(docSegment.getMediaRecordId());
            docSource.setPage(docSegment.getPage());
            return docSource;
        } else if (segment instanceof VideoRecordSegment videoSegment) {
            VideoSource videoSource = new VideoSource();
            videoSource.setMediaRecordId(videoSegment.getMediaRecordId());
            videoSource.setStartTime(videoSegment.getStartTime());
            return videoSource;
        } else {
            return null;
        }
    }

    /**
     * Handles code feedback questions by analyzing student's code submission and providing personalized feedback.
     * Retrieves the student's latest code submission and generates feedback based on their player type and skill level.
     * 
     * @param question the question asked by the student about their code
     * @param courseId the ID of the course
     * @param currentUser the currently logged-in user
     * @return a response containing the feedback
     */
    private LectureQuestionResponse answerCodeFeedbackQuestion(String question, UUID courseId, LoggedInUser currentUser) {
        if (courseId == null) {
            String response = "Something went wrong! If your question is about code for an assignment, " +
                    "please navigate to the course it relates to. Thank you! :)";
            return new LectureQuestionResponse(response, List.of());
        }

        Optional<HexadPlayerType> playerType = userPlayerTypeService.getPrimaryPlayerType(currentUser.getId());
        double averageSkillLevel = getAverageSkillLevel(currentUser.getId());
        String feedbackStyle = determineFeedbackStyle(playerType.orElse(null), averageSkillLevel);

        // Note: We need to determine the assignmentId from the context. For now, we'll try to get the most recent submission
        List<de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.StudentCodeSubmissionEntity> submissions =
                studentCodeSubmissionService.getCodeSubmissionsForStudent(currentUser.getId());

        if (submissions.isEmpty()) {
            log.info("[TUTOR-CODE-FEEDBACK] No code submissions found for user {}", currentUser.getId());
            return new LectureQuestionResponse(
                    CODE_FEEDBACK_NO_SUBMISSION_MESSAGE,
                    List.of());
        }

        de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.StudentCodeSubmissionEntity mostRecentSubmission =
                submissions.stream()
                        .max(Comparator.comparing(de.unistuttgart.iste.meitrex.tutor_service.persistence.entity.StudentCodeSubmissionEntity::getLastUpdated))
                        .orElse(null);

        if (mostRecentSubmission == null) {
            log.info("[TUTOR-CODE-FEEDBACK] Most recent submission is null for user {}", currentUser.getId());
            return new LectureQuestionResponse(
                    CODE_FEEDBACK_NO_SUBMISSION_MESSAGE,
                    List.of());
        }

        Optional<String> codeContext = studentCodeSubmissionService.getCodeSubmissionContextForTutor(
                currentUser.getId(),
                mostRecentSubmission.getPrimaryKey().getAssignmentId());

        if (codeContext.isEmpty()) {
            log.info("[TUTOR-CODE-FEEDBACK] Code context is empty for user {} and assignment {}", 
                    currentUser.getId(), mostRecentSubmission.getPrimaryKey().getAssignmentId());
            return new LectureQuestionResponse(
                    CODE_FEEDBACK_NO_SUBMISSION_MESSAGE,
                    List.of());
        }

        String conversationHistory = conversationHistoryService.formatHistoryForPrompt(
                currentUser.getId(), courseId);

        log.info("[TUTOR-CODE-FEEDBACK] Preparing to query LLM for code feedback - user: {}, assignment: {}, code context length: {}",
                currentUser.getId(), mostRecentSubmission.getPrimaryKey().getAssignmentId(), codeContext.get().length());

        String prompt = ollamaService.getTemplate(PROMPT_TEMPLATES.get(2));
        List<TemplateArgs> promptArgs = List.of(
                TemplateArgs.builder().argumentName("question").argumentValue(question).build(),
                TemplateArgs.builder().argumentName("codeContext").argumentValue(codeContext.get()).build(),
                TemplateArgs.builder().argumentName("feedbackStyle").argumentValue(feedbackStyle).build(),
                TemplateArgs.builder().argumentName("conversationHistory").argumentValue(conversationHistory).build()
        );

        TutorAnswer response = ollamaService.startQuery(
                TutorAnswer.class, prompt, promptArgs, new TutorAnswer(ERROR_MESSAGE));

        conversationHistoryService.addConversationExchange(
                currentUser.getId(), courseId, question, response.getAnswer());

        log.info("[TUTOR-CODE-FEEDBACK] Code feedback response generated for user {}", currentUser.getId());
        return new LectureQuestionResponse(response.getAnswer(), List.of());
    }

    /**
     * Determines the feedback style based on the user's player type and skill level.
     * 
     * @param playerType the user's primary player type
     * @param skillLevel the user's average skill level (0-1)
     * @return feedback style instructions for the AI
     */
    private String determineFeedbackStyle(HexadPlayerType playerType, double skillLevel) {
        if (playerType == null) {
            return getSkillBasedFeedbackStyle(skillLevel);
        }

        return switch (playerType) {
            case ACHIEVER -> "Provide a tip that helps the student achieve their goal. " + getSkillLevelGuidanceAchiever(skillLevel) + ".";
            
            case PHILANTHROPIST -> "Instead of directly pointing out the error, ask the student to explain the part of their code " +
                    "where you detected an issue. Encourage them to articulate their thought process, which will help them discover " +
                    "the problem themselves. Be supportive and guide them through reflection. If they do not identify the issue, " +
                    "and ask again, you can then point out the error.";
            
            case DISRUPTOR -> "Provide a tip with additional focus on edge cases and unconventional scenarios. " +
                    "Challenge the student to think about how their code handles unusual inputs or boundary conditions.";
            
            case SOCIALISER, FREE_SPIRIT, PLAYER -> getSkillBasedFeedbackStyle(skillLevel);
        };
    }

    /**
     * Gets skill level guidance text based on the average skill level.
     * 
     * @param skillLevel the average skill level (0-1)
     * @return skill level guidance text
     */
    private String getSkillLevelGuidanceAchiever(double skillLevel) {
        if (skillLevel <= skillLevelLowThreshold) {
            return "Focus on providing clear, simple hints that guide the student toward the next step without overwhelming them.";
        } else if (skillLevel < skillLevelHighThreshold) {
            return "Offer balanced hints that assist the student's reasoning while still allowing them to work out the solution independently. " +
                   "Assume general familiarity with the topic.";
        } else {
            return "Assume advanced understanding.";
        }
    }

    /**
     * Gets feedback style based only on skill level (for player types that don't need special treatment).
     * 
     * @param skillLevel the average skill level (0-1)
     * @return feedback style instructions
     */
    private String getSkillBasedFeedbackStyle(double skillLevel) {
        if (skillLevel <= skillLevelLowThreshold) {
            return SKILL_LEVEL_PROMPT_TEMPLATES.get(0);
        } else if (skillLevel < skillLevelHighThreshold) {
            return SKILL_LEVEL_PROMPT_TEMPLATES.get(1);
        } else {
            return SKILL_LEVEL_PROMPT_TEMPLATES.get(2);
        }
    }

    /**
     * Calculates and returns the average skill level for a user.
     * Requests skill levels if none are available and defaults to 0.5.
     * 
     * @param userId the user's ID
     * @return average skill level (0-1), defaults to 0.5 if unavailable
     */
    private double getAverageSkillLevel(UUID userId) {
        List<UserSkillLevelEntity> skillLevels = userSkillLevelService.getAllSkillLevelsForUser(userId);
        
        if (skillLevels.isEmpty()) {
            RequestUserSkillLevelEvent requestEvent = RequestUserSkillLevelEvent.builder()
                    .userId(userId)
                    .build();
            topicPublisher.notifyRequestUserSkillLevel(requestEvent);
            
            return 0.5;
        }
        
        return skillLevels.stream()
                .filter(Objects::nonNull)
                .mapToDouble(UserSkillLevelEntity::getSkillLevelValue)
                .average()
                .orElse(0.5);
    }


}
