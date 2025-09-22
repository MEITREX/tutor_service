package de.unistuttgart.iste.meitrex.tutor_service.service.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Map;

@Getter
public class OllamaRequest {

    @JsonProperty("model")
    final String model;
    @JsonProperty("prompt")
    final String prompt;
    @JsonProperty("stream")
    final boolean stream;
    @JsonProperty("format")
    final Map<String, Object> format;

    public OllamaRequest(String model, String prompt, boolean stream, Map<String, Object> format) {
        this.model = model;
        this.prompt = prompt;
        this.stream = stream;
        this.format = format;
    }
}
