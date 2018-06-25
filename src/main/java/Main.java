import net.leanix.api.common.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

  private static void automation1(ApiClient apiClient) throws ApiException {
    Query mainQuery = new Query(apiClient, "src/main/resources/main1.graphql", new HashMap<String, String>());
    Map<String, Map<String, Object>> mainData = mainQuery.execute();

    List<Map<String, Object>> edgeList = (List<Map<String, Object>>) mainData.get("allFactSheets").get("edges");

    // Iterate through all behaviors
    for (Map<String, Object> edge : edgeList) {
      Map<String, Object> node = (Map<String, Object>) edge.get("node");
      System.out.println("Behavior: " + node.get("displayName") + " (" + node.get("id") + ")");

      // Get behavior Provider IDs
      Map<String, Object> relInterfaceToProviderApplication = (Map<String, Object>)
              node.get("relInterfaceToProviderApplication");
      List<Map<String, Object>> applications = (List<Map<String, Object>>)
              relInterfaceToProviderApplication.get("edges");
      Map<String, Object> behaviorProviderNode = (Map<String, Object>) applications.get(0).get("node");
      Map<String, Object> behaviorProviderFactSheet = (Map<String, Object>) behaviorProviderNode.get("factSheet");

      String behaviorProviderDisplayName = (String) behaviorProviderFactSheet.get("displayName");
      String behaviorProviderId = (String) behaviorProviderFactSheet.get("id");

      System.out.println("Behavior Provider: " + behaviorProviderDisplayName + " (" + behaviorProviderId + ")");

      // Look through IT Components to see if they are each associated with the Behavior Provider
      Map<String, Object> relInterfaceToITComponent = (Map<String, Object>) node.get("relInterfaceToITComponent");
      List<Map<String, Object>> itComponents = (List<Map<String, Object>>) relInterfaceToITComponent.get("edges");

      System.out.println("\nIT Components contain Behavior Provider:");
      for (Map<String, Object> itEdge : itComponents) {
        Map<String, Object> itNode = (Map<String, Object>) itEdge.get("node");
        Map<String, Object> itFactSheet = (Map<String, Object>) itNode.get("factSheet");
        Map<String, Object> relITComponentToApplication = (Map<String, Object>)
                itFactSheet.get("relITComponentToApplication");
        List<Map<String, Object>> itApplications = (List<Map<String, Object>>)
                relITComponentToApplication.get("edges");

        // Try to find Behavior Provider ID in the IT Component's Bounded Countexts
        boolean containsBehaviourProviderId = false;
        for (Map<String, Object> applicationEdge : itApplications) {
          Map<String, Object> applicationNode = (Map<String, Object>) applicationEdge.get("node");
          Map<String, Object> applicationFactSheet = (Map<String, Object>) applicationNode.get("factSheet");
          if (applicationFactSheet.get("id").equals(behaviorProviderId)) {
            containsBehaviourProviderId = true;
            break;
          }
        }

        System.out.println("* " + itFactSheet.get("displayName") + " (" + itFactSheet.get("id") + "): "
                + containsBehaviourProviderId);

        // If IT Component is not related to the Behavior Provider, create the relation
        if (!containsBehaviourProviderId) {
          // Get the revision number
          Map<String, String> revIds = new HashMap<String, String>();
          revIds.put("id", (String) itFactSheet.get("id"));

          Query revQuery = new Query(apiClient, "src/main/resources/rev.graphql", revIds);
          Map<String, Map<String, Object>> revData = revQuery.execute();

          String rev = revData.get("factSheet").get("rev").toString();

          // Make the mutation
          Map<String, String> mutationIds = new HashMap<String, String>();
          mutationIds.put("itcomponentid", (String) itFactSheet.get("id"));
          mutationIds.put("rev", rev);
          mutationIds.put("providerid", behaviorProviderId);

          Query mutationQuery = new Query(apiClient, "src/main/resources/mutation1.graphql", mutationIds);
          Map<String, Map<String, Object>> mutationData = mutationQuery.execute();

          if (mutationData != null) {
            System.out.println("  >>> Relation between " + itFactSheet.get("displayName")
                    + " and " + behaviorProviderDisplayName + " added.");
          }
          else {
            System.out.println("  >>> WARNING: No response for relation mutation .");
          }
        }
      }

      System.out.println("\n------------------------------------------------------------------\n");
    }
  }

  private static void automation2(ApiClient apiClient) throws ApiException{
    Query mainQuery = new Query(apiClient, "src/main/resources/main2.graphql", new HashMap<String, String>());
    Map<String, Map<String, Object>> mainData = mainQuery.execute();

    List<Map<String, Object>> edgeList = (List<Map<String, Object>>) mainData.get("allFactSheets").get("edges");

    // Iterate through all behaviors
    for (Map<String, Object> edge : edgeList) {
      Map<String, Object> node = (Map<String, Object>) edge.get("node");
      System.out.println("Behavior: " + node.get("displayName") + " (" + node.get("id") + ")");

      // Get behavior Provider IDs
      Map<String, Object> relInterfaceToProviderApplication = (Map<String, Object>)
              node.get("relInterfaceToProviderApplication");
      List<Map<String, Object>> applications = (List<Map<String, Object>>)
              relInterfaceToProviderApplication.get("edges");
      Map<String, Object> behaviorProviderNode = (Map<String, Object>) applications.get(0).get("node");
      Map<String, Object> behaviorProviderFactSheet = (Map<String, Object>) behaviorProviderNode.get("factSheet");

      String behaviorProviderDisplayName = (String) behaviorProviderFactSheet.get("displayName");
      String behaviorProviderId = (String) behaviorProviderFactSheet.get("id");

      System.out.println("Behavior Provider: " + behaviorProviderDisplayName + " (" + behaviorProviderId + ")");

      // Look through Data Objects to see if they are each associated with the Behavior Provider
      Map<String, Object> relInterfaceToDataObject = (Map<String, Object>) node.get("relInterfaceToDataObject");
      List<Map<String, Object>> dataObjects = (List<Map<String, Object>>) relInterfaceToDataObject.get("edges");

      System.out.println("\nData Objects contain Behavior Provider:");
      for (Map<String, Object> dataEdge : dataObjects) {
        Map<String, Object> dataNode = (Map<String, Object>) dataEdge.get("node");
        Map<String, Object> dataFactSheet = (Map<String, Object>) dataNode.get("factSheet");
        Map<String, Object> relITComponentToApplication = (Map<String, Object>)
                dataFactSheet.get("relDataObjectToApplication");
        List<Map<String, Object>> dataApplications = (List<Map<String, Object>>)
                relITComponentToApplication.get("edges");

        // Try to find Behavior Provider ID in the IT Component's Bounded Countexts
        boolean containsBehaviourProviderId = false;
        for (Map<String, Object> applicationEdge : dataApplications) {
          Map<String, Object> applicationNode = (Map<String, Object>) applicationEdge.get("node");
          Map<String, Object> applicationFactSheet = (Map<String, Object>) applicationNode.get("factSheet");
          if (applicationFactSheet.get("id").equals(behaviorProviderId)) {
            containsBehaviourProviderId = true;
            break;
          }
        }

        System.out.println("* " + dataFactSheet.get("displayName") + " (" + dataFactSheet.get("id") + "): "
                + containsBehaviourProviderId);

        // If Data Object is not related to the Behavior Provider, create the relation
        if (!containsBehaviourProviderId) {
          // Get the revision number
          Map<String, String> revIds = new HashMap<String, String>();
          revIds.put("id", (String) dataFactSheet.get("id"));

          Query revQuery = new Query(apiClient, "src/main/resources/rev.graphql", revIds);
          Map<String, Map<String, Object>> revData = revQuery.execute();

          String rev = revData.get("factSheet").get("rev").toString();

          // Make the mutation
          Map<String, String> mutationIds = new HashMap<String, String>();
          mutationIds.put("dataobjectid", (String) dataFactSheet.get("id"));
          mutationIds.put("rev", rev);
          mutationIds.put("providerid", behaviorProviderId);

          Query mutationQuery = new Query(apiClient, "src/main/resources/mutation2.graphql", mutationIds);
          Map<String, Map<String, Object>> mutationData = mutationQuery.execute();

          if (mutationData != null) {
            System.out.println("  >>> Relation between " + dataFactSheet.get("displayName")
                    + " and " + behaviorProviderDisplayName + " added.");
          } else {
            System.out.println("  >>> WARNING: No response for relation mutation .");
          }
        }
      }

      System.out.println("\n------------------------------------------------------------------\n");
    }
  }

  private static void automation3(ApiClient apiClient) throws ApiException {
    Query finalQuery = new Query(apiClient, "src/main/resources/main3.graphql", new HashMap<String, String>());
    Map<String, Map<String, Object>> finalData = finalQuery.execute();

    List<Map<String, Object>> edgeList = (List<Map<String, Object>>) finalData.get("allFactSheets").get("edges");
    for (Map<String, Object> edge : edgeList) {
      Map<String, Object> node = (Map<String, Object>) edge.get("node");
      // TODO: Finish
    }
  }

  public static void main(String[] args) throws Exception {
    ApiClient apiClient = new ApiClientBuilder()
            .withBasePath("https://us.leanix.net/services/pathfinder/v1")
            .withApiToken("d3S2nyQCFVzskhjDf34ChA3jPp2UzMKpgdUv7MN3")
            .withTokenProviderHost("us.leanix.net")
            .build();

    System.out.println("AUTOMATION 1");

    automation1(apiClient);

    System.out.println("\n==================================================================\n");
    System.out.println("AUTOMATION 2");

    automation2(apiClient);

    System.out.println("\n==================================================================\n");
    System.out.println("AUTOMATION 3");

    //automation3(apiClient);
  }
}
