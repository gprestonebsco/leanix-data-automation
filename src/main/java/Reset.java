import net.leanix.api.common.ApiClient;
import net.leanix.api.common.ApiClientBuilder;
import net.leanix.api.common.ApiException;

import java.util.*;

public class Reset {

  public static void main(String[] args) {
    FileUtils relationsFile = new FileUtils("newrelations.txt");
    List<String> relationsStr = relationsFile.readlines();

    List<List<String>> relations = new ArrayList<List<String>>();
    for (String s : relationsStr) {
      relations.add(Arrays.asList(s.split(",")));
    }

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

    reset(apiClient, relations);
  }

  // Remove newly created relations
  private static void reset(ApiClient apiClient, List<List<String>> relations) {
    for (List<String> ids : relations) {
      // Get the revision number
      Map<String, String> revIds = new HashMap<String, String>();
      revIds.put("id", ids.get(1));

      Query revQuery = new Query(apiClient, "rev.graphql", revIds);
      Map<String, Map<String, Object>> revData = null;
      try {
        revData = revQuery.execute();
      }
      catch (ApiException e) {
        e.printStackTrace();
        System.exit(1);
      }

      String rev = revData.get("factSheet").get("rev").toString();
      String type = revData.get("factSheet").get("type").toString();

      // Remove the relation
      Map<String, String> removeIds = new HashMap<String, String>();
      removeIds.put("id", ids.get(1));
      removeIds.put("type", type);
      removeIds.put("rev", rev);
      removeIds.put("relid", ids.get(2));
      removeIds.put("appid", ids.get(0));

      Query removeQuery = new Query(apiClient, "remove.graphql", removeIds);
      try {
        removeQuery.execute();
        System.out.println(">>> Relation between " + ids.get(0) + " and " + ids.get(1) + " removed.");
      }
      catch (ApiException e) {
        System.out.println("Relation between " + ids.get(0) + " and " + ids.get(1) + " not found.");
      }
    }
  }
}
