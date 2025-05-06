package operators.tableOperator;

import Page.Page;
import Row.Row;
import buffer.BufferManager;
import operators.Operator;

//Since table operators are pretty much the same, we can create a template for them that they can inherit from
public class tableOperatorTemplate implements Operator {
    public String fileName = null;
    public BufferManager bufferManager = null;
    public Page currentPageObject = null;
    public int currentPageId = 0;
    public int currentRow = 0;

    // The table operator should override this method to open the operator with the
    // specific file name
    public void open(BufferManager bufferManager) {
        this.fileName = "";
        this.currentPageId = 0;
        this.currentRow = 0;
        this.bufferManager = bufferManager;
        this.currentPageObject = fileName != null ? bufferManager.getPage(currentPageId, fileName) : null;
    }

    public void open(BufferManager bufferManager, String startRange, String endRange, boolean useIndex) {
        open(bufferManager);
    }

    public Row next() {
        if (currentPageObject == null) {
            return null;
        }

        // gets the next row from the current page otherwise moves to the next page
        if (currentRow >= currentPageObject.getRowCount()) {
            if (!moveToNextPage()) {
                return null;
            }
        }

        Row row = currentPageObject.getRow(currentRow);
        currentRow++;
        return row;
    }

    // Closes the operator and unpins the page
    public void close() {
        if (currentPageObject != null) {
            bufferManager.unpinPage(currentPageId, fileName);
            currentPageObject = null;
        }
    }

    // Moves to the next page and unpins the current page
    private boolean moveToNextPage() {
        currentPageId++;
        currentRow = 0;
        this.bufferManager.unpinPage(currentPageId - 1, fileName);
        this.currentPageObject = bufferManager.getPage(currentPageId, fileName);
        if (currentPageObject == null) {
            return false;
        }
        return true;
    }
}
