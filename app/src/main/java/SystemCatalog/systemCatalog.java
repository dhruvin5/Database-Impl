package SystemCatalog;

import java.util.HashMap;
import java.util.ArrayList;

public class systemCatalog {

    private final HashMap<String, Boolean> isIndex;
    private final HashMap<String, tableMetaData> tables; // HashMap to store table metadata with table name as the key
    private final HashMap<String, indexMetaData> indexes; // HashMap to store index metadata with index file name as the
                                                          // key

    public systemCatalog() {

        this.tables = new HashMap<>();
        this.indexes = new HashMap<>();
        this.isIndex = new HashMap<>();

        // Defines the Movie Table
        ArrayList<columnMetaData> columns = new ArrayList<>();
        columns.add(new columnMetaData("movieId", "INTEGER", 9));
        columns.add(new columnMetaData("title", "STRING", 30));
        addTable("Movies", "movies.bin", columns); // Adds the Movie table to the catalog

        // creates page schema for the Movie table
        ArrayList<columnMetaData> movie_index_columns = new ArrayList<>();
        movie_index_columns.add(new columnMetaData("isLeaf", "BOOLEAN", 1)); // pageId for the B+ tree
        movie_index_columns.add(new columnMetaData("pid", "INTEGER", 4));
        movie_index_columns.add(new columnMetaData("movieId", "INTEGER", 9));

        // creates page schema for the Movie table
        ArrayList<columnMetaData> title_index_columns = new ArrayList<>();
        title_index_columns.add(new columnMetaData("isLeaf", "BOOLEAN", 1)); // pageId for the B+ tree
        title_index_columns.add(new columnMetaData("pid", "INTEGER", 4));
        title_index_columns.add(new columnMetaData("title", "STRING", 30));

        addIndex("Movie_Index", "Movies", "movieId", "movieId.bin"); // Adds an index on the movieId column
        addIndex("Movies_title", "Movies", "title", "title.bin"); // Adds an index on the title column

        addTable("", "movieId.bin", movie_index_columns);
        addTable(" ", "title.bin", title_index_columns);

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
        isIndex.put(fileName, isIndex.getOrDefault(fileName, false)); // Mark the file as a table (not an index)

        return true;
    }

    // Returns a boolean if a index was added successfully to the table
    private boolean addIndex(String indexName, String tableName, String key, String indexFile) {
        tableMetaData table = tables.get(tableName);
        if (table == null || !table.getColumnNames().contains(key) || indexes.containsKey(indexName)) {
            return false;
        }
        indexes.put(indexName, new indexMetaData(table, key, indexFile));
        isIndex.put(indexFile, true); // Mark the file as an index
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

    public boolean isIndexFile(String fileName) {
        return isIndex.getOrDefault(fileName, false);
    }
}