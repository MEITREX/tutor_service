package de.unistuttgart.iste.meitrex.tutor_service.service.models;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class VideoRecordSegment extends MediaRecordSegment {
    private double startTime;
}

