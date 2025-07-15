package de.unistuttgart.iste.meitrex.tutor_service.service;

import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser.UserRoleInCourse;
import de.unistuttgart.iste.meitrex.content_service.client.ContentServiceClient;
import de.unistuttgart.iste.meitrex.content_service.exception.ContentServiceConnectionException;
import de.unistuttgart.iste.meitrex.tutor_service.client.DocProcAIServiceClient;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.models.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Optional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.user_handling.UserCourseAccessValidator.validateUserHasAccessToCourse;

@Service
@RequiredArgsConstructor
public class TutorService {

    private final String model = "llama3:8b-instruct-q4_0";
    private final DocProcAIServiceClient docProcAiServiceClient;
    private final ContentServiceClient contentServiceClient;
    private final OllamaService ollamaService;

    private final String ERROR_MESSAGE = ("Oops, something went wrong! " +
            "The request could not be processed. Please try again.");

    private static final List<String> PROMPT_TEMPLATES = List.of(
            "categorize_message_prompt.txt",
            "answer_lecture_question_prompt.txt"
    );

    private String getTemplate(String templateFileName)  {
        try{
            InputStream inputStream = this.getClass().getResourceAsStream("/prompt_templates/" + templateFileName);
            if (inputStream == null) {
                throw new FileNotFoundException("Template file not found: " + templateFileName);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder template = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                template.append(line).append("\n");
            }
            reader.close();
            return template.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read template file: " + templateFileName, e);
        }
    }

    private String fillTemplate(String promptTemplate, List<TemplateArgs> args) {
        String filledTemplate = promptTemplate;
        for (TemplateArgs arg : args) {
            String placeholder = "{{" + arg.getArgumentName() + "}}";
            if(!promptTemplate.contains(placeholder)){
                throw new IllegalArgumentException("No such argument in this prompt");
            }
            filledTemplate = filledTemplate.replace(placeholder, arg.getArgumentValue());
        }
        return filledTemplate;
    }


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
            return answerLectureQuestion(userQuestion, courseId);
        } else if (category == Category.SYSTEM) {
            return new LectureQuestionResponse(
                    "At the moment, I can't answer any questions about the MEITREX system :(");
        }
        return new LectureQuestionResponse(ERROR_MESSAGE);
    }

    private LectureQuestionResponse answerLectureQuestion(String question, UUID courseId){
        if(courseId == null){
            String response =
                "Something went wrong! If your question is about lecture materials, " +
                        "please navigate to the course it relates to. Thank you! :)";
            return new LectureQuestionResponse(response);
        }
        validateUserHasAccessToCourse(currentUser, UserRoleInCourse.STUDENT, courseId);
        List<SemanticSearchResult> relevantSegments = semanticSearch(question, courseId);

        if(relevantSegments.isEmpty()){
            return new LectureQuestionResponse("No answer was found in the lecture.");
        }
        LectureQuestionResponse errorResponse = new LectureQuestionResponse(ERROR_MESSAGE);
        String prompt = getTemplate(PROMPT_TEMPLATES.get(1));
        List<TemplateArgs> promptArgs = List.of(
            TemplateArgs.builder().argumentName("question").argumentValue(question).build(),
            TemplateArgs
                .builder()
                .argumentName("content")
                .argumentValue(formatRetrievedContent(relevantSegments))
                .build()
        );
        return startQuery(LectureQuestionResponse.class, prompt, promptArgs, errorResponse);
    }

    private List<SemanticSearchResult> semanticSearch(String question, UUID courseId) {
        try {

            List<UUID> contentIdsOfCourse = contentServiceClient.queryContentIdsOfCourse(courseId);

            return docProcAiServiceClient.semanticSearch(question, contentIdsOfCourse);

        } catch (ContentServiceConnectionException e) {
            throw new RuntimeException(String.valueOf(e));
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    /**
     * preprocesses the question send by the user to be categorized into material or system question
     * @param userQuestion The question the user asked the AI Tutor
     * @return categorized question
     */
    public CategorizedQuestion preprocessQuestion(final String userQuestion){
        CategorizedQuestion error = new CategorizedQuestion("", Category.ERROR);
        String prompt = getTemplate(PROMPT_TEMPLATES.get(0));
        List<TemplateArgs> preprocessArgs = List.of(TemplateArgs.builder()
                .argumentName("question")
                .argumentValue(userQuestion)
                .build());
        return startQuery(CategorizedQuestion.class, prompt, preprocessArgs, error);
    }

    private <ResponseType> ResponseType startQuery(
            Class<ResponseType> responseType, String prompt, List<TemplateArgs> templateArgs, ResponseType error) {
        try {
            String filledPrompt = fillTemplate(prompt, templateArgs);

            OllamaRequest request = new OllamaRequest(model, filledPrompt);
            OllamaResponse response = ollamaService.queryLLM(request);
            Optional<ResponseType> parsedResponse =
                    ollamaService.parseResponse(response, responseType);
            return parsedResponse.orElse(error);
        }catch (IOException | RuntimeException exception){
            System.err.println(exception.getMessage());
            return error;
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
            Thread.currentThread().interrupt();
            return error;
        }
    }

    public String formatRetrievedContent(List<SemanticSearchResult> results) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            SemanticSearchResult result = results.get(i);
            MediaRecordSegment segment = result.getMediaRecordSegment();
            if (segment == null || segment.getText() == null) continue;

            sb.append("[").append(i + 1).append("] ")
                    .append(segment.getText().trim())
                    .append("\n\n");
        }
        return sb.toString().trim();
    }

}
