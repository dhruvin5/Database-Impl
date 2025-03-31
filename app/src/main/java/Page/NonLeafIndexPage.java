package Page;

import java.nio.ByteBuffer;
import java.util.Arrays;

import Row.nonLeafRow;
import Row.Row;
import configs.Config;

public class NonLeafIndexPage implements Page {

    private static final int ROW_COUNT_SIZE = 4;

    private static final int BOOL_SIZE = 1;

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

    public NonLeafIndexPage(int currentPageId, byte boolValue, int keySize, int pidSize) {
        this.currentPageId = currentPageId;
        this.rows = new byte[Config.PAGE_SIZE];
        setRowCount(0);
        setBoolValue(boolValue);

        this.keySize = keySize;
        this.pidSize = pidSize;
        this.ROW_SIZE = keySize + pidSize;
        this.MAX_ROW_COUNT = (Config.PAGE_SIZE - ROW_COUNT_SIZE - BOOL_SIZE) / ROW_SIZE;
    }

    // if loading an existing page in Buffer
    public NonLeafIndexPage(int currentPageId, byte[] existingRows, int keySize, int pidSize) {
        if (existingRows.length != Config.PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be 4KB!");
        }
        this.currentPageId = currentPageId;
        this.rows = existingRows;

        this.keySize = keySize;
        this.pidSize = pidSize;
        this.ROW_SIZE = keySize + pidSize;
        this.MAX_ROW_COUNT = (Config.PAGE_SIZE - ROW_COUNT_SIZE - BOOL_SIZE) / ROW_SIZE;
    }

    public Row getRow(int rowId) {
        int rowCount = getRowCount();

        // if less than 0 or greater than rowCount. Invalid rowId
        if (rowId < 0 || rowId >= rowCount) {
            return null;
        }

        // go to the offset
        int offset = BOOL_SIZE + ROW_COUNT_SIZE + rowId * ROW_SIZE;

        // get the data from the page
        byte[] key = Arrays.copyOfRange(rows, offset, (offset += this.keySize));
        byte[] column = Arrays.copyOfRange(rows, offset, (offset += this.pidSize));

        // create a row with the data
        return new nonLeafRow(key, column);
    }

    public int insertRow(Row row) {
        // rigorous check on the data to avoid null entries

        if (row == null  || row.pid == null
                || row.slotid != null || row.title != null) {
            return -1;
        }

        // check if the page is full
        if (isFull()) {
            return -1;
        }

        // get the row count
        int rowCount = getRowCount();
        int offset = BOOL_SIZE + ROW_COUNT_SIZE + rowCount * ROW_SIZE;

        // copy the data to the page
        copyAndPaste(row.key, this.keySize, offset);


        offset += this.keySize;
        copyAndPaste(row.pid, this.pidSize, offset);





        setRowCount(rowCount + 1);
        return rowCount;
    }

    private void copyAndPaste(byte[] data, int size, int offset) {
        byte[] fixed_copy = new byte[size];

        if (data != null) {
            System.arraycopy(data, 0, fixed_copy, 0, Math.min(data.length, size));
        }
        for (int i = 0; i < size; i++) {
            this.rows[offset + i] = fixed_copy[i];
        }

        // Copy the fixed_copy into the rows array starting at the given offset.

    }

    public boolean isFull() {
        int rowCount = getRowCount();
        if (rowCount >= MAX_ROW_COUNT) {
            return true;
        }
        return false;
    }

    public int getPid() {
        return this.currentPageId;
    }

    // get the data of the rows

    public byte[] getRows() {

        return this.rows;
    }

    // get the rowcount by accessing the first 4 bytes

    public int getRowCount() {
        return ByteBuffer.wrap(rows, 1, ROW_COUNT_SIZE).getInt();
    }

    // set the row count in first 4 bytes
    public void setRowCount(int count) {
        ByteBuffer.wrap(rows, 1, ROW_COUNT_SIZE).putInt(count);
    }

    public boolean getBoolValue() {
        return this.rows[0] != 0;
    }

    private void setBoolValue(byte boolValue) {
        this.rows[0] = boolValue;
    }

    public void setNextPointer(int nextPointer) {
        return;
    }

    public int getNextPointer() {
        return -1;
    }

}
