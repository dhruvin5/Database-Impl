package Row;

// Uses the Row class to represent a row in the join operation between the movie and work datasets
public class joinRow1 extends Row {
    public joinRow1(byte[] movieId, byte[] title, byte[] personId) {
        this.movieId = movieId;
        this.title = title;
        this.personId = personId;
    }

}
