package de.unistuttgart.iste.meitrex.tutor_service.service.models;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SemanticSearchQuery {
    private String query;
}
