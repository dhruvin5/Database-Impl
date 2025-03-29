package Page;

import java.nio.ByteBuffer;
import java.util.Arrays;

import Row.Row;
import configs.Config;

public class PageImpl implements Page {

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

    private int offSet1;

    private int offSet2;

    private int offSet3;

    // if the page is being created
    // initialize with page size
    // set row count as 0
    public PageImpl(int pageId, int ROW_SIZE, int MAX_ROW_COUNT, int offSet1, int offSet2, int offSet3) {
        this.pageId = pageId;
        this.rows = new byte[Config.PAGE_SIZE];
        setRowCount(0);

        this.ROW_SIZE = ROW_SIZE;
        this.MAX_ROW_COUNT = MAX_ROW_COUNT;
        this.offSet1 = offSet1;
        this.offSet2 = offSet2;
        this.offSet3 = -1;
    }

    // if loading an existing page in Buffer
    public PageImpl(int pageId, byte[] existingRows, int ROW_SIZE, int MAX_ROW_COUNT, int offSet1, int offSet2,
            int offSet3) {
        if (existingRows.length != Config.PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be 4KB!");
        }
        this.pageId = pageId;
        this.rows = existingRows;

        this.ROW_SIZE = ROW_SIZE;
        this.MAX_ROW_COUNT = MAX_ROW_COUNT;
        this.offSet1 = offSet1;
        this.offSet2 = offSet2;
        this.offSet3 = offSet3;
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
        byte[] column1 = Arrays.copyOfRange(rows, offset, offset + this.offSet1);
        byte[] column2 = Arrays.copyOfRange(rows, offset + this.offSet1, offset + this.offSet1 + this.offSet2);

        if (this.offSet3 != -1) {
            byte[] column3 = Arrays.copyOfRange(rows, offset + this.offSet1 + this.offSet2,
                    offset + this.offSet1 + this.offSet2 + this.offSet3);
            return new Row(column1, column2, column3);
        }

        // create a row with the data
        return new Row(column1, column2);
    }

    @Override
    public int insertRow(Row row) {
        // rigorous check on the data to avoid null entries
        if (row == null || row.movieId == null || row.title == null) {
            return -1;
        }

        if (isFull()) {
            return -1;
        }

        byte[] movieIdFixed = new byte[this.offSet1];
        byte[] titleFixed = new byte[this.offSet2];

        System.arraycopy(row.movieId, 0, movieIdFixed, 0,
                Math.min(row.movieId.length, this.offSet1));
        System.arraycopy(row.title, 0, titleFixed, 0, Math.min(row.title.length,
                this.offSet2));

        int rowCount = getRowCount();
        int offset = ROW_COUNT_SIZE + rowCount * ROW_SIZE;

        // copy the row into the page data
        for (int i = 0; i < this.offSet1; i++) {
            this.rows[offset + i] = movieIdFixed[i];
        }

        for (int i = 0; i < this.offSet2; i++) {
            this.rows[offset + this.offSet2 + i] = titleFixed[i];
        }

        if (this.offSet3 != -1) {
            byte[] dataFixed = new byte[this.offSet3];
            System.arraycopy(row.data, 0, dataFixed, 0,
                    Math.min(row.data.length, this.offSet3));
            for (int i = 0; i < this.offSet3; i++) {
                this.rows[offset + this.offSet1 + this.offSet2 + i] = dataFixed[i];
            }
        }

        setRowCount(rowCount + 1);
        return rowCount;
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

    private int getRowCount() {
        return ByteBuffer.wrap(rows, 0, ROW_COUNT_SIZE).getInt();
    }

    // set the row count in first 4 bytes
    private void setRowCount(int count) {
        ByteBuffer.wrap(rows, 0, ROW_COUNT_SIZE).putInt(count);
    }
}