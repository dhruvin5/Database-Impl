package SystemCatalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

// import org.checkerframework.checker.units.qual.A;

public class tableMetaData {
    private final String tableName;
    private final String fileName;
    private HashMap<String, columnMetaData> columnData;
    private HashMap<String, Integer> columnOrder;
    private final int rowSize;

    public tableMetaData(String tableName, String fileName, ArrayList<columnMetaData> columns) {
        this.tableName = tableName;
        this.fileName = fileName;
        this.columnData = new HashMap<>();
        this.columnOrder = new HashMap<>();

        int size = 0;
        for (int i = 0; i < columns.size(); i++) {
            columnMetaData column = columns.get(i);
            this.columnData.put(column.getColumnName(), column);
            this.columnOrder.put(column.getColumnName(), i);
            size += column.getSize();
        }
        this.rowSize = size;
    }

    public String getFileName() {
        return fileName;
    }

    public String getTableName() {
        return tableName;
    }

    public Set<String> getColumnNames() {
        return this.columnData.keySet();
    }

    public String getColumnType(String columnName) {
        columnMetaData column = this.columnData.get(columnName);
        if (column != null) {
            return column.getColumnType();
        }
        return null;
    }

    public int getColumnSize(String columnName) {
        if (!this.columnData.containsKey(columnName)) {
            return -1;
        }
        return this.columnData.get(columnName).getSize();
    }

    public String getIndexFile(String columnName) {
        if (!this.columnData.containsKey(columnName)) {
            return null;
        }
        return this.columnData.get(columnName).getIndexFile();
    }

    public boolean addIndex(String columnName, String indexFile) {
        if (!this.columnData.containsKey(columnName)) {
            return false;
        }
        this.columnData.get(columnName).setIndexFile(indexFile);
        return true;

    }

    public int getRowSize() {
        return this.rowSize;
    }
}
