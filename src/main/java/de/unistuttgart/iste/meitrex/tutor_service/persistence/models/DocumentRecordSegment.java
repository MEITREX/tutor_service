package de.unistuttgart.iste.meitrex.tutor_service.persistence.models;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class DocumentRecordSegment extends MediaRecordSegment {
    private int page;
    private String text;
}
