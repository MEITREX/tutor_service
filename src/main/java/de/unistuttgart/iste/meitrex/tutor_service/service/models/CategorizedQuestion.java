package de.unistuttgart.iste.meitrex.tutor_service.service.models;

import de.unistuttgart.iste.meitrex.common.event.TutorCategory;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategorizedQuestion {
    private String question;
    private TutorCategory category;

    public CategorizedQuestion(String question, TutorCategory category) {
        this.question = question;
        this.category = category;
    }

}
