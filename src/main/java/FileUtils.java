import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class FileUtils {

  private String fileName;
  private InputStream in;

  FileUtils(String fileName) {
    this.fileName = fileName;
    this.in = getClass().getResourceAsStream(this.fileName);
  }

  FileUtils(String fileName, boolean resource) throws FileNotFoundException {
    this.fileName = fileName;

    if (resource) {
      this.in = getClass().getResourceAsStream(this.fileName);
    }
    else {
      this.in = new FileInputStream(this.fileName);
    }
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

    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));

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
