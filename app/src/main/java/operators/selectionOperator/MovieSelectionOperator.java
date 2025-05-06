package operators.selectionOperator;

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
    private Collator collator; // using collator for comparing strings
    // Initialising collator constructor for comparing strings

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
                System.out.println("moviename:" + movieName);
                return row;
            }
        }
        return null;
    }

    // Closes the operator and releases any resources it holds
    public void close() {
        if (movieOperator != null) {
            movieOperator.close();
            movieOperator = null;
        }
    }

}