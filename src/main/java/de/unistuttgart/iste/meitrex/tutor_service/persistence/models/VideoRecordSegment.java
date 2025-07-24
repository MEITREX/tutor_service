package de.unistuttgart.iste.meitrex.tutor_service.persistence.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class VideoRecordSegment extends MediaRecordSegment {
    private double startTime;
}

