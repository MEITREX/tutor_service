package de.unistuttgart.iste.meitrex.tutor_service.service;

import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.tutor_service.service.models.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HintService {
    private final OllamaService ollamaService;
    private final SemanticSearchService semanticSearchService;

    @Value("${semantic.search.threshold.hint:0.4}")
    private double scoreThreshold;

    private static final Map<String, String> PROMPT_TEMPLATES = Map.of(
            "GENERATION", "generate_hint.md",
            "QUESTION", "question_prompt_{QUESTION_TYPE}.md",
            "SEMANTIC_SEARCH_QUERY_ASSOCIATION", "generate_semantic_search_query_association.md",
            "SEMANTIC_SEARCH_QUERY_CLOZE", "generate_semantic_search_query_cloze.md"
    );


    public HintResponse generateHintWithQuestion(
            HintGenerationInput input,
            UUID courseId,
            final LoggedInUser currentUser
    ) {
        // First fill the prompt corresponding to the question type
        HintGenerationData generationData = getGenerationData(input);
        String promptName = PROMPT_TEMPLATES.get("QUESTION").replace("{QUESTION_TYPE}", input.getType().toString());
        String questionPrompt = ollamaService.getTemplate(promptName);
        List<TemplateArgs> questionPromptArgs = List.of(
                TemplateArgs.builder().argumentName("questionText").argumentValue(generationData.getQuestionText()).build(),
                TemplateArgs.builder().argumentName("options").argumentValue(generationData.getOptionsText()).build());
        questionPrompt = ollamaService.fillTemplate(questionPrompt, questionPromptArgs);

        /*
        * Perform a semantic search using the query defined in HintGenerationData
        * Query is either generated or the provided question text based on the question type
        */
        List<SemanticSearchResult> searchResults =
                semanticSearchService.semanticSearch(generationData.getSemanticSearchQuery(), courseId, currentUser);
        if (searchResults.isEmpty()) {
            return new HintResponse("No relevant content found in the lecture for this question");
        }

        List<DocumentRecordSegment> documentSegments = searchResults.stream()
                .filter(result -> result.getScore() <= scoreThreshold)
                .map(SemanticSearchResult::getMediaRecordSegment)
                .filter(segment -> segment instanceof DocumentRecordSegment)
                .map(segment -> (DocumentRecordSegment) segment)
                .toList();

        if (documentSegments.isEmpty()) {
            return new HintResponse("No relevant content found in the documents of this lecture for this question");
        }

        /*
        *  Now generate the hint given the question, answer options (which is injected via the questionPrompt) and
        * the text inside segments which have been found relevant via the semantic search
        */
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
            case ASSOCIATION -> buildAssociationData(input.getAssociation());
            case MULTIPLE_CHOICE -> buildMultipleChoiceData(input.getMultipleChoice());
            default -> throw new IllegalArgumentException("Unsupported hint question type: " + input.getType());
        };
    }

    private HintGenerationData buildMultipleChoiceData(HintMultipleChoiceInput input) {
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

    private HintGenerationData buildAssociationData(HintAssociationInput input) {
        if (input == null || input.getText().isBlank() || input.getPairs().isEmpty()) {
            throw new IllegalArgumentException("Multiple choice input is invalid or empty");
        }
        String questionText = input.getText();
        String optionsString = semanticSearchService.formatIntoNumberedListForPrompt(
                input.getPairs().stream().map(pair -> (
                    "Left: " + pair.getLeft() + " <-> Right: " + pair.getRight()))
                    .toList());

        // Generate a semantic search query based on the question text and the pairs
        List<TemplateArgs> promptArgs = List.of(
            TemplateArgs.builder().argumentName("pairs").argumentValue(optionsString).build()
        );
        String promptName = ollamaService.getTemplate(PROMPT_TEMPLATES.get("SEMANTIC_SEARCH_QUERY_ASSOCIATION"));
        SemanticSearchQuery semanticSearchQuery = ollamaService.startQuery(
                SemanticSearchQuery.class, promptName, promptArgs , new SemanticSearchQuery(questionText));

        log.info("Generated search query {}", semanticSearchQuery.getQuery());

        return HintGenerationData.builder()
                .questionText(questionText)
                .optionsText(optionsString)
                .semanticSearchQuery(semanticSearchQuery.getQuery())
                .build();
    }

    private HintGenerationData buildClozeData(HintClozeInput input) {
        if (input == null || input.getText().isBlank() || input.getBlanks().isEmpty()) {
            throw new IllegalArgumentException("Cloze input is invalid or empty");
        }
        String questionText = input.getText();
        List<String> blanks = input.getBlanks();
        String optionsString = semanticSearchService.formatIntoNumberedListForPrompt(blanks);

        // Generate a semantic search query based on the cloze and its corresponding answer options
        List<TemplateArgs> promptArgs = List.of(
            TemplateArgs.builder().argumentName("clozeText").argumentValue(questionText).build(),
            TemplateArgs.builder().argumentName("answers").argumentValue(optionsString).build()
        );
        String promptName = ollamaService.getTemplate(PROMPT_TEMPLATES.get("SEMANTIC_SEARCH_QUERY_CLOZE"));
        SemanticSearchQuery semanticSearchQuery = ollamaService.startQuery(
                SemanticSearchQuery.class, promptName, promptArgs , new SemanticSearchQuery(questionText));

        log.info("Generated search query {}", semanticSearchQuery.getQuery());

        return HintGenerationData
            .builder()
            .questionText(questionText)
            .optionsText(optionsString)
            .semanticSearchQuery(semanticSearchQuery.getQuery())
            .build();
    }

}
