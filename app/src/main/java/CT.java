import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

public class CT {
    public static void main(String[] args) {
        String file1 = "/Users/simranmalik/645-Lab-32966720340112693401883534060222/app/sim_output_files/Ba_Bm_output.csv";
        String file2 = "/Users/simranmalik/645-Lab-32966720340112693401883534060222/app/output_sim19.csv";

        Set<String> set1 = readNormalizedCSV(file1, true);  // skip header
        Set<String> set2 = readNormalizedCSV(file2, false); // no header

        if (set1.equals(set2)) {
            System.out.println("Files match!");
        } else {
            System.out.println("Files do NOT match.");

            Set<String> onlyInFile1 = new HashSet<>(set1);
            onlyInFile1.removeAll(set2);
            System.out.println("\nLines in file1 but not in file2:");
            onlyInFile1.forEach(s -> System.out.println("[" + s + "]"));

            Set<String> onlyInFile2 = new HashSet<>(set2);
            onlyInFile2.removeAll(set1);
            System.out.println("\nLines in file2 but not in file1:");
            onlyInFile2.forEach(s -> System.out.println("[" + s + "]"));
        }
    }

    private static Set<String> readNormalizedCSV(String filePath, boolean skipHeader) {
        Set<String> result = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            if (skipHeader) br.readLine(); // skip header

            while ((line = br.readLine()) != null) {
                line = line.trim().replace("\uFFFD", ""); // Remove corrupted char

                int lastComma = line.lastIndexOf(',');
                if (lastComma == -1) continue; // skip malformed

                String title = line.substring(0, lastComma).trim();
                String name = line.substring(lastComma + 1).trim();

                // Remove outer quotes and normalize whitespace
                title = title.replaceAll("^\"|\"$", "").replaceAll("\\s+", " ");
                name = name.replaceAll("^\"|\"$", "").replaceAll("\\s+", " ");

                String normalized = (title + "," + name).replaceAll(" ,", ",").trim();
                result.add(normalized);
            }
        } catch (Exception e) {
            System.err.println("Error reading file: " + filePath);
            e.printStackTrace();
        }
        return result;
    }
}
