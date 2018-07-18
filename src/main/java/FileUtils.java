import java.io.*;
import java.util.ArrayList;
import java.util.List;

class FileUtils {

  private String fileName;

  FileUtils(String fileName) {
    this.fileName = fileName;
  }

  String read() {
    String line;
    StringBuilder contents = new StringBuilder();
    InputStream in = getClass().getResourceAsStream(this.fileName);

    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));

      while((line = reader.readLine()) != null) {
        contents.append(line);
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

  List<String> readlines() {
    String line;
    List<String> contents = new ArrayList<String>();
    InputStream in = getClass().getResourceAsStream(this.fileName);

    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));

      while((line = reader.readLine()) != null) {
        contents.add(line);
      }

      reader.close();
    }
    catch(FileNotFoundException ex) {
      System.out.println("Unable to open file '" + fileName + "'");
    }
    catch(IOException ex) {
      System.out.println("Error reading file '" + fileName + "'");
    }

    return contents;
  }
}
