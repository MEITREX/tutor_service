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

    public List<SemanticSearchResult> semanticSearch(String queryText, List<UUID> contentIdsOfCourse) {
        System.out.println("IDS in the Service Client class: " + contentIdsOfCourse);

        final String semanticSearchQuery = """
            query ($queryText: String!, $contentWhitelist: [UUID!]!) {
                _internal_noauth_semanticSearch(queryText: $queryText, contentWhitelist: $contentWhitelist) {
                    score
                    ... on MediaRecordSegmentSemanticSearchResult {
                      __typename
                      mediaRecordSegment {
                        id
                        ... on DocumentRecordSegment {
                          text
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
