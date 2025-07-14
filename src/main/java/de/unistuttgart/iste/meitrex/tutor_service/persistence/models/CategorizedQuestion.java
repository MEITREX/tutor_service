package de.unistuttgart.iste.meitrex.tutor_service.persistence.models;

public class CategorizedQuestion {
    private final String question;
    private final Category category;

    public CategorizedQuestion(String question, Category category) {
        this.question = question;
        this.category = category;
    }

    public String getQuestion() {
        return question;
    }

    public Category getCategory() {
        return category;
    }

}
