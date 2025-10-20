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

    /**
     * Generates a hint for a given question based on relevant lecture content.
     *
     * @param input The input containing the question text, type, and options.
     * @param courseId The ID of the course to search for relevant content.
     * @param currentUser The user requesting the hint.
     * @return A HintResponse containing the generated hint or an error message.
     */
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
        log.debug("Starting hint generation for course {} and question type {}", courseId, input.getType());
        // First fill the prompt corresponding to the question type
        HintGenerationData generationData = getGenerationData(input);
        String questionPrompt = prepareQuestionPrompt(input.getType(), generationData);

        List<DocumentRecordSegment> documentSegments =
                findRelevantDocumentSegments(generationData, courseId, currentUser);

        if (documentSegments.isEmpty()) {
            log.warn("No relevant document segments found for hint generation. CourseId: {}", courseId);
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

    /**
     * Prepares the initial prompt that describes the question and its answer options.
     *
     * @param questionType   The type of the question (e.g., MULTIPLE_CHOICE).
     * @param generationData The data object containing the question text and options.
     * @return A formatted string to be used as a prompt for the hint generation model.
     */
    private String prepareQuestionPrompt(HintQuestionType questionType, HintGenerationData generationData) {
        String promptName = PROMPT_TEMPLATES.get("QUESTION").replace("{QUESTION_TYPE}", questionType.toString());
        String questionPromptTemplate = ollamaService.getTemplate(promptName);

        List<TemplateArgs> questionPromptArgs = List.of(
                TemplateArgs.builder()
                        .argumentName("questionText").argumentValue(generationData.getQuestionText()).build(),
                TemplateArgs.builder()
                        .argumentName("options").argumentValue(generationData.getOptionsText()).build());

        return ollamaService.fillTemplate(questionPromptTemplate, questionPromptArgs);
    }

    /**
     * Performs a semantic search and filters the results to find relevant document segments.
     *
     * @param generationData The data containing the semantic search query.
     * @param courseId       The ID of the course to search in.
     * @param currentUser    The user performing the search.
     * @return A list of {@link DocumentRecordSegment}s that are deemed relevant based on the search score.
     */
    private List<DocumentRecordSegment> findRelevantDocumentSegments(
            HintGenerationData generationData, UUID courseId, LoggedInUser currentUser) {

        List<SemanticSearchResult> searchResults =
                semanticSearchService.semanticSearch(generationData.getSemanticSearchQuery(), courseId, currentUser);

        if (searchResults.isEmpty()) {
            log.info("Semantic search returned no results for query: {}", generationData.getSemanticSearchQuery());
            return List.of();
        }

        log.debug("Found {} semantic search results before filtering.", searchResults.size());
        List <DocumentRecordSegment> thresholdedList = searchResults.stream()
            .filter(result -> result.getScore() <= scoreThreshold)
            .map(SemanticSearchResult::getMediaRecordSegment)
            .filter(DocumentRecordSegment.class::isInstance)
            .map(DocumentRecordSegment.class::cast)
            .toList();
        log.debug("Found {} document segments after filtering with threshold {}.",
                thresholdedList.size(), scoreThreshold);
        return thresholdedList;
    }

    /**
     * Factory method to create {@link HintGenerationData} based on the question type.
     *
     * @param input The raw hint generation input.
     * @return A populated {@link HintGenerationData} object.
     */
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

        log.info("Generated search query for association question {}", semanticSearchQuery.getQuery());

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

        log.info("Generated search query for cloze question {}", semanticSearchQuery.getQuery());

        return HintGenerationData
            .builder()
            .questionText(questionText)
            .optionsText(optionsString)
            .semanticSearchQuery(semanticSearchQuery.getQuery())
            .build();
    }

}
