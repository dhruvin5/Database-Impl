public interface Page {
    /**
     * Fetches a row from the page by its row ID.
     * @param rowId The ID of the row to retrieve.
     * @return The Row object containing the requested data.
     */
    Row getRow(int rowId);

    /**
     * Inserts a new row into the page.
     * @param row The Row object containing the data to insert.
     * @return The row ID of the inserted row, or -1 if the page is full
     */
    int insertRow(Row row);

    /**
     * Check if the page is full.
     * @return true if the page is full, false otherwise 
     */
    boolean isFull();

    /**
     * Returns the page id
     * @return page id of this page
     */
    int getPid();
} 

/**
 * Struct representing a database row, *containing primitive data types ONLY* to enable serialization.
 */
public class Row {
    // Define primary data type fields, depending on the schema of the table
    // These fields are for the Movies table described below
    public byte[] movieId;
    public byte[] title;
    
    public Row(byte[] movieId, byte[] title) {
        this.movieId = movieId;
        this.title = title;
    }
}

public class Utilities{
    // Loads the buffer manager with the imdb dataset
    public static void loadDataset(BufferManager bf, String filepath){

    }
}