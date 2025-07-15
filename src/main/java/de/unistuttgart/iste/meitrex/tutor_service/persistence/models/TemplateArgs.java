package de.unistuttgart.iste.meitrex.tutor_service.persistence.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TemplateArgs {
    private String argumentName;
    private String argumentValue;
}
