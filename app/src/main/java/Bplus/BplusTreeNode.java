package Bplus;

import java.util.ArrayList;
import java.util.List;

public class BplusTreeNode<K extends Comparable<K>> {
    boolean isLeaf;
    List<K> keys;           // Stores the keys
    //List<Integer> pageIds;  // Stores the page ID of the current node
    List<Integer> children; // Stores the page numbers of child nodes (only for internal nodes)
    List<Rid> values;       // Stores record pointers (only for leaf nodes)
    Integer next;           // Pointer to the next leaf node (only for leaf nodes)

    public BplusTreeNode(boolean isLeaf) {
        this.isLeaf = isLeaf;
        this.keys = new ArrayList<>();
        //this.pageIds = new ArrayList<>();

        if (isLeaf) {
            this.values = new ArrayList<>();
            this.children = null; // Not used in leaf nodes
            this.next = null; // Used only for leaf nodes
        } else {
            this.children = new ArrayList<>();
            this.values = null; // Not used in internal nodes
            this.next = null; // Not needed in internal nodes
        }
    }
}