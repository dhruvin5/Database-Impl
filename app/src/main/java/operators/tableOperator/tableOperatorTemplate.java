package operators.tableOperator;

import Page.Page;
import Row.Row;
import buffer.BufferManager;
import operators.Operator;

public class tableOperatorTemplate implements Operator {
    public String fileName = null;
    public BufferManager bufferManager = null;
    public Page currentPageObject = null;
    public int currentPageId = 0;
    public int currentRow = 0;

    public void open(BufferManager bufferManager) {
        this.fileName = "";
        this.currentPageId = 0;
        this.currentRow = 0;
        this.bufferManager = bufferManager;
        this.currentPageObject = fileName != null ? bufferManager.getPage(currentPageId, fileName) : null;
    }

    public void open(BufferManager bufferManager, String startRange, String endRange) {
        this.fileName = "";
        this.bufferManager = bufferManager;
        this.currentPageObject = fileName != null ? bufferManager.getPage(currentPageId, fileName) : null;
    }

    public Row next() {
        if (currentPageObject == null) {
            return null;
        }

        if (currentRow >= currentPageObject.getRowCount()) {
            if (!moveToNextPage()) {
                return null;
            }
        }

        Row row = currentPageObject.getRow(currentRow);
        currentRow++;
        return row;
    }

    public void close() {
        if (currentPageObject != null) {
            bufferManager.unpinPage(currentPageId, fileName);
            currentPageObject = null;
        }
    }

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
