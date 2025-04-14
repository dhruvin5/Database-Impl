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

public class BNLOperator1 implements Operator {
    protected String fileName;
    protected BufferManager bufferManager;
    protected Operator outer;
    protected Operator inner;

    protected int[] blockIds;
    protected boolean firstRun;
    protected Map<String, ArrayList<Rid>> ridMap;
    protected Iterator<Rid> ridIterator = null;
    protected Row currInnerRow;

    public void open(BufferManager bufferManager) {
        return;
    }

    public void open(BufferManager bufferManager, String startRange, String endRange, boolean useIndex) {
        Operator temp = useIndex ? new titleIndexOperator() : new MovieSelectionOperator();
        initialize(bufferManager, "-1", temp, new WorkProjectionOperator());
        this.outer.open(bufferManager, startRange, endRange, useIndex);
        this.inner.open(bufferManager);
    }

    public Row next() {
        if (outer == null || inner == null) {
            return null;
        }

        if (ridIterator != null && ridIterator.hasNext()) {
            return getJoinedRow(ridIterator.next());
        }

        ridIterator = null;
        currInnerRow = null;

        while (true) {
            currInnerRow = inner.next();
            if (currInnerRow == null || firstRun) {
                firstRun = false;
                if (!fillBlocks("movieId"))
                    return null;
                if (currInnerRow == null)
                    currInnerRow = inner.next();
            }

            ArrayList<Rid> ridList = ridMap.get(new String(currInnerRow.movieId));
            if (ridList != null) {
                ridIterator = ridList.iterator();
                return getJoinedRow(ridIterator.next());
            }
        }
    }

    public void close() {
        if (blockIds != null) {
            for (int blockId : blockIds) {
                bufferManager.unpinPage(blockId, fileName);
            }
            blockIds = null;
        }
        if (outer != null) {
            outer.close();
            outer = null;
        }
        if (inner != null) {
            inner.close();
            inner = null;
        }
    }

    protected void initialize(BufferManager bufferManager, String fileName, Operator outer, Operator inner) {
        this.bufferManager = bufferManager;
        this.fileName = fileName;
        this.outer = outer;
        this.inner = inner;

        int blockSize = (bufferManager.getBufferCapacity() - 4) / 2;
        this.blockIds = new int[blockSize];
        for (int i = 0; i < blockSize; i++) {
            blockIds[i] = bufferManager.createPage(fileName).getPid();
            bufferManager.markUndirty(blockIds[i], fileName);
        }

        this.firstRun = true;
        this.ridMap = new HashMap<>();
    }

    protected boolean fillBlocks(String keyField) {
        boolean hasData = false;
        ridMap.clear();
        resetBlocks();

        int currIndex = 0;
        Page currentPage = bufferManager.getPage(blockIds[currIndex], fileName);
        bufferManager.unpinPage(currentPage.getPid(), fileName);
        while (currIndex < blockIds.length) {
            if (currentPage.isFull()) {
                currIndex++;
                if (currIndex >= blockIds.length)
                    break;
                currentPage = bufferManager.getPage(blockIds[currIndex], fileName);
                bufferManager.unpinPage(currentPage.getPid(), fileName);
            }

            Row row = outer.next();
            if (row == null)
                break;

            hasData = true;
            insertRowIntoPage(currentPage, row, keyField);
        }
        return hasData;
    }

    private void resetBlocks() {
        for (int blockId : blockIds) {
            Page page = bufferManager.getPage(blockId, fileName);
            page.setRowCount(0);
            bufferManager.unpinPage(blockId, fileName);
        }
    }

    private void insertRowIntoPage(Page currentPage, Row row, String keyField) {
        Rid rid = new Rid(currentPage.getPid(), currentPage.getRowCount());
        String key = keyField.equals("movieId") ? new String(row.movieId) : new String(row.personId);
        ArrayList<Rid> ridList = ridMap.getOrDefault(key, new ArrayList<>());
        ridList.add(rid);
        ridMap.put(key, ridList);
        currentPage.insertRow(row);
    }

    protected Row getJoinedRow(Rid rid) {
        Page currentPage = bufferManager.getPage(rid.getPageId(), fileName);
        Row outerRow = currentPage.getRow(rid.getSlotId());
        bufferManager.unpinPage(currentPage.getPid(), fileName);
        return new joinRow1(outerRow.movieId, outerRow.title, currInnerRow.personId);
    }
}
