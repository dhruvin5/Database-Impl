package operators.projectionOperator;

import java.nio.Buffer;

import operators.Operator;
import buffer.BufferManager;
import operators.selectionOperator.WorkSelectionOperator;
import Row.Row;
import Page.Page;

public class WorkProjectionOperator implements Operator {
    private String fileName;
    private BufferManager bufferManager;
    private Operator workSelectionOperator;
    private Page currentPage;
    private boolean firstCall;

    public void open(BufferManager bufferManager, String startRange, String endRange) {
        return;
    }

    public void open(BufferManager bufferManager) {
        this.fileName = "projection.bin";
        this.bufferManager = bufferManager;
        this.currentPage = bufferManager.createPage(fileName);

        this.workSelectionOperator = new WorkSelectionOperator();
        this.workSelectionOperator.open(bufferManager);
        this.firstCall = true;
    }

    public Row next() {
        if (workSelectionOperator == null) {
            return null;
        }

        // Perform Materialization
        if (this.firstCall) {
            materialize();
        }

        return null;

    }

    // Needs to also delete the FILE
    public void close() {
        if (currentPage != null) {
            bufferManager.unpinPage(currentPage.getPid(), fileName);
            currentPage = null;
        }
        if (workSelectionOperator != null) {
            workSelectionOperator.close();
            workSelectionOperator = null;
        }
    }

    private void materialize() {

        Row row = null;
        while ((row = workSelectionOperator.next()) != null) {
            if (currentPage.isFull()) {
                bufferManager.unpinPage(currentPage.getPid(), fileName);
                currentPage = bufferManager.createPage(fileName);
            }
            // Write the row to the current page
            currentPage.insertRow(row);
        }

        this.firstCall = false;
    }

}
