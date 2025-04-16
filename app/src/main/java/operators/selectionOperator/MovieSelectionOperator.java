package operators.selectionOperator;

import operators.Operator;
import buffer.BufferManager;
import operators.tableOperator.MovieOperator;
import Row.Row;

// Filters the data from the movies dataset based on a range of movie names
public class MovieSelectionOperator implements Operator {
    private Operator movieOperator; // Operator Pull data from the movies dataset
    private String startRange;
    private String endRange;

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

    // Retrieves the next row of data from the movieOperator, filtering based on the
    // specified range
    public Row next() {
        if (movieOperator == null) {
            return null;
        }
        Row row;
        while ((row = movieOperator.next()) != null) {
            String movieName = new String(row.title);
            if (movieName.compareTo(startRange) >= 0 && movieName.compareTo(endRange) <= 0) {
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
