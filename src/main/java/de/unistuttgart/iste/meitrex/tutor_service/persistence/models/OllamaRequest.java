package de.unistuttgart.iste.meitrex.tutor_service.persistence.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class OllamaRequest {

    @JsonProperty("model")
    final String model;
    @JsonProperty("prompt")
    final String prompt;
    @JsonProperty("stream")
    final boolean stream;
    @JsonProperty("format")
    final String format;

    public OllamaRequest(String model, String prompt, boolean stream) {
        this.model = model;
        this.prompt = prompt;
        this.stream = stream;
        this.format = "json";
    }

    public OllamaRequest(String model, String prompt) {
        this(model, prompt, false);
    }

}
