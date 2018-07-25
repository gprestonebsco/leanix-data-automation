import net.leanix.api.common.ApiClient;
import net.leanix.api.common.ApiClientBuilder;
import net.leanix.api.common.ApiException;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Reset {

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

    // Get relation information
    FileUtils relationsFile1 = new FileUtils("ids/a1.txt", false);
    FileUtils relationsFile2 = new FileUtils("ids/a2.txt", false);

    List<String> relationsStr = new ArrayList<String>(relationsFile1.readlines());
    relationsStr.addAll(relationsFile2.readlines());

    List<List<String>> relations = new ArrayList<List<String>>();
    for (String s : relationsStr) {
      relations.add(Arrays.asList(s.split(",")));
    }

    // Test API access
    Query test = new Query(apiClient, "test.graphql");
    try {
      test.execute();

      reset(apiClient, relations);
      // If reset was successful, erase the files to stay up to date
      // TODO: Come up with a better way of storing change history so it can be kept even after a reset
      erase("a1.txt");
      erase("a2.txt");
    }
    catch (ApiException e) {
      e.printStackTrace();
      System.out.println("Invalid API token.");
    }
  }

  // Remove newly created relations
  private static void reset(ApiClient apiClient, List<List<String>> relations) {
    for (List<String> ids : relations) {
      try {
        // This whole block of code is put in the try-catch so that multiple redundant try-catches don't
        // need to be created.

        // Get the revision number
        int rev = QueryUtils.getRev(apiClient, ids.get(1));
        String type = QueryUtils.getType(apiClient, ids.get(1));

        // Remove the relation
        Map<String, String> removeIds = new HashMap<String, String>();
        removeIds.put("id", ids.get(1));
        removeIds.put("type", type);
        removeIds.put("rev", Integer.toString(rev));
        removeIds.put("relid", ids.get(2));

        Query removeQuery = new Query(apiClient, "remove.graphql", removeIds);

        removeQuery.execute();
        System.out.println(">>> Relation between " + ids.get(0) + " and " + ids.get(1) + " removed.");
      }
      catch (ApiException e) {
        // Query will print a more detailed error message
        System.out.println("An error occurred.");
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
