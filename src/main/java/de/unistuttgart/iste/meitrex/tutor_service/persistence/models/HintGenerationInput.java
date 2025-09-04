package de.unistuttgart.iste.meitrex.tutor_service.persistence.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HintGenerationInput {

    private HintQuestionType type;
    private HintMultipleChoiceInput multipleChoice;
    private HintAssociationInput association;
    private HintClozeInput cloze;

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HintMultipleChoiceInput {
        private String text;
        private List<String> answers;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HintClozeInput {
        private String text;
        private List<String> blanks;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HintAssociationInput {
        private String text;
        private List<AssociationPairInput> pairs;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AssociationPairInput {
        private String left;
        private String right;
    }
}

