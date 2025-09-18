package de.unistuttgart.iste.meitrex.tutor_service.service;

import de.unistuttgart.iste.meitrex.common.event.AskedTutorAQuestionEvent;
import de.unistuttgart.iste.meitrex.common.event.TutorCategory;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.common.dapr.TopicPublisher;
import de.unistuttgart.iste.meitrex.generated.dto.DocumentSource;
import de.unistuttgart.iste.meitrex.generated.dto.LectureQuestionResponse;
import de.unistuttgart.iste.meitrex.generated.dto.Source;
import de.unistuttgart.iste.meitrex.generated.dto.VideoSource;
import de.unistuttgart.iste.meitrex.tutor_service.service.models.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TutorService {

    private final OllamaService ollamaService;
    private final SemanticSearchService semanticSearchService;
    private final TopicPublisher topicPublisher;
    @Value("${semantic.search.threshold.tutor:0.4}")
    private double scoreThreshold;

    private final String ERROR_MESSAGE = ("Oops, something went wrong! " +
            "The request could not be processed. Please try again.");

    private static final List<String> PROMPT_TEMPLATES = List.of(
            "categorize_message_prompt.txt",
            "answer_lecture_question_prompt.txt"
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
                .map(SemanticSearchResult::getMediaRecordSegment)
                .filter(segment -> segment instanceof DocumentRecordSegment)
                .map(segment -> (DocumentRecordSegment) segment)
                .toList();

        if(documentSegments.isEmpty()){
            return new LectureQuestionResponse("No answer was found in the documents of the lecture.", List.of());
        }

        LectureQuestionResponse errorResponse = new LectureQuestionResponse(ERROR_MESSAGE, List.of());
        String prompt = ollamaService.getTemplate(PROMPT_TEMPLATES.get(1));
        String contentString = semanticSearchService.formatIntoNumberedListForPrompt(
                documentSegments.stream().map(DocumentRecordSegment::getText).toList());
        List<TemplateArgs> promptArgs = List.of(
            TemplateArgs.builder().argumentName("question").argumentValue(question).build(),
            TemplateArgs.builder().argumentName("content").argumentValue(contentString).build()
        );

        LectureQuestionResponse response = ollamaService.startQuery(
                LectureQuestionResponse.class, prompt, promptArgs, errorResponse);

        List<Source> sources = segmentSearchResults.stream()
                .map(this::generateSource)
                .filter(Objects::nonNull)
                .toList();

        if(!sources.isEmpty()){
            response.setSources(sources);
        }

        return response;
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
