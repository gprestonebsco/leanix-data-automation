import net.leanix.api.common.ApiClient;
import net.leanix.api.common.ApiException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Automation {

  private ApiClient apiClient;
  private int checkedBehaviors;
  private List<List<String>> newRelations;

  Automation(ApiClient apiClient) {
    this.apiClient = apiClient;
    this.checkedBehaviors = 0;
    this.newRelations = new ArrayList<List<String>>();
  }

  // Output changed IDs of affected fact sheets
  private void writeOutput(String fname) {
    List<List<String>> relations = this.getNewRelations();

    List<String> relationsStr = new ArrayList<String>();
    for (List<String> ids : relations) {
      relationsStr.add(String.join(",", ids));
    }

    String dirName = "ids";

    // Create ids directory if it doesn't exist
    File dir = new File(dirName);
    if (!dir.exists()) {
      boolean created = dir.mkdir();
      if (created) {
        System.out.println("ids/ directory created.");
      }
    }

    Path mainFile = Paths.get(dirName + "/" + fname);
    Path testFile = Paths.get(dirName + "/" + fname);
    try {
      Files.write(mainFile, relationsStr, Charset.forName("UTF-8"));
      Files.write(testFile, relationsStr, Charset.forName("UTF-8"));
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  // Return the number of checked Behaviors on the most recent run
  int getNumCheckedBehaviors() {
    return this.checkedBehaviors;
  }

  // Return a list of the relevant IDs to the relations created on the most recent run
  List<List<String>> getNewRelations() {
    return this.newRelations;
  }

  // Given the data from a new relation mutation and the affected Behavior Provider ID, create a list of length 3
  // to store the IDs of the Behavior Provider, ITComponent/DataObject, and relation in that order.
  private List<String> newRelationInfo(Map<String, Map<String, Object>> data, String providerId) {
    List<String> ids = new ArrayList<String>();
    ids.add(providerId); // Behavior Provider ID

    Map<String, Object> factSheet = (Map<String, Object>) data.get("updateFactSheet").get("factSheet");

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
    return ids;
  }

  // Roll up Behavior Provider and ITComponents.
  // Return a Map of Provider ID (String) -> List<mutation query data> (Map<String, Map<String, Object>>) for checking.
  Map<String, List<Map<String, Map<String, Object>>>> automation1() {
    this.checkedBehaviors = 0;
    this.newRelations = new ArrayList<List<String>>();

    // Map Behavior Provider ID to a list of return values of mutations involving that Provider
    Map<String, List<Map<String, Map<String, Object>>>> ret =
            new HashMap<String, List<Map<String, Map<String, Object>>>>();

    try {
      Query mainQuery = new Query(this.apiClient, "main1.graphql", new HashMap<String, String>());
      Map<String, Map<String, Object>> mainData = mainQuery.execute();

      List<Map<String, Object>> edgeList = (List<Map<String, Object>>) mainData.get("allFactSheets").get("edges");

      // Keep track of newly created relations to not create them twice
      List<String> editedItComponents = new ArrayList<String>();

      // Iterate through all behaviors
      for (Map<String, Object> edge : edgeList) {
        this.checkedBehaviors += 1;

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
              int rev = QueryUtils.getRev(this.apiClient, itComponentId);

              // Make the mutation
              Map<String, String> mutationIds = new HashMap<String, String>();
              mutationIds.put("itcomponentid", itComponentId);
              mutationIds.put("rev", Integer.toString(rev));
              mutationIds.put("providerid", behaviorProviderId);

              Query mutationQuery = new Query(this.apiClient, "mutation1.graphql", mutationIds);
              Map<String, Map<String, Object>> mutationData = mutationQuery.execute();

              if (mutationData != null) {
                editedItComponents.add(itComponentId);
                if (!ret.containsKey(behaviorProviderId)) {
                  ret.put(behaviorProviderId, new ArrayList<Map<String, Map<String, Object>>>());
                }
                ret.get(behaviorProviderId).add(mutationData);

                this.newRelations.add(this.newRelationInfo(mutationData, behaviorProviderId));

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
    }
    catch (ApiException e) {
      // If something goes wrong, save changes already made
      this.writeOutput("a1.txt");
    }
    this.writeOutput("a1.txt");
    return ret;
  }

  // Roll up Behavior Provider and DataObjects.
  // Return a Map of Provider ID (String) -> List<mutation query data> (Map<String, Map<String, Object>>) for checking.
  Map<String, List<Map<String, Map<String, Object>>>> automation2() {
    this.checkedBehaviors = 0;
    this.newRelations = new ArrayList<List<String>>();

    // Map Behavior Provider ID to a list of return values of mutations involving that Provider
    Map<String, List<Map<String, Map<String, Object>>>> ret =
            new HashMap<String, List<Map<String, Map<String, Object>>>>();

    try {
      Query mainQuery = new Query(this.apiClient, "main2.graphql", new HashMap<String, String>());
      Map<String, Map<String, Object>> mainData = mainQuery.execute();

      List<Map<String, Object>> edgeList = (List<Map<String, Object>>) mainData.get("allFactSheets").get("edges");

      // Keep track of newly created relations to not create them twice
      List<String> editedDataObjects = new ArrayList<String>();

      // Iterate through all behaviors
      for (Map<String, Object> edge : edgeList) {
        this.checkedBehaviors += 1;

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
              int rev = QueryUtils.getRev(this.apiClient, dataObjectId);

              // Make the mutation
              Map<String, String> mutationIds = new HashMap<String, String>();
              mutationIds.put("dataobjectid", dataObjectId);
              mutationIds.put("rev", Integer.toString(rev));
              mutationIds.put("providerid", behaviorProviderId);

              Query mutationQuery = new Query(this.apiClient, "mutation2.graphql", mutationIds);
              Map<String, Map<String, Object>> mutationData = mutationQuery.execute();

              if (mutationData != null) {
                editedDataObjects.add(dataObjectId);
                if (!ret.containsKey(behaviorProviderId)) {
                  ret.put(behaviorProviderId, new ArrayList<Map<String, Map<String, Object>>>());
                }
                ret.get(behaviorProviderId).add(mutationData);

                this.newRelations.add(this.newRelationInfo(mutationData, behaviorProviderId));

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
    }
    catch (ApiException e) {
      // If something goes wrong, save changes already made
      this.writeOutput("a2.txt");
    }
    this.writeOutput("a2.txt");
    return ret;
  }
}
