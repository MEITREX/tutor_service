package de.unistuttgart.iste.meitrex.tutor_service.service;

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

@Service
@RequiredArgsConstructor
public class TutorService {

    private final String model = "mistral-nemo";
    private final DocProcAIServiceClient docProcAiServiceClient;
    private final ContentServiceClient contentServiceClient;

    private String ERROR_MESSAGE = ("Ups etwas ist schiefgegangen!"
            + "Die Anfrage kann nicht verarbeitet werden. Bitte versuchen Sie es nocheinmal");

    private static final List<String> PROMPT_TEMPLATES = List.of(
            "categorize_message_prompt.txt"
    );
    private final OllamaService ollamaService;

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

    private String fillTemplate(String promptTemplate, String question) {
        String filledTemplate = promptTemplate;
        filledTemplate = filledTemplate.replace("{{question}}", question);
        return filledTemplate;
    }


    /**
     * Takes the user input and passes it thorugh the whole pipeline/llm before returning the answer
     * @param userQuestion The question the user asked the AI Tutor
     * @return The answer of the LLM or "Error message"
     */
    public String handleUserQuestion(String userQuestion, UUID courseId){

        CategorizedQuestion categorizedQuestion = preprocessQuestion(userQuestion);

        Category category = categorizedQuestion.getCategory();
        
        //Return Answers for user-input that cannot be handled right now
        if(category == Category.UNRECOGNIZABLE){
            return ("Ich konnte Ihre Frage leider nicht verstehen."
                    + "Formulieren Sie die Frage bitte anders und stellen Sie diese erneut. Vielen Dank :)");
        }
        if(category == Category.OTHER){
            return ("So eine Art von Nachricht kann ich derzeit nicht beantworten. Bei Fragen über"
                    + " Vorlesungsmaterialien oder das MEITREX System kann ich Ihnen dennoch behilflich sein :)");
        }
        
        //Further process the question for the remaining categories 
        if(category == Category.LECTURE){
            if(courseId == null){
              return "Es ist etwas schiefgegangen! Sollte es sich um eine Frage über Vorlesungsmaterialien handeln, "
                      + "gehen Sie bitte in den Kurs auf den sich diese Frage bezieht. Vielen Dank! :)";
            }
            List<SemanticSearchResult> relevantSegments = semanticSearch(userQuestion, courseId);
            //TODO: Ticket for answering questions about material (Tests nicht vergessen!)
            return "Es wurden " + relevantSegments.size() + " relevante Segmente gefunden. "
                    + "Aktuell kann ich noch keine Fragen zum Lehrmaterial beantworten :(";
        } else if (category == Category.SYSTEM) {
            //TODO: Ticket for answering questions about system (Tests nicht vergessen!)
            return "Aktuell kann ich noch keine Fragen zum MEITREX System beantworten :(";
        }

        return ERROR_MESSAGE;

    }

    private List<SemanticSearchResult> semanticSearch(String question, UUID courseId) {
        try {

            List<UUID> contentIdsOfCourse = contentServiceClient.queryContentIdsOfCourse(courseId);

            return docProcAiServiceClient.semanticSearch(question, contentIdsOfCourse);

        } catch (ContentServiceConnectionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * preprocesses the question send by the user to be categorized into material or system question
     * @param userQuestion The question the user asked the AI Tutor
     * @return categorized question
     */
    public CategorizedQuestion preprocessQuestion(final String userQuestion){
        try{
            String prompt = getTemplate(PROMPT_TEMPLATES.get(0));
            String filledPrompt = fillTemplate(prompt, userQuestion);

            OllamaRequest request = new OllamaRequest(model, filledPrompt);
            OllamaResponse response = ollamaService.queryLLM(request);
            Optional<CategorizedQuestion> parsedResponse =
                    ollamaService.parseResponse(response, CategorizedQuestion.class);
            return parsedResponse.orElseGet(() -> new CategorizedQuestion("", Category.ERROR));

        }catch (IOException | InterruptedException | RuntimeException e){
            return new CategorizedQuestion("", Category.ERROR);
        }

    }

}
