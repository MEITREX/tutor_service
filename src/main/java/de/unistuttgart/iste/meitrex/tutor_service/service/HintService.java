package de.unistuttgart.iste.meitrex.tutor_service.service;

import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.models.DocumentRecordSegment;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.models.HintResponse;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.models.SemanticSearchResult;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.models.TemplateArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HintService {
    private final OllamaService ollamaService;
    private final SemanticSearchService semanticSearchService;

    private static final List<String> PROMPT_TEMPLATES = List.of(
            "generate_hint.txt"
    );

    public HintResponse generateHintWithQuestion(
            String questionText,
            UUID courseId,
            final LoggedInUser currentUser
    ) {
        List<SemanticSearchResult> searchResults =
                semanticSearchService.semanticSearch(questionText, courseId, currentUser);
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

        String prompt = ollamaService.getTemplate(PROMPT_TEMPLATES.get(0));
        String contentString = semanticSearchService.formatDocumentSegmentsForPrompt(documentSegments);
        List<TemplateArgs> promptArgs = List.of(
                TemplateArgs.builder().argumentName("question").argumentValue(questionText).build(),
                TemplateArgs.builder().argumentName("content").argumentValue(contentString).build()
        );

        return ollamaService.startQuery(HintResponse.class, prompt, promptArgs, new HintResponse("An error occurred"));
    }

}
