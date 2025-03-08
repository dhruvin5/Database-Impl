package Page;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import Row.Row;
import configs.*;

public class PageImpl implements Page {

    private static final int ROW_COUNT_SIZE = 4;
    private static final int ROW_SIZE = 39;
    private static final int MAX_ROW_COUNT = 104;

    private final int pageId;
    private final byte[] rows;

    public PageImpl(int pageId) {
        this.pageId = pageId;
        this.rows = new byte[Config.PAGE_SIZE];
        setRowCount(0);
    }

    public PageImpl(int pageId, byte[] existingRows) {
        if (existingRows.length != Config.PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be 4KB!");
        }
        this.pageId = pageId;
        this.rows = existingRows;
    }

    @Override
    public Row getRow(int rowId)
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

    @Override
    public int insertRow(Row row)
    {
        if (row == null || row.movieId == null || row.title == null) {
            return -1;
        }

        if (isFull()) {
            return -1;
        }


        byte[] movieIdFixed = new byte[9];
        byte[] titleFixed = new byte[30];
        
        System.arraycopy(row.movieId, 0, movieIdFixed, 0, Math.min(row.movieId.length, 9));
        System.arraycopy(row.title, 0, titleFixed, 0, Math.min(row.title.length, 30));
        

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

    @Override
    public boolean isFull()
    {
        int rowCount = getRowCount();
        if(rowCount == MAX_ROW_COUNT)
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

    @Override
    public byte[] getRows()
    {
        return this.rows;
    }

    private int getRowCount() {
        return ByteBuffer.wrap(rows, 0, ROW_COUNT_SIZE).getInt();
    }

    private void setRowCount(int count) {
        ByteBuffer.wrap(rows, 0, ROW_COUNT_SIZE).putInt(count);
    }



}
