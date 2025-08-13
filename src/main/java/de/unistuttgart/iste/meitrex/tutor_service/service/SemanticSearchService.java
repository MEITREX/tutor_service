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

    public String formatDocumentSegmentsForPrompt(List<DocumentRecordSegment> documentSegments) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < documentSegments.size(); i++) {
            DocumentRecordSegment segment = documentSegments.get(i);
            String text = segment.getText();
            if (text == null || text.isBlank()) continue;

            sb.append("[").append(i + 1).append("] ")
                    .append(text.trim())
                    .append("\n\n");
        }
        return sb.toString().trim();
    }
}
