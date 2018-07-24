import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import net.leanix.api.common.ApiException;
import org.junit.jupiter.api.Test;
import net.leanix.api.common.ApiClient;
import net.leanix.api.common.ApiClientBuilder;

class QueryUtilsTests {

  private ApiClient apiClient;

  QueryUtilsTests() {
    String token = new FileUtils("apitoken.txt").read().trim();
    this.apiClient = new ApiClientBuilder()
            .withBasePath("https://us.leanix.net/services/pathfinder/v1")
            .withApiToken(token)
            .withTokenProviderHost("us.leanix.net")
            .build();
  }

  @Test
  // Test for QueryUtils.getRev()
  void testGetRev() {
    try {
      int rev1 = QueryUtils.getRev(this.apiClient, "1c8b4a0a-c2b9-42b3-8bad-93dc2d5ece55");
      int rev2 = QueryUtils.getRev(this.apiClient, "d960c788-f127-4ed4-93ab-be7e84fa0ce8");

      // The revision number should be greater than or equal to the last revision number manually checked
      assertTrue(rev1 >= 628);
      assertTrue(rev2 >= 6);
    }
    catch (ApiException e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  // Test for QueryUtils.getType()
  void testGetType() {
    try {
      String type1 = QueryUtils.getType(this.apiClient, "1c8b4a0a-c2b9-42b3-8bad-93dc2d5ece55");
      String type2 = QueryUtils.getType(this.apiClient, "d960c788-f127-4ed4-93ab-be7e84fa0ce8");

      // The revision number should be greater than or equal to the last revision number manually checked
      assertEquals(type1, "Application");
      assertEquals(type2, "Interface");
    }
    catch (ApiException e) {
      e.printStackTrace();
      fail();
    }
  }
}
