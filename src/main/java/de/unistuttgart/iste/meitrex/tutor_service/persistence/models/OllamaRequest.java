package de.unistuttgart.iste.meitrex.tutor_service.persistence.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OllamaRequest {

    @JsonProperty("model")
    final String model;
    @JsonProperty("prompt")
    final String prompt;
    @JsonProperty("stream")
    final boolean stream;

    public OllamaRequest(String model, String prompt, boolean stream) {
        this.model = model;
        this.prompt = prompt;
        this.stream = stream;
    }

    public OllamaRequest(String model, String prompt) {
        this(model, prompt, false);
    }

    /**
     * Getters for the fields
     */
    public String getModel() {
        return model;
    }

    /**
     * Getters for the fields
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * Getters for the fields
     */
    public boolean isStream() {
        return stream;
    }
}
