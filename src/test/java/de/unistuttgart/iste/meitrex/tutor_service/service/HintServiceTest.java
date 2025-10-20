package de.unistuttgart.iste.meitrex.tutor_service.service;

import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.tutor_service.service.models.DocumentRecordSegment;
import de.unistuttgart.iste.meitrex.tutor_service.service.models.SemanticSearchQuery;
import de.unistuttgart.iste.meitrex.tutor_service.service.models.SemanticSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipInCourseWithId;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class HintServiceTest {

    private final OllamaService ollamaService = Mockito.mock(OllamaService.class);
    private final SemanticSearchService semanticSearchService = Mockito.mock(SemanticSearchService.class);
    private HintService hintService;

    @BeforeEach
    void setUp() {
        hintService = new HintService(ollamaService, semanticSearchService);
        ReflectionTestUtils.setField(hintService, "scoreThreshold", 0.4);
    }

    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, LoggedInUser.UserRoleInCourse.STUDENT);

    @Test
    void testGenerateHint_Success_MultipleChoice() {
        // Arrange
        HintMultipleChoiceInput mcInput = HintMultipleChoiceInput.builder()
                .setText("What is software engineering?")
                .setAnswers(List.of("A discipline", "A hobby"))
                .build();
        HintGenerationInput input = HintGenerationInput.builder()
                .setType(HintQuestionType.MULTIPLE_CHOICE)
                .setMultipleChoice(mcInput)
                .build();

        List<SemanticSearchResult> searchResults = List.of(
                SemanticSearchResult.builder()
                        .score(0.2)
                        .mediaRecordSegment(DocumentRecordSegment.builder().text("Relevant content").build())
                        .build()
        );

        String expectedHint = "This is the generated hint.";

        when(ollamaService.getTemplate(any())).thenReturn("Mocked template");
        when(ollamaService.fillTemplate(any(), any())).thenReturn("Filled question prompt");
        when(semanticSearchService.semanticSearch(eq(mcInput.getText()), eq(courseId), any())).thenReturn(searchResults);
        when(semanticSearchService.formatIntoNumberedListForPrompt(any())).thenReturn("Formatted content");
        when(ollamaService.startQuery(eq(HintResponse.class), any(), any(), any()))
                .thenReturn(new HintResponse(expectedHint));

        HintResponse response = hintService.generateHintWithQuestion(input, courseId, loggedInUser);

        assertNotNull(response);
        assertEquals(expectedHint, response.getHint());
    }

    @Test
    void testGenerateHint_Success_Association() {
        HintAssociationInput assocInput = HintAssociationInput.builder()
                .setText("Match the term to the definition.")
                .setPairs(List.of(
                        AssociationPairInput.builder().setLeft("Agile").setRight("An iterative approach").build()))
                .build();
        HintGenerationInput input = HintGenerationInput.builder()
                .setType(HintQuestionType.ASSOCIATION)
                .setAssociation(assocInput)
                .build();

        List<SemanticSearchResult> searchResults = List.of(
                SemanticSearchResult.builder()
                        .score(0.3)
                        .mediaRecordSegment(DocumentRecordSegment.builder().text("Some text about Agile").build())
                        .build()
        );
        String generatedQuery = "What is Agile?";
        String expectedHint = "A hint about Agile.";

        when(ollamaService.getTemplate(any())).thenReturn("Mocked template");
        when(ollamaService.fillTemplate(any(), any())).thenReturn("Filled question prompt");

        when(ollamaService.startQuery(eq(SemanticSearchQuery.class), any(), any(), any()))
                .thenReturn(new SemanticSearchQuery(generatedQuery));
        when(semanticSearchService.semanticSearch(eq(generatedQuery), eq(courseId), any())).thenReturn(searchResults);
        when(semanticSearchService.formatIntoNumberedListForPrompt(any())).thenReturn("Formatted content");
        when(ollamaService.startQuery(eq(HintResponse.class), any(), any(), any()))
                .thenReturn(new HintResponse(expectedHint));

        HintResponse response = hintService.generateHintWithQuestion(input, courseId, loggedInUser);

        assertNotNull(response);
        assertEquals(expectedHint, response.getHint());
    }

    @Test
    void testGenerateHint_WhenNoRelevantSegments_ReturnsSpecificMessage() {
        HintMultipleChoiceInput mcInput = HintMultipleChoiceInput.builder()
                .setText("A question with no relevant content.")
                .setAnswers(List.of("A", "B"))
                .build();
        HintGenerationInput input = HintGenerationInput.builder()
                .setType(HintQuestionType.MULTIPLE_CHOICE)
                .setMultipleChoice(mcInput)
                .build();

        // This can happen either by semantic search returning empty, or by all results being filtered out.
        // We test the empty case here.
        when(semanticSearchService.semanticSearch(any(), any(), any())).thenReturn(List.of());

        HintResponse response = hintService.generateHintWithQuestion(input, courseId, loggedInUser);

        assertNotNull(response);
        assertEquals("No relevant content found in the documents of this lecture for this question", response.getHint());
    }

    @Test
    void testGenerateHint_Failure_ServiceThrowsException() {
        HintMultipleChoiceInput mcInput = HintMultipleChoiceInput.builder()
                .setText("What is software engineering?")
                .setAnswers(List.of("A discipline", "A hobby"))
                .build();
        HintGenerationInput input = HintGenerationInput.builder()
                .setType(HintQuestionType.MULTIPLE_CHOICE)
                .setMultipleChoice(mcInput)
                .build();

        when(semanticSearchService.semanticSearch(any(), any(), any()))
                .thenThrow(new RuntimeException("Service unavailable"));

        assertThrows(RuntimeException.class, () -> {
            hintService.generateHintWithQuestion(input, courseId, loggedInUser);
        });
    }

    @Test
    void testGenerateHint_Failure_InvalidInput() {
        HintGenerationInput input = HintGenerationInput.builder()
                .setType(HintQuestionType.MULTIPLE_CHOICE)
                .setMultipleChoice(null)
                .build();

        // We expect an IllegalArgumentException from the buildMultipleChoiceData method
        assertThrows(IllegalArgumentException.class, () -> {
            hintService.generateHintWithQuestion(input, courseId, loggedInUser);
        });
    }
}

