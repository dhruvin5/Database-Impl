package Page;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class PageImpl extends Page{
    private static final int PAGE_SIZE = 4096;
    private static final int ROW_COUNT_SIZE = 4;
    private static final int ROW_SIZE = 39;
    private static final int MAX_ROW_COUNT = 104;

    private final int pageId;
    private final byte[] rows;

    public PageImpl(int pageId) {
        this.pageId = pageId;
        this.rows = new byte[PAGE_SIZE];
        setRowCount(0);
    }

    public PageImpl(int pageId, byte[] rows) {
        if (existingRows.length != PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be 4KB!");
        }
        this.pageId = pageId;
        this.rows = rows;
    }


    Row getRow(int rowId)
    {
        int rowCount = getRowCount();
        if (rowId < 0 || rowId >= rowCount) {
            return null;
        }
        int offset = ROW_COUNT_SIZE + rowId * ROW_SIZE;
        byte[] movieId = Arrays.copyOfRange(rows, offset, offset + 9);
        byte[] title = Arrays.copyOfRange(rows, offset + 9, offset + 39);
        return new Row(movieId, title);
    }

    int insertRow(Row row)
    {
        if (isFull()) {
            return -1;
        }
        int rowCount = getRowCount();
        int offset = ROW_COUNT_SIZE + rowCount * ROW_SIZE;

        for (int i = 0; i < 9; i++) {
            this.rows[offset + i] = row.movieId[i];
        }

        for (int i = 0; i < 30; i++) {
            this.rows[offset + 9 + i] = row.title[i];
        }

        setRowCount(rowCount + 1);
        return rowCount;
    }

    boolean isFull()
    {
        int rowCount = getRowCount();
        if(rowCount == MAX_ROW_COUNT)
        {
            return true;
        }
        return false;
    }

    int getPid()
    {
        return this.pageId;
    }

    private int getRowCount() {
        return ByteBuffer.wrap(data, 0, ROW_COUNT_SIZE).getInt();
    }

    private void setRowCount(int count) {
        ByteBuffer.wrap(data, 0, ROW_COUNT_SIZE).putInt(count);
    }

}
