package Page;

import java.nio.ByteBuffer;
import java.util.Arrays;

import Row.Row;
import configs.Config;

public class IndexPageImpl implements Page {

    private static final int ROW_COUNT_SIZE = 4;

    // fixed row size
    private final int ROW_SIZE;

    // this is calculated by pagesize - 4 / rowSize
    private final int MAX_ROW_COUNT;

    // the pageId should be final. Once assigned cannot be changed
    private final int pageId;

    // the actual data
    private final byte[] rows;

    private int boolValueSize = 0;

    private int keySize = 0;

    private int pidSize = 0;

    private int slotIdSize = 0;

    public IndexPageImpl(int pageId, int boolValueSize, int keySize, int pidSize, int slotIdSize) {
        this.pageId = pageId;
        this.rows = new byte[Config.PAGE_SIZE];
        setRowCount(0);

        this.boolValueSize = boolValueSize;
        this.keySize = keySize;
        this.pidSize = pidSize;
        this.slotIdSize = slotIdSize;
        this.ROW_SIZE = boolValueSize + keySize + pidSize + slotIdSize;
        this.MAX_ROW_COUNT = (Config.PAGE_SIZE - 4) / ROW_SIZE;
    }

    // if loading an existing page in Buffer
    public IndexPageImpl(int pageId, byte[] existingRows, int booleanValue, int key, int pid, int slotId) {
        if (existingRows.length != Config.PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be 4KB!");
        }
        this.pageId = pageId;
        this.rows = existingRows;

        this.boolValueSize = booleanValue;
        this.keySize = key;
        this.pidSize = pid;
        this.slotIdSize = slotId;
        this.ROW_SIZE = boolValueSize + keySize + pidSize + slotIdSize;
        this.MAX_ROW_COUNT = (Config.PAGE_SIZE - 4) / ROW_SIZE;
    }

    public Row getRow(int rowId) {
        int rowCount = getRowCount();

        // if less than 0 or greater than rowCount. Invalid rowId
        if (rowId < 0 || rowId >= rowCount) {
            return null;
        }

        // go to the offset
        int offset = ROW_COUNT_SIZE + rowId * ROW_SIZE;
        byte[] column1 = Arrays.copyOfRange(rows, offset, (offset += this.boolValueSize));
        byte[] column2 = Arrays.copyOfRange(rows, offset, (offset += this.keySize));
        byte[] column3 = Arrays.copyOfRange(rows, offset, (offset += this.pidSize));
        byte[] column4 = Arrays.copyOfRange(rows, offset, (offset += this.slotIdSize));

        // create a row with the data
        return new Row(column1[0], column2, column3, column4);
    }

    @Override
    public int insertRow(Row row) {
        // rigorous check on the data to avoid null entries
        if (row == null || row.key == null || row.pid == null || row.slotid == null) {
            return -1;
        }

        if (isFull()) {
            return -1;
        }

        int rowCount = getRowCount();
        int offset = ROW_COUNT_SIZE + rowCount * ROW_SIZE;

        // copy the data to the page
        copyAndPaste(new byte[] { row.booleanValue }, this.boolValueSize, offset);
        copyAndPaste(row.key, this.keySize, (offset += this.boolValueSize));
        copyAndPaste(row.pid, this.pidSize, (offset += this.keySize));
        copyAndPaste(row.slotid, this.slotIdSize, (offset += this.pidSize));

        setRowCount(rowCount + 1);
        return rowCount;
    }

    private void copyAndPaste(byte[] data, int size, int offset) {
        byte[] fixed_copy = new byte[size];
        System.arraycopy(data, 0, fixed_copy, 0, Math.min(data.length, size));

        for (int i = 0; i < size; i++) {
            this.rows[offset + i] = fixed_copy[i];
        }
    }

    public boolean isFull() {
        int rowCount = getRowCount();
        if (rowCount >= MAX_ROW_COUNT) {
            return true;
        }
        return false;
    }

    @Override
    public int getPid() {
        return this.pageId;
    }

    // get the data of the rows
    @Override
    public byte[] getRows() {
        return this.rows;
    }

    // get the rowcount by accessing the first 4 bytes

    private int getRowCount() {
        return ByteBuffer.wrap(rows, 0, ROW_COUNT_SIZE).getInt();
    }

    // set the row count in first 4 bytes
    private void setRowCount(int count) {
        ByteBuffer.wrap(rows, 0, ROW_COUNT_SIZE).putInt(count);
    }

}
