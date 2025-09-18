package de.unistuttgart.iste.meitrex.tutor_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.iste.meitrex.tutor_service.config.OllamaConfig;
import de.unistuttgart.iste.meitrex.tutor_service.service.models.OllamaRequest;
import de.unistuttgart.iste.meitrex.tutor_service.service.models.OllamaResponse;
import de.unistuttgart.iste.meitrex.tutor_service.service.models.TemplateArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaService {

    private final OllamaConfig config;

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    /**
     * Loads a prompt template from the prompt_templates resource folder and returns its content as a string.
     *
     * @param templateFileName the name of the template file
     * @return the content of the template file as a UTF-8 encoded string
     * @throws RuntimeException if the file is not found or cannot be read
     */
    public String getTemplate(String templateFileName)  {
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
            log.error("Failed to read template file: {}", templateFileName, e);
            throw new RuntimeException("Failed to read template file: " + templateFileName, e);
        }
    }

    /**
     * Replaces placeholders in a given prompt template with the corresponding argument values.
     * <p>
     * Each placeholder must be wrapped in double curly braces, e.g., <code>{{argumentName}}</code>.
     * For every {@link TemplateArgs} provided, the method searches for its placeholder in the template
     * and replaces it with the associated argument value.
     * </p>
     *
     * @param promptTemplate the template string containing placeholders in the form <code>{{argumentName}}</code>
     * @param args a list of {@link TemplateArgs} objects providing argument names and their values
     * @return the template string with all placeholders replaced by their corresponding values
     * @throws IllegalArgumentException if the template does not contain a placeholder for any provided argument
     */
    public String fillTemplate(String promptTemplate, List<TemplateArgs> args) {
        String filledTemplate = promptTemplate;
        for (TemplateArgs arg : args) {
            String placeholder = "{{" + arg.getArgumentName() + "}}";
            if(!promptTemplate.contains(placeholder)){
                throw new IllegalArgumentException("No such argument in this prompt: " + placeholder);
            }
            filledTemplate = filledTemplate.replace(placeholder, arg.getArgumentValue());
        }
        return filledTemplate;
    }

    /**
     * Starts a query to the LLM by filling a prompt template, sending it to Ollama,
     * and parsing the response into the given type.
     *
     * @param responseType the target class to parse the response into
     * @param prompt the template prompt text
     * @param templateArgs the arguments used to fill the template
     * @param error the fallback value if parsing or the request fails
     * @return the parsed response or the fallback error value
     */
    public <ResponseType> ResponseType startQuery(
            Class<ResponseType> responseType, String prompt, List<TemplateArgs> templateArgs, ResponseType error) {
        try {
            String filledPrompt = fillTemplate(prompt, templateArgs);

            OllamaRequest request = new OllamaRequest(this.config.getModel(), filledPrompt);
            OllamaResponse response = queryLLM(request);
            Optional<ResponseType> parsedResponse =
                    parseResponse(response, responseType);
            return parsedResponse.orElse(error);
        }catch (IOException | RuntimeException exception){
            log.error("Error while starting query: {}", exception.getMessage(), exception);
            return error;
        } catch (InterruptedException e) {
            log.error("Query interrupted: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            return error;
        }
    }

    /**
     * Sends the given request to the Ollama LLM endpoint and returns the raw response.
     *
     * @param request the request payload
     * @return the response from Ollama
     * @throws IOException if the request or response handling fails
     * @throws InterruptedException if the HTTP call is interrupted
     */
    private OllamaResponse queryLLM(OllamaRequest request) throws IOException, InterruptedException {
        final String json = jsonMapper.writeValueAsString(request);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(this.config.getUrl() + "/" + this.config.getEndpoint()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());

        OllamaResponse result = jsonMapper.readValue(response.body(), OllamaResponse.class);

        if (result.getError() != null) {
            throw new RuntimeException("Ollama returned error: " + result.getError());
        }

        return result;
    }

    /**
     * parse an ollama response to a specify type. It expects the response to be a valid json
     * If it fails to parse the response, it returns an empty optional
     *
     * @param ollamaResponse the response from the ollama server
     * @param responseType the type to parse the response to
     * @return an optional of the parsed response
     * @param <ResponseType> the type to cast to
     */
    public <ResponseType> Optional<ResponseType> parseResponse(OllamaResponse ollamaResponse, Class<ResponseType> responseType) {
        final String response = ollamaResponse.getResponse();
        if(responseType == null || response == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(jsonMapper.readValue(response, responseType));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
