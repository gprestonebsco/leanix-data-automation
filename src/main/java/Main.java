import net.leanix.api.common.*;

import java.io.*;
import java.net.URL;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

  // TODO: Create method that removes all relations created in the current run
  // TODO: Print metrics of number of checked Behaviors and number of created relations at the end

  // Return a Map of Provider ID (String) -> List<mutation query data> (Map<String, Map<String, Object>>) for checking
  static Map<String, List<Map<String, Map<String, Object>>>> automation1(ApiClient apiClient) throws ApiException {
    Query mainQuery = new Query(apiClient, "main1.graphql", new HashMap<String, String>());
    Map<String, Map<String, Object>> mainData = mainQuery.execute();

    List<Map<String, Object>> edgeList = (List<Map<String, Object>>) mainData.get("allFactSheets").get("edges");

    // Keep track of newly created relations to not create them twice
    List<String> editedItComponents = new ArrayList<String>();

    // Map Behavior Provider ID to a list of return values of mutations involving that Provider
    Map<String, List<Map<String, Map<String, Object>>>> ret =
            new HashMap<String, List<Map<String, Map<String, Object>>>>();

    // Iterate through all behaviors
    for (Map<String, Object> edge : edgeList) {
      System.out.println("------------------------------------------------------------------\n");

      Map<String, Object> node = (Map<String, Object>) edge.get("node");
      System.out.println("Behavior: " + node.get("displayName") + " (" + node.get("id") + ")");

      // Get Behavior Provider ID
      Map<String, Object> relInterfaceToProviderApplication = (Map<String, Object>)
              node.get("relInterfaceToProviderApplication");
      List<Map<String, Object>> applications = (List<Map<String, Object>>)
              relInterfaceToProviderApplication.get("edges");

      if (applications.size() > 0) {
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

          String itComponentId = (String) itFactSheet.get("id");
          String itComponentDisplayName = (String) itFactSheet.get("displayName");
          boolean editNeeded = !(containsBehaviourProviderId || editedItComponents.contains(itComponentId));

          System.out.println("* " + itComponentDisplayName + " (" + itComponentId + "): "
                  + !editNeeded);

          // If IT Component is not related to the Behavior Provider, create the relation
          if (editNeeded) {
            // Get the revision number
            Map<String, String> revIds = new HashMap<String, String>();
            revIds.put("id", itComponentId);

            Query revQuery = new Query(apiClient, "rev.graphql", revIds);
            Map<String, Map<String, Object>> revData = revQuery.execute();

            String rev = revData.get("factSheet").get("rev").toString();

            // Make the mutation
            Map<String, String> mutationIds = new HashMap<String, String>();
            mutationIds.put("itcomponentid", itComponentId);
            mutationIds.put("rev", rev);
            mutationIds.put("providerid", behaviorProviderId);

            Query mutationQuery = new Query(apiClient, "mutation1.graphql", mutationIds);
            Map<String, Map<String, Object>> mutationData = mutationQuery.execute();

            if (mutationData != null) {
              editedItComponents.add(itComponentId);
              if (!ret.containsKey(behaviorProviderId)) {
                ret.put(behaviorProviderId, new ArrayList<Map<String, Map<String, Object>>>());
              }
              ret.get(behaviorProviderId).add(mutationData);

              System.out.println("  >>> Relation between " + itComponentDisplayName
                      + " and " + behaviorProviderDisplayName + " added.");
            }
            else {
              System.out.println("  >>> WARNING: No response for relation mutation .");
            }
          }
        }
      }
    }
    return ret;
  }

  static Map<String, List<Map<String, Map<String, Object>>>> automation2(ApiClient apiClient) throws ApiException {
    Query mainQuery = new Query(apiClient, "main2.graphql", new HashMap<String, String>());
    Map<String, Map<String, Object>> mainData = mainQuery.execute();

    List<Map<String, Object>> edgeList = (List<Map<String, Object>>) mainData.get("allFactSheets").get("edges");

    // Keep track of newly created relations to not create them twice
    List<String> editedDataObjects = new ArrayList<String>();

    // Map Behavior Provider ID to a list of return values of mutations involving that Provider
    Map<String, List<Map<String, Map<String, Object>>>> ret =
            new HashMap<String, List<Map<String, Map<String, Object>>>>();

    // Iterate through all behaviors
    for (Map<String, Object> edge : edgeList) {
      System.out.println("------------------------------------------------------------------\n");

      Map<String, Object> node = (Map<String, Object>) edge.get("node");
      System.out.println("Behavior: " + node.get("displayName") + " (" + node.get("id") + ")");

      // Get Behavior Provider ID
      Map<String, Object> relInterfaceToProviderApplication = (Map<String, Object>)
              node.get("relInterfaceToProviderApplication");
      List<Map<String, Object>> applications = (List<Map<String, Object>>)
              relInterfaceToProviderApplication.get("edges");

      if (applications.size() > 0) {
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

          String dataObjectId = (String) dataFactSheet.get("id");
          String dataObjectDisplayName = (String) dataFactSheet.get("displayName");
          boolean editNeeded = !(containsBehaviourProviderId || editedDataObjects.contains(dataObjectId));

          System.out.println("* " + dataObjectDisplayName + " (" + dataObjectId + "): "
                  + !editNeeded);

          // If Data Object is not related to the Behavior Provider, create the relation
          if (editNeeded) {
            // Get the revision number
            Map<String, String> revIds = new HashMap<String, String>();
            revIds.put("id", dataObjectId);

            Query revQuery = new Query(apiClient, "rev.graphql", revIds);
            Map<String, Map<String, Object>> revData = revQuery.execute();

            String rev = revData.get("factSheet").get("rev").toString();

            // Make the mutation
            Map<String, String> mutationIds = new HashMap<String, String>();
            mutationIds.put("dataobjectid", dataObjectId);
            mutationIds.put("rev", rev);
            mutationIds.put("providerid", behaviorProviderId);

            Query mutationQuery = new Query(apiClient, "mutation2.graphql", mutationIds);
            Map<String, Map<String, Object>> mutationData = mutationQuery.execute();

            if (mutationData != null) {
              editedDataObjects.add(dataObjectId);
              if (!ret.containsKey(behaviorProviderId)) {
                ret.put(behaviorProviderId, new ArrayList<Map<String, Map<String, Object>>>());
              }
              ret.get(behaviorProviderId).add(mutationData);

              System.out.println("  >>> Relation between " + dataObjectDisplayName
                      + " and " + behaviorProviderDisplayName + " added.");
            }
            else {
              System.out.println("  >>> WARNING: No response for relation mutation .");
            }
          }
        }
      }
    }
    return ret;
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

  private void output(String out) throws Exception {
    URL resourceUrl = getClass().getResource("newrelations.txt");
    File file = new File(resourceUrl.toURI());
    FileWriter fileWriter = new FileWriter(file);
    BufferedWriter outStream = new BufferedWriter(fileWriter);
    outStream.write(out);
    outStream.close();
    System.out.println("Relevant IDs written to newrelations.txt.");
  }

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

    Map<String, List<Map<String, Map<String, Object>>>> automation1Data = automation1(apiClient);

    System.out.println("\n==================================================================");
    System.out.println("AUTOMATION 2");

    Map<String, List<Map<String, Map<String, Object>>>> automation2Data = automation2(apiClient);

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
}
