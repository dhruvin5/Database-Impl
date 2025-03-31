package Bplus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import Page.Page;
import Row.leafRow;
import Row.nonLeafRow;
import buffer.BufferManager;

public class BplusTreeImplem<K extends Comparable<K>> implements BplusTree<K, Rid> {
    final String indexFile;
    private int rootPageId;
    private BufferManager bm;
    private final int order; // Degree of the B+ Tree
    private final int maxKeys; // Max keys in a node before split

    public BplusTreeImplem(String indexFile, BufferManager bm, int order) throws IOException {
        this.indexFile = indexFile;
        this.bm = bm;
        this.order = order;
        this.maxKeys = order - 1;

        File file = new File(indexFile);
        if (!file.exists()) {
            this.rootPageId = bm.createPage(indexFile).getPid();
            bm.unpinPage(rootPageId, indexFile);
            writeNode(new BplusTreeNode<>(true), rootPageId);
        }
    }

    // Serializes and writes a B+ Tree node to the index file
    private void writeNode(BplusTreeNode<K> node, int pageId) throws IOException {
        Page page = bm.getPage(pageId, indexFile);
        bm.unpinPage(pageId, indexFile);

        if (node.isLeaf) {
            for (int i = 0; i < node.keys.size(); i++) {
                Rid rowId = node.values.get(i);

                // Convert pageId to bytes
                byte[] pid = ByteBuffer.allocate(4).putInt(rowId.pageId).array();
                // Convert slotId to bytes
                byte[] slot_id = ByteBuffer.allocate(4).putInt(rowId.slotId).array();
                // Serialize key
                byte[] keyBytes = serializeKey(node.keys.get(i));

                // Insert into page
                page.insertRow(new leafRow(keyBytes, pid, slot_id));
            }
            if (node.next != null) {
                page.setNextPointer(node.next);
            }
        } else {
            for (int i = 0; i < node.keys.size(); i++) {
                byte[] keyBytes = serializeKey(node.keys.get(i));
                byte[] childPointer = ByteBuffer.allocate(4).putInt(node.children.get(i + 1)).array();
                page.insertRow(new nonLeafRow(keyBytes, childPointer));
            }
        }

        bm.markDirty(pageId, indexFile);
    }

    // Convert a single K key to byte[]
    private byte[] serializeKey(K key) throws IOException {
        if (key instanceof Integer) {
            return ByteBuffer.allocate(4).putInt((Integer) key).array();
        } else if (key instanceof Double) {
            return ByteBuffer.allocate(8).putDouble((Double) key).array();
        } else if (key instanceof String) {
            byte[] strBytes = ((String) key).getBytes();
            ByteBuffer buffer = ByteBuffer.allocate(4 + strBytes.length);
            buffer.putInt(strBytes.length); // Store length first
            buffer.put(strBytes);
            return buffer.array();
        } else {
            // Generic Serialization (Ensure K implements Serializable)
            return serializeUsingObjectStream(key);
        }
    }

