package operators;

import Row.Row;
import buffer.BufferManager;

public interface Operator {

    /**
     * Opens the operator, preparing it for data retrieval.
     */
    public void open(BufferManager bufferManager, String startRange, String endRange, boolean useIndex);

    public void open(BufferManager bufferManager);

    /**
     * Retrieves the next row of data from the operator.
     * 
     * @return The next Row object, or null if there are no more rows.
     */
    public Row next();

    /**
     * Closes the operator, releasing any resources it holds.
     */
    public void close();

}
