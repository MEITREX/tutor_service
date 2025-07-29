package de.unistuttgart.iste.meitrex.tutor_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.iste.meitrex.tutor_service.config.OllamaConfig;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.models.OllamaRequest;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.models.OllamaResponse;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.models.TemplateArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OllamaService {


    private final String model = "llama3:8b-instruct-q4_0";
    private final String endpoint = "api/generate";
    private final OllamaConfig config;

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    public String getTemplate(String templateFileName)  {
        try{
            InputStream inputStream = this.getClass().getResourceAsStream("prompt_templates/" + templateFileName);
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
            System.out.println(e.getMessage());
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

    public <ResponseType> ResponseType startQuery(
            Class<ResponseType> responseType, String prompt, List<TemplateArgs> templateArgs, ResponseType error) {
        try {
            String filledPrompt = fillTemplate(prompt, templateArgs);

            OllamaRequest request = new OllamaRequest(model, filledPrompt);
            OllamaResponse response = queryLLM(request);
            Optional<ResponseType> parsedResponse =
                    parseResponse(response, responseType);
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

    /**
     * query the ollama server to query the LLM
     * @param request
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public OllamaResponse queryLLM(OllamaRequest request) throws IOException, InterruptedException {
        final String json = jsonMapper.writeValueAsString(request);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(this.config.getUrl() + "/" + endpoint))
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
