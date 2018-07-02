import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.leanix.api.common.ApiClient;
import net.leanix.api.common.ApiClientBuilder;
import net.leanix.api.common.ApiException;

import java.util.HashMap;

class AutomationTests {

  private ApiClient apiClient;

  @BeforeEach
  void init() {
    String token = new FileUtils("apitoken.txt").read();

    this.apiClient = new ApiClientBuilder()
            .withBasePath("https://us.leanix.net/services/pathfinder/v1")
            .withApiToken(token)
            .withTokenProviderHost("us.leanix.net")
            .build();
  }

  @Test
  void testApi() {
    // Test API access
    Query test = new Query(apiClient, "test.graphql", new HashMap<String, String>());
    try {
      test.execute();
    }
    catch (ApiException e) {
      fail();
    }
  }

  @Test
  void testAutomation1() {
    // If relationships exist, remove them
    Query test = new Query(apiClient, "test.graphql", new HashMap<String, String>());
    try {
      test.execute();
    }
    catch (ApiException e) {
      fail();
    }

    // Execute query/mutation
    // Check if desired relationships exist
  }
}
