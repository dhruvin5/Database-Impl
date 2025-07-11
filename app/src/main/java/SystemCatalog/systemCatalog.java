package SystemCatalog;

import java.util.HashMap;
import java.util.ArrayList;

public class systemCatalog {

    private final HashMap<String, Boolean> isIndex;
    private final HashMap<String, tableMetaData> tables;
    private final HashMap<String, Integer> offsets; // HashMap to store table metadata with table name as the key
    private final HashMap<String, indexMetaData> indexes;// HashMap to store index metadata with index file name as the
    private final int leafPageOffset;
    private final int nonLeafPageOffset;

    // key

    public systemCatalog() {

        this.tables = new HashMap<>();
        this.indexes = new HashMap<>();
        this.isIndex = new HashMap<>();
        this.leafPageOffset = 5;
        this.nonLeafPageOffset = 5;
        this.offsets = new HashMap<>();

        // Creates the column metadata for the Movie table
        columnMetaData movieID_column = new columnMetaData("movieId", "INTEGER", 9);
        columnMetaData title_column = new columnMetaData("title", "STRING", 30);
        offsets.put("movie_Id_index.bin", 9);
        offsets.put("title_index.bin", 30);

        // Creates the column metadata for the Movie ID and title index table
        columnMetaData pid_column = new columnMetaData("pid", "INTEGER", 4);
        columnMetaData slotID_column = new columnMetaData("slotID", "INTEGER", 4);

        // Defines the Movie Table
        ArrayList<columnMetaData> columns = new ArrayList<>();
        columns.add(movieID_column);
        columns.add(title_column);

        // creates page schema for the Movie ID index table
        ArrayList<columnMetaData> movie_index_columns = new ArrayList<>();
        movie_index_columns.add(movieID_column);
        movie_index_columns.add(pid_column);
        movie_index_columns.add(slotID_column);

        // creates page schema for the title ID table
        ArrayList<columnMetaData> title_index_columns = new ArrayList<>();
        title_index_columns.add(title_column);
        title_index_columns.add(pid_column);
        title_index_columns.add(slotID_column);

        // Adds the Movie table to the catalog
        addTable("Movies", "movies.bin", columns); // Adds the Movie table to the catalog
        addIndex("Movie_Index", "movies.bin", "movieId", "movie_Id_index.bin", "b+ tree"); // Adds an index on the
        addIndex("Movies_title", "movies.bin", "title", "title_index.bin", "b+ tree"); // Adds an index on the title

        addTable("-1", "-1", columns);
        addTable("Movie_ID_Index", "movie_Id_index.bin", movie_index_columns);
        addTable("Title_Index", "title_index.bin", title_index_columns);

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
    private boolean addIndex(String indexName, String tablefile, String key, String indexFile, String indexType) {
        tableMetaData table = tables.get(tablefile);
        if (table == null || !table.getColumnNames().contains(key) || indexes.containsKey(indexName)) {
            return false;
        }
        indexes.put(indexFile, new indexMetaData(table, key, indexFile, indexType));
        isIndex.put(indexFile, true); // Mark the file as an index
        return true;
    }

    // Returns the file name of a table given its name
    public String getTableFile(String tableName) {
        if (!tables.containsKey(tableName)) {
            return null;
        }
        return tables.get(tableName).getFile();
    }

    public int getOffsets(String filename) {
        if (!offsets.containsKey(filename)) {
            return 0;
        }
        return offsets.get(filename);
    }

    // Returns the metadata of an index given its name
    public indexMetaData getIndex(String indexName) {
        if (!indexes.containsKey(indexName)) {
            return null;
        }
        return indexes.get(indexName);
    }

    // Returns the all the file names of the tables in the catalog
    public String[] getAllTableNames() {
        return tables.keySet().toArray(new String[0]);
    }

    // Returns the all the index file names in the catalog
    public String[] getAllIndexFiles() {
        return indexes.keySet().toArray(new String[0]);
    }

    // Returns the metadata of a table given its name
    public tableMetaData getTableMetaData(String tableName) {
        return tables.get(tableName);
    }

    public int getPageOffset(boolean isLeaf) {
        if (isLeaf) {
            return leafPageOffset;
        }
        return nonLeafPageOffset;
    }

    // Returns the metadata of a table given its file name
    public boolean isIndexFile(String fileName) {
        return isIndex.getOrDefault(fileName, false);
    }
}