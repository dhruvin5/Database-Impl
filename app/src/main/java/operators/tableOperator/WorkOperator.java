package operators.tableOperator;

import buffer.BufferManager;

public class WorkOperator extends tableOperatorTemplate {

    public void open(BufferManager bufferManager) {
        this.fileName = "work.bin";
        this.bufferManager = bufferManager;
        this.currentPageObject = fileName != null ? bufferManager.getPage(currentPageId, fileName) : null;
    }
}