    // Generic serialization for unknown types
    private byte[] serializeUsingObjectStream(K key) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(key);
            oos.flush();
            return bos.toByteArray();
        }
    }

    // Reads a B+ Tree node from disk
    private BplusTreeNode<K> readNode(int pageId) throws IOException, ClassNotFoundException {
        Page page = bm.getPage(pageId, indexFile);
        byte[] data = page.getRows();
        ByteBuffer buffer = ByteBuffer.wrap(data);

        // Determine if it's a leaf node based on the presence of slot_id in the row
        boolean isLeaf = dataContainsSlotId(data);
        BplusTreeNode<K> node = new BplusTreeNode<>(isLeaf);

        int keyCount = buffer.getInt(); // First, read the number of keys
        for (int i = 0; i < keyCount; i++) {
            int keySize = buffer.getInt(); // Read key size
            byte[] keyBytes = new byte[keySize];
            buffer.get(keyBytes);
            K key = deserializeKey(keyBytes, node);
            node.keys.add(key);
        }

        if (isLeaf) {
            int valueCount = buffer.getInt(); // Read number of values
            for (int i = 0; i < valueCount; i++) {
                int pageID = buffer.getInt();
                int slotID = buffer.getInt();
                node.values.add(new Rid(pageID, slotID));
            }
            node.next = buffer.getInt(); // Read next pointer
        } else {
            int childCount = buffer.getInt(); // Read number of child pointers
            for (int i = 0; i < childCount; i++) {
                node.children.add(buffer.getInt());
            }
        }

        return node;
    }

    // Helper method to check if data contains slot_id
    private boolean dataContainsSlotId(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int keyCount = buffer.getInt();
        if (keyCount == 0) {
            return true;
        }
        for (int i = 0; i < keyCount; i++) {
            int keySize = buffer.getInt();
            buffer.position(buffer.position() + keySize);
        }
        return buffer.remaining() > 0 && buffer.getInt() > 0; // If slot_id exists, it's a leaf node
    }

    // Deserialize key from byte[] to K
    private K deserializeKey(byte[] keyBytes, BplusTreeNode<K> node) throws IOException, ClassNotFoundException {
        ByteBuffer buffer = ByteBuffer.wrap(keyBytes);
        Class<K> keyClass = !node.keys.isEmpty() ? (Class<K>) node.keys.get(0).getClass() : null;

        if (keyClass == Integer.class) {
            return keyClass.cast(buffer.getInt());
        } else if (keyClass == Double.class) {
            return keyClass.cast(buffer.getDouble());
        } else if (keyClass == String.class) {
            int length = buffer.getInt(); // Read string length
            byte[] strBytes = new byte[length];
            buffer.get(strBytes);
            return keyClass.cast(new String(strBytes));
        } else {
            // Generic deserialization
            return deserializeUsingObjectStream(keyBytes);
        }
    }

    // Generic deserialization for unknown types
    private K deserializeUsingObjectStream(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (K) ois.readObject();
        }
    }

    // Inserts a key-value pair into the B+ tree
    @Override
    public void insert(K key, Rid rid) {
        try {
            BplusTreeNode<K> root = readNode(rootPageId);

            // If root is full, create a new root
            if (root.keys.size() >= this.maxKeys) {
                BplusTreeNode<K> newRoot = new BplusTreeNode<>(false);
                int newRootPageId = bm.createPage(indexFile).getPid();
                newRoot.children.add(rootPageId);

                splitChild(newRoot, 0, root);
                rootPageId = newRootPageId;
                writeNode(newRoot, newRootPageId);
            }

            insertNonFull(rootPageId, key, rid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Handles insertion in a non-full node
    private void insertNonFull(int pageId, K key, Rid rid) throws IOException, ClassNotFoundException {
        BplusTreeNode<K> node = readNode(pageId);
        if (node.isLeaf) {
            int i = Collections.binarySearch(node.keys, key);
            if (i < 0)
                i = -(i + 1);
            node.keys.add(i, key);
            node.values.add(i, rid);
            writeNode(node, pageId);
        } else {
            int i = Collections.binarySearch(node.keys, key);
            if (i < 0)
                i = -(i + 1);
            BplusTreeNode<K> child = readNode(node.children.get(i));
            if (child.keys.size() >= maxKeys) {
                splitChild(node, i, child);
                if (key.compareTo(node.keys.get(i)) > 0) {
                    i++;
                }
            }
            insertNonFull(node.children.get(i), key, rid);
        }
    }

    // Splits a child node into two when it overflows
    private void splitChild(BplusTreeNode<K> parent, int index, BplusTreeNode<K> child) throws IOException {
        BplusTreeNode<K> sibling = new BplusTreeNode<>(child.isLeaf);
        int newPageId = bm.createPage(indexFile).getPid();
        int mid = this.maxKeys / 2; // Middle key index

        // Move half the keys to the sibling
        sibling.keys.addAll(child.keys.subList(mid + 1, child.keys.size()));
        child.keys.subList(mid, child.keys.size()).clear(); // Remove moved keys

        if (child.isLeaf) {
            sibling.values.addAll(child.values.subList(mid, child.values.size()));
            child.values.subList(mid, child.values.size()).clear();

            // Maintain linked list structure
            sibling.next = child.next;
            child.next = newPageId;
        } else {
            sibling.children.addAll(child.children.subList(mid + 1, child.children.size()));
            child.children.subList(mid + 1, child.children.size()).clear();
        }

        // Move the median key to the parent
        parent.keys.add(index, child.keys.remove(mid));
        parent.children.add(index + 1, newPageId);

        // Write updated nodes back to disk
        writeNode(child, parent.children.get(index));
        writeNode(sibling, newPageId);
        writeNode(parent, rootPageId);
    }

    // Searches for a key in the B+ tree and returns an iterator over the matching
    // records
    @Override
    public Iterator<Rid> search(K key) {
        List<Rid> matchingRids = new ArrayList<>();
        try {
            BplusTreeNode<K> node = readNode(rootPageId);
            while (!node.isLeaf) {
                int i = Collections.binarySearch(node.keys, key);
                if (i < 0)
                    i = -(i + 1);
                node = readNode(node.children.get(i));
            }

            // Collect all values with the matching key
            int i = Collections.binarySearch(node.keys, key);
            if (i >= 0) {
                while (i < node.keys.size() && node.keys.get(i).compareTo(key) == 0) {
                    matchingRids.add(node.values.get(i));
                    i++;
                }

                // Continue to next leaf if more duplicates exist
                while (node.next != null) {
                    node = readNode(node.next);
                    i = 0;
                    while (i < node.keys.size() && node.keys.get(i).compareTo(key) == 0) {
                        matchingRids.add(node.values.get(i));
                        i++;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return matchingRids.iterator();
    }

    // Performs a range search between two keys
    @Override
    public Iterator<Rid> rangeSearch(K startKey, K endKey) {
        List<Rid> results = new ArrayList<>();

        try {
            BplusTreeNode<K> node = readNode(rootPageId);

            // Traverse to the correct starting leaf node
            while (!node.isLeaf) {
                int i = Collections.binarySearch(node.keys, startKey);
                if (i < 0)
                    i = -(i + 1);
                node = readNode(node.children.get(i));
            }

            // Traverse leaf nodes and collect values in range
            while (node != null) {
                for (int i = 0; i < node.keys.size(); i++) {
                    if (node.keys.get(i).compareTo(startKey) >= 0 && node.keys.get(i).compareTo(endKey) <= 0) {
                        results.add(node.values.get(i));
                    }
                }
                node = (node.next == null) ? null : readNode(node.next);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results.iterator();
    }

}
