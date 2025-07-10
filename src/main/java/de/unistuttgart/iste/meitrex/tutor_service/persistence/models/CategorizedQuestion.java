package de.unistuttgart.iste.meitrex.tutor_service.persistence.models;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategorizedQuestion {
    private String question;
    private Category category;

    public CategorizedQuestion(String question, Category category) {
        this.question = question;
        this.category = category;
    }

}
