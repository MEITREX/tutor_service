package de.unistuttgart.iste.meitrex.tutor_service.service;

import de.unistuttgart.iste.meitrex.common.event.HexadPlayerType;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.tutor_service.service.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipInCourseWithId;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HintService.
 */
@ExtendWith(MockitoExtension.class)
class HintServiceTest {

    @Mock
    private OllamaService ollamaService;

    @Mock
    private SemanticSearchService semanticSearchService;

    @Mock
    private UserPlayerTypeService userPlayerTypeService;

    @InjectMocks
    private HintService hintService;

    private UUID courseId;

    @InjectCurrentUserHeader
    private LoggedInUser currentUser;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
        currentUser = userWithMembershipInCourseWithId(courseId, LoggedInUser.UserRoleInCourse.STUDENT);
        ReflectionTestUtils.setField(hintService, "scoreThreshold", 0.4);
    }

    @Test
    void testGenerateHintWithQuestion_multipleChoice_success() {
        HintMultipleChoiceInput multipleChoiceInput = HintMultipleChoiceInput.builder()
                .setText("What is Java?")
                .setAnswers(List.of("A programming language", "A type of coffee", "An island"))
                .build();

        HintGenerationInput input = HintGenerationInput.builder()
                .setType(HintQuestionType.MULTIPLE_CHOICE)
                .setMultipleChoice(multipleChoiceInput)
                .build();

        List<SemanticSearchResult> searchResults = List.of(
                SemanticSearchResult.builder()
                        .score(0.25)
                        .typename("DocumentRecordSegment")
                        .mediaRecordSegment(DocumentRecordSegment.builder()
                                .text("Java is a high-level programming language.")
                                .page(1)
                                
                                .build())
                        .build()
        );

        String expectedHint = "Think about what Java is commonly used for in software development.";
        HintResponse expectedResponse = new HintResponse(expectedHint);

        when(ollamaService.getTemplate(contains("question_prompt_MULTIPLE_CHOICE")))
                .thenReturn("Question: {questionText}\nOptions: {options}");
        when(ollamaService.fillTemplate(anyString(), anyList()))
                .thenReturn("Filled question prompt");
        when(semanticSearchService.semanticSearch(anyString(), eq(courseId), eq(currentUser)))
                .thenReturn(searchResults);
        when(semanticSearchService.formatIntoNumberedListForPrompt(anyList()))
                .thenReturn("1. Java is a high-level programming language.");
        when(ollamaService.getTemplate(contains("generate_hint")))
                .thenReturn("Generate hint template");
        when(userPlayerTypeService.getPrimaryPlayerType(currentUser.getId()))
                .thenReturn(Optional.empty());
        when(ollamaService.startQuery(eq(HintResponse.class), anyString(), anyList(), any()))
                .thenReturn(expectedResponse);

        HintResponse result = hintService.generateHintWithQuestion(input, courseId, currentUser);

        assertNotNull(result);
        assertEquals(expectedHint, result.getHint());
        verify(semanticSearchService).semanticSearch(eq("What is Java?"), eq(courseId), eq(currentUser));
        verify(ollamaService).startQuery(eq(HintResponse.class), anyString(), anyList(), any());
    }

    @Test
    void testGenerateHintWithQuestion_cloze_success() {
        HintClozeInput clozeInput = HintClozeInput.builder()
                .setText("Java is a ____ programming language")
                .setBlanks(List.of("object-oriented", "functional", "procedural"))
                .build();

        HintGenerationInput input = HintGenerationInput.builder()
                .setType(HintQuestionType.CLOZE)
                .setCloze(clozeInput)
                .build();

        List<SemanticSearchResult> searchResults = List.of(
                SemanticSearchResult.builder()
                        .score(0.20)
                        .typename("DocumentRecordSegment")
                        .mediaRecordSegment(DocumentRecordSegment.builder()
                                .text("Java supports object-oriented programming principles.")
                                .page(2)
                                
                                .build())
                        .build()
        );

        String semanticSearchQuery = "Java programming language paradigm";
        when(ollamaService.getTemplate(contains("question_prompt_CLOZE")))
                .thenReturn("Question: {questionText}\nOptions: {options}");
        when(ollamaService.fillTemplate(anyString(), anyList()))
                .thenReturn("Filled cloze prompt");
        when(ollamaService.getTemplate(contains("generate_semantic_search_query_cloze")))
                .thenReturn("Generate search query for cloze");
        when(ollamaService.startQuery(eq(SemanticSearchQuery.class), anyString(), anyList(), any()))
                .thenReturn(new SemanticSearchQuery(semanticSearchQuery));
        when(semanticSearchService.semanticSearch(eq(semanticSearchQuery), eq(courseId), eq(currentUser)))
                .thenReturn(searchResults);
        when(semanticSearchService.formatIntoNumberedListForPrompt(anyList()))
                .thenReturn("1. Java supports object-oriented programming principles.");
        when(ollamaService.getTemplate(contains("generate_hint")))
                .thenReturn("Generate hint template");
        when(userPlayerTypeService.getPrimaryPlayerType(currentUser.getId()))
                .thenReturn(Optional.empty());
        when(ollamaService.startQuery(eq(HintResponse.class), anyString(), anyList(), any()))
                .thenReturn(new HintResponse("Consider the programming paradigms Java supports."));

        HintResponse result = hintService.generateHintWithQuestion(input, courseId, currentUser);

        assertNotNull(result);
        assertEquals("Consider the programming paradigms Java supports.", result.getHint());
        verify(semanticSearchService).semanticSearch(eq(semanticSearchQuery), eq(courseId), eq(currentUser));
    }

    @Test
    void testGenerateHintWithQuestion_association_success() {
        HintAssociationInput associationInput = HintAssociationInput.builder()
                .setText("Match the Java concepts")
                .setPairs(List.of(
                        new AssociationPairInput("JVM", "Java Virtual Machine"),
                        new AssociationPairInput("JDK", "Java Development Kit")
                ))
                .build();

        HintGenerationInput input = HintGenerationInput.builder()
                .setType(HintQuestionType.ASSOCIATION)
                .setAssociation(associationInput)
                .build();

        List<SemanticSearchResult> searchResults = List.of(
                SemanticSearchResult.builder()
                        .score(0.30)
                        .typename("DocumentRecordSegment")
                        .mediaRecordSegment(DocumentRecordSegment.builder()
                                .text("JVM is the Java Virtual Machine that executes Java bytecode.")
                                .page(3)
                                
                                .build())
                        .build()
        );

        String semanticSearchQuery = "JVM JDK Java tools components";
        when(ollamaService.getTemplate(contains("question_prompt_ASSOCIATION")))
                .thenReturn("Question: {questionText}\nPairs: {options}");
        when(ollamaService.fillTemplate(anyString(), anyList()))
                .thenReturn("Filled association prompt");
        when(ollamaService.getTemplate(contains("generate_semantic_search_query_association")))
                .thenReturn("Generate search query for association");
        when(semanticSearchService.formatIntoNumberedListForPrompt(anyList()))
                .thenReturn("1. Left: JVM <-> Right: Java Virtual Machine\n2. Left: JDK <-> Right: Java Development Kit");
        when(ollamaService.startQuery(eq(SemanticSearchQuery.class), anyString(), anyList(), any()))
                .thenReturn(new SemanticSearchQuery(semanticSearchQuery));
        when(semanticSearchService.semanticSearch(eq(semanticSearchQuery), eq(courseId), eq(currentUser)))
                .thenReturn(searchResults);
        when(ollamaService.getTemplate(contains("generate_hint")))
                .thenReturn("Generate hint template");
        when(userPlayerTypeService.getPrimaryPlayerType(currentUser.getId()))
                .thenReturn(Optional.empty());
        when(ollamaService.startQuery(eq(HintResponse.class), anyString(), anyList(), any()))
                .thenReturn(new HintResponse("Think about what each acronym stands for."));

        HintResponse result = hintService.generateHintWithQuestion(input, courseId, currentUser);

        assertNotNull(result);
        assertEquals("Think about what each acronym stands for.", result.getHint());
        verify(semanticSearchService).semanticSearch(eq(semanticSearchQuery), eq(courseId), eq(currentUser));
    }

    @Test
    void testGenerateHintWithQuestion_noSearchResults() {
        HintMultipleChoiceInput multipleChoiceInput = HintMultipleChoiceInput.builder()
                .setText("What is Quantum Computing?")
                .setAnswers(List.of("A type of computer", "A theory", "A field"))
                .build();

        HintGenerationInput input = HintGenerationInput.builder()
                .setType(HintQuestionType.MULTIPLE_CHOICE)
                .setMultipleChoice(multipleChoiceInput)
                .build();

        when(ollamaService.getTemplate(contains("question_prompt_MULTIPLE_CHOICE")))
                .thenReturn("Question: {questionText}");
        when(ollamaService.fillTemplate(anyString(), anyList()))
                .thenReturn("Filled question prompt");
        when(semanticSearchService.semanticSearch(anyString(), eq(courseId), eq(currentUser)))
                .thenReturn(List.of());

        HintResponse result = hintService.generateHintWithQuestion(input, courseId, currentUser);

        assertNotNull(result);
        assertEquals("No relevant content found in the lecture for this question", result.getHint());
        verify(ollamaService, never()).startQuery(eq(HintResponse.class), anyString(), anyList(), any());
    }

    @Test
    void testGenerateHintWithQuestion_noDocumentSegments() {
        HintMultipleChoiceInput multipleChoiceInput = HintMultipleChoiceInput.builder()
                .setText("What is Python?")
                .setAnswers(List.of("A language", "A snake", "Both"))
                .build();

        HintGenerationInput input = HintGenerationInput.builder()
                .setType(HintQuestionType.MULTIPLE_CHOICE)
                .setMultipleChoice(multipleChoiceInput)
                .build();

        // Search results with high scores (above threshold)
        List<SemanticSearchResult> searchResults = List.of(
                SemanticSearchResult.builder()
                        .score(0.85)  // Above threshold
                        .typename("VideoRecordSegment")
                        .mediaRecordSegment(VideoRecordSegment.builder()
                                .startTime(100)
                                
                                .build())
                        .build()
        );

        when(ollamaService.getTemplate(contains("question_prompt_MULTIPLE_CHOICE")))
                .thenReturn("Question: {questionText}");
        when(ollamaService.fillTemplate(anyString(), anyList()))
                .thenReturn("Filled question prompt");
        when(semanticSearchService.semanticSearch(anyString(), eq(courseId), eq(currentUser)))
                .thenReturn(searchResults);

        HintResponse result = hintService.generateHintWithQuestion(input, courseId, currentUser);

        assertNotNull(result);
        assertEquals("No relevant content found in the documents of this lecture for this question", result.getHint());
    }

    @Test
    void testGenerateHintWithQuestion_withAchieverPlayerType() {
        HintMultipleChoiceInput multipleChoiceInput = HintMultipleChoiceInput.builder()
                .setText("What is polymorphism?")
                .setAnswers(List.of("Multiple forms", "Single form", "No form"))
                .build();

        HintGenerationInput input = HintGenerationInput.builder()
                .setType(HintQuestionType.MULTIPLE_CHOICE)
                .setMultipleChoice(multipleChoiceInput)
                .build();

        List<SemanticSearchResult> searchResults = List.of(
                SemanticSearchResult.builder()
                        .score(0.15)
                        .typename("DocumentRecordSegment")
                        .mediaRecordSegment(DocumentRecordSegment.builder()
                                .text("Polymorphism allows objects to take multiple forms.")
                                .page(5)
                                
                                .build())
                        .build()
        );

        when(ollamaService.getTemplate(contains("question_prompt_MULTIPLE_CHOICE")))
                .thenReturn("Question: {questionText}");
        when(ollamaService.fillTemplate(anyString(), anyList()))
                .thenReturn("Filled question prompt");
        when(semanticSearchService.semanticSearch(anyString(), eq(courseId), eq(currentUser)))
                .thenReturn(searchResults);
        when(semanticSearchService.formatIntoNumberedListForPrompt(anyList()))
                .thenReturn("1. Polymorphism allows objects to take multiple forms.");
        when(ollamaService.getTemplate(contains("generate_hint")))
                .thenReturn("Generate hint template");
        when(userPlayerTypeService.getPrimaryPlayerType(currentUser.getId()))
                .thenReturn(Optional.of(HexadPlayerType.ACHIEVER));
        when(ollamaService.startQuery(eq(HintResponse.class), anyString(), anyList(), any()))
                .thenReturn(new HintResponse("Focus on mastering the concept of polymorphism."));

        HintResponse result = hintService.generateHintWithQuestion(input, courseId, currentUser);

        assertNotNull(result);
        assertEquals("Focus on mastering the concept of polymorphism.", result.getHint());
        verify(userPlayerTypeService).getPrimaryPlayerType(currentUser.getId());
    }

    @Test
    void testGenerateHintWithQuestion_withPhilanthropistPlayerType() {
        HintMultipleChoiceInput multipleChoiceInput = HintMultipleChoiceInput.builder()
                .setText("What is inheritance?")
                .setAnswers(List.of("Code reuse", "Code duplication", "Code removal"))
                .build();

        HintGenerationInput input = HintGenerationInput.builder()
                .setType(HintQuestionType.MULTIPLE_CHOICE)
                .setMultipleChoice(multipleChoiceInput)
                .build();

        List<SemanticSearchResult> searchResults = List.of(
                SemanticSearchResult.builder()
                        .score(0.18)
                        .typename("DocumentRecordSegment")
                        .mediaRecordSegment(DocumentRecordSegment.builder()
                                .text("Inheritance allows classes to inherit properties from parent classes.")
                                .page(6)
                                
                                .build())
                        .build()
        );

        when(ollamaService.getTemplate(contains("question_prompt_MULTIPLE_CHOICE")))
                .thenReturn("Question: {questionText}");
        when(ollamaService.fillTemplate(anyString(), anyList()))
                .thenReturn("Filled question prompt");
        when(semanticSearchService.semanticSearch(anyString(), eq(courseId), eq(currentUser)))
                .thenReturn(searchResults);
        when(semanticSearchService.formatIntoNumberedListForPrompt(anyList()))
                .thenReturn("1. Inheritance allows classes to inherit properties from parent classes.");
        when(ollamaService.getTemplate(contains("generate_hint")))
                .thenReturn("Generate hint template");
        when(userPlayerTypeService.getPrimaryPlayerType(currentUser.getId()))
                .thenReturn(Optional.of(HexadPlayerType.PHILANTHROPIST));
        when(ollamaService.startQuery(eq(HintResponse.class), anyString(), anyList(), any()))
                .thenReturn(new HintResponse("Consider how inheritance helps share knowledge between classes."));

        HintResponse result = hintService.generateHintWithQuestion(input, courseId, currentUser);

        assertNotNull(result);
        verify(userPlayerTypeService).getPrimaryPlayerType(currentUser.getId());
    }

    @Test
    void testGenerateHintWithQuestion_withPlayerPlayerType() {
        HintMultipleChoiceInput multipleChoiceInput = HintMultipleChoiceInput.builder()
                .setText("What is encapsulation?")
                .setAnswers(List.of("Data hiding", "Data exposure", "Data deletion"))
                .build();

        HintGenerationInput input = HintGenerationInput.builder()
                .setType(HintQuestionType.MULTIPLE_CHOICE)
                .setMultipleChoice(multipleChoiceInput)
                .build();

        List<SemanticSearchResult> searchResults = List.of(
                SemanticSearchResult.builder()
                        .score(0.22)
                        .typename("DocumentRecordSegment")
                        .mediaRecordSegment(DocumentRecordSegment.builder()
                                .text("Encapsulation is bundling data and methods together.")
                                .page(7)
                                
                                .build())
                        .build()
        );

        when(ollamaService.getTemplate(contains("question_prompt_MULTIPLE_CHOICE")))
                .thenReturn("Question: {questionText}");
        when(ollamaService.fillTemplate(anyString(), anyList()))
                .thenReturn("Filled question prompt");
        when(semanticSearchService.semanticSearch(anyString(), eq(courseId), eq(currentUser)))
                .thenReturn(searchResults);
        when(semanticSearchService.formatIntoNumberedListForPrompt(anyList()))
                .thenReturn("1. Encapsulation is bundling data and methods together.");
        when(ollamaService.getTemplate(contains("generate_hint")))
                .thenReturn("Generate hint template");
        when(userPlayerTypeService.getPrimaryPlayerType(currentUser.getId()))
                .thenReturn(Optional.of(HexadPlayerType.PLAYER));
        when(ollamaService.startQuery(eq(HintResponse.class), anyString(), anyList(), any()))
                .thenReturn(new HintResponse("Be careful not to reveal the answer directly!"));

        HintResponse result = hintService.generateHintWithQuestion(input, courseId, currentUser);

        assertNotNull(result);
        verify(userPlayerTypeService).getPrimaryPlayerType(currentUser.getId());
    }

    @Test
    void testGenerateHintWithQuestion_playerTypeFetchException() {
        HintMultipleChoiceInput multipleChoiceInput = HintMultipleChoiceInput.builder()
                .setText("What is abstraction?")
                .setAnswers(List.of("Hiding complexity", "Adding complexity", "Removing code"))
                .build();

        HintGenerationInput input = HintGenerationInput.builder()
                .setType(HintQuestionType.MULTIPLE_CHOICE)
                .setMultipleChoice(multipleChoiceInput)
                .build();

        List<SemanticSearchResult> searchResults = List.of(
                SemanticSearchResult.builder()
                        .score(0.19)
                        .typename("DocumentRecordSegment")
                        .mediaRecordSegment(DocumentRecordSegment.builder()
                                .text("Abstraction hides implementation details.")
                                .page(8)
                                
                                .build())
                        .build()
        );

        when(ollamaService.getTemplate(contains("question_prompt_MULTIPLE_CHOICE")))
                .thenReturn("Question: {questionText}");
        when(ollamaService.fillTemplate(anyString(), anyList()))
                .thenReturn("Filled question prompt");
        when(semanticSearchService.semanticSearch(anyString(), eq(courseId), eq(currentUser)))
                .thenReturn(searchResults);
        when(semanticSearchService.formatIntoNumberedListForPrompt(anyList()))
                .thenReturn("1. Abstraction hides implementation details.");
        when(ollamaService.getTemplate(contains("generate_hint")))
                .thenReturn("Generate hint template");
        when(userPlayerTypeService.getPrimaryPlayerType(currentUser.getId()))
                .thenThrow(new RuntimeException("Player type fetch error"));
        when(ollamaService.startQuery(eq(HintResponse.class), anyString(), anyList(), any()))
                .thenReturn(new HintResponse("Think about how abstraction simplifies complex systems."));

        HintResponse result = hintService.generateHintWithQuestion(input, courseId, currentUser);

        assertNotNull(result);
        assertEquals("Think about how abstraction simplifies complex systems.", result.getHint());
    }

    @Test
    void testBuildMultipleChoiceData_invalidInput_nullInput() { 
        HintGenerationInput input = HintGenerationInput.builder()
                .setType(HintQuestionType.MULTIPLE_CHOICE)
                .setMultipleChoice(null)
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            hintService.generateHintWithQuestion(input, courseId, currentUser);
        });
    }

    @Test
    void testBuildClozeData_invalidInput_emptyBlanks() {
        HintClozeInput clozeInput = HintClozeInput.builder()
                .setText("Java is a ____ language")
                .setBlanks(List.of())
                .build();

        HintGenerationInput input = HintGenerationInput.builder()
                .setType(HintQuestionType.CLOZE)
                .setCloze(clozeInput)
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            hintService.generateHintWithQuestion(input, courseId, currentUser);
        });
    }

    @Test
    void testBuildAssociationData_invalidInput_emptyPairs() {
        HintAssociationInput associationInput = HintAssociationInput.builder()
                .setText("Match the concepts")
                .setPairs(List.of())
                .build();

        HintGenerationInput input = HintGenerationInput.builder()
                .setType(HintQuestionType.ASSOCIATION)
                .setAssociation(associationInput)
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            hintService.generateHintWithQuestion(input, courseId, currentUser);
        });
    }
}


