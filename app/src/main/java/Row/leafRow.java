package Row;

public class leafRow extends Row {
    /*
     * Leaf row class for B+ tree
     * Contains key, page ID, and slot ID for the data entry
     */
    public leafRow(byte[] key, byte[] pid, byte[] slotid) {
        this.key = key;
        this.pid = pid;
        this.slotid = slotid;
    }
}
