package de.unistuttgart.iste.meitrex.tutor_service.service;

import de.unistuttgart.iste.meitrex.generated.dto.HintResponse;
import de.unistuttgart.iste.meitrex.generated.dto.LectureQuestionResponse;
import de.unistuttgart.iste.meitrex.tutor_service.service.models.CategorizedQuestion;
import de.unistuttgart.iste.meitrex.tutor_service.service.models.TutorAnswer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

public class JsonSchemaServiceTest {

    private JsonSchemaService jsonSchemaService;

    @BeforeEach
    void setUp() {
        jsonSchemaService = new JsonSchemaService();
    }

    @Test
    void testGenerateSchema_forHintResponse() {
        Class<HintResponse> targetClass = HintResponse.class;

        String schema = jsonSchemaService.getJsonSchema(targetClass);

        assertNotNull(schema);
        assertTrue(schema.contains("\"type\":\"object\""));
        assertTrue(schema.contains("\"properties\":{\"hint\":{\"type\":\"string\"}}"));
    }

    @Test
    void testGenerateSchema_forTutorAnswer() {
        Class<TutorAnswer> targetClass = TutorAnswer.class;

        String schema = jsonSchemaService.getJsonSchema(targetClass);

        assertNotNull(schema);
        assertTrue(schema.contains("\"type\":\"object\""));
        assertTrue(schema.contains("\"properties\":{\"answer\":{\"type\":\"string\"}}"));
    }

    @Test
    void testCaching_shouldReturnSameInstanceOnSecondCall() {
        Class<LectureQuestionResponse> targetClass = LectureQuestionResponse.class;

        String schemaInstance1 = jsonSchemaService.getJsonSchema(targetClass);
        String schemaInstance2 = jsonSchemaService.getJsonSchema(targetClass);

        
        assertNotNull(schemaInstance1);
        assertSame(schemaInstance1, schemaInstance2,
                "The schema should be the same instance from the cache on the second call.");
    }

    @Test
    void testGenerateSchema_forCategorizedQuestion() {
        Class<CategorizedQuestion> targetClass = CategorizedQuestion.class;

        String schema = jsonSchemaService.getJsonSchema(targetClass);

        assertNotNull(schema);
        assertTrue(schema.contains("\"question\":{\"type\":\"string\"}"));
        // Note: The enum order in the schema might differ from the order in TutorCategory
        assertTrue(schema.contains("\"category\"") && schema.contains("\"enum\"") && 
                   schema.contains("\"CODE_FEEDBACK\""), 
                   "Schema should contain category enum with CODE_FEEDBACK");
    }

    @Test
    void testSchemaGeneration_forDifferentClasses() {
        Class<HintResponse> hintClass = HintResponse.class;
        Class<LectureQuestionResponse> lectureQuestionClass = LectureQuestionResponse.class;

        String hintSchema = jsonSchemaService.getJsonSchema(hintClass);
        String lectureQuestionSchema = jsonSchemaService.getJsonSchema(lectureQuestionClass);

        assertNotNull(hintSchema);
        assertNotNull(lectureQuestionSchema);
        assertNotEquals(hintSchema, lectureQuestionSchema,
                "Schemas for different classes should not be identical.");
    }
}