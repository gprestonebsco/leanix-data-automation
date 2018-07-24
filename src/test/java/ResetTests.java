import net.leanix.api.common.ApiClient;
import net.leanix.api.common.ApiClientBuilder;
import net.leanix.api.common.ApiException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("LoopConditionNotUpdatedInsideLoop")
class ResetTests {
  // 1. Remove desired relations if they exist
  // 2. Run automation1 to re-add some and set ids/
  // 3. Run Reset and check if the correct relations are removed
  // 4. Run automation2 to re-add the rest and set ids/
  // 5. Run Reset and check if the correct relations are removed

  private ApiClient apiClient;

  ResetTests() throws ApiException {
    String token = new FileUtils("apitoken.txt").read();
    this.apiClient = new ApiClientBuilder()
            .withBasePath("https://us.leanix.net/services/pathfinder/v1")
            .withApiToken(token)
            .withTokenProviderHost("us.leanix.net")
            .build();

    Query resetQuery = new Query(this.apiClient,"resetquery.graphql", new HashMap<String, String>());
    Map<String, Map<String, Object>> resetData = resetQuery.execute();
    String providerId = (String) resetData.get("factSheet").get("id");

    // Find related ITComponents and DataObjects
    Map<String, Object> relApplicationToITComponent = (Map<String, Object>)
            resetData.get("factSheet").get("relApplicationToITComponent");
    Map<String, Object> relApplicationToDataObject = (Map<String, Object>)
            resetData.get("factSheet").get("relApplicationToDataObject");

    List<Map<String, Object>> itComponentEdges = (List<Map<String, Object>>) relApplicationToITComponent.get("edges");
    List<Map<String, Object>> dataObjectEdges = (List<Map<String, Object>>) relApplicationToDataObject.get("edges");

    List<String> itComponentIds = new ArrayList<String>();
    List<String> dataObjectIds = new ArrayList<String>();

    // Iterate through ITComponent edges
    for (Map<String, Object> itComponentEdge : itComponentEdges) {
      Map<String, Object> itComponentNode = (Map<String, Object>) itComponentEdge.get("node");
      itComponentIds.add((String) itComponentNode.get("id"));
    }

    // Iterate through DataObject edges
    for (Map<String, Object> dataObjectEdge : dataObjectEdges) {
      Map<String, Object> dataObjectNode = (Map<String, Object>) dataObjectEdge.get("node");
      dataObjectIds.add((String) dataObjectNode.get("id"));
    }

    // Remove found relations
    this.removeRelations(providerId, itComponentIds);
    this.removeRelations(providerId, dataObjectIds);
  }

  // Remove the given relations from the given fact sheet (used specifically for provider reset)
  private void removeRelations(String providerId, List<String> relationIds) throws ApiException {
    for (String relId : relationIds) {
      String rev = QueryUtils.getRev(this.apiClient, providerId);

      Map<String, String> removeIds = new HashMap<String, String>();
      removeIds.put("id", providerId);
      removeIds.put("rev", rev);
      removeIds.put("type", "ITComponent");
      removeIds.put("relid", relId);

      Query removeQuery = new Query(this.apiClient, "remove-reset.graphql", removeIds);
      removeQuery.execute();
    }
  }

  @Test
  // Test if Reset correctly removes relations created by automation 1
  synchronized void testResetAutomation1() throws InterruptedException {
    // Re-add automation 1 relations so they can be reset
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

    String[] args = {new FileUtils("apitoken.txt").read()};
    Reset.main(args);

    // Check if ITComponents are gone
    Query checkResetQuery = new Query(this.apiClient, "checkreset.graphql", new HashMap<String, String>());
    try {
      Map<String, Map<String, Object>> checkResetData = checkResetQuery.execute();
      this.checkType(true, checkResetData);
    }
    catch (ApiException e) {
      fail();
    }
  }

  @Test
  // Test if Reset correctly removes relations created by automation 2
  synchronized void testResetAutomation2() throws InterruptedException {
    // Re-add automation 2 relations so they can be reset
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

    String[] args = {new FileUtils("apitoken.txt").read()};
    Reset.main(args);

    // Check if DataObjects are gone
    Query checkResetQuery = new Query(this.apiClient, "checkreset.graphql", new HashMap<String, String>());
    try {
      Map<String, Map<String, Object>> checkResetData = checkResetQuery.execute();
      this.checkType(false, checkResetData);
    }
    catch (ApiException e) {
      fail();
    }
  }

  private void checkType(boolean itComponent, Map<String, Map<String, Object>> data) {
    String type;
    if (itComponent) {
      type = "ITComponent";
    }
    else {
      type = "DataObject";
    }

    Map<String, Object> relToType = (Map<String, Object>)
            data.get("factSheet") .get("relApplicationTo" + type);
    List<Map<String, Object>> edges = (List<Map<String, Object>>) relToType.get("edges");

    // edges should be empty if Reset worked correctly
    if (edges.size() > 0) {
      fail();
    }
  }
}
