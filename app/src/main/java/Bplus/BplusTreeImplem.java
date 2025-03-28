package Bplus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import Page.Page;
import Row.Row;
import buffer.BufferManager;

public class BplusTreeImplem<K extends Comparable<K>> implements BplusTree<K, Rid> {
    final String indexFile;
    private int rootPageId;
    private BufferManager bm;


     //Initializes the index file and creates the root node if it does not exist.
    public BplusTreeImplem(String indexFile, BufferManager bm) throws IOException {
        this.indexFile = indexFile;
        this.bm = bm;
        File file = new File(indexFile);
        
        if (!file.exists()) {
            // Create root page if it doesn't exist
            this.rootPageId = bm.createPage().getPid();
            writeNode(new BplusTreeNode<>(true), rootPageId); // Write an empty leaf node
        }
    }


    
     // Serializes and writes a B+ Tree node to the index file, ensures that any updates to the node are persisted.
    
    private void writeNode(BplusTreeNode<K> node, int pageId) throws IOException {
        byte[] keysData = serializeNode(node.keys);
        byte[] valuesData = serializeNode(node.values);
        Row row = new Row(keysData, valuesData);
        
        Page page = bm.getPage(indexFile,pageId);
        page.insertRow(row);
        bm.markDirty(indexFile,pageId);  // Mark the page as modified
        bm.flushPage(indexFile,pageId);   // Ensure it is written to disk
    }

    //Reads a B+ Tree node from disk.
    
    private BplusTreeNode<K> readNode(int pageId) throws IOException, ClassNotFoundException {
        Page page = bm.getPage(indexFile,pageId);
        byte[] data = page.getRows();
        return deserializeNode(data);
    }

    
     //Serializes an object into a byte array for storage.
    private byte[] serializeNode(Object obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(obj);
        return bos.toByteArray();
    }

    
    //Deserializes a byte array into a B+ Tree node.
     
    private BplusTreeNode<K> deserializeNode(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream in = new ObjectInputStream(bis);
        return (BplusTreeNode<K>) in.readObject();
    }

    
     //Inserts a key-value pair into the B+ tree.
     //If the root node is full, a split occurs, and a new root is created.
     
    @Override
    public void insert(K key, Rid rid) {
        try {
            BplusTreeNode<K> root = readNode(rootPageId);
            if (root.keys.size() >= 3) { // Max keys per node before split
                BplusTreeNode<K> newRoot = new BplusTreeNode<>(false);
                int newPageId = bm.createPage().getPid();
                newRoot.children.add(rootPageId);
                splitChild(newRoot, 0, root);
                rootPageId = newPageId;
                writeNode(newRoot, newPageId);
            }
            insertNonFull(rootPageId, key, rid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
     //Handles insertion in a non-full node.
     //If a leaf node is reached, inserts the key in sorted order. If a child node is full, it is split before inserting the key.
     
    private void insertNonFull(int pageId, K key, Rid rid) throws IOException, ClassNotFoundException {
        BplusTreeNode<K> node = readNode(pageId);
        if (node.is_leaf) {
            int i = Collections.binarySearch(node.keys, key);
            if (i < 0) i = -(i + 1);
            node.keys.add(i, key);
            node.values.add(i, rid);
            writeNode(node, pageId);
        } else {
            int i = Collections.binarySearch(node.keys, key);
            if (i < 0) i = -(i + 1);
            BplusTreeNode<K> child = readNode(node.children.get(i));
            if (child.keys.size() >= 3) {
                splitChild(node, i, child);
                if (key.compareTo(node.keys.get(i)) > 0) {
                    i++;
                }
            }
            insertNonFull(node.children.get(i), key, rid);
        }
    }

    
     //Splits a child node into two when it overflows.
     //Updates the parent node accordingly.
     
    private void splitChild(BplusTreeNode<K> parent, int index, BplusTreeNode<K> child) throws IOException {
        BplusTreeNode<K> sibling = new BplusTreeNode<>(child.is_leaf);
        int newPageId = bm.createPage().getPid();
        int mid = child.keys.size() / 2;

        sibling.keys.addAll(child.keys.subList(mid + 1, child.keys.size()));
        child.keys.subList(mid, child.keys.size()).clear();

        if (child.is_leaf) {
            sibling.values.addAll(child.values.subList(mid, child.values.size()));
            child.values.subList(mid, child.values.size()).clear();
            sibling.next = child.next; // Maintain linked list structure
            child.next = newPageId;
        } else {
            sibling.children.addAll(child.children.subList(mid + 1, child.children.size()));
            child.children.subList(mid + 1, child.children.size()).clear();
        }

        parent.keys.add(index, child.keys.get(mid));
        parent.children.add(index + 1, newPageId);

        // Write child and sibling nodes and the parent node to disk
        writeNode(child, parent.children.get(index));
        writeNode(sibling, newPageId);
        writeNode(parent, rootPageId);  // Write the parent node

        // Ensure the changes are flushed to disk
        bm.flushPage(parent.children.get(index));
        bm.flushPage(newPageId);
        bm.flushPage(rootPageId);
    }

    
     //Searches for a key in the B+ tree and returns an iterator over the matching records.
     
    @Override
    public Iterator<Rid> search(K key) {
        List<Rid> matchingRids = new ArrayList<>();
        try {
            BplusTreeNode<K> node = readNode(rootPageId);
            while (!node.is_leaf) {
                int i = Collections.binarySearch(node.keys, key);
                if (i < 0) i = -(i + 1);
                node = readNode(node.children.get(i));
            }
            int i = Collections.binarySearch(node.keys, key);
            if (i >= 0) {
                // Collect all matching RIDs if the key is not unique
                while (i < node.keys.size() && node.keys.get(i).compareTo(key) == 0) {
                    matchingRids.add(node.values.get(i));
                    i++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return matchingRids.iterator();  // Return all matching RIDs
    }

    
     //Performs a range search between two keys.
     //Returns all matching records within the range.
     
    @Override
    public Iterator<Rid> rangeSearch(K startKey, K endKey) {
        List<Rid> results = new ArrayList<>();
        try {
            BplusTreeNode<K> node = readNode(rootPageId);
            while (!node.is_leaf) {
                int i = Collections.binarySearch(node.keys, startKey);
                if (i < 0) i = -(i + 1);
                node = readNode(node.children.get(i));
            }
            while (node != null) {
                for (int i = 0; i < node.keys.size(); i++) {
                    if (node.keys.get(i).compareTo(startKey) >= 0 && node.keys.get(i).compareTo(endKey) <= 0) {
                        results.add(node.values.get(i));
                    }
                }
                node = node.children.isEmpty() ? null : readNode(node.children.get(0));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results.iterator();
    }
}
