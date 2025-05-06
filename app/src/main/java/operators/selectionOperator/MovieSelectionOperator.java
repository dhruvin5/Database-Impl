package operators.selectionOperator;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.Collator;
import java.text.Normalizer;
import java.util.Locale;

import Row.Row;
import buffer.BufferManager;
import operators.Operator;
import operators.tableOperator.MovieOperator;

// Filters the data from the movies dataset based on a range of movie names
public class MovieSelectionOperator implements Operator {
    private Operator movieOperator; // Operator Pull data from the movies dataset
    private String startRange;
    private String endRange;

    private Collator collator; //using collator for comparing strings
    private int matchCount = 0; // Counter to track matched rows
    //Initialising collator constructor for comparing strings
    public MovieSelectionOperator() {

        collator = Collator.getInstance(Locale.forLanguageTag("es-ES"));
        collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION); // Normalising to Canonical Form
        collator.setStrength(Collator.IDENTICAL);

    }

    // Initalizes the MovieSelectionOperator with the given start and end range for
    // movie names
    public void open(BufferManager bufferManager, String startRange, String endRange, boolean useIndex) {
        this.startRange = startRange;
        this.endRange = endRange;

        movieOperator = new MovieOperator();
        movieOperator.open(bufferManager);
    }

    public void open(BufferManager bufferManager) {
        return;
    }

    static String stripAccents(String s) {
        // 1) decompose: "á" → "a\u0301"
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        // 2) remove all combining marks (the \p{M} class)
        return n.replaceAll("\\p{M}", "");
    }

    // Retrieves the next row of data from the movieOperator, filtering based on the
    // specified range
    public Row next() {
        if (movieOperator == null) {
            return null;
        }
        Row row;
        while ((row = movieOperator.next()) != null) {
            String movieName = new String(row.title);
            String lo = stripAccents(startRange)
                    .toLowerCase(Locale.ROOT);
            String hi = stripAccents(endRange)
                    .toLowerCase(Locale.ROOT);
            String nm = stripAccents(movieName)
                    .toLowerCase(Locale.ROOT)
                    .trim();

            if (nm.compareTo(lo) >= 0 && nm.compareTo(hi) <= 0) {
                // System.out.println("moviename:"+movieName);
                matchCount++; // Increment counter if row is in valid range of queries
                return row;
            }
        }
        return null;
    }

    // dumps matching count of rows in csv file
    public void dumpMatchStats(String outputDir) {
        String filename = outputDir + "\\" + startRange + "_" + endRange + "_analytical_match.csv";
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("start_query,end_query,matches");
            pw.printf("%s,%s,%d\n", startRange, endRange, matchCount);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Closes the operator and releases any resources it holds
    public void close() {
        if (movieOperator != null) {
            // dumps result for the matching count. For correctness testing, the next line can be commented out and is optional. But we need it for perf testing
            dumpMatchStats("C:\\Users\\HP\\Desktop\\ms\\645\\lab1\\645-Lab-32966720340112693401883534060222\\app\\shreya_perf_op");
            movieOperator.close();
            movieOperator = null;
        }
    }

    // return the count of matching rows
    public int getMatchCount() {
        return matchCount;
    }

}
