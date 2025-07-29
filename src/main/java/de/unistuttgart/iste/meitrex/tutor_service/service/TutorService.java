package de.unistuttgart.iste.meitrex.tutor_service.service;

import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.models.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

import java.util.List;
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
        List<SemanticSearchResult> relevantSegments = semanticSearchService.semanticSearch(question, courseId, currentUser);

        if(relevantSegments.isEmpty()){
            return new LectureQuestionResponse("No answer was found in the lecture.");
        }

        List<DocumentRecordSegment> documentSegments = relevantSegments.stream()
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

        List<String> relevantLinks = relevantSegments.stream()
                .flatMap(segment -> generateLinkForSegment(segment, courseId).stream())
                .toList();

        response.setLinks(relevantLinks);

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

    private List<String> generateLinkForSegment(SemanticSearchResult result, UUID courseId) {
        MediaRecordSegment segment = result.getMediaRecordSegment();
        MediaRecord mediaRecord = segment.getMediaRecord();
        if (mediaRecord == null) return List.of();
        if (mediaRecord.getContents() == null || mediaRecord.getContents().isEmpty()) return List.of();

        List<String> links = new ArrayList<>();

        for (Content content : mediaRecord.getContents()) {
            if (content == null || content.getId() == null) continue;

            UUID contentId = content.getId();
            UUID mediaRecordId = mediaRecord.getId();

            if (segment instanceof DocumentRecordSegment docSegment) {
                int page = docSegment.getPage() + 1;
                String url = "/courses/" + courseId + "/media/" + contentId +
                        "?selectedDocument=" + mediaRecordId + "&page=" + page;
                links.add(url);

            } else if (segment instanceof VideoRecordSegment videoSegment) {
                double startTime = videoSegment.getStartTime();
                String url = "/courses/" + courseId + "/media/" + contentId +
                        "?selectedVideo=" + mediaRecordId + "&videoPosition=" + startTime;
                links.add(url);
            }
        }

        return links;
    }

}
