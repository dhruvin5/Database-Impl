package Page;

import java.nio.ByteBuffer;
import java.util.Arrays;

import Row.*;
import configs.Config;

public class LeafIndexPageImpl implements Page {

    private static final int ROW_COUNT_SIZE = 4;

    private static final int BOOL_SIZE = 1;

    private static final int NEXT_LEAF_POINTER = 4;

    // fixed row size
    private final int ROW_SIZE;

    // this is calculated by pagesize - 4 / rowSize
    private final int MAX_ROW_COUNT;

    // the pageId should be final. Once assigned cannot be changed
    private final int currentPageId;

    // the actual data
    private final byte[] rows;

    private int keySize;

    private int pidSize;

    private int slotIdSize;

    public LeafIndexPageImpl(int currentPageId, byte boolValue, int keySize, int pidSize, int slotIdSize) {
        this.currentPageId = currentPageId;

        this.rows = new byte[Config.PAGE_SIZE];
        setRowCount(0);
        setBoolValue(boolValue);

        this.keySize = keySize;
        this.pidSize = pidSize;
        this.slotIdSize = slotIdSize;
        this.ROW_SIZE = keySize + pidSize + slotIdSize;
        this.MAX_ROW_COUNT = (Config.PAGE_SIZE - ROW_COUNT_SIZE - BOOL_SIZE - NEXT_LEAF_POINTER) / ROW_SIZE;
    }

    // if loading an existing page in Buffer
    public LeafIndexPageImpl(int currentPageId, byte[] existingRows, int keySize, int pidSize, int slotIdSize) {
        if (existingRows.length != Config.PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be 4KB!");
        }
        this.currentPageId = currentPageId;
        this.rows = existingRows;

        this.keySize = keySize;
        this.pidSize = pidSize;
        this.slotIdSize = slotIdSize;
        this.ROW_SIZE = keySize + pidSize + slotIdSize;
        this.MAX_ROW_COUNT = (Config.PAGE_SIZE - ROW_COUNT_SIZE - BOOL_SIZE - NEXT_LEAF_POINTER) / ROW_SIZE;
    }

    // get the row by rowId
    public Row getRow(int rowId) {
        int rowCount = getRowCount();

        // if less than 0 or greater than rowCount. Invalid rowId
        if (rowId < 0 || rowId >= rowCount) {
            return null;
        }

        // go to the offset
        int offset = BOOL_SIZE + ROW_COUNT_SIZE + NEXT_LEAF_POINTER + rowId * ROW_SIZE;
        byte[] column1 = Arrays.copyOfRange(rows, offset, (offset += this.keySize));
        byte[] column2 = Arrays.copyOfRange(rows, offset, (offset += this.pidSize));
        byte[] column3 = Arrays.copyOfRange(rows, offset, (offset += this.slotIdSize));

        // create a row with the data
        return new leafRow(column1, column2, column3);
    }

    @Override
    public int insertRow(Row row) {
        // rigorous check on the data to avoid null entries
        if (row == null || row.key == null || row.pid == null || row.slotid == null
                || row.title != null || row.movieId != null) {
            return -1;
        }

        if (isFull()) {
            return -1;
        }

        int rowCount = getRowCount();
        int offset = BOOL_SIZE + ROW_COUNT_SIZE + NEXT_LEAF_POINTER + rowCount * ROW_SIZE;

        // copy the data to the page
        copyAndPaste(row.key, this.keySize, offset);
        copyAndPaste(row.pid, this.pidSize, (offset += this.keySize));
        copyAndPaste(row.slotid, this.slotIdSize, (offset += this.pidSize));

        setRowCount(rowCount + 1);
        return rowCount;
    }

    // copy the data to the page
    private void copyAndPaste(byte[] data, int size, int offset) {
        byte[] fixed_copy = new byte[size];
        System.arraycopy(data, 0, fixed_copy, 0, Math.min(data.length, size));

        for (int i = 0; i < size; i++) {
            this.rows[offset + i] = fixed_copy[i];
        }
    }

    // check if the page is full
    public boolean isFull() {
        int rowCount = getRowCount();
        if (rowCount >= MAX_ROW_COUNT) {
            return true;
        }
        return false;
    }

    @Override
    public int getPid() {
        return this.currentPageId;
    }

    // get the data of the rows
    @Override
    public byte[] getRows() {
        return this.rows;
    }

    // get the rowcount by accessing the first 4 bytes

    private int getRowCount() {
        return ByteBuffer.wrap(rows, 1, ROW_COUNT_SIZE).getInt();
    }

    // set the row count in first 4 bytes
    private void setRowCount(int count) {
        ByteBuffer.wrap(rows, 1, ROW_COUNT_SIZE).putInt(count);
    }

    // Set the page isLeaf status in the first byte of the page
    private void setBoolValue(byte boolValue) {
        this.rows[0] = boolValue;
    }

    // gets the pointer to the next leaf page
    public void setNextPointer(int nextPageId) {
        ByteBuffer.wrap(rows, 5, 4).putInt(nextPageId);
    }

    // gets the pointer to the next leaf page
    public int getNextPointer() {
        return ByteBuffer.wrap(rows, 5, 4).getInt();
    }
}
