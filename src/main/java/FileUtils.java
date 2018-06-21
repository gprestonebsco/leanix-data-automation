import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

class FileUtils {

  private String fileName;

  FileUtils(String fileName) {
    this.fileName = fileName;
  }

  String readlines() {
    String line;
    StringBuilder contents = new StringBuilder();

    try {
      FileReader fileReader =  new FileReader(this.fileName);
      BufferedReader bufferedReader =
              new BufferedReader(fileReader);

      while((line = bufferedReader.readLine()) != null) {
        contents.append(line);
      }

      bufferedReader.close();
    }
    catch(FileNotFoundException ex) {
      System.out.println(
              "Unable to open file '" +
                      fileName + "'");
    }
    catch(IOException ex) {
      System.out.println(
              "Error reading file '"
                      + fileName + "'");
    }

    return contents.toString();
  }
}
