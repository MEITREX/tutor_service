package de.unistuttgart.iste.meitrex.tutor_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration properties for student code submission filtering.
 */
@Configuration
@ConfigurationProperties(prefix = "student.code.submission")
@Getter
@Setter
public class StudentCodeSubmissionConfig {

    /**
     * List of file endings to save from student code submissions.
     * Example: [".java", ".kt", ".py"]
     */
    private List<String> fileEndings = List.of(".java");
}
