package SystemCatalog;

import java.util.HashMap;
import java.util.ArrayList;
import buffer.BufferManagerImplem;
import buffer.BufferManager;

public class systemCatalog {

    private final BufferManager bufferManager;
    private static systemCatalog instance;
    private final HashMap<String, tableMetaData> tables;
    private final HashMap<Integer, String> pidToTable;

    private systemCatalog() {
        this.bufferManager = new BufferManagerImplem(10);
        this.tables = new HashMap<>();
        this.pidToTable = new HashMap<>();

    }

    // Creates a singleton instance of systemCatalog
    public static systemCatalog getInstance() {
        if (instance == null) {
            instance = new systemCatalog();
        }
        return instance;
    }

    // Returns the BufferManager instance
    public BufferManager getBufferManager() {
        return bufferManager;
    }

    public boolean addTable(String tableName, String fileName, ArrayList<columnMetaData> columns) {
        if (tables.containsKey(tableName)) {
            return false;
        }

        tableMetaData table = new tableMetaData(tableName, fileName, columns);
        tables.put(tableName, table);
        return true;
    }

    public boolean addIndex(String tableName, String columnName, String indexFile) {
        tableMetaData table = tables.get(tableName);
        if (table == null || !table.addIndex(columnName, indexFile)) {
            return false;
        }
        return true;
    }

    public boolean tableExists(String tableName) {
        return tables.containsKey(tableName);
    }

    public tableMetaData getTable(String tableName) {
        return tables.get(tableName);
    }

    public String[] getAllTableNames() {
        return tables.keySet().toArray(new String[0]);
    }

    public boolean indexExists(String tableName, String columnName) {
        tableMetaData table = tables.get(tableName);
        if (table == null) {
            return false;
        }
        return table.getIndexFile(columnName) != null;
    }

    public String getIndexFile(String tableName, String columnName) {
        tableMetaData table = tables.get(tableName);
        if (table == null) {
            return null;
        }
        return table.getIndexFile(columnName);
    }
}
