import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

public class CT {
    public static void main(String[] args) {
        String Postgres_output = "app/PSQL_Output/Sample_Caa_Cab_PSQL_OutputFile.csv";
        String LAB3_output = "app/LAB3_OUTPUT/Sample_Without_Index_Caa_Cab_LAB3.csv";

        Set<String> Set_1 = readnormalisedCSV(Postgres_output, true); // header skipped in postgres output
        Set<String> Set_2 = readnormalisedCSV(LAB3_output, false); // Header not present in LAB3 output

        if (Set_1.equals(Set_2)) {
            System.out.println("PERFECT MATCH!!!!      (Between Outputs from Lab3 code and Postgres)");
        } else {
            System.out.println("MISMATCH in Outputs from Lab3 code and Postgres");

            Set<String> onlyInPostgres_output = new HashSet<>(Set_1);
            // Printing mistmatched tuples in Postgres Output
            onlyInPostgres_output.removeAll(Set_2);
            System.out.println("\nTuples in Postgres_output but not in LAB3_output:");
            onlyInPostgres_output.forEach(s -> System.out.println("[" + s + "]"));

            // Printing mistmatched tuples in LAB3 Output
            Set<String> onlyInLAB3_output = new HashSet<>(Set_2);
            onlyInLAB3_output.removeAll(Set_1);
            System.out.println("\nTuples in LAB3_output but not in Postgres_output:");
            onlyInLAB3_output.forEach(s -> System.out.println("[" + s + "]"));
        }
    }

    private static Set<String> readnormalisedCSV(String filePath, boolean skipHeader) {
        Set<String> result = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            if (skipHeader)
                br.readLine(); // skipping header if skipHeader bool is false

            while ((line = br.readLine()) != null) {
                line = line.trim().replace("\uFFFD", ""); // trimming corrupted chars

                int lastComma = line.lastIndexOf(',');
                if (lastComma == -1)
                    continue; // skipping tuples with malformation

                String title = line.substring(0, lastComma).trim();
                String name = line.substring(lastComma + 1).trim();

                // Normalizing tuples
                title = title.replaceAll("^\"|\"$", "").replaceAll("\\s+", " ");
                name = name.replaceAll("^\"|\"$", "").replaceAll("\\s+", " ");

                String normalised = (title + "," + name).replaceAll(" ,", ",").trim();
                result.add(normalised);
            }
        } catch (Exception e) {
            System.err.println("Error in reading Output File: " + filePath);
            e.printStackTrace();
        }
        return result;
    }
}