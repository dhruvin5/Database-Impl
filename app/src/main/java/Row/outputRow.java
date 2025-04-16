package Row;

// Uses the Row class to represent a row in the output of the top projection operator
public class outputRow extends Row {
    public outputRow(byte[] title, byte[] name) {
        this.title = title;
        this.name = name;
    }

}
