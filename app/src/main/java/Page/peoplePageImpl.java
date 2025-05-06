package Page;

import java.nio.ByteBuffer;
import java.util.Arrays;

import Row.Row;
import Row.peopleRow;
import configs.Config;

public class peoplePageImpl implements Page {
    // has the row count
    private static final int ROW_COUNT_SIZE = 4;

    // fixed row size
    private final int ROW_SIZE;

    // this is calculated by pagesize - 4 / rowSize
    private final int MAX_ROW_COUNT;

    // the pageId should be final. Once assigned cannot be changed
    private final int pageId;

    // the actual data
    private final byte[] rows;

    private final int personIdSize = 10;

    private final int nameSize = 105;

    public peoplePageImpl(int pageId) {
        this.pageId = pageId;
        this.rows = new byte[Config.PAGE_SIZE];
        setRowCount(0);

        this.ROW_SIZE = personIdSize + nameSize;
        this.MAX_ROW_COUNT = (Config.PAGE_SIZE - ROW_COUNT_SIZE) / ROW_SIZE;
    }

    // if loading an existing page in Buffer
    public peoplePageImpl(int pageId, byte[] existingRows) {
        if (existingRows.length != Config.PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be 4KB!");
        }
        this.pageId = pageId;
        this.rows = existingRows;
        this.ROW_SIZE = personIdSize + nameSize;
        this.MAX_ROW_COUNT = (Config.PAGE_SIZE - ROW_COUNT_SIZE) / ROW_SIZE;
    }

    // gets the row using the rowId
    @Override
    public Row getRow(int rowId) {
        int rowCount = getRowCount();

        // if less than 0 or greater than rowCount. Invalid rowId
        if (rowId < 0 || rowId >= rowCount) {
            return null;
        }

        // go to the offset
        int offset = ROW_COUNT_SIZE + rowId * ROW_SIZE;
        byte[] personId = Arrays.copyOfRange(rows, offset, (offset += this.personIdSize));
        byte[] name = Arrays.copyOfRange(rows, offset, (offset += this.nameSize));

        // create a row with the data
        return new peopleRow(personId, name);
    }

    @Override
    public int insertRow(Row row) {
        // rigorous check on the data to avoid null entries

        if (row == null || row.movieId != null || row.personId == null || row.name == null ||
                row.category != null || row.title != null ||
                row.key != null || row.pid != null || row.slotid != null) {
            return -1;
        }

        if (isFull()) {
            return -1;
        }

        // get the row count
        int rowCount = getRowCount();
        int offset = ROW_COUNT_SIZE + rowCount * ROW_SIZE;

        // copy the data to the page
        copyAndPaste(row.personId, this.personIdSize, offset);
        copyAndPaste(row.name, this.nameSize, (offset += this.personIdSize));
        setRowCount(rowCount + 1);
        return rowCount;
    }

    private void copyAndPaste(byte[] data, int size, int offset) {
        // Copy the fixed_copy into the rows array starting at the given offset.
        byte[] fixed_copy = new byte[size];

        if (data != null) {
            System.arraycopy(data, 0, fixed_copy, 0, Math.min(data.length, size));
        }
        for (int i = 0; i < size; i++) {
            this.rows[offset + i] = fixed_copy[i];
        }
    }

    // if row count >= max row count than page is full
    @Override
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
    public int getRowCount() {
        return ByteBuffer.wrap(rows, 0, ROW_COUNT_SIZE).getInt();
    }

    // since this is data page, we dont have bool value
    public boolean getBoolValue() {
        return false;
    }

    // set the row count in first 4 bytes
    public void setRowCount(int count) {
        ByteBuffer.wrap(rows, 0, ROW_COUNT_SIZE).putInt(count);
    }

    // since it is a data page, we dont have next pointer
    public void setNextPointer(int nextPointer) {
        return;
    }

    // since it is a data page, we dont have next pointer
    public int getNextPointer() {
        return -1;
    }
}
