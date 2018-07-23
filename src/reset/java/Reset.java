import net.leanix.api.common.ApiClient;
import net.leanix.api.common.ApiClientBuilder;
import net.leanix.api.common.ApiException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Reset {

  // TODO: Figure out a better way to handle exception
  public static void main(String[] args) throws FileNotFoundException {
    FileUtils relationsFile1 = new FileUtils("ids/a1.txt", false);
    FileUtils relationsFile2 = new FileUtils("ids/a2.txt", false);

    List<String> relationsStr = new ArrayList<String>(relationsFile1.readlines());
    relationsStr.addAll(relationsFile2.readlines());

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

      reset(apiClient, relations);
      // If reset was successful, erase the files to stay up to date
      // TODO: Come up with a better way of storing change history so it can be kept even after a reset
      erase("a1.txt");
      erase("a2.txt");
    }
    catch (ApiException e) {
      System.out.println("Invalid API token.");
    }
  }

  // Remove newly created relations
  private static void reset(ApiClient apiClient, List<List<String>> relations) {
    for (List<String> ids : relations) {
      // Get the revision number
      Map<String, String> revIds = new HashMap<String, String>();
      revIds.put("id", ids.get(1));

      Query revQuery = new Query(apiClient, "rev.graphql", revIds);
      try {
        Map<String, Map<String, Object>> revData = revQuery.execute();

        // All this needs to be in the try as well because the program will not be able to continue
        // to this code if revQuery.execute() throws an ApiException. The only workaround is
        // System.exit(1) in the catch block, which is bad practice.
        String rev = revData.get("factSheet").get("rev").toString();
        String type = revData.get("factSheet").get("type").toString();

        // Remove the relation
        Map<String, String> removeIds = new HashMap<String, String>();
        removeIds.put("id", ids.get(1));
        removeIds.put("type", type);
        removeIds.put("rev", rev);
        removeIds.put("relid", ids.get(2));

        Query removeQuery = new Query(apiClient, "remove.graphql", removeIds);
        try {
          removeQuery.execute();
          System.out.println(">>> Relation between " + ids.get(0) + " and " + ids.get(1) + " removed.");
        }
        catch (ApiException e) {
          System.out.println("Relation between " + ids.get(0) + " and " + ids.get(1) + " not found.");
        }
      }
      catch (ApiException e) {
        e.printStackTrace();
      }
    }
  }

  // Output changed IDs of affected fact sheets
  private static void erase(String fname) {
    Path mainFile = Paths.get("ids/" + fname);
    Path testFile = Paths.get("ids/" + fname);
    try {
      Files.write(mainFile, new ArrayList<String>(), Charset.forName("UTF-8"));
      Files.write(testFile, new ArrayList<String>(), Charset.forName("UTF-8"));
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }
}
