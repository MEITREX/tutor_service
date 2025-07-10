package de.unistuttgart.iste.meitrex.tutor_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.iste.meitrex.tutor_service.config.OllamaConfig;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.models.OllamaRequest;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.models.OllamaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OllamaService {

    private final String endpoint = "api/generate";
    private final OllamaConfig config;

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

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
