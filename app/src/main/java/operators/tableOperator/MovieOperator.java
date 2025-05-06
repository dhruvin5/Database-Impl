package operators.tableOperator;

import buffer.BufferManager;

// Pull data from the movies dataset
public class MovieOperator extends tableOperatorTemplate {
    public void open(BufferManager bufferManager) {
        this.fileName = "movies.bin";
        this.currentPageId = 0;
        this.currentRow = 0;
        this.bufferManager = bufferManager;
        this.currentPageObject = fileName != null ? bufferManager.getPage(currentPageId, fileName) : null;
    }
}
