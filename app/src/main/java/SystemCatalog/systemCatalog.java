package SystemCatalog;

import java.util.HashMap;
import java.util.ArrayList;

public class systemCatalog {

    private static systemCatalog instance; // Singleton instance of systemCatalog
    private final HashMap<String, tableMetaData> tables; // HashMap to store table metadata with table name as the key
    private final HashMap<Integer, String> pidToTable; // HashMap to map page IDs to table names
    private final HashMap<Integer, String> pidToFile; // HashMap to map page IDs to file names
    private final HashMap<String, String> FileToTable; // HashMap to map file names to table names
    private String currentFile; // Current table being accessed

    // Private constructor to initialize the systemCatalog instance
    private systemCatalog() {

        this.tables = new HashMap<>();
        this.pidToTable = new HashMap<>();
        this.FileToTable = new HashMap<>();
        this.pidToFile = new HashMap<>();
        this.currentFile = null;
    }

    // Creates a singleton instance of systemCatalog
    public static systemCatalog getInstance() {
        if (instance == null) {
            instance = new systemCatalog();
        }
        return instance;
    }

    // Returns a boolean if the table was added successfully to catalog
    public boolean addTable(String tableName, String fileName, ArrayList<columnMetaData> columns) {
        // Check if the table already exists in the catalog
        if (tables.containsKey(tableName)) {
            return false;
        }

        // Creates and stores the new table metadata in the catalog
        tableMetaData table = new tableMetaData(tableName, fileName, columns);
        tables.put(tableName, table); // creates a mapping of table name to table metadata
        this.FileToTable.put(fileName, tableName); // Map file name to table name

        return true;
    }

    // Returns a boolean if the current file was set successfully
    public boolean setCurrentFile(String File) {
        // Only set the current file if it exists in the catalog
        if (FileToTable.containsKey(File)) {
            this.currentFile = FileToTable.get(File);
            return true;
        }
        return false;
    }

    // Returns the current file name being accessed
    public String getCurrentFile() {
        return this.currentFile;
    }

    // Returns a boolean if a index was added successfully to the table
    public boolean addIndex(String tableName, String columnName, String indexFile) {
        // Check if the table exists in the catalog and adds the index to a column if it
        // exist
        tableMetaData table = tables.get(tableName);
        if (table == null || !table.addIndex(columnName, indexFile)) {
            return false;
        }
        // Map the index file to the table name for easy access
        this.FileToTable.put(indexFile, tableName);
        return true;
    }

    // Returns tablemeta data if the table exists, null otherwise
    public tableMetaData getTable(String tableName) {
        return tables.get(tableName);
    }

    // Returns the all the file names of the tables in the catalog
    public String[] getAllTableNames() {
        return tables.keySet().toArray(new String[0]);
    }

    // Returns the index file name for a given table and column name if it exists,
    // null otherwise
    public String getIndexFile(String tableName, String columnName) {
        tableMetaData table = tables.get(tableName);
        if (table == null) {
            return null;
        }
        return table.getIndexFile(columnName);
    }

    // Maps page IDs to table names for easy access
    // Useful for identifying which table a page belongs to when working with the
    // buffer manager
    public void addPidToTable(int pid, String tableName) {
        pidToTable.put(pid, tableName);
    }

    // Returns the table name associated with a given page ID, if it exists
    public String getTableNameFromPid(int pid) {
        return pidToTable.get(pid);
    }

    // Returns the table name associated with a given file name, if it exists
    public String getTableNameFromFile(String tableName) {
        return FileToTable.get(tableName);
    }

    public void addPidToFile(int pid, String fileName) {
        pidToFile.put(pid, fileName);
    }

    public String getFileNameFromPid(int pid) {
        return pidToFile.get(pid);
    }
}
