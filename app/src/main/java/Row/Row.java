package Row;

import java.util.ArrayList;

/**
 * Struct representing a database row, *containing primitive data types ONLY* to
 * enable serialization.
 */
public class Row {
    // Define primary data type fields, depending on the schema of the table
    // These fields are for the Movies table described below
    public byte[] movieId;
    public byte[] title;
    public byte[] data;

    // just take in the movie id and title
    // length checks are done during insertRow()
    public Row(byte[] movieId, byte[] title) {
        this.movieId = movieId;
        this.title = title;
    }

    public Row(byte[] movieId, byte[] title, byte[] data) {
        this.movieId = movieId;
        this.title = title;
        this.data = data;
    }
}
