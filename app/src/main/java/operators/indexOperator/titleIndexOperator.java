package operators.indexOperator;

import buffer.BufferManager;
import operators.Operator;
import Bplus.*;
import Row.Row;
import Page.Page;

import java.util.Iterator;
import java.io.IOException;

// Represents the title index operator that pulls data from the title_index dataset
//NOTE: I HAVENT USED THIS OPERATOR
public class titleIndexOperator implements Operator {
    private BufferManager bufferManager;
    private Iterator<Rid> ridIterator = null;
    private BplusTreeImplem<String> titleIndex = null;
    private String fileName;

    public void open(BufferManager bufferManager) {
        return;
    }

    // Initializes the titleIndexOperator to pull data from the title_index dataset
    public void open(BufferManager bufferManager, String startRange, String endRange, boolean useIndex) {
        this.fileName = "title_index.bin";
        this.bufferManager = bufferManager;
        try {
            this.titleIndex = new BplusTreeImplem<>("title_index.bin", bufferManager);
            this.ridIterator = titleIndex.rangeSearch(startRange, endRange); // Get the iterator for the range of titles
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize BplusTreeImplem", e);
        }
    }

    // Retrieves the next row of data from the title_index
    public Row next() {
        if (titleIndex == null || this.ridIterator == null || !this.ridIterator.hasNext()) {
            return null;
        }
        // Returns the next row from the title_index as long as there is data
        if (this.ridIterator.hasNext()) {
            Rid rid = this.ridIterator.next();
            Page currentPage = bufferManager.getPage(rid.getPageId(), fileName);
            Row row = currentPage.getRow(rid.getSlotId());
            bufferManager.unpinPage(currentPage.getPid(), fileName);
            return row;
        }
        return null;
    }

    // Sets things to null
    public void close() {
        if (titleIndex != null) {
            titleIndex = null;
        }
        if (ridIterator != null) {
            ridIterator = null;
        }
    }

}
