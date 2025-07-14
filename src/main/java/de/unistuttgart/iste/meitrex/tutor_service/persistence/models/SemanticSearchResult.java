package de.unistuttgart.iste.meitrex.tutor_service.persistence.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class SemanticSearchResult {
    private double score;
    @JsonProperty("__typename")
    private String typename;
    private MediaRecordSegment mediaRecordSegment;

}
