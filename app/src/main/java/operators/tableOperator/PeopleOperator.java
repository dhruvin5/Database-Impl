package operators.tableOperator;

import buffer.BufferManager;

public class PeopleOperator extends tableOperatorTemplate {
    public void open(BufferManager bufferManager) {
        this.fileName = "people.bin";
        this.bufferManager = bufferManager;
        this.currentPageObject = fileName != null ? bufferManager.getPage(currentPageId, fileName) : null;
    }
}
