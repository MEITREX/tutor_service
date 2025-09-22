package de.unistuttgart.iste.meitrex.tutor_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Spring service that generates and caches JSON schemas from Java classes.
 */
@Service
public class JsonSchemaService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // A thread-safe cache to store generated schemas and avoid re-computation.
    private final Map<Class<?>, String> schemaCache = new ConcurrentHashMap<>();

    /**
     * Generates a JSON schema for the given class.
     * Results are cached to improve performance on subsequent calls for the same class.
     *
     * @param dtoClass The class for which to generate the schema.
     * @return A JSON string representing the schema.
     */
    public String getJsonSchema(Class<?> dtoClass) {
        // Use computeIfAbsent to generate the schema only if it's not already in the cache.
        return schemaCache.computeIfAbsent(dtoClass, key -> {
            try {
                JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(objectMapper);
                JsonSchema schema = schemaGen.generateSchema(key);
                schema.setId(null);
                return objectMapper.writeValueAsString(schema);
            } catch (JsonProcessingException e) {
                // If schema generation fails, wrap the exception in a runtime exception.
                throw new RuntimeException("Failed to generate JSON schema for class: " + key.getName(), e);
            }
        });
    }
}