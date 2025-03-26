package Page;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import Row.Row;
import configs.Config;
import SystemCatalog.systemCatalog;
import SystemCatalog.tableMetaData;

import java.util.ArrayList;

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

    // gets the shared systemCatalog instance
    private final systemCatalog catalog;

    // table name associated with the pageId
    private final String tableName;

    // if the page is being created
    // initialize with page size
    // set row count as 0
    public PageImpl(int pageId) {
        this.pageId = pageId;
        this.rows = new byte[Config.PAGE_SIZE];
        setRowCount(0);

        this.catalog = systemCatalog.getInstance();
        this.tableName = catalog.getTableNameFromPid(pageId);
        tableMetaData table = catalog.getTable(this.tableName);

        this.ROW_SIZE = table.getRowSize();
        this.MAX_ROW_COUNT = (Config.PAGE_SIZE - ROW_COUNT_SIZE) / ROW_SIZE; // max rows that can fit in a page

    }

    // if loading an existing page in Buffer
    public PageImpl(int pageId, byte[] existingRows) {
        if (existingRows.length != Config.PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be 4KB!");
        }
        this.pageId = pageId;
        this.rows = existingRows;
        this.catalog = systemCatalog.getInstance();
        this.tableName = catalog.getTableNameFromPid(pageId);

        tableMetaData table = catalog.getTable(this.tableName);

        this.ROW_SIZE = table.getRowSize();
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

        tableMetaData table = catalog.getTable(this.tableName);
        ArrayList<String> columnNames = table.getColumnNames();

        // int column_1_size = table.getColumnSize(columnNames.get(0));
        // int column_2_size = table.getColumnSize(columnNames.get(1));

        // // go to the offset
        // int offset = ROW_COUNT_SIZE + rowId * ROW_SIZE;
        // byte[] column1 = Arrays.copyOfRange(rows, offset, offset + column_1_size);
        // byte[] column2 = Arrays.copyOfRange(rows, offset + column_1_size, offset +
        // column_1_size + column_2_size);

        // // create a row with the data
        // return new Row(column1, column2);
        return getRowHelper(rowId, columnNames, table);
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

        tableMetaData table = catalog.getTable(this.tableName);
        ArrayList<String> columnNames = table.getColumnNames();

        insertRowHelper(row, columnNames, table);
        int rowCount = getRowCount();
        setRowCount(rowCount + 1);
        return rowCount;

        // int column_1_Size = table.getColumnSize(columnNames.get(0));
        // int column_2_Size = table.getColumnSize(columnNames.get(1));

        // byte[] movieIdFixed = new byte[column_1_Size];
        // byte[] titleFixed = new byte[column_2_Size];

        // // movieId should be of size 9
        // // truncate longer title to 30. pad if less
        // System.arraycopy(row.movieId, 0, movieIdFixed, 0,
        // Math.min(row.movieId.length, column_1_Size));
        // System.arraycopy(row.title, 0, titleFixed, 0, Math.min(row.title.length,
        // column_2_Size));

        // int rowCount = getRowCount();
        // int offset = ROW_COUNT_SIZE + rowCount * ROW_SIZE;

        // // copy the row into the page data
        // for (int i = 0; i < column_1_Size; i++) {
        // this.rows[offset + i] = movieIdFixed[i];
        // }

        // for (int i = 0; i < column_2_Size; i++) {
        // this.rows[offset + column_1_Size + i] = titleFixed[i];
        // }

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

    private Row getRowHelper(int rowId, ArrayList<String> columnNames, tableMetaData table) {
        if (columnNames.size() == 2) {
            int column_1_size = table.getColumnSize(columnNames.get(0));
            int column_2_size = table.getColumnSize(columnNames.get(1));

            // go to the offset
            int offset = ROW_COUNT_SIZE + rowId * ROW_SIZE;
            byte[] column1 = Arrays.copyOfRange(rows, offset, offset + column_1_size);
            byte[] column2 = Arrays.copyOfRange(rows, offset + column_1_size, offset + column_1_size + column_2_size);

            // create a row with the data
            return new Row(column1, column2);

        }
        ArrayList<byte[]> data = new ArrayList<>();
        for (String columnName : columnNames) {
            int columnSize = table.getColumnSize(columnName);
            int offset = ROW_COUNT_SIZE + rowId * ROW_SIZE + data.size() * columnSize;
            byte[] columnData = Arrays.copyOfRange(rows, offset, offset + columnSize);
            data.add(columnData);
        }
        return new Row(data);
    }

    private void insertRowHelper(Row row, ArrayList<String> columnNames, tableMetaData table) {
        if (row.data.size() == 0) {
            int column_1_size = table.getColumnSize(columnNames.get(0));
            int column_2_size = table.getColumnSize(columnNames.get(1));

            // go to the offset
            int offset = ROW_COUNT_SIZE + getRowCount() * ROW_SIZE;
            byte[] column1 = Arrays.copyOfRange(row.movieId, 0, column_1_size);
            byte[] column2 = Arrays.copyOfRange(row.title, 0, column_2_size);

            // copy the row into the page data
            System.arraycopy(column1, 0, rows, offset, column_1_size);
            System.arraycopy(column2, 0, rows, offset + column_1_size, column_2_size);

            for (int i = 0; i < column_1_size; i++) {
                this.rows[offset + i] = column1[i];
            }

            for (int i = 0; i < column_2_size; i++) {
                this.rows[offset + column_1_size + i] = column2[i];
            }

        } else {
            int curr = 0;
            ArrayList<byte[]> data = row.data;
            for (int i = 0; i < data.size(); i++) {
                int columnSize = table.getColumnSize(columnNames.get(i));
                int offset = ROW_COUNT_SIZE + getRowCount() * ROW_SIZE + curr;
                byte[] columnData = Arrays.copyOfRange(data.get(i), 0, columnSize);
                System.arraycopy(columnData, 0, rows, offset, columnSize);
                curr += columnSize;

                for (int j = 0; j < columnSize; j++) {
                    this.rows[offset + j] = columnData[j];
                }
            }

        }
    }

}
