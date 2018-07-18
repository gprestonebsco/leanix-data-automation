import net.leanix.api.common.*;


import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

  // TODO: Handle ApiException correctly while keeping track of already mutated fact sheets
  public static void main(String[] args) throws ApiException {
    ApiClient apiClient = new ApiClientBuilder()
            .withBasePath("https://us.leanix.net/services/pathfinder/v1")
            .withApiToken(args[0])
            .withTokenProviderHost("us.leanix.net")
            .build();

    // Test API access
    Query test = new Query(apiClient, "test.graphql", new HashMap<String, String>());
    try {
      test.execute();
    }
    catch (ApiException e) {
      System.out.println("Invalid API token.");
      System.exit(1);
    }

    System.out.println("\n==================================================================");
    System.out.println("AUTOMATION 1");

    Automation a1 = new Automation(apiClient);
    Map<String, List<Map<String, Map<String, Object>>>> automation1Data = a1.automation1();

    System.out.println("\n==================================================================");
    System.out.println("AUTOMATION 2");

    Automation a2 = new Automation(apiClient);
    Map<String, List<Map<String, Map<String, Object>>>> automation2Data = a2.automation2();

    // TODO: Print metrics of number of checked Behaviors and other info
    System.out.println("\n==================================================================");
    System.out.println("METRICS");
    System.out.println("------------------------------------------------------------------\n");

    System.out.println(genMetrics(automation1Data, "ITComponent"));
    System.out.println(genMetrics(automation2Data, "DataObject"));
    System.out.println("------------------------------------------------------------------\n");

    List<List<String>> relations = new ArrayList<List<String>>(newRelationInfo(automation1Data));
    relations.addAll(newRelationInfo(automation2Data));

    List<String> relationsStr = new ArrayList<String>();
    for (List<String> ids : relations) {
      relationsStr.add(String.join(",", ids));
    }

    // Output changed IDs of affected fact sheets to newrelations.txt
    Path mainFile = Paths.get("src/main/resources/newrelations.txt");
    Path testFile = Paths.get("src/test/resources/newrelations.txt");
    try {
      Files.write(mainFile, relationsStr, Charset.forName("UTF-8"));
      System.out.println("Relevant IDs written to:");
      System.out.println("* src/main/resources/newrelations.txt.");
      Files.write(testFile, relationsStr, Charset.forName("UTF-8"));
      System.out.println("* src/test/resources/newrelations.txt.");
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  // Compute list of newly related fact sheets. Each element is a List of length 3 to store the
  // IDs of the Behavior Provider, ITComponent/DataObject, and relation in that order.
  private static List<List<String>> newRelationInfo(Map<String, List<Map<String, Map<String, Object>>>> data) {
    List<List<String>> relations = new ArrayList<List<String>>();

    // Iterate over different provider IDs
    for (String providerId : data.keySet()) {
      // Iterate over mutation data for a given provider ID
      for (Map<String, Map<String, Object>> datum : data.get(providerId)) {
        List<String> ids = new ArrayList<String>();
        ids.add(providerId); // Behavior Provider ID

        Map<String, Object> factSheet = (Map<String, Object>) datum.get("updateFactSheet").get("factSheet");

        String type = (String) factSheet.get("type");
        String typeId = (String) factSheet.get("id");
        ids.add(typeId); // ITComponent/DataObject ID

        Map<String, Object> relTypeToApplication = (Map<String, Object>) factSheet.get("rel" + type + "ToApplication");
        List<Map<String, Object>> edges = (List<Map<String, Object>>) relTypeToApplication.get("edges");

        // Find the newly created relation and document the relation ID
        String relationId;
        for (Map<String, Object> e : edges) {
          Map<String, Object> applicationNode = (Map<String, Object>) e.get("node");
          Map<String, Object> applicationFactSheet = (Map<String, Object>) applicationNode.get("factSheet");
          String applicationId = (String) applicationFactSheet.get("id");

          if (applicationId.equals(providerId)) {
            relationId = (String) applicationNode.get("id");
            ids.add(relationId); // Relation ID
            break;
          }
        }
        relations.add(ids);
      }
    }
    return relations;
  }

  // Generate metrics from automation return value
  // TODO: Figure out a way to not pass type
  private static String genMetrics(Map<String, List<Map<String, Map<String, Object>>>> ret, String type) {
    List<List<String>> relations = newRelationInfo(ret);
    return "Newly created " + type + " relations: " + relations.size();
  }
}
