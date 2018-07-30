import net.leanix.api.common.*;

import java.util.List;

public class Main {

  public static void main(String[] args) {
    if (args.length == 0 || args[0].equals("--help")) {
      printUsage();
      System.exit(1);
    }

    ApiClient apiClient = new ApiClientBuilder()
            .withBasePath("https://us.leanix.net/services/pathfinder/v1")
            .withApiToken(args[0])
            .withTokenProviderHost("us.leanix.net")
            .build();

    // Test API access
    Query test = new Query(apiClient, "test.graphql");
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
    a1.automation1();

    System.out.println("\n==================================================================");
    System.out.println("AUTOMATION 2");

    Automation a2 = new Automation(apiClient);
    a2.automation2();

    System.out.println("\n==================================================================");
    System.out.println("METRICS");
    System.out.println("------------------------------------------------------------------\n");

    printMetrics(a1, a2);
    System.out.println("------------------------------------------------------------------\n");
    System.out.println("Automation 1 IDs saved to ids/a1.txt");
    System.out.println("Automation 2 IDs saved to ids/a2.txt");
  }

  // Print usage message
  private static void printUsage() {
    System.out.println("usage: java -jar main.jar <API token>");
  }

  // Print metrics from automation return values
  private static void printMetrics(Automation a1, Automation a2) {
    List<List<String>> relations1 = a1.getNewRelations();
    List<List<String>> relations2 = a2.getNewRelations();

    System.out.println("ITComponent");
    System.out.println("* Behaviors checked: " + a1.getNumCheckedBehaviors());
    System.out.println("* Newly created relations: " + relations1.size() + "\n");

    System.out.println("DataObject");
    System.out.println("* Behaviors checked: " + a2.getNumCheckedBehaviors());
    System.out.println("* Newly created DataObject relations: " + relations2.size());
  }
}
