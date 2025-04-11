package operators.selectionOperator;

import buffer.BufferManager;
import operators.Operator;
import operators.tableOperator.WorkOperator;
import Row.Row;

public class WorkSelectionOperator implements Operator {

    private WorkOperator workOperator;

    public void open(BufferManager bufferManager, String startRange, String endRange) {
        return;
    }

    public void open(BufferManager bufferManager) {
        workOperator = new WorkOperator();
        workOperator.open(bufferManager);
    }

    // Operator that returns a row if it is a director catagory
    public Row next() {
        if (workOperator == null) {
            return null;
        }
        Row row;
        while ((row = workOperator.next()) != null) {
            String workName = new String(row.category).trim();
            if (workName.equals("director")) {
                return row;
            }
        }
        return null; // No more rows in the specified range
    }

    public void close() {
        if (workOperator != null) {
            workOperator.close();
            workOperator = null;
        }
    }

}
