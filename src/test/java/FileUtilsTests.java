import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class FileUtilsTests {

  @Test
  // Test for FileUtils.read()
  void testRead() {
    FileUtils f = new FileUtils("fileutilstest.txt");
    String expected = "This is a file reading test file.\n" +
            "Hopefully it works correctly.\n" +
            "Goodbye.\n";
    // read() must automatically insert a newline, so even though this technically isn't the exact file,
    // the difference does not really have consequences.

    assertEquals(f.read(), expected);
  }

  @Test
  // Test for FileUtils.readlines()
  void testReadlines() {
    FileUtils f = new FileUtils("fileutilstest.txt");
    List<String> expected = new ArrayList<String>();
    expected.add("This is a file reading test file.");
    expected.add("Hopefully it works correctly.");
    expected.add("Goodbye.");

    assertEquals(f.readlines(), expected);
  }
}
