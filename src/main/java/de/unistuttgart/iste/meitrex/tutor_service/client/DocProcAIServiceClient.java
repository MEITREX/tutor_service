package de.unistuttgart.iste.meitrex.tutor_service.client;

import de.unistuttgart.iste.meitrex.tutor_service.persistence.models.SemanticSearchResult;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.GraphQlClient;
import reactor.core.publisher.SynchronousSink;

import java.util.List;
import java.util.UUID;

public class DocProcAIServiceClient {

    private static final long RETRY_COUNT = 3;
    private final GraphQlClient graphQlClient;

    public DocProcAIServiceClient(GraphQlClient graphQlClient) {
        this.graphQlClient = graphQlClient;
    }

    /**
     * Performs a semantic search for the given query text within the specified course content.
     *
     * @param queryText the user query to search for
     * @param contentIdsOfCourse the list of content IDs belonging to the course
     * @return a list of semantic search results with scores and segment details
     * @throws RuntimeException if the GraphQL request fails or returns errors
     */
    public List<SemanticSearchResult> semanticSearch(String queryText, List<UUID> contentIdsOfCourse) {

        final String semanticSearchQuery = """
            query ($queryText: String!, $contentWhitelist: [UUID!]!) {
                _internal_noauth_semanticSearch(queryText: $queryText, contentWhitelist: $contentWhitelist) {
                    score
                    ... on MediaRecordSegmentSemanticSearchResult {
                      __typename
                      mediaRecordSegment {
                        id
                        __typename
                        mediaRecordId
                        ... on DocumentRecordSegment {
                          page
                          text
                        }
                        ... on VideoRecordSegment {
                            startTime
                        }
                      }
                    }
                }
            }
            """;

        try {
            return graphQlClient.document(semanticSearchQuery)
                    .variable("queryText", queryText)
                    .variable("contentWhitelist", contentIdsOfCourse)
                    .execute()
                    .handle((ClientGraphQlResponse response, SynchronousSink<List<SemanticSearchResult>> sink) -> {
                        if (response.isValid()) {
                            // extract only the nested object from { semanticSearch { score } }
                            List<SemanticSearchResult> results = response
                                    .field("_internal_noauth_semanticSearch")
                                    .toEntityList(SemanticSearchResult.class);
                            sink.next(results);
                            sink.complete();
                        } else {
                            sink.error(new RuntimeException("GraphQL errors: " + response.getErrors()));
                        }
                    })
                    .retry(RETRY_COUNT)
                    .block();
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }

    }

}
