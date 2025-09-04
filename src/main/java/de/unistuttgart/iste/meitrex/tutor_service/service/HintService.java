package de.unistuttgart.iste.meitrex.tutor_service.service;

import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.models.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HintService {
    private final OllamaService ollamaService;
    private final SemanticSearchService semanticSearchService;

    private static final Map<String, String> PROMPT_TEMPLATES = Map.of(
            "GENERATION", "generate_hint.md",
            "QUESTION", "question_prompt_{QUESTION_TYPE}.md"
    );


    public HintResponse generateHintWithQuestion(
            HintGenerationInput input,
            UUID courseId,
            final LoggedInUser currentUser
    ) {
        HintGenerationData generationData = getGenerationData(input);
        String promptName = PROMPT_TEMPLATES.get("QUESTION").replace("{QUESTION_TYPE}", input.getType().toString());
        List<TemplateArgs> questionPromptArgs = List.of(
                TemplateArgs.builder().argumentName("questionText").argumentValue(generationData.getQuestionText()).build(),
                TemplateArgs.builder().argumentName("options").argumentValue(generationData.getOptionsText()).build());
        String questionPrompt = ollamaService.fillTemplate(promptName, questionPromptArgs);

        List<SemanticSearchResult> searchResults =
                semanticSearchService.semanticSearch(generationData.getSemanticSearchQuery(), courseId, currentUser);
        if (searchResults.isEmpty()) {
            return new HintResponse("No relevant content found in the lecture for this question");
        }

        List<DocumentRecordSegment> documentSegments = searchResults.stream()
                .map(SemanticSearchResult::getMediaRecordSegment)
                .filter(segment -> segment instanceof DocumentRecordSegment)
                .map(segment -> (DocumentRecordSegment) segment)
                .toList();

        if (documentSegments.isEmpty()) {
            return new HintResponse("No relevant content found in the lecture for this question");
        }

        String prompt = ollamaService.getTemplate(PROMPT_TEMPLATES.get("GENERATION"));
        String contentString = semanticSearchService.formatIntoNumberedListForPrompt(
                documentSegments.stream().map(DocumentRecordSegment::getText).toList());
        List<TemplateArgs> promptArgs = List.of(
                TemplateArgs.builder().argumentName("questionPrompt").argumentValue(questionPrompt).build(),
                TemplateArgs.builder().argumentName("content").argumentValue(contentString).build()
        );

        return ollamaService.startQuery(HintResponse.class, prompt, promptArgs, new HintResponse("An error occurred"));
    }

    private HintGenerationData getGenerationData(HintGenerationInput input) {
        return switch (input.getType()) {
            case CLOZE -> buildClozeData(input.getCloze());
            case MULTIPLE_CHOICE -> buildAssociationData(input.getAssociation());
            case ASSOCIATION -> buildMultipleChoiceData(input.getMultipleChoice());
            default -> throw new IllegalArgumentException("Unsupported hint question type: " + input.getType());
        };
    }

    private HintGenerationData buildMultipleChoiceData(HintGenerationInput.HintMultipleChoiceInput input) {
        if (input == null || input.getText().isBlank() || input.getAnswers().isEmpty()) {
            throw new IllegalArgumentException("Multiple choice input is invalid or empty");
        }
        String questionText = input.getText();
        String optionsString = semanticSearchService.formatIntoNumberedListForPrompt(input.getAnswers());

        return HintGenerationData.builder()
                .questionText(questionText)
                .optionsText(optionsString)
                .semanticSearchQuery(questionText)
                .build();
    }

    private HintGenerationData buildAssociationData(HintGenerationInput.HintAssociationInput input) {
        if (input == null || input.getText().isBlank() || input.getPairs().isEmpty()) {
            throw new IllegalArgumentException("Multiple choice input is invalid or empty");
        }
        String questionText = input.getText();
        String optionsString = semanticSearchService.formatIntoNumberedListForPrompt(
                input.getPairs().stream().map(pair -> (
                    "Left: " + pair.getLeft() + " <-> Right: " + pair.getRight()))
                    .toList());

        return HintGenerationData.builder()
                .questionText(questionText)
                .optionsText(optionsString)
                .semanticSearchQuery(questionText)
                .build();
    }

    private HintGenerationData buildClozeData(HintGenerationInput.HintClozeInput input) {
        if (input == null || input.getText().isBlank() || input.getBlanks().isEmpty()) {
            throw new IllegalArgumentException("Cloze input is invalid or empty");
        }
        String questionText = input.getText();
        List<String> blanks = input.getBlanks();
        String optionsString = semanticSearchService.formatIntoNumberedListForPrompt(blanks);
        String semanticSearchQuery = fillClozeText(questionText, blanks);

        return HintGenerationData.builder()
                .questionText(questionText)
                .optionsText(optionsString)
                .semanticSearchQuery(semanticSearchQuery)
                .build();
    }

    private String fillClozeText(String text, List<String> blanks) {
        String result = text;
        for (int i = 0; i < blanks.size(); i++) {
            result = result.replace("[" + (i + 1) + "]", blanks.get(i));
        }
        return result;
    }

}
