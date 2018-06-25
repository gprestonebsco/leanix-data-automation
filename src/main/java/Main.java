import com.google.common.graph.Graph;
import net.leanix.api.common.*;
import net.leanix.api.models.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

  public static void main(String[] args) throws Exception {
    ApiClient apiClient = new ApiClientBuilder()
            .withBasePath("https://us.leanix.net/services/pathfinder/v1")
            .withApiToken("d3S2nyQCFVzskhjDf34ChA3jPp2UzMKpgdUv7MN3")
            .withTokenProviderHost("us.leanix.net")
            .build();

    Query mainQuery = new Query(apiClient, "src/main/resources/main.graphql", new HashMap<String, String>());
    GraphQLResult mainResult = mainQuery.execute();

    if (mainResult.getErrors() != null) {
      System.out.println("ERROR (main):");
      System.out.println(mainResult.getErrors());
    }

    if (mainResult.getData() != null) {
      Map<String, Map<String, Object>> data = (Map<String, Map<String, Object>>) mainResult.getData();
      List<Map<String, Object>> edgeList = (List<Map<String, Object>>) data.get("allFactSheets").get("edges");

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

        // Look through IT Components to see if they are each associated with the behavior Provider
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
          boolean containsBehaviousProviderId = false;
          for (Map<String, Object> applicationEdge : itApplications) {
            Map<String, Object> applicationNode = (Map<String, Object>) applicationEdge.get("node");
            Map<String, Object> applicationFactSheet = (Map<String, Object>) applicationNode.get("factSheet");
            if (applicationFactSheet.get("id").equals(behaviorProviderId)) {
              containsBehaviousProviderId = true;
              break;
            }
          }

          System.out.println(itFactSheet.get("displayName") + " (" + itFactSheet.get("id") + "): "
                  + containsBehaviousProviderId);

          // If IT Component is not related to the Behavior Provider, create the relation
          if (!containsBehaviousProviderId) {
            // Get the revision number
            Map<String, String> revIds = new HashMap<String, String>();
            revIds.put("id", (String) itFactSheet.get("id"));

            Query revQuery = new Query(apiClient, "src/main/resources/rev.graphql", revIds);
            GraphQLResult revResult = revQuery.execute();

            if (revResult.getErrors() != null) {
              System.out.println("ERROR (rev):");
              System.out.println(revResult.getErrors());
            }

            if (revResult.getData() != null) {
              Map<String, Map<String, Object>> revData = (Map<String, Map<String, Object>>) revResult.getData();
              String rev = revData.get("factSheet").get("rev").toString();

              // Make the mutation
              Map<String, String> mutationIds = new HashMap<String, String>();
              mutationIds.put("itcomponentid", (String) itFactSheet.get("id"));
              mutationIds.put("rev", rev);
              mutationIds.put("providerid", behaviorProviderId);

              Query mutationQuery = new Query(apiClient, "src/main/resources/mutation.graphql", mutationIds);
              GraphQLResult mutationResult = mutationQuery.execute();

              if (mutationResult.getErrors() != null) {
                System.out.println("ERROR (mutation):");
                System.out.println(mutationResult.getErrors());
              }

              if (revResult.getData() != null) {
                /*
                Map<String, Map<String, Object>> mutationData = (Map<String, Map<String, Object>>)
                        mutationResult.getData();
                System.out.println(mutationData);
                */
                System.out.println("Relation between " + itFactSheet.get("displayName")
                        + " and " + behaviorProviderDisplayName + " added.");
              }
            }
          }
        }
        System.out.println("\n==================================================================\n");
      }
    }

    /*
    Map<String, Map<String, Object>> data = (Map<String, Map<String, Object>>) mainResult.getData();
    List<Map<String, Object>> edgeList = (List<Map<String, Object>>) data.get("allFactSheets").get("edges");

    for (Map<String, Object> edge : edgeList) {
      Map<String, Object> node = (Map<String, Object>) edge.get("node");
      System.out.println(node.get("displayName"));
    }
    */
  }
}
