package operators.selectionOperator;

import operators.Operator;
import buffer.BufferManager;
import operators.tableOperator.MovieOperator;
import Row.Row;

public class MovieSelectionOperator implements Operator {
    private MovieOperator movieOperator;
    private String startRange;
    private String endRange;

    public void open(BufferManager bufferManager, String startRange, String endRange) {
        this.startRange = startRange;
        this.endRange = endRange;

        movieOperator = new MovieOperator();
        movieOperator.open(bufferManager);
    }

    public void open(BufferManager bufferManager) {
        return;
    }

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

    public void close() {
        if (movieOperator != null) {
            movieOperator.close();
            movieOperator = null;
        }
    }

}
