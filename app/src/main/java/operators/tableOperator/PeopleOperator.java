package operators.tableOperator;

import buffer.BufferManager;

// Pull data from the people dataset
public class PeopleOperator extends tableOperatorTemplate {
    public void open(BufferManager bufferManager) {
        this.fileName = "people.bin";
        this.currentPageId = 0;
        this.currentRow = 0;
        this.bufferManager = bufferManager;
        this.currentPageObject = fileName != null ? bufferManager.getPage(currentPageId, fileName) : null;
    }
}
