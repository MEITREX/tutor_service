package de.unistuttgart.iste.meitrex.tutor_service.persistence.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;

import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "__typename",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DocumentRecordSegment.class, name = "DocumentRecordSegment"),
        @JsonSubTypes.Type(value = VideoRecordSegment.class, name = "VideoRecordSegment")
})
@NoArgsConstructor
@Getter
@Setter
public abstract class MediaRecordSegment {
    private UUID id;

    @JsonProperty("__typename")
    private String typename;
    private UUID mediaRecordId;

}


