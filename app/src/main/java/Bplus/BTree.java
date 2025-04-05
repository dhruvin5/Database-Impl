package Bplus;
import java.util.Iterator;

public interface BTree<K extends Comparable<K>, V> {
    void insert(K key, Rid rid);
    Iterator<Rid> search(K key);
    Iterator<Rid> rangeSearch(K startKey, K endKey);

    void printTree();
}

