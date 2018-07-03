import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.leanix.api.common.ApiClient;
import net.leanix.api.common.ApiClientBuilder;
import net.leanix.api.common.ApiException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  // Remove existing ITComponent -> Behavior Provider and DataObject -> Behavior Provider relations
  private void resetRelations() {
    // TODO: Create abstraction that allows for faster traversal of the query result
    Query check = new Query(apiClient, "checkrelations.graphql", new HashMap<String, String>());
    Map<String, Map<String, Object>> checkData = new HashMap<String, Map<String, Object>>();
    try {
      checkData = check.execute();
    }
    catch (ApiException e) {
      fail();
    }
    // Get Behavior Provider ID
    Map<String, Object> relInterfaceToProviderApplication = checkData.get("relInterfaceToProviderApplication");
    List<Map<String, Object>> providers = (List<Map<String, Object>>)
            relInterfaceToProviderApplication.get("edges");

    if (providers.size() > 0) {
      Map<String, Object> behaviorProviderNode = (Map<String, Object>) providers.get(0).get("node");
      Map<String, Object> behaviorProviderFactSheet = (Map<String, Object>) behaviorProviderNode.get("factSheet");

      String behaviorProviderId = (String) behaviorProviderFactSheet.get("id");

      // Look for Behavior Provider ID in ITComponent -> Application relations
      List<Map<String, Object>> itComponentEdges = (List<Map<String, Object>>)
              checkData.get("relInterfaceToITComponent").get("edges");

      for (Map<String, Object> itEdge : itComponentEdges) {
        Map<String, Object> itNode = (Map<String, Object>) itEdge.get("node");
        Map<String, Object> itFactSheet = (Map<String, Object>) itNode.get("factsheet");

        String itId = (String) itFactSheet.get("id");

        Map<String, Object> relITComponentToApplication = (Map<String, Object>)
                itFactSheet.get("relITComponentToApplication");
        String relationId = (String) relInterfaceToProviderApplication.get("id");

        List<Map<String, Object>> applications = (List<Map<String, Object>>) relITComponentToApplication.get("edges");

        for (Map<String, Object> applicationEdge : applications) {
          Map<String, Object> applicationNode = (Map<String, Object>) applicationEdge.get("node");
          Map<String, Object> applicationFactSheet = (Map<String, Object>) applicationNode.get("factsheet");

          String applicationId = (String) applicationFactSheet.get("id");

          // If this Application is the Behavior Provider, remove it
          if (applicationId.equals(behaviorProviderId)) {
            // Get the revision number
            Map<String, String> revIds = new HashMap<String, String>();
            revIds.put("id", itId);

            Query revQuery = new Query(apiClient, "rev.graphql", revIds);
            Map<String, Map<String, Object>> revData = new HashMap<String, Map<String, Object>>();
            try {
              revData = revQuery.execute();
            }
            catch (ApiException e) {
              fail();
            }

            String rev = revData.get("factSheet").get("rev").toString();

            // Remove the relation
            Map<String, String> removeIds = new HashMap<String, String>();
            removeIds.put("id", itId);
            removeIds.put("rev", rev);
            removeIds.put("relid", relationId);
            removeIds.put("appid", applicationId);

            Query removeQuery = new Query(apiClient, "remove.graphql", removeIds);
            try {
              removeQuery.execute();
            }
            catch (ApiException e) {
              fail();
            }
          }
        }
      }
    }
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
    this.resetRelations();

    // Execute query/mutation
    // Check if desired relationships exist
  }
}
