package Row;

//Uses the Row class to represent a row in the work dataset
public class workRow extends Row {
    public workRow(byte[] movieId, byte[] personId, byte[] category) {
        this.movieId = movieId;
        this.personId = personId;
        this.category = category;
    }
}
