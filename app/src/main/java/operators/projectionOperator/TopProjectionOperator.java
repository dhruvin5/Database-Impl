package operators.projectionOperator;

import java.nio.Buffer;

import operators.Operator;
import operators.joinOperator.BNLOperator2;
import buffer.BufferManager;
import Row.Row;
import Row.outputRow;

// Represents the top projection operator in a database query execution plan
public class TopProjectionOperator implements Operator {
    private BufferManager bufferManager;
    private Operator operator;

    public void open(BufferManager bufferManager) {
        return;
    }

    // Initializes the top projection operator with the given buffer manager and
    // range
    public void open(BufferManager bufferManager, String startRange, String endRange, boolean useIndex) {
        this.bufferManager = bufferManager;
        this.operator = new BNLOperator2(); // Pulls data from the BNL Operator between people and movie-work table
        this.operator.open(bufferManager, startRange, endRange, useIndex);
    }

    // Retrieves the next row of data from the operator, projecting only the title
    // and name fields
    public Row next() {
        if (operator == null) {
            return null;
        }
        Row row = operator.next();
        if (row == null) {
            return null;
        }

        return new outputRow(row.title, row.name);
    }

    // Closes the operator and releases any resources it holds (recursively closes
    // all the operators)
    public void close() {
        if (operator != null) {
            operator.close();
            operator = null;
        }
    }

}
