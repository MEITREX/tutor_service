package de.unistuttgart.iste.meitrex.tutor_service.persistence.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HintGenerationInput {

    private HintQuestionType type;
    private HintMultipleChoiceInput multipleChoice;
    private HintAssociationInput association;
    private HintClozeInput cloze;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HintMultipleChoiceInput {
        private String text;
        private List<String> answers;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HintClozeInput {
        private String text;
        private List<String> blanks;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HintAssociationInput {
        private String text;
        private List<AssociationPairInput> pairs;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AssociationPairInput {
        private String left;
        private String right;
    }
}

