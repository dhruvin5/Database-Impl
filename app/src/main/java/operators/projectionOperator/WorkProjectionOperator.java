package operators.projectionOperator;

import java.nio.Buffer;

import operators.Operator;
import buffer.BufferManager;
import operators.selectionOperator.WorkSelectionOperator;
import Row.Row;
import Row.materializedRow;
import Page.Page;

public class WorkProjectionOperator implements Operator {
    private String fileName;
    private BufferManager bufferManager;
    private Operator workSelectionOperator;
    private Page currentPage;
    private boolean firstCall;
    private int currentRowIndex;
    private int currentPageId;

    public void open(BufferManager bufferManager, String startRange, String endRange, boolean useIndex) {
        open(bufferManager);
    }

    public void open(BufferManager bufferManager) {
        this.fileName = "materialized.bin";
        this.bufferManager = bufferManager;
        this.currentPage = bufferManager.createPage(fileName);
        this.currentPageId = currentPage.getPid();
        bufferManager.markDirty(currentPageId, fileName);

        this.currentRowIndex = 0;

        this.workSelectionOperator = new WorkSelectionOperator();
        this.workSelectionOperator.open(bufferManager);
        this.firstCall = true;
    }

    public Row next() {
        if (workSelectionOperator == null) {
            return null;
        }

        if (this.firstCall)
            materialize();

        // get the next row from the current
        if (currentRowIndex < currentPage.getRowCount()) {
            Row row = currentPage.getRow(currentRowIndex);
            currentRowIndex++;
            return row;
        }

        // go to the next page
        bufferManager.unpinPage(currentPageId, fileName);
        currentPage = bufferManager.getPage(++currentPageId, fileName);
        currentRowIndex = 0;
        if (currentPage != null) {
            Row row = currentPage.getRow(currentRowIndex);
            currentRowIndex++;
            return row;
        } else {
            // No more pages to read so we need to reset the current page and row index
            bufferManager.unpinPage(currentPageId, fileName);
            currentPageId = 0;
            currentRowIndex = 0;
            currentPage = bufferManager.getPage(currentPageId, fileName);
            return null;
        }
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
        this.bufferManager.deleteFile(fileName);
    }

    private void materialize() {
        Row row = null;
        while ((row = workSelectionOperator.next()) != null) {
            if (currentPage.isFull()) {
                bufferManager.unpinPage(currentPage.getPid(), fileName);
                currentPage = bufferManager.createPage(fileName);
            }
            currentPage.insertRow(new materializedRow(row.movieId, row.personId));
        }
        this.firstCall = false;
        this.bufferManager.unpinPage(currentPage.getPid(), fileName);
        this.currentPage = this.bufferManager.getPage(currentPageId, fileName);
    }
}
