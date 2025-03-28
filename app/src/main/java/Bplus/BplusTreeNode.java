package Bplus;

import java.util.ArrayList;
import java.util.List;

public class BplusTreeNode<K extends Comparable<K>> {
    boolean is_leaf;
    List<K> keys;
    List<Integer> children; // Page IDs of child nodes
    List<Rid> values; // Only for leaf nodes
    Integer next; // Next leaf node for linked list structure

    public BplusTreeNode(boolean is_leaf) {
        this.is_leaf = is_leaf;
        this.keys = new ArrayList<>();
        this.children = new ArrayList<>();
        this.values = new ArrayList<>();
        this.next = null; // Initially no next node
    }
}
