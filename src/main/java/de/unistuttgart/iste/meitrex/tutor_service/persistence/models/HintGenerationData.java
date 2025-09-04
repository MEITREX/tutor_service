package de.unistuttgart.iste.meitrex.tutor_service.persistence.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class HintGenerationData {
    private final String questionText;
    private final String optionsText;
    private final String semanticSearchQuery;
}
