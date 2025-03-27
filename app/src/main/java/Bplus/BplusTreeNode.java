package Bplus;

import java.util.ArrayList;
import java.util.List;

public class BplusTreeNode<K extends Comparable<K>> {
    boolean is_leaf;
    List<K> keys;
    List<Integer> children; // Page IDs of child nodes
    List<Rid> values; // Only for leaf nodes

    public BplusTreeNode(boolean is_leaf) {
        this.is_leaf = is_leaf;
        this.keys = new ArrayList<>();
        this.children = new ArrayList<>();
        this.values = new ArrayList<>();
    }
}

