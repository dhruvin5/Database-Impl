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

    private static class Column_Spec {
        int index;
        int length;
        String name;

        Column_Spec(int index, int length, String name) {
            this.index = index;
            this.length = length;
            this.name = name;
        }
    }

    private static void TsvClean(String InPath,
                                 String OutPath,
                                 List<Column_Spec> Specs,
                                 boolean Check_MovieID,
                                 boolean Check_Title) throws IOException {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(InPath), StandardCharsets.UTF_8
            ));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(OutPath), StandardCharsets.UTF_8
            ))
        ) {
            // writing headers
            for (int i = 0; i < Specs.size(); i++) {
                writer.write(Specs.get(i).name);
                if (i < Specs.size() - 1) writer.write("\t");
            }
            writer.newLine();
            reader.readLine();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\t", -1);
                boolean invalid_movieId=false;
                boolean invalid_title=false;


                if (Check_MovieID) {
                    // check if MovieId size is valid
                    String movieId = Specs.get(0).index < tokens.length
                                     ? tokens[Specs.get(0).index].trim()
                                     : "";
                    if (movieId.length() != 9) {
                        invalid_movieId=true; //flag row to skip
                    }
                    // check if title is valid
                    if (Check_Title) {
                        String title = Specs.get(1).index < tokens.length
                                       ? tokens[Specs.get(1).index]
                                       : "";
                        if (HasInvalidCharacter(title)) {
                            invalid_title=true;  // flag row to skip
                        }
                    }
                    if (invalid_movieId||invalid_title)
                    {
                        continue; // Skipping row with invalid MovieID/Title
                    }
                }

                StringBuilder sb = new StringBuilder();

                for (int i = 0; i < Specs.size(); i++) {
                    Column_Spec Spec = Specs.get(i);
                    String raw = Spec.index < tokens.length ? tokens[Spec.index] : "";
                    // Normalising newline characters, tabs, etc.
                    String value = raw.replaceAll("[\\r\\n]+", " ")
                                       .replaceAll("\\s+", " ")
                                       .replaceAll("[\\p{C}]", "")
                                       .trim();

                    // truncating and padding bytes to match given schema
                    ByteArrayOutputStream Bos = new ByteArrayOutputStream();
                    int num_bytes = 0;
                    for (char c : value.toCharArray()) {
                        byte[] cBytes = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                        if (num_bytes + cBytes.length > Spec.length) break;
                        Bos.write(cBytes, 0, cBytes.length);
                        num_bytes += cBytes.length;
                    }
                    // padding shorter values with spaces
                    while (num_bytes < Spec.length) {
                        Bos.write(' ');
                        num_bytes++;
                    }
                    // writing final value
                    String Final_value = new String(Bos.toByteArray(), StandardCharsets.UTF_8);
                    sb.append(Final_value);

                    if (i < Specs.size() - 1) sb.append("\t");
                }

                String finalLine = sb.toString().replaceAll("[\\r\\n]+", " ").trim();
                writer.write(finalLine);
                writer.write("\n");
            }
        }
    }

    //method to remove invalid chars in title
    public static boolean HasInvalidCharacter(String str) {
        if (str == null) return false;
        for (char c : str.toCharArray()) {
            if (c == ',' || c == '"' || c == '\'' || c > 127) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        String Base_Path = "/Users/simranmalik/Desktop/";
        try {
            TsvClean(
                Base_Path + "name.basics.tsv",
                Base_Path + "cleaned_people.tsv",
                Arrays.asList(
                    new Column_Spec(0, 10,  "personId"),
                    new Column_Spec(1,105,  "name")
                ),
                false, // Check_MovieID bool set to false
                false  // Check_Title bool set to false
            );
            TsvClean(
                Base_Path + "title.principals.tsv",
                Base_Path + "cleaned_workedon.tsv",
                Arrays.asList(
                    new Column_Spec(0,  9,  "movieId"),
                    new Column_Spec(2, 10, "personId"),
                    new Column_Spec(3, 20, "category")
                ),
                true,  // Check_MovieID bool set to true
                false  // Check_Title bool set to false
            );
            TsvClean(
                Base_Path + "title.basics.tsv",
                Base_Path + "cleaned_movies.tsv",
                Arrays.asList(
                    new Column_Spec(0,  9,  "movieId"),
                    new Column_Spec(2, 30, "title")
                ),
                true,  // Check_MovieID bool set to true
                true   // Check_Title bool set to true
            );
            
            

            System.out.println("Successfully Created Cleaned files!");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
