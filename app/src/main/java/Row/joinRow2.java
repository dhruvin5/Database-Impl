package Row;

// Uses the Row class to represent a row in the join operation between movie and people datasets
public class joinRow2 extends Row {
    public joinRow2(byte[] movieId, byte[] title, byte[] personId, byte[] name) {
        this.movieId = movieId;
        this.title = title;
        this.personId = personId;
        this.name = name;
    }
}
