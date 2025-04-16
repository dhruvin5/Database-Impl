package Row;

// Uses the Row class to represent a row in the people dataset
public class peopleRow extends Row {
    public peopleRow(byte[] personId, byte[] name) {
        this.personId = personId;
        this.name = name;
    }
}
