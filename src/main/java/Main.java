import net.leanix.api.common.*;
import org.apache.commons.cli.*;

import java.util.List;

public class Main {

  public static void main(String[] args) {
    // Command line options to either enter API token directly or read from a file
    Options options = new Options();

    Option tokenOpt = new Option("t", "token", true, "API token");
    options.addOption(tokenOpt);

    Option fileOpt = new Option("f", "file", true, "File containing API token");
    options.addOption(fileOpt);

    Option helpOpt = new Option("h", "help", false, "Display this message");
    options.addOption(helpOpt);

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();
    CommandLine cmd = null;

    try {
      cmd = parser.parse(options, args);
    }
    catch (ParseException e) {
      System.out.println(e.getMessage());
      formatter.printHelp("utility-name", options);
      System.exit(1);
    }

    boolean helpVal = cmd.hasOption("help");
    String tokenVal = cmd.getOptionValue("token");
    String fileVal = cmd.getOptionValue("file");

    if (helpVal) {
      formatter.printHelp("main.jar [option] <arg>", options);
      System.exit(0);
    }

    // Determine where to get API token from
    String token = null;
    if (tokenVal != null) {
      if (fileVal != null) {
        // Can only use one token
        System.out.println("Please enter only either a token or a file.");
        System.exit(1);
      }
      else {
        token = tokenVal;
      }
    }
    else if (fileVal != null) {
      FileUtils tokenFile = new FileUtils(fileVal, false);
      token = tokenFile.read().trim();
    }
    else {
      // No token specified
      System.out.println("No API token specified.");
      System.exit(1);
    }

    ApiClient apiClient = new ApiClientBuilder()
            .withBasePath("https://us.leanix.net/services/pathfinder/v1")
            .withApiToken(token)
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
