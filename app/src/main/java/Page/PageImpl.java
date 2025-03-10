package Page;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import Row.Row;
import configs.Config;

public class PageImpl implements Page {

    // has the row count
    private static final int ROW_COUNT_SIZE = 4;

    // fixed row size
    private static final int ROW_SIZE = 39;

    // this is calculated by pagesize - 4 / 39
    private static final int MAX_ROW_COUNT = 104;

    // the pageId should be final. Once assigned cannot be changed
    private final int pageId;

    // the actual data
    private final byte[] rows;


    // if the page is being created
    // initialize with page size
    // set row count as 0
    public PageImpl(int pageId) {
        this.pageId = pageId;
        this.rows = new byte[Config.PAGE_SIZE];
        setRowCount(0);
    }

    // if loading an existing page in Buffer
    public PageImpl(int pageId, byte[] existingRows) {
        if (existingRows.length != Config.PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be 4KB!");
        }
        this.pageId = pageId;
        this.rows = existingRows;
    }

    // gets the row using the rowId
    @Override
    public Row getRow(int rowId)
    {
        int rowCount = getRowCount();

        // if less than 0 or greater than rowCount. Invalid rowId
        if (rowId < 0 || rowId >= rowCount) {
            return null;
        }

        // go to the offset
        int offset = ROW_COUNT_SIZE + rowId * ROW_SIZE;
        byte[] movieId = Arrays.copyOfRange(rows, offset, offset + 9);
        byte[] title = Arrays.copyOfRange(rows, offset + 9, offset + 39);

        // create a row with the data
        return new Row(movieId, title);
    }

    @Override
    public int insertRow(Row row)
    {
        // rigorous check on the data to avoid null entries
        if (row == null || row.movieId == null || row.title == null) {
            return -1;
        }

        if (isFull()) {
            return -1;
        }

        byte[] movieIdFixed = new byte[9];
        byte[] titleFixed = new byte[30];

        // movieId should be of size 9
        // truncate longer title to 30. pad if less
        System.arraycopy(row.movieId, 0, movieIdFixed, 0, Math.min(row.movieId.length, 9));
        System.arraycopy(row.title, 0, titleFixed, 0, Math.min(row.title.length, 30));
        

        int rowCount = getRowCount();
        int offset = ROW_COUNT_SIZE + rowCount * ROW_SIZE;

        // copy the row into the page data
        for (int i = 0; i < 9; i++) {
            this.rows[offset + i] = movieIdFixed[i];
        }

        for (int i = 0; i < 30; i++) {
            this.rows[offset + 9 + i] = titleFixed[i];
        }

        setRowCount(rowCount + 1);
        return rowCount;
    }

    // if row count >= max row count than page is full
    @Override
    public boolean isFull()
    {
        int rowCount = getRowCount();
        if(rowCount >= MAX_ROW_COUNT)
        {
            return true;
        }
        return false;
    }

    @Override
    public int getPid()
    {
        return this.pageId;
    }

    // get the data of the rows
    @Override
    public byte[] getRows()
    {
        return this.rows;
    }

    //get the rowcount by accessing the first 4 bytes

    private int getRowCount() {
        return ByteBuffer.wrap(rows, 0, ROW_COUNT_SIZE).getInt();
    }

    // set the row count in first 4 bytes
    private void setRowCount(int count) {
        ByteBuffer.wrap(rows, 0, ROW_COUNT_SIZE).putInt(count);
    }



}
