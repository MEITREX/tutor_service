package de.unistuttgart.iste.meitrex.tutor_service.persistence.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class SemanticSearchResult {
    private double score;
    private String __typename;
    private MediaRecordSegment mediaRecordSegment;

}
