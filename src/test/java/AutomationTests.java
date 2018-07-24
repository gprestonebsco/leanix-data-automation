import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import net.leanix.api.common.ApiClient;
import net.leanix.api.common.ApiClientBuilder;
import net.leanix.api.common.ApiException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("LoopConditionNotUpdatedInsideLoop")
class AutomationTests {

  private ApiClient apiClient;

  AutomationTests() {
    String token = new FileUtils("apitoken.txt").read().trim();
    this.apiClient = new ApiClientBuilder()
            .withBasePath("https://us.leanix.net/services/pathfinder/v1")
            .withApiToken(token)
            .withTokenProviderHost("us.leanix.net")
            .build();
  }

  @Test
  // Test API access
  void testApi() {
    Query test = new Query(this.apiClient, "test.graphql", new HashMap<String, String>());
    try {
      test.execute();
    }
    catch (ApiException e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  // Makes sure Data Automation Test Provider is correctly related to Data Automation Test
  // IT Component after automation1 due to relations to Data Automation Test Behavior.
  // Also makes sure no errors occur when Data Automation Test Behavior 2 is reached,
  // which would try to create the same relations as Data Automation Test Behavior.
  synchronized void testAutomation1() throws InterruptedException {
    // If relationships exist, remove them
    this.resetType(true);

    // Execute query/mutation
    Automation a1 = new Automation(this.apiClient);
    Map<String, List<Map<String, Map<String, Object>>>> automation1Data = a1.automation1();

    // Make sure automation has completed before continuing
    while (automation1Data == null) {
      this.wait();
    }
    this.notifyAll();

    // If nothing was changed, something went wrong
    if (automation1Data.size() == 0) {
      fail();
    }

    // Check if desired relationships exist
    String providerId = automation1Data.keySet().iterator().next();
    this.checkType(true, providerId, automation1Data.get(providerId).get(0));
  }

  @Test
  // Makes sure Data Automation Test Provider is correctly related to Data Automation Test
  // Data Object after automation1 due to relations to Data Automation Test Behavior.
  // Also makes sure no errors occur when Data Automation Test Behavior 2 is reached,
  // which would try to create the same relations as Data Automation Test Behavior.
  synchronized void testAutomation2() throws InterruptedException {
    // If relationships exist, remove them
    this.resetType(false);

    // Execute query/mutation
    Automation a2 = new Automation(this.apiClient);
    Map<String, List<Map<String, Map<String, Object>>>> automation2Data = a2.automation2();

    // Make sure automation has completed before continuing
    while (automation2Data == null) {
      this.wait();
    }
    this.notifyAll();

    // If nothing was changed, something went wrong
    if (automation2Data.size() == 0) {
      fail();
    }

    // Check if desired relationships exist
    String providerId = automation2Data.keySet().iterator().next();
    this.checkType(false, providerId, automation2Data.get(providerId).get(0));
  }

  // Remove existing ITComponent -> Behavior Provider or DataObject -> Behavior Provider relations.
  // resetType(true) indicates ITComponent, resetType(false) indicates DataObject.
  private void resetType(boolean itComponent) {
    String type;
    if (itComponent) {
      type = "ITComponent";
    }
    else {
      type = "DataObject";
    }
    System.out.println("Reset " + type + "\n");

    Query check = new Query(this.apiClient, "checkrelations.graphql", new HashMap<String, String>());
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
        String typeName = (String) typeFactSheet.get("displayName");
        String typeType = (String) typeFactSheet.get("type");

        Map<String, Object> relTypeToApplication = (Map<String, Object>)
                typeFactSheet.get("rel" + type + "ToApplication");
        List<Map<String, Object>> applications = (List<Map<String, Object>>) relTypeToApplication.get("edges");

        for (Map<String, Object> applicationEdge : applications) {
          Map<String, Object> applicationNode = (Map<String, Object>) applicationEdge.get("node");
          Map<String, Object> applicationFactSheet = (Map<String, Object>) applicationNode.get("factSheet");

          String relationId = (String) applicationNode.get("id");
          String applicationId = (String) applicationFactSheet.get("id");
          String applicationName = (String) applicationFactSheet.get("displayName");

          // If this Application is the Behavior Provider, remove it
          if (applicationId.equals(behaviorProviderId)) {
            // Get the revision number
            Integer rev = null;
            try {
              rev = QueryUtils.getRev(this.apiClient, typeId);
            }
            catch (ApiException e) {
              fail();
            }

            // Remove the relation
            Map<String, String> removeIds = new HashMap<String, String>();
            removeIds.put("id", typeId);
            removeIds.put("type", typeType);
            removeIds.put("rev", Integer.toString(rev));
            removeIds.put("relid", relationId);
            removeIds.put("appid", applicationId);

            Query removeQuery = new Query(this.apiClient, "remove.graphql", removeIds);
            try {
              removeQuery.execute();
              System.out.println(">>> Relation between " + typeName + " and " + applicationName + " removed.");
            }
            catch (ApiException e) {
              fail();
            }
          }
        }
      }
    }
    // Wait 0.5s to make sure API call finishes
    try {
      TimeUnit.MILLISECONDS.sleep(500);
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
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
}
