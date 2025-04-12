package operators.tableOperator;

import buffer.BufferManager;

public class MovieOperator extends tableOperatorTemplate {

    public void open(BufferManager bufferManager) {
        this.fileName = "movies.bin";
        this.currentPageId = 0;
        this.currentRow = 0;
        this.bufferManager = bufferManager;
        this.currentPageObject = fileName != null ? bufferManager.getPage(currentPageId, fileName) : null;
    }
}
