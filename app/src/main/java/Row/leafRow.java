package Row;

public class leafRow extends Row {
    // Leaf row class for leaf index pages
    public leafRow(byte[] key, byte[] pid, byte[] slotid) {
        this.key = key;
        this.pid = pid;
        this.slotid = slotid;
    }
}
