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
    private final String indexFile;
    private int rootPageId;
    private BufferManager bm;

    public BplusTreeImplem(String indexFile, BufferManager bufferManager) throws IOException {
        this.indexFile = indexFile;
        this.bm = bm;
        File file = new File(indexFile);
        if (!file.exists()) {
            // Use BufferManager's createPage method for page allocation
            this.rootPageId = bufferManager.createPage().getPid();
            writeNode(new BplusTreeNode<>(true), rootPageId); // Write the initial empty leaf node
        }
    }

    private void writeNode(BplusTreeNode<K> node, int pageId) throws IOException {
        // Serialize the keys and values separately for Row constructor
        byte[] keysData = serializeNode(node.keys);
        byte[] valuesData = serializeNode(node.values);

        // Create a Row object with both byte arrays (keys and values)
        Row row = new Row(keysData, valuesData);

        // Get the Page and insert the Row
        Page page = bm.getPage(pageId);
        page.insertRow(row);
        bm.markDirty(pageId);
    }

    private BplusTreeNode<K> readNode(int pageId) throws IOException, ClassNotFoundException {
        Page page = bm.getPage(pageId);
        byte[] data = page.getRows();
        return deserializeNode(data);
    }

    private byte[] serializeNode(Object obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(obj);
        return bos.toByteArray();
    }

    private BplusTreeNode<K> deserializeNode(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream in = new ObjectInputStream(bis);
        return (BplusTreeNode<K>) in.readObject();
    }

    @Override
    public void insert(K key, Rid rid) {
        try {
            BplusTreeNode<K> root = readNode(rootPageId);
            if (root.keys.size() >= 3) { // setting order: 3
                BplusTreeNode<K> newRoot = new BplusTreeNode<>(false);
                int newPageId = bm.createPage().getPid(); // Use createPage for new root page
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

    private void splitChild(BplusTreeNode<K> parent, int index, BplusTreeNode<K> child) throws IOException {
        BplusTreeNode<K> sibling = new BplusTreeNode<>(child.is_leaf);
        int newPageId = bm.createPage().getPid(); // Create a new page for the sibling
        int mid = child.keys.size() / 2;
        
        sibling.keys.addAll(child.keys.subList(mid + 1, child.keys.size()));
        child.keys.subList(mid, child.keys.size()).clear();
        
        if (child.is_leaf) {
            sibling.values.addAll(child.values.subList(mid + 1, child.values.size()));
            child.values.subList(mid, child.values.size()).clear();
        } else {
            sibling.children.addAll(child.children.subList(mid + 1, child.children.size()));
            child.children.subList(mid + 1, child.children.size()).clear();
        }

        parent.keys.add(index, child.keys.get(mid));
        parent.children.add(index + 1, newPageId);
        writeNode(child, parent.children.get(index));
        writeNode(sibling, newPageId);
    }

    @Override
    public Iterator<Rid> search(K key) {
        try {
            BplusTreeNode<K> node = readNode(rootPageId);
            while (!node.is_leaf) {
                int i = Collections.binarySearch(node.keys, key);
                if (i < 0) i = -(i + 1);
                node = readNode(node.children.get(i));
            }
            int i = Collections.binarySearch(node.keys, key);
            if (i >= 0) {
                return Collections.singletonList(node.values.get(i)).iterator();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyIterator();
    }

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
