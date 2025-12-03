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

    private static final List<String> PROMPT_TEMPLATES = List.of(
            "categorize_message_prompt.txt",
            "answer_lecture_question_prompt.txt"
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
     *
     * @param userQuestion the question asked by the user
     * @param courseId     the ID of the course, required for lecture-related questions
     * @param currentUser  the currently logged-in user
     * @return a response object containing the answer or a default message
     */
    public LectureQuestionResponse handleUserQuestion(String userQuestion, UUID courseId, LoggedInUser currentUser){

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
            String other = ("I'm currently unable to answer this type of message. " +
                    "However, I can still help you with questions about lecture materials or the MEITREX system :)");
            return new LectureQuestionResponse(other, List.of());
        }
        
        //Further process the question for the remaining categories 
        if(category == TutorCategory.LECTURE){
            return answerLectureQuestion(userQuestion, courseId, currentUser);
        } else if (category == TutorCategory.SYSTEM) {
            return new LectureQuestionResponse(
                    "At the moment, I can't answer any questions about the MEITREX system :(", List.of());
        }
        return new LectureQuestionResponse(ERROR_MESSAGE, List.of());
    }

    private LectureQuestionResponse answerLectureQuestion(String question, UUID courseId, LoggedInUser currentUser){        
        Optional<HexadPlayerType> playerType = userPlayerTypeService.getPrimaryPlayerType(currentUser.getId());
        if (playerType.isPresent()) {
            log.info("User {} has player type: {}", currentUser.getId(), playerType.get());
        } else {
            log.info("User {} has no player type set", currentUser.getId());
        }

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
                .limit(topSourceCount)
                .toList();

        if(documentSegments.isEmpty()){
            return new LectureQuestionResponse("No answer was found in the documents of the lecture.", List.of());
        }

        // Skill level range from 0 to 1
        List<UserSkillLevelEntity> skillLevels = userSkillLevelService.getAllSkillLevelsForUser(currentUser.getId());
        double averageSkillLevel;
        
        if (skillLevels.isEmpty()) {
            RequestUserSkillLevelEvent requestEvent = RequestUserSkillLevelEvent.builder()
                    .userId(currentUser.getId())
                    .build();
            topicPublisher.notifyRequestUserSkillLevel(requestEvent);
            
            averageSkillLevel = 0.5;
        } else {
            averageSkillLevel = skillLevels.stream()
                    .filter(Objects::nonNull)
                    .mapToDouble(UserSkillLevelEntity::getSkillLevelValue)
                    .average()
                    .orElse(0.5);
            log.info("User {} has {} skill levels with average: {}", 
                    currentUser.getId(), skillLevels.size(), averageSkillLevel);
        }
        
        String skillLevelPromotContent;
        if (averageSkillLevel <= skillLevelLowThreshold) {
            skillLevelPromotContent = SKILL_LEVEL_PROMPT_TEMPLATES.get(0);
            log.info("Using low skill level prompt for user {}", currentUser.getId());
        } else if (averageSkillLevel < skillLevelHighThreshold) {
            skillLevelPromotContent = SKILL_LEVEL_PROMPT_TEMPLATES.get(1);
        } else {
            skillLevelPromotContent = SKILL_LEVEL_PROMPT_TEMPLATES.get(2);
        }

        LectureQuestionResponse errorResponse = new LectureQuestionResponse(ERROR_MESSAGE, List.of());
        String prompt = ollamaService.getTemplate(PROMPT_TEMPLATES.get(1));
        String contentString = semanticSearchService.formatIntoNumberedListForPrompt(
                documentSegments.stream().map(DocumentRecordSegment::getText).toList());
        List<TemplateArgs> promptArgs = List.of(
            TemplateArgs.builder().argumentName("question").argumentValue(question).build(),
            TemplateArgs.builder().argumentName("content").argumentValue(contentString).build(),
            TemplateArgs.builder().argumentName("skill").argumentValue(skillLevelPromotContent).build()
        );

        TutorAnswer response = ollamaService.startQuery(
                TutorAnswer.class, prompt, promptArgs, new TutorAnswer(ERROR_MESSAGE));

        List<Source> sources = segmentSearchResults.stream()
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


}
