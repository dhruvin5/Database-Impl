package Row;

public class nonLeafRow extends Row {
    /*
     * Non-leaf row class for B+ tree
     * Contains key and page ID for the child node
     */
    public nonLeafRow(byte[] key, byte[] pid) {
        this.key = key;
        this.pid = pid;
    }
}
