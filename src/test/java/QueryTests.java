import static org.junit.jupiter.api.Assertions.assertEquals;

import net.leanix.api.common.ApiClient;
import net.leanix.api.common.ApiClientBuilder;
import net.leanix.api.common.ApiException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;

class QueryTests {

  private ApiClient apiClient;

  QueryTests() {
    String token = new FileUtils("apitoken.txt").read().trim();
    this.apiClient = new ApiClientBuilder()
            .withBasePath("https://us.leanix.net/services/pathfinder/v1")
            .withApiToken(token)
            .withTokenProviderHost("us.leanix.net")
            .build();
  }

  @Test
  // Test if the query substitutions are made correctly
  void testConstructor() {
    Map<String, String> subtestIds = new HashMap<String, String>();
    subtestIds.put("id", "d960c788-f127-4ed4-93ab-be7e84fa0ce8");

    Query subtestQuery = new Query(this.apiClient, "subtest.graphql", subtestIds);
    String q = subtestQuery.getQuery();
    String expected = "{\n  factSheet(id: \"d960c788-f127-4ed4-93ab-be7e84fa0ce8\") {\n    id\n    type\n  }\n}\n";

    assertEquals(q, expected);
  }

  @Test
  // Test query execution
  void testExecute() {
    Map<String, String> subtestIds = new HashMap<String, String>();
    subtestIds.put("id", "d960c788-f127-4ed4-93ab-be7e84fa0ce8");

    Query subtestQuery = new Query(this.apiClient, "subtest.graphql", subtestIds);

    try {
      // If ApiException isn't caught, then execution worked.
      subtestQuery.execute();
    }
    catch (ApiException e) {
      e.printStackTrace();
      fail();
    }
  }
}
