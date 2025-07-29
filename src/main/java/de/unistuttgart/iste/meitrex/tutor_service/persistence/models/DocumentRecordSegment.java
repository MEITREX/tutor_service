package de.unistuttgart.iste.meitrex.tutor_service.persistence.models;

import lombok.*;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
public class DocumentRecordSegment extends MediaRecordSegment {
    private int page;
    private String text;
}
