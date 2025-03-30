package Row;

/**
 * Struct representing a database row, *containing primitive data types ONLY* to
 * enable serialization.
 */
public class movieRow extends Row {

    // just take in the movie id and title
    // length checks are done during insertRow()
    public movieRow(byte[] movieId, byte[] title) {
        this.movieId = movieId;
        this.title = title;
    }

}
