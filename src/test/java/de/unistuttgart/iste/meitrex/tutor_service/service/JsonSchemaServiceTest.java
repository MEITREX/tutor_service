package de.unistuttgart.iste.meitrex.tutor_service.service;

import de.unistuttgart.iste.meitrex.generated.dto.HintResponse;
import de.unistuttgart.iste.meitrex.generated.dto.LectureQuestionResponse;
import de.unistuttgart.iste.meitrex.tutor_service.service.models.CategorizedQuestion;
import de.unistuttgart.iste.meitrex.tutor_service.service.models.TutorAnswer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JsonSchemaServiceTest {

    private JsonSchemaService jsonSchemaService;

    @BeforeEach
    void setUp() {
        jsonSchemaService = new JsonSchemaService();
    }

    @Test
    void testGenerateSchema_forHintResponse() {
        // Arrange
        Class<HintResponse> targetClass = HintResponse.class;

        // Act
        String schema = jsonSchemaService.getJsonSchema(targetClass);

        // Assert
        assertNotNull(schema);
        assertTrue(schema.contains("\"type\":\"object\""));
        assertTrue(schema.contains("\"properties\":{\"hint\":{\"type\":\"string\"}}"));
    }

    @Test
    void testGenerateSchema_forTutorAnswer() {
        // Arrange
        Class<TutorAnswer> targetClass = TutorAnswer.class;

        // Act
        String schema = jsonSchemaService.getJsonSchema(targetClass);

        // Assert
        assertNotNull(schema);
        assertTrue(schema.contains("\"type\":\"object\""));
        assertTrue(schema.contains("\"properties\":{\"answer\":{\"type\":\"string\"}}"));
    }

    @Test
    void testCaching_shouldReturnSameInstanceOnSecondCall() {
        // Arrange
        Class<LectureQuestionResponse> targetClass = LectureQuestionResponse.class;

        // Act
        // The first call generates and caches the schema
        String schemaInstance1 = jsonSchemaService.getJsonSchema(targetClass);
        // The second call should retrieve the instance from the cache
        String schemaInstance2 = jsonSchemaService.getJsonSchema(targetClass);

        // Assert
        assertNotNull(schemaInstance1);
        // assertSame checks if both variables refer to the exact same object in memory.
        // This confirms the schema was not regenerated, proving the cache is working.
        assertSame(schemaInstance1, schemaInstance2,
                "The schema should be the same instance from the cache on the second call.");
    }

    @Test
    void testGenerateSchema_forCategorizedQuestion() {
        // Arrange
        Class<CategorizedQuestion> targetClass = CategorizedQuestion.class;

        // Act
        String schema = jsonSchemaService.getJsonSchema(targetClass);

        // Assert
        assertNotNull(schema);
        assertTrue(schema.contains("\"question\":{\"type\":\"string\"}"));
        // Jackson correctly identifies the enum and lists its possible values as a constraint
        assertTrue(schema.contains("\"category\":{\"type\":\"string\"," +
                "\"enum\":[\"SYSTEM\",\"LECTURE\",\"UNRECOGNIZABLE\",\"OTHER\",\"ERROR\"]}"));
    }

    @Test
    void testSchemaGeneration_forDifferentClasses() {
        // Arrange
        Class<HintResponse> hintClass = HintResponse.class;
        Class<LectureQuestionResponse> lectureQuestionClass = LectureQuestionResponse.class;

        // Act
        String hintSchema = jsonSchemaService.getJsonSchema(hintClass);
        String lectureQuestionSchema = jsonSchemaService.getJsonSchema(lectureQuestionClass);

        // Assert
        assertNotNull(hintSchema);
        assertNotNull(lectureQuestionSchema);
        assertNotEquals(hintSchema, lectureQuestionSchema,
                "Schemas for different classes should not be identical.");
    }
}