package de.unistuttgart.iste.meitrex.tutor_service.persistence.models;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LectureQuestionResponse {
    private String answer;
    private List<String> links = new ArrayList<>();

    public LectureQuestionResponse(String answer) {
        this.answer = answer;
    }
}
