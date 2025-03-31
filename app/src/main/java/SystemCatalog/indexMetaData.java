package SystemCatalog;

public class indexMetaData {
    private tableMetaData table; // Reference to the table metadata
    private String Key;
    private String file;

    public indexMetaData(tableMetaData table, String Key, String file) {
        this.table = table;
        this.Key = Key;
        this.file = file;
    }

    public String getTableName() {
        return table.getTableName(); // Returns the name of the table associated with this index
    }

    public String getKey() {
        return Key;
    }

    public String getFile() {
        return file;
    }
}
