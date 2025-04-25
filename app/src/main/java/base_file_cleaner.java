import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class base_file_cleaner {

    private static class ColumnSpec {
        int index;
        int length;
        String name;

        ColumnSpec(int index, int length, String name) {
            this.index = index;
            this.length = length;
            this.name = name;
        }
    }

    private static void processTSV(String inputPath, String outputPath, List<ColumnSpec> specs) throws IOException {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(inputPath), StandardCharsets.UTF_8
            ));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(outputPath), StandardCharsets.UTF_8
            ))
        ) {
            // Write headers
            for (int i = 0; i < specs.size(); i++) {
                writer.write(specs.get(i).name);
                if (i < specs.size() - 1) writer.write("\t");
            }
            writer.newLine();

            // Skip original header
            reader.readLine();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\t", -1);
                StringBuilder sb = new StringBuilder();

                for (int i = 0; i < specs.size(); i++) {
                    ColumnSpec spec = specs.get(i);
                    String raw = spec.index < tokens.length ? tokens[spec.index] : "";
                    // Remove newlines and control characters, collapse whitespace
                    String value = raw.replaceAll("[\\r\\n]+", " ")
                                       .replaceAll("\\s+", " ")
                                       .replaceAll("[\\p{C}]", "").trim();

                    // Truncate and pad bytes
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int byteCount = 0;
                    for (char c : value.toCharArray()) {
                        byte[] cBytes = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                        if (byteCount + cBytes.length > spec.length) break;
                        baos.write(cBytes, 0, cBytes.length);
                        byteCount += cBytes.length;
                    }
                    // Pad with ASCII spaces
                    while (byteCount < spec.length) {
                        baos.write(' ');
                        byteCount++;
                    }
                    // Preserve bytes exactly
                    String finalValue = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                    sb.append(finalValue);

                    if (i < specs.size() - 1) sb.append("\t");
                }

                String finalLine = sb.toString().replaceAll("[\\r\\n]+", " ").trim();
                writer.write(finalLine);
                writer.write("\n");

            }
        }
    }

    public static void main(String[] args) {
        String basePath = "/Users/simranmalik/Desktop/";
        try {
            processTSV(
                basePath + "title.basics.tsv",
                basePath + "cleaned_movies.tsv",
                Arrays.asList(
                    new ColumnSpec(0, 9, "movieId"),
                    new ColumnSpec(2, 30, "title")
                )
            );
            processTSV(
                basePath + "title.principals.tsv",
                basePath + "cleaned_workedon.tsv",
                Arrays.asList(
                    new ColumnSpec(0, 9, "movieId"),
                    new ColumnSpec(2, 10, "personId"),
                    new ColumnSpec(3, 20, "category")
                )
            );
            processTSV(
                basePath + "name.basics.tsv",
                basePath + "cleaned_people.tsv",
                Arrays.asList(
                    new ColumnSpec(0, 10, "personId"),
                    new ColumnSpec(1, 105, "name")
                )
            );
            
            System.out.println("Cleaned files created.");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
