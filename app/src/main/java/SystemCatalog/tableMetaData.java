package SystemCatalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * tableMetaData class represents the metadata of a table in the system catalog.
 * It contains information about the table name, file name, columns, and their
 * properties.
 */
public class tableMetaData {
    private final String tableName; // Name of the table
    private final String fileName; // Name of the file where the table data is stored
    private HashMap<String, columnMetaData> columnData; // HashMap to store column metadata with column name as the key
    private HashMap<String, Integer> columnOrder; // HashMap to store the order of columns
    private final int rowSize; // Size of a row in bytes, calculated as the sum of sizes of all columns

    public tableMetaData(String tableName, String fileName, ArrayList<columnMetaData> columns) {
        this.tableName = tableName;
        this.fileName = fileName;
        this.columnData = new HashMap<>();
        this.columnOrder = new HashMap<>();

        // Calculate Row Size and populate columnData and columnOrder
        int size = 0;
        for (int i = 0; i < columns.size(); i++) {
            columnMetaData column = columns.get(i);
            this.columnData.put(column.getColumnName(), column);
            this.columnOrder.put(column.getColumnName(), i);
            size += column.getSize();
        }
        this.rowSize = size;
    }

    // Returns the file where the file data is stored
    public String getFileName() {
        return fileName;
    }

    // Returns table name
    public String getTableName() {
        return tableName;
    }

    // Returns the list of column names ordered by their insertion order
    public ArrayList<String> getColumnNames() {
        Set<String> keys = this.columnOrder.keySet();
        ArrayList<String> columnNames = new ArrayList<>(keys);
        columnNames.sort((i, j) -> Integer.compare(this.columnOrder.get(i), this.columnOrder.get(j)));
        return columnNames;
    }

    // Returns the column type given a column name
    public String getColumnType(String columnName) {
        columnMetaData column = this.columnData.get(columnName);
        if (column != null) {
            return column.getColumnType();
        }
        return null;
    }

    // Return the column size given a column name
    public int getColumnSize(String columnName) {
        if (!this.columnData.containsKey(columnName)) {
            return -1;
        }
        return this.columnData.get(columnName).getSize();
    }

    // Retrieves Index file name for a given column name if it exists
    public String getIndexFile(String columnName) {
        if (!this.columnData.containsKey(columnName)) {
            return null;
        }
        return this.columnData.get(columnName).getIndexFile();
    }

    // adds an index file to the column metadata for a given column name
    public boolean addIndex(String columnName, String indexFile) {
        if (!this.columnData.containsKey(columnName)) {
            return false;
        }
        this.columnData.get(columnName).setIndexFile(indexFile);
        return true;

    }

    // Returns the Row Size
    public int getRowSize() {
        return this.rowSize;
    }
}
