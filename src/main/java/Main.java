import net.leanix.api.*;
import net.leanix.api.common.*;
import net.leanix.api.models.*;

import java.util.List;
import java.util.Map;

public class Main {

  public static void main(String[] args) throws Exception {
    ApiClient apiClient = new ApiClientBuilder()
            .withBasePath("https://us.leanix.net/services/pathfinder/v1")
            .withApiToken("d3S2nyQCFVzskhjDf34ChA3jPp2UzMKpgdUv7MN3")
            .withTokenProviderHost("us.leanix.net")
            .build();

    GraphqlApi graphqlApi = new GraphqlApi(apiClient);

    FileUtils f = new FileUtils("src/main/resources/query.graphql");
    String query = f.readlines();

    GraphQLRequest graphqlRequest = new GraphQLRequest();
    graphqlRequest.setQuery(query);

    GraphQLResult graphqlResult = graphqlApi.processGraphQL(graphqlRequest);

    if (graphqlResult.getErrors() != null) {
      System.out.println("GraphQL response includes errors");
    }

    if (graphqlResult.getData() != null) {
      Map<String, Map<String, Object>> data = (Map<String, Map<String, Object>>) graphqlResult.getData();
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

          // Try to find Behavious Provider ID in the IT Component's Bounded Countexts
          boolean containsBehaviousProviderId = false;
          for (Map<String, Object> applicationEdge : itApplications) {
            Map<String, Object> applicationNode = (Map<String, Object>) applicationEdge.get("node");
            Map<String, Object> applicationFactSheet = (Map<String, Object>) applicationNode.get("factSheet");
            if (applicationFactSheet.get("id").equals(behaviorProviderId)) {
              containsBehaviousProviderId = true;
            }
          }
          System.out.println(itFactSheet.get("displayName") + " (" + itFactSheet.get("id") + "): "
                  + containsBehaviousProviderId);
        }
        System.out.println("\n==================================================================\n");
      }
    }

    /*
    Map<String, Map<String, Object>> data = (Map<String, Map<String, Object>>) graphqlResult.getData();
    List<Map<String, Object>> edgeList = (List<Map<String, Object>>) data.get("allFactSheets").get("edges");

    for (Map<String, Object> edge : edgeList) {
      Map<String, Object> node = (Map<String, Object>) edge.get("node");
      System.out.println(node.get("displayName"));
    }
    */
  }
}
