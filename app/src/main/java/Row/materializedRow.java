package Row;

// Uses the Row class to represent a row in the materialized view dataset
public class materializedRow extends Row {
    public materializedRow(byte[] movieId, byte[] personId) {
        this.movieId = movieId;
        this.personId = personId;
    }
}
