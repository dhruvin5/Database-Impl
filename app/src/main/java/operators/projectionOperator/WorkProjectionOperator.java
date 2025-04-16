package operators.projectionOperator;

import operators.Operator;
import buffer.BufferManager;
import operators.selectionOperator.WorkSelectionOperator;
import Row.Row;
import Row.materializedRow;
import Page.Page;

// Applies a projection operation on the work dataset, filtering by category = director
// and materializing the results into a new file
public class WorkProjectionOperator implements Operator {
    private String fileName;
    private BufferManager bufferManager;
    private Operator workSelectionOperator; // Pull data from the work dataset and filter by category = director
    private Page currentPage;
    private boolean firstCall; // Flag to check if this is the first call to next()
    private int currentRowIndex; // Index of the current row in the current page
    private int currentPageId; // ID of the current page

    public void open(BufferManager bufferManager, String startRange, String endRange, boolean useIndex) {
        open(bufferManager);
    }

    public void open(BufferManager bufferManager) {
        this.fileName = "materialized.bin"; // hard coded file name for the materialized results
        this.bufferManager = bufferManager;
        this.currentPage = bufferManager.createPage(fileName); // create a new page for the materialized results
        this.currentPageId = currentPage.getPid(); // get the ID of the current page
        bufferManager.markDirty(currentPageId, fileName); // mark the page as dirty

        this.currentRowIndex = 0; // index of the current row in the current page

        // Grab the work rows filtered by category = director
        this.workSelectionOperator = new WorkSelectionOperator();
        this.workSelectionOperator.open(bufferManager);
        this.firstCall = true; // flag to check if this is the first call to next() to materialize the results
    }

    public Row next() {
        if (workSelectionOperator == null) {
            return null;
        }

        // If this is the first call to next(), materialize the results
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
        // makes sure that everything is closed and unpinned
        if (currentPage != null) {
            bufferManager.unpinPage(currentPage.getPid(), fileName);
            currentPage = null;
        }
        // close the work selection operator
        if (workSelectionOperator != null) {
            workSelectionOperator.close();
            workSelectionOperator = null;
        }
        // delete the materialized file
        this.bufferManager.deleteFile(fileName);
    }

    // Materializes the results of the work selection operator into the current page
    private void materialize() {
        Row row = null;
        // Loop through the work selection operator and inserting movieId and personId
        // into the current page
        while ((row = workSelectionOperator.next()) != null) {
            if (currentPage.isFull()) {
                bufferManager.unpinPage(currentPage.getPid(), fileName);
                currentPage = bufferManager.createPage(fileName);
            }
            currentPage.insertRow(new materializedRow(row.movieId, row.personId));
        }
        // Mark the page as dirty and unpin it
        this.firstCall = false;
        this.bufferManager.unpinPage(currentPage.getPid(), fileName);
        this.currentPage = this.bufferManager.getPage(currentPageId, fileName);
    }
}
