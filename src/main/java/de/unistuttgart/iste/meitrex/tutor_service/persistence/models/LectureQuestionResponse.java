package de.unistuttgart.iste.meitrex.tutor_service.persistence.models;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LectureQuestionResponse {
    private String answer;

    public LectureQuestionResponse(String answer) {
        this.answer = answer;
    }
}
