package SystemCatalog;

public class columnMetaData {

    private final String columnName; // Name of the column
    private final String columnType; // Type of the column
    private final int size; // Size of the column in bytes

    // Constructor to initialize the column metadata
    public columnMetaData(String columnName, String columnType, int size) {
        this.columnName = columnName;
        this.columnType = columnType;
        this.size = size;
    }

    // Returns name of the column
    public String getColumnName() {
        return columnName;
    }

    // Returns type of the column
    public String getColumnType() {
        return columnType;
    }

    // Returns size of the column in bytes
    public int getSize() {
        return size;
    }
}
