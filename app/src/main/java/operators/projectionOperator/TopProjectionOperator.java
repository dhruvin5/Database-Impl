package operators.projectionOperator;

import java.nio.Buffer;

import operators.Operator;
import operators.joinOperator.BNLOperator2;
import buffer.BufferManager;
import Row.Row;
import Row.outputRow;

public class TopProjectionOperator implements Operator {
    private BufferManager bufferManager;
    private Operator operator;

    public void open(BufferManager bufferManager) {
        return;
    }

    public void open(BufferManager bufferManager, String startRange, String endRange, boolean useIndex) {
        this.bufferManager = bufferManager;
        this.operator = new BNLOperator2();
        this.operator.open(bufferManager, startRange, endRange, useIndex);
    }

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

    public void close() {
        if (operator != null) {
            operator.close();
            operator = null;
        }
    }

}
