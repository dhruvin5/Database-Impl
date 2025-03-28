package SystemCatalog;

import java.util.HashMap;
import java.util.ArrayList;

public class systemCatalog {

    private final HashMap<String, tableMetaData> tables; // HashMap to store table metadata with table name as the key
    private final HashMap<String, indexMetaData> indexes; // HashMap to store index metadata with index file name as the
                                                          // key

    public systemCatalog() {

        this.tables = new HashMap<>();
        this.indexes = new HashMap<>();

        // Defines the Movie Table
        ArrayList<columnMetaData> columns = new ArrayList<>();
        columns.add(new columnMetaData("movieId", "INTEGER", 9));
        columns.add(new columnMetaData("title", "STRING", 30));
        addTable("Movies", "movies.bin", columns); // Adds the Movie table to the catalog

        // Adds an index on the movieId and title column of the Movies table
        addIndex("Movie_Index", "Movies", "movieId", "movieId.bin"); // Adds an index on the movieId column
        addIndex("Movies_title", "Movies", "title", "title.bin"); // Adds an index on the title column
    }

    // Returns a boolean if the table was added successfully to catalog
    private boolean addTable(String tableName, String fileName, ArrayList<columnMetaData> columns) {
        // Check if the table already exists in the catalog
        if (tables.containsKey(tableName)) {
            return false;
        }

        // Creates and stores the new table metadata in the catalog
        tableMetaData table = new tableMetaData(tableName, fileName, columns);
        tables.put(fileName, table);

        return true;
    }

    // Returns a boolean if a index was added successfully to the table
    private boolean addIndex(String indexName, String tableName, String key, String indexFile) {
        tableMetaData table = tables.get(tableName);
        if (table == null || !table.getColumnNames().contains(key) || indexes.containsKey(indexName)) {
            return false;
        }
        indexes.put(indexName, new indexMetaData(table, key, indexFile));
        return true;
    }

    public String getTableFile(String tableName) {
        if (!tables.containsKey(tableName)) {
            return null;
        }
        return tables.get(tableName).getFile();
    }

    public String getIndexFile(String indexName) {
        if (!indexes.containsKey(indexName)) {
            return null;
        }
        return indexes.get(indexName).getFile();
    }

    // Returns the all the file names of the tables in the catalog
    public String[] getAllTableNames() {
        return tables.keySet().toArray(new String[0]);
    }

    // Returns the all the index file names in the catalog
    public String[] getAllIndexFiles() {
        return indexes.keySet().toArray(new String[0]);
    }

    public tableMetaData getTableMetaData(String tableName) {
        return tables.get(tableName);
    }
}