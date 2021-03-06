import java.io.*;
import java.util.ArrayList;
import java.util.List;

class FileUtils {

  private String fileName;
  private InputStream in;

  FileUtils(String fileName) {
    this.fileName = fileName;
    this.in = getClass().getResourceAsStream(this.fileName);
  }

  FileUtils(String fileName, boolean resource) {
    this.fileName = fileName;

    this.in = null;
    if (resource) {
      this.in = getClass().getResourceAsStream(this.fileName);
    }
    else {
      try {
        this.in = new FileInputStream(this.fileName);
      }
      catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }
  }

  // Get the file's contents as a single String
  // NOTE: Adds a newline at the end of the returned String, whether the file being read has one or not.
  // Use String.trim() to fix this if it is an issue.
  String read() {
    String line;
    StringBuilder contents = new StringBuilder();

    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(this.in));

      while((line = reader.readLine()) != null) {
        contents.append(line);
        // Append a newline
        contents.append("\n");
      }

      reader.close();
    }
    catch(FileNotFoundException ex) {
      System.out.println("Unable to open file '" + fileName + "'");
    }
    catch(IOException ex) {
      System.out.println("Error reading file '" + fileName + "'");
    }

    return contents.toString();
  }

  // Get the file's lines as a List of Strings
  List<String> readlines() {
    String line;
    List<String> contents = new ArrayList<String>();

    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(this.in));

      while((line = reader.readLine()) != null) {
        contents.add(line);
      }

      reader.close();
      in.close();
    }
    catch(FileNotFoundException ex) {
      System.out.println("Unable to open file '" + this.fileName + "'");
    }
    catch(IOException ex) {
      System.out.println("Error reading file '" + this.fileName + "'");
    }

    return contents;
  }
}
