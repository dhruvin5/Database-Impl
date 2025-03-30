package Row;

public class nonLeafRow extends Row {
    // Non-leaf row class for non-leaf index pages
    public nonLeafRow(byte[] key, byte[] pid) {
        this.key = key;
        this.pid = pid;
    }
}
