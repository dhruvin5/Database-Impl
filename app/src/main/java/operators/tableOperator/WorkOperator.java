package operators.tableOperator;

import buffer.BufferManager;

public class WorkOperator extends tableOperatorTemplate {

    public void open(BufferManager bufferManager) {
        this.fileName = "work.bin";
        this.currentPageId = 0;
        this.currentRow = 0;
        this.bufferManager = bufferManager;
        this.currentPageObject = fileName != null ? bufferManager.getPage(currentPageId, fileName) : null;
    }

    public void open(BufferManager bufferManager, String startRange, String endRange) {
        open(bufferManager);
    }
}
