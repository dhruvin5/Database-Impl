package SystemCatalog;

public class indexMetaData {
    private tableMetaData table; // Reference to the table metadata
    private String Key;
    private String file;
    private String indexType;

    public indexMetaData(tableMetaData table, String Key, String file, String indexType) {
        this.table = table;
        this.Key = Key;
        this.file = file;
        this.indexType = indexType;
    }

    // Get the table the index is associated with
    public String getTableName() {
        return table.getTableName(); // Returns the name of the table associated with this index
    }

    // Get the key name of the index
    public String getKey() {
        return Key;
    }

    // Get the file name of the index
    public String getFile() {
        return file;
    }
}
