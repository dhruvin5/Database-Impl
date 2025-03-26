package SystemCatalog;

import java.util.HashMap;
import java.util.ArrayList;
// import buffer.BufferManagerImplem;
// import buffer.BufferManager;

public class systemCatalog {

    // private final BufferManager bufferManager; // Shared BufferManager instance
    // for managing buffer pages
    private static systemCatalog instance; // Singleton instance of systemCatalog
    private final HashMap<String, tableMetaData> tables; // HashMap to store table metadata with table name as the key
    private final HashMap<Integer, String> pidToTable; // HashMap to map page IDs to table names
    private final HashMap<String, String> FileToTable; // HashMap to map file names to table names
    private String currentFile; // Current table being accessed

    // Private constructor to initialize the systemCatalog instance
    private systemCatalog() {
        // this.bufferManager = new BufferManagerImplem(10);
        this.tables = new HashMap<>();
        this.pidToTable = new HashMap<>();
        this.FileToTable = new HashMap<>();
        this.currentFile = null;
    }

    // Creates a singleton instance of systemCatalog
    public static systemCatalog getInstance() {
        if (instance == null) {
            instance = new systemCatalog();
        }
        return instance;
    }

    // // Returns the BufferManager instance
    // public BufferManager getBufferManager() {
    // return bufferManager;
    // }

    // Returns a boolean if the table was added successfully to catalog
    public boolean addTable(String tableName, String fileName, ArrayList<columnMetaData> columns) {
        if (tables.containsKey(tableName)) {
            return false;
        }

        tableMetaData table = new tableMetaData(tableName, fileName, columns);
        tables.put(tableName, table);
        this.FileToTable.put(fileName, tableName); // Map file name to table name
        return true;
    }

    public boolean setCurrentFile(String File) {
        if (FileToTable.containsKey(File)) {
            this.currentFile = FileToTable.get(File);
            return true;
        }
        return false;
    }

    public String getCurrentFile() {
        return this.currentFile;
    }

    // Returns a boolean if a index was added successfully to the table
    public boolean addIndex(String tableName, String columnName, String indexFile) {
        tableMetaData table = tables.get(tableName);
        if (table == null || !table.addIndex(columnName, indexFile)) {
            return false;
        }
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

    // Returns the column type for a given table and column name if it exists,
    public void addPidToTable(int pid, String tableName) {
        pidToTable.put(pid, tableName);
    }

    public String getTableNameFromPid(int pid) {
        return pidToTable.get(pid);
    }

    public String getTableNameFromFile(String tableName) {
        return FileToTable.get(tableName);
    }
}
