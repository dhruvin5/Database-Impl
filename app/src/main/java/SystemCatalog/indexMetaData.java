package SystemCatalog;

public class indexMetaData {
    private tableMetaData table; // Reference to the table metadata
    private String Key;
    private String file;

    // Constructor to initialize the index metadata
    public indexMetaData(tableMetaData table, String Key, String file) {
        this.table = table;
        this.Key = Key;
        this.file = file;
    }

    // Gets the table associated with this index
    public String getTableName() {
        return table.getTableName(); // Returns the name of the table associated with this index
    }

    // Gets the key of the index
    public String getKey() {
        return Key;
    }

    // Gets the file name of the index
    public String getFile() {
        return file;
    }
}
