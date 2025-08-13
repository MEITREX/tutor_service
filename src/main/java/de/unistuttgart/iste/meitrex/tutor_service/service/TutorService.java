package de.unistuttgart.iste.meitrex.tutor_service.service;

import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.models.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TutorService {

    private final OllamaService ollamaService;
    private final SemanticSearchService semanticSearchService;

    private final String ERROR_MESSAGE = ("Oops, something went wrong! " +
            "The request could not be processed. Please try again.");

    private static final List<String> PROMPT_TEMPLATES = List.of(
            "categorize_message_prompt.txt",
            "answer_lecture_question_prompt.txt"
    );

    /**
     * Takes the user input and passes it thorugh the whole pipeline/llm before returning the answer
     * @param userQuestion The question the user asked the AI Tutor
     * @return The answer of the LLM or "Error message"
     */
    public LectureQuestionResponse handleUserQuestion(String userQuestion, UUID courseId, LoggedInUser currentUser){

        CategorizedQuestion categorizedQuestion = preprocessQuestion(userQuestion);

        Category category = categorizedQuestion.getCategory();
        
        //Return Answers for user-input that cannot be handled right now
        if(category == Category.UNRECOGNIZABLE){
            String unrecognizable = ("Unfortunately, I couldn't understand your question. " +
                    "Please rephrase it and ask again. Thank you :)");
            return new LectureQuestionResponse(unrecognizable);
        }
        if(category == Category.OTHER){
            String other = ("I'm currently unable to answer this type of message. " +
                    "However, I can still help you with questions about lecture materials or the MEITREX system :)");
            return new LectureQuestionResponse(other);
        }
        
        //Further process the question for the remaining categories 
        if(category == Category.LECTURE){
            return answerLectureQuestion(userQuestion, courseId, currentUser);
        } else if (category == Category.SYSTEM) {
            return new LectureQuestionResponse(
                    "At the moment, I can't answer any questions about the MEITREX system :(");
        }
        return new LectureQuestionResponse(ERROR_MESSAGE);
    }

    private LectureQuestionResponse answerLectureQuestion(String question, UUID courseId, LoggedInUser currentUser){
        if(courseId == null){
            String response =
                "Something went wrong! If your question is about lecture materials, " +
                        "please navigate to the course it relates to. Thank you! :)";
            return new LectureQuestionResponse(response);
        }
        List<SemanticSearchResult> searchResults = semanticSearchService.semanticSearch(question, courseId, currentUser);

        List<SemanticSearchResult> segmentSearchResults = searchResults.stream()
                .filter(result -> result.getMediaRecordSegment() != null)
                .toList();

        if(segmentSearchResults.isEmpty()){
            return new LectureQuestionResponse("No answer was found in the lecture.");
        }

        List<DocumentRecordSegment> documentSegments = segmentSearchResults.stream()
                .map(SemanticSearchResult::getMediaRecordSegment)
                .filter(segment -> segment instanceof DocumentRecordSegment)
                .map(segment -> (DocumentRecordSegment) segment)
                .toList();

        if(documentSegments.isEmpty()){
            return new LectureQuestionResponse("No answer was found in the documents of the lecture.");
        }

        LectureQuestionResponse errorResponse = new LectureQuestionResponse(ERROR_MESSAGE);
        String prompt = ollamaService.getTemplate(PROMPT_TEMPLATES.get(1));
        String contentString = semanticSearchService.formatDocumentSegmentsForPrompt(documentSegments);
        List<TemplateArgs> promptArgs = List.of(
            TemplateArgs.builder().argumentName("question").argumentValue(question).build(),
            TemplateArgs.builder().argumentName("content").argumentValue(contentString).build()
        );

        LectureQuestionResponse response = ollamaService.startQuery(
                LectureQuestionResponse.class, prompt, promptArgs, errorResponse);

        segmentSearchResults.forEach(result -> {
            if (result.getMediaRecordSegment() instanceof DocumentRecordSegment) {
                System.out.println("Score for the one with id "
                        + ((DocumentRecordSegment) result.getMediaRecordSegment()).getPage() + ": " + result.getScore());
            }
        });

        List<LectureQuestionResponse.Source> sources = segmentSearchResults.stream()
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
    public CategorizedQuestion preprocessQuestion(final String userQuestion){
        CategorizedQuestion error = new CategorizedQuestion("", Category.ERROR);
        String templateName = PROMPT_TEMPLATES.get(0);
        List<TemplateArgs> preprocessArgs = List.of(TemplateArgs.builder()
                .argumentName("question")
                .argumentValue(userQuestion)
                .build());
        String prompt = ollamaService.getTemplate(templateName);
        return ollamaService.startQuery(CategorizedQuestion.class, prompt, preprocessArgs, error);
    }

    private LectureQuestionResponse.Source generateSource(SemanticSearchResult result){
        MediaRecordSegment segment = result.getMediaRecordSegment();

        if (segment instanceof DocumentRecordSegment docSegment) {
            LectureQuestionResponse.DocumentSource docSource = new LectureQuestionResponse.DocumentSource();
            docSource.setMediaRecordId(docSegment.getMediaRecordId());
            docSource.setPage(docSegment.getPage());
            return docSource;
        } else if (segment instanceof VideoRecordSegment videoSegment) {
            LectureQuestionResponse.VideoSource videoSource = new LectureQuestionResponse.VideoSource();
            videoSource.setMediaRecordId(videoSegment.getMediaRecordId());
            videoSource.setStartTime(videoSegment.getStartTime());
            return videoSource;
        } else {
            return null;
        }
    }


}
