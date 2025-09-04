package de.unistuttgart.iste.meitrex.tutor_service.service;

import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.content_service.client.ContentServiceClient;
import de.unistuttgart.iste.meitrex.content_service.exception.ContentServiceConnectionException;
import de.unistuttgart.iste.meitrex.tutor_service.client.DocProcAIServiceClient;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.models.DocumentRecordSegment;
import de.unistuttgart.iste.meitrex.tutor_service.persistence.models.SemanticSearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.user_handling.UserCourseAccessValidator.validateUserHasAccessToCourse;

@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private final DocProcAIServiceClient docProcAiServiceClient;
    private final ContentServiceClient contentServiceClient;

    /**
     * Performs a semantic search for lecture-related content within a given course.
     * Validates that the user has access to the course before querying.
     *
     * @param question    the user’s question to search for
     * @param courseId    the ID of the course to search in
     * @param currentUser the currently logged-in user
     * @return a list of semantic search results, or an empty list if none are found
     * @throws RuntimeException if the content service connection fails
     */
    public List<SemanticSearchResult> semanticSearch(String question, UUID courseId, LoggedInUser currentUser) {
        try {
            validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.STUDENT, courseId);
            List<UUID> contentIdsOfCourse = contentServiceClient.queryContentIdsOfCourse(courseId);

            return docProcAiServiceClient.semanticSearch(question, contentIdsOfCourse);

        } catch (ContentServiceConnectionException e) {
            throw new RuntimeException(String.valueOf(e));
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    /**
     * Formats a list of Strings into a numbered string for use in prompts.
     *
     * @param texts the list of Strings to format into a numbered list
     * @return a formatted string with each segment numbered, or an empty string if no valid text is present
     */
    public String formatIntoNumberedListForPrompt(List<String> texts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            if (text == null || text.isBlank()) continue;

            sb.append("[").append(i + 1).append("] ")
                    .append(text.trim())
                    .append("\n\n");
        }
        return sb.toString().trim();
    }
}
