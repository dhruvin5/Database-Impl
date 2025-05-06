package operators.joinOperator;

import buffer.BufferManager;
import operators.Operator;
import operators.selectionOperator.MovieSelectionOperator;
import operators.projectionOperator.WorkProjectionOperator;
import operators.indexOperator.titleIndexOperator;

import Row.Row;
import Row.joinRow1;
import Page.Page;
import Bplus.Rid;

import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Map;

// Represents the Block Nested Loop Join between Movie and Work tables
public class BNLOperator1 implements Operator {
    protected String fileName;
    protected BufferManager bufferManager;
    protected Operator outer;
    protected Operator inner;

    protected int[] blockIds; // Stores the ids of the temporary blocks
    protected boolean firstRun;
    protected Map<String, ArrayList<Rid>> ridMap; // Maps the key to the list of RIDs for faster lookups
    protected Iterator<Rid> ridIterator = null;
    protected Row currInnerRow;

    public void open(BufferManager bufferManager) {
        return;
    }

    // Initializes the BNL operator
    public void open(BufferManager bufferManager, String startRange, String endRange, boolean useIndex) {
        // Initializes the outer operator base on the useIndex flag
        Operator temp = useIndex ? new titleIndexOperator() : new MovieSelectionOperator();
        initialize(bufferManager, "-1", temp, new WorkProjectionOperator());
        this.outer.open(bufferManager, startRange, endRange, useIndex);
        this.inner.open(bufferManager);
    }

    // Returns the rows joined on movieId on movie and work tables
    public Row next() {
        if (outer == null || inner == null) {
            return null;
        }

        // If the ridIterator is not null and has next, return the joined row
        // Theoretically, since movieId should be unquie in the movie table, so
        // we should not have duplicates keys in the outer, thus it SHOULD NOT USE this
        // But i didn't test this
        if (ridIterator != null && ridIterator.hasNext()) {
            return getJoinedRow(ridIterator.next());
        }

        ridIterator = null;
        currInnerRow = null;

        // Continously samples rows from the inner operator until it finds a match in
        // the outer operator
        while (true) {
            currInnerRow = inner.next(); // gets the first row from the inner operator
            if (currInnerRow == null || firstRun) {
                firstRun = false;
                if (!fillBlocks("movieId")) // fill blocks if the inner operator is null or first run
                    return null; // if there is no more data to fill the blocks return null
                if (currInnerRow == null)
                    currInnerRow = inner.next(); // resets the inner operator to the first row
            }

            // Checks if the current inner row has a match in the outer operator's keys
            ArrayList<Rid> ridList = ridMap.get(new String(currInnerRow.movieId));
            if (ridList != null) {
                // if there is a match, get the list of RIDs and returns the first join
                ridIterator = ridList.iterator();
                return getJoinedRow(ridIterator.next());
            }
        }
    }

    // unpins blocks and closes the inner and outer operators
    public void close() {
        // unpins the blocks
        if (blockIds != null) {
            for (int blockId : blockIds) {
                bufferManager.unpinPage(blockId, fileName);
            }
            blockIds = null;
        }
        // closes the outer and inner operators
        if (outer != null) {
            outer.close();
            outer = null;
        }
        if (inner != null) {
            inner.close();
            inner = null;
        }
    }

    // Initializes the BNL operator with the buffer manager, file name, outer and
    // inner operators
    protected void initialize(BufferManager bufferManager, String fileName, Operator outer, Operator inner) {
        this.bufferManager = bufferManager;
        this.fileName = fileName;
        this.outer = outer;
        this.inner = inner;

        int blockSize = (bufferManager.getBufferCapacity() - 4) / 2; // HARD CODED BLOCK SIZE
        // Create the blocks and store the pageIds and make sure it is not dirty
        this.blockIds = new int[blockSize];
        for (int i = 0; i < blockSize; i++) {
            blockIds[i] = bufferManager.createPage(fileName).getPid();
            bufferManager.markUndirty(blockIds[i], fileName);
        }
        // Need to set fill the blocks in the first run
        this.firstRun = true;
        this.ridMap = new HashMap<>();
    }

    // Fills the blocks with data from the outer operator and maps the keys to their
    // corresponding RIDs
    protected boolean fillBlocks(String keyField) {
        boolean hasData = false; // Make sure we have data to fill the blocks
        ridMap.clear(); // refreshes the map for the new data
        resetBlocks(); // clears the blocks before filling them

        // fills blocks from the outer operator and maps the keys to their corresponding
        // RIDs in the blocks
        int currIndex = 0;
        Page currentPage = bufferManager.getPage(blockIds[currIndex], fileName);
        bufferManager.unpinPage(currentPage.getPid(), fileName);
        while (currIndex < blockIds.length) {
            if (currentPage.isFull()) {
                currIndex++; // move to the next block
                if (currIndex >= blockIds.length)
                    break;
                currentPage = bufferManager.getPage(blockIds[currIndex], fileName);
                bufferManager.unpinPage(currentPage.getPid(), fileName);
            }

            Row row = outer.next(); // get the next row from the outer operator
            if (row == null)
                break;

            hasData = true;
            // insert the row into the current page and adds it to the map
            insertRowIntoPage(currentPage, row, keyField);
        }
        return hasData;
    }

    // Resets the blocks by setting the row count to 0
    private void resetBlocks() {
        for (int blockId : blockIds) {
            Page page = bufferManager.getPage(blockId, fileName);
            page.setRowCount(0);
            bufferManager.unpinPage(blockId, fileName);
        }
    }

    // Inserts a row into the current page and maps the key to its corresponding RID
    private void insertRowIntoPage(Page currentPage, Row row, String keyField) {
        Rid rid = new Rid(currentPage.getPid(), currentPage.getRowCount());
        String key = keyField.equals("movieId") ? new String(row.movieId) : new String(row.personId);
        ArrayList<Rid> ridList = ridMap.getOrDefault(key, new ArrayList<>());
        ridList.add(rid);
        ridMap.put(key, ridList);
        currentPage.insertRow(row);
    }

    // Returns the joined row based on the RID if it finds a valid match
    protected Row getJoinedRow(Rid rid) {
        Page currentPage = bufferManager.getPage(rid.getPageId(), fileName);
        Row outerRow = currentPage.getRow(rid.getSlotId());
        bufferManager.unpinPage(currentPage.getPid(), fileName);
        return new joinRow1(outerRow.movieId, outerRow.title, currInnerRow.personId);
    }
}
