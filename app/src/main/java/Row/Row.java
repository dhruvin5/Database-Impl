package Row;

/**
 * Struct representing a database row, *containing primitive data types ONLY* to
 * enable serialization.
 */
public class Row {
    // Define primary data type fields, depending on the schema of the table
    // These fields are for the Movies table described below
    public byte[] movieId;
    public byte[] title;

    // just take in the movie id and title
    // length checks are done during insertRow()
    public Row(byte[] movieId, byte[] title) {
        this.movieId = movieId;
        this.title = title;
    }
    
    public byte booleanValue;
    public byte[] key;
    public byte[] pid;
    public byte[] slotid;

    public Row(byte booleanValue, byte[] key, byte[] pid, byte[] slotid) {
        this.booleanValue = booleanValue;
        this.key = key;
        this.pid = pid;
        this.slotid = slotid;
    }
}
