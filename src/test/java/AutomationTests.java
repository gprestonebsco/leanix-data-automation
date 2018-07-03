import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.leanix.api.common.ApiClient;
import net.leanix.api.common.ApiClientBuilder;
import net.leanix.api.common.ApiException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
  synchronized void testAutomation1() throws InterruptedException {
    // If relationships exist, remove them
    this.resetType(true);

    // Execute query/mutation
    Map<String, List<Map<String, Map<String, Object>>>> automation1Data = null;
    try {
      automation1Data = Main.automation1(this.apiClient);
      this.notify();
    }
    catch (ApiException e) {
      fail();
    }

    while (automation1Data == null) {
      this.wait();
    }

    // TODO: Sometimes fails here. Indicates that resetting isn't always removing relation properly.
    if (automation1Data.size() == 0) {
      fail();
    }

    // Check if desired relationships exist
    String providerId = automation1Data.keySet().iterator().next();
    this.checkType(true, providerId, automation1Data.get(providerId).get(0));
  }

  @Test
  synchronized void testAutomation2() throws InterruptedException {
    // If relationships exist, remove them
    this.resetType(true);

    // Execute query/mutation
    Map<String, List<Map<String, Map<String, Object>>>> automation1Data = null;
    try {
      automation1Data = Main.automation1(this.apiClient);
      this.notify();
    }
    catch (ApiException e) {
      fail();
    }

    while (automation1Data == null) {
      this.wait();
    }

    // TODO: Sometimes fails here. Indicates that resetting isn't always removing relation properly.
    if (automation1Data.size() == 0) {
      fail();
    }

    // Check if desired relationships exist
    String providerId = automation1Data.keySet().iterator().next();
    this.checkType(true, providerId, automation1Data.get(providerId).get(0));
  }

  // Remove existing ITComponent -> Behavior Provider or DataObject -> Behavior Provider relations.
  // resetType(true) indicates ITComponent, resetType(false) indicates DataObject
  private void resetType(boolean itComponent) {
    // TODO: Create abstraction that allows for faster traversal of the query result
    String type;
    if (itComponent) {
      type = "ITComponent";
    }
    else {
      type = "DataObject";
    }

    Query check = new Query(apiClient, "checkrelations.graphql", new HashMap<String, String>());
    Map<String, Map<String, Object>> checkData = new HashMap<String, Map<String, Object>>();
    try {
      checkData = check.execute();
    }
    catch (ApiException e) {
      fail();
    }
    // Get Behavior Provider ID
    Map<String, Object> relInterfaceToProviderApplication = (Map<String, Object>)
            checkData.get("factSheet").get("relInterfaceToProviderApplication");
    List<Map<String, Object>> providers = (List<Map<String, Object>>)
            relInterfaceToProviderApplication.get("edges");

    if (providers.size() > 0) {
      Map<String, Object> behaviorProviderNode = (Map<String, Object>) providers.get(0).get("node");
      Map<String, Object> behaviorProviderFactSheet = (Map<String, Object>) behaviorProviderNode.get("factSheet");

      String behaviorProviderId = (String) behaviorProviderFactSheet.get("id");

      // Look for Behavior Provider ID in Type -> Application relations
      Map<String, Object> relInterfaceToType = (Map<String, Object>)
              checkData.get("factSheet").get("relInterfaceTo" + type);
      List<Map<String, Object>> typeEdges = (List<Map<String, Object>>)
              relInterfaceToType.get("edges");

      for (Map<String, Object> typeEdge : typeEdges) {
        Map<String, Object> typeNode = (Map<String, Object>) typeEdge.get("node");
        Map<String, Object> typeFactSheet = (Map<String, Object>) typeNode.get("factSheet");

        String typeId = (String) typeFactSheet.get("id");
        String typeType = (String) typeFactSheet.get("type");

        Map<String, Object> relTypeToApplication = (Map<String, Object>)
                typeFactSheet.get("rel" + type + "ToApplication");
        List<Map<String, Object>> applications = (List<Map<String, Object>>) relTypeToApplication.get("edges");

        for (Map<String, Object> applicationEdge : applications) {
          Map<String, Object> applicationNode = (Map<String, Object>) applicationEdge.get("node");
          Map<String, Object> applicationFactSheet = (Map<String, Object>) applicationNode.get("factSheet");

          String relationId = (String) applicationNode.get("id");
          String applicationId = (String) applicationFactSheet.get("id");

          // If this Application is the Behavior Provider, remove it
          if (applicationId.equals(behaviorProviderId)) {
            // Get the revision number
            Map<String, String> revIds = new HashMap<String, String>();
            revIds.put("id", typeId);

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
            removeIds.put("id", typeId);
            removeIds.put("type", typeType);
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

  private void checkType(boolean itComponent, String providerId, Map<String, Map<String, Object>> data) {
    String type;
    if (itComponent) {
      type = "ITComponent";
    }
    else {
      type = "DataObject";
    }

    Map<String, Object> factSheet = (Map<String, Object>) data.get("updateFactSheet").get("factSheet");
    Map<String, Object> relTypeToApplication = (Map<String, Object>) factSheet.get("rel" + type + "ToApplication");
    List<Map<String, Object>> edges = (List<Map<String, Object>>) relTypeToApplication.get("edges");

    // Check if Behavior Provider is related to the given fact sheet
    boolean containsProviderId = false;
    for (Map<String, Object> e : edges) {
      Map<String, Object> applicationNode = (Map<String, Object>) e.get("node");
      Map<String, Object> applicationFactSheet = (Map<String, Object>) applicationNode.get("factSheet");
      String applicationId = (String) applicationFactSheet.get("id");

      if (applicationId.equals(providerId)) {
        containsProviderId = true;
        break;
      }
    }

    if (!containsProviderId) {
      fail();
    }
  }

  // Check if desired relations exist for either ITComponents or DataObjects.
  // checkType(true) indicates ITComponent, checkType(false) indicates DataObject
  private void checkType(boolean itComponent) {
    String type;
    if (itComponent) {
      type = "ITComponent";
    }
    else {
      type = "DataObject";
    }

    Query check = new Query(apiClient, "checkrelations.graphql", new HashMap<String, String>());
    Map<String, Map<String, Object>> checkData = new HashMap<String, Map<String, Object>>();
    try {
      checkData = check.execute();
    }
    catch (ApiException e) {
      fail();
    }
    // Get Behavior Provider ID
    Map<String, Object> relInterfaceToProviderApplication = (Map<String, Object>)
            checkData.get("factSheet").get("relInterfaceToProviderApplication");
    List<Map<String, Object>> providers = (List<Map<String, Object>>)
            relInterfaceToProviderApplication.get("edges");

    if (providers.size() > 0) {
      Map<String, Object> behaviorProviderNode = (Map<String, Object>) providers.get(0).get("node");
      Map<String, Object> behaviorProviderFactSheet = (Map<String, Object>) behaviorProviderNode.get("factSheet");

      String behaviorProviderId = (String) behaviorProviderFactSheet.get("id");

      // Look for Behavior Provider ID in Type -> Application relations
      Map<String, Object> relInterfaceToType = (Map<String, Object>)
              checkData.get("factSheet").get("relInterfaceTo" + type);
      List<Map<String, Object>> typeEdges = (List<Map<String, Object>>)
              relInterfaceToType.get("edges");

      for (Map<String, Object> typeEdge : typeEdges) {
        Map<String, Object> typeNode = (Map<String, Object>) typeEdge.get("node");
        Map<String, Object> typeFactSheet = (Map<String, Object>) typeNode.get("factSheet");

        Map<String, Object> relTypeToApplication = (Map<String, Object>)
                typeFactSheet.get("rel" + type + "ToApplication");
        List<Map<String, Object>> applications = (List<Map<String, Object>>) relTypeToApplication.get("edges");

        // Check if this fact sheet has a relation to the Behavior Provider
        boolean containsBehaviorProviderId = false;
        for (Map<String, Object> applicationEdge : applications) {
          Map<String, Object> applicationNode = (Map<String, Object>) applicationEdge.get("node");
          Map<String, Object> applicationFactSheet = (Map<String, Object>) applicationNode.get("factSheet");
          String applicationId = (String) applicationFactSheet.get("id");

          if (applicationId.equals(behaviorProviderId)) {
            containsBehaviorProviderId = true;
            break;
          }
        }

        // If desired relation doesn't exist, fail
        if (!containsBehaviorProviderId) {
          fail();
        }
      }
    }
  }
}
