package de.unistuttgart.iste.meitrex.tutor_service.persistence.models;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LectureQuestionResponse {
    private String answer;
    private List<Source> sources = new ArrayList<>();

    public LectureQuestionResponse(String answer) {
        this.answer = answer;
    }

    public interface Source {
        UUID getMediaRecordId();
    }

    @Getter
    @Setter
    public static class DocumentSource implements Source {
        private UUID mediaRecordId;
        private int page;
    }

    @Getter
    @Setter
    public static class VideoSource implements Source {
        private UUID mediaRecordId;
        private double startTime;
    }

}
