package Bplus;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

import Page.Page;
import Row.leafRow;
import Row.nonLeafRow;
import SystemCatalog.systemCatalog;
import SystemCatalog.tableMetaData;
import buffer.BufferManager;
import configs.Config;

public class BplusTreeImplem<K extends Comparable<K>> implements BplusTree<K, Rid> {
    final String indexFile;
    private int rootPageId;
    private BufferManager bm;
    private final int order, leafOrder; // Degree of the B+ Tree
    private final int maxKeys;
    private BplusTreeNode<K> root; // The in-memory root reference

    private final systemCatalog catalog;

    private final int index_info_page_id;

    private static class SplitResult<K> {
        final K splitKey;
        final int newPageId;

        SplitResult(K key, int pid) {
            this.splitKey = key;
            this.newPageId = pid;
        }
    }

    public BplusTreeImplem(String indexFile, BufferManager bm) throws IOException {
        this.catalog = new systemCatalog();
        this.indexFile = indexFile;
        this.bm = bm;
        this.leafOrder = maxOrderSize(true);
        this.order = maxOrderSize(false);
        this.maxKeys = order - 1;

        // this.index_info_page_id = 0;

        File file = new File(indexFile);
        if (!file.exists()) {

            index_info_page_id = bm.createIndexPage(indexFile, false).getPid();

            // Create a new "leaf" page for the root
            this.rootPageId = bm.createIndexPage(indexFile, true).getPid();
            // System.out.println("RootPage PID:-" + this.rootPageId);

            // Initialize an empty (leaf = false?) node.
            // Typically, if we say "leaf" in B+TreeNode<K>" we do "true" for leaves.
            // But your code used new BplusTreeNode<>(false) as root, so we'll keep that
            // pattern:
            BplusTreeNode<K> rootNode = new BplusTreeNode<>(true);
            writeNode(rootNode, rootPageId);

            bm.unpinPage(this.rootPageId, this.indexFile);

            writeRootNodeInfo(index_info_page_id, rootPageId);

            bm.unpinPage(this.index_info_page_id, this.indexFile);
            // this.root = rootNode;
        } else {
            index_info_page_id = 0;

            rootPageId = readRootNodeInfo(index_info_page_id);

            System.out.println("ROOT PAGE ID: " + rootPageId);
        }
    }

    private void writeRootNodeInfo(int pageId, int rootPageId) throws IOException {
        Page page = bm.getPage(pageId, indexFile);
        page.setRowCount(0);
        byte[] rootPointer = ByteBuffer.allocate(4).putInt(rootPageId).array();
        page.insertRow(new nonLeafRow(null, rootPointer));
        bm.markDirty(pageId, indexFile);
        bm.unpinPage(pageId, indexFile);
    }

    private int readRootNodeInfo(int pageId) throws IOException {
        Page page = bm.getPage(pageId, this.indexFile);
        byte[] data = page.getRows();
        int offset = catalog.getPageOffset(false);

        byte[] colBytes = Arrays.copyOfRange(data, offset + catalog.getOffsets(indexFile),
                offset + catalog.getOffsets(indexFile) + 4);
        int new_rootPageId = ByteBuffer.wrap(colBytes).getInt();

        bm.unpinPage(pageId, this.indexFile);

        return new_rootPageId;
    }

    private void writeNode(BplusTreeNode<K> node, int pageId) throws IOException {
        Page page = bm.getPage(pageId, indexFile);

        page.setRowCount(0);

        if (node.isLeaf) {
            for (int i = 0; i < node.keys.size(); i++) {
                Rid rowId = node.values.get(i);

                byte[] pid = ByteBuffer.allocate(4).putInt(rowId.pageId).array();
                byte[] slot_id = ByteBuffer.allocate(4).putInt(rowId.slotId).array();
                byte[] keyBytes = serializeKey(node.keys.get(i));

                page.insertRow(new leafRow(keyBytes, pid, slot_id));
            }

            if (node.next != null) {
                page.setNextPointer(node.next);
            }
        } else {
            for (int i = 0; i < node.keys.size(); i++) {

                if (i == 0) {
                    byte[] childPointer = ByteBuffer.allocate(4).putInt(node.children.get(i)).array();
                    page.insertRow(new nonLeafRow(null, childPointer));
                }

                byte[] keyBytes = serializeKey(node.keys.get(i));
                byte[] childPointer = ByteBuffer.allocate(4).putInt(node.children.get(i + 1)).array();

                page.insertRow(new nonLeafRow(keyBytes, childPointer));
            }

        }
        bm.markDirty(pageId, indexFile);
        bm.unpinPage(pageId, indexFile);
    }

    private byte[] serializeKey(K key) throws IOException {
        byte[] keyBytes = ((String) key).getBytes(StandardCharsets.UTF_8);
        return keyBytes;
    }

    private byte[] serializeUsingObjectStream(K key) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(key);
            oos.flush();
            return bos.toByteArray();
        }
    }

    private BplusTreeNode<K> readNode(int pageId) throws IOException, ClassNotFoundException {
        Page page = bm.getPage(pageId, this.indexFile);
        byte[] data = page.getRows();

        // System.out.println("CALLING PAGE ID: " + pageId);

        int totalRows = page.getRowCount();

        ArrayList<String> columns = this.catalog.getTableMetaData(this.indexFile).getColumnNames();

        boolean isLeaf = page.getBoolValue();
        BplusTreeNode<K> node = new BplusTreeNode<>(isLeaf);

        int offset = catalog.getPageOffset(isLeaf);

        if (!isLeaf) {

            byte[] colBytes = Arrays.copyOfRange(data, offset + catalog.getOffsets(indexFile),
                    offset + catalog.getOffsets(indexFile) + 4);
            int pid = ByteBuffer.wrap(colBytes).getInt();

            if (pid != index_info_page_id) {
                node.children.add(pid);
            }

            offset += catalog.getOffsets(indexFile) + 4;
            totalRows--;
            columns.remove(columns.size() - 1);
        } else {
            byte[] colBytes = Arrays.copyOfRange(data, offset, offset + 4);
            node.next = ByteBuffer.wrap(colBytes).getInt();
            offset += 4;
        }

        // System.out.println(offset);

        for (int r = 0; r < totalRows; r++) {
            Map<String, byte[]> columnMap = new HashMap<>();

            for (String columnName : columns) {

                int colSize = this.catalog.getTableMetaData(this.indexFile).getColumnSize(columnName);
                byte[] colBytes = Arrays.copyOfRange(data, offset, offset + colSize);

                offset += colSize;
                columnMap.put(columnName, colBytes);
            }

            byte[] keyBytes = columnMap.get(catalog.getIndex(indexFile).getKey());
            if (keyBytes != null) {
                K key = (K) new String(keyBytes, StandardCharsets.UTF_8);
                node.keys.add(key);
            }

            byte[] pageIdBytes = columnMap.get("pid");
            int pid = ByteBuffer.wrap(pageIdBytes).getInt();

            if (isLeaf) {
                byte[] slotIdBytes = columnMap.get("slotID");
                int slotId = ByteBuffer.wrap(slotIdBytes).getInt();
                node.values.add(new Rid(pid, slotId));
            } else {

                node.children.add(pid);
            }
        }
        bm.unpinPage(pageId, indexFile);
        return node;
    }

    @SuppressWarnings("unchecked")
    private K deserializeKey(byte[] keyBytes) throws IOException {
        if (keyBytes.length == 4) {
            return (K) Integer.valueOf(ByteBuffer.wrap(keyBytes).getInt());
        } else if (keyBytes.length == 8) {
            return (K) Double.valueOf(ByteBuffer.wrap(keyBytes).getDouble());
        } else {
            ByteBuffer buffer = ByteBuffer.wrap(keyBytes);
            int strLen = buffer.getInt();
            byte[] strBytes = new byte[strLen];
            buffer.get(strBytes);
            return (K) new String(strBytes);
        }
    }

    @SuppressWarnings("unchecked")
    private K deserializeUsingObjectStream(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (K) ois.readObject();
        }
    }

    @Override
    public void insert(K key, Rid rid) {
        try {
            SplitResult<K> result = insertRecursiveFunc(rootPageId, key, rid);
            if (result != null) {
                BplusTreeNode<K> newRoot = new BplusTreeNode<>(false);
                int newRootPageId = bm.createPage(indexFile).getPid();

                newRoot.children.add(rootPageId);

                newRoot.keys.add(result.splitKey);

                newRoot.children.add(result.newPageId);

                rootPageId = newRootPageId;

                writeRootNodeInfo(index_info_page_id, newRootPageId);

                writeNode(newRoot, newRootPageId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private SplitResult<K> insertRecursiveFunc(int pageId, K key, Rid rid) throws IOException, ClassNotFoundException {

        BplusTreeNode<K> node = readNode(pageId);

        if (node.isLeaf) {

            int i = Collections.binarySearch(node.keys, key);
            if (i < 0)
                i = -(i + 1);

            node.keys.add(i, key);
            node.values.add(i, rid);

            if (node.keys.size() >= this.leafOrder) {
                return splitLeafNode(node, pageId);
            } else {
                writeNode(node, pageId);
                // No split, just write node
                return null;
            }
        } else {

            int i = Collections.binarySearch(node.keys, key);
            if (i < 0)
                i = -(i + 1);

            // System.out.println(key + "%%%" + node.children + "%%%" +i);

            // System.out.println("CHILD :" + node.children.get(i));
            SplitResult<K> childSplit = insertRecursiveFunc(node.children.get(i), key, rid);

            if (childSplit == null) {
                return null;
            } else {
                K splitKey = childSplit.splitKey;
                int newPageId = childSplit.newPageId;

                int pos = Collections.binarySearch(node.keys, splitKey);
                if (pos < 0)
                    pos = -(pos + 1);

                node.keys.add(pos, splitKey);
                node.children.add(pos + 1, newPageId);

                if (node.keys.size() >= this.order) {
                    return splitInternalNode(node, pageId);
                } else {
                    writeNode(node, pageId);
                    return null;
                }
            }
        }
    }

    private SplitResult<K> splitLeafNode(BplusTreeNode<K> leaf, int leafPageId) throws IOException {

        // System.out.println("Splitting Leaf Node");
        BplusTreeNode<K> newLeaf = new BplusTreeNode<>(true);
        int newLeafPageId = bm.createIndexPage(indexFile, true).getPid();

        // Calculate the 'mid' position where we split

        if (leaf.next == -1) {
            newLeaf.next = null;
        } else {
            newLeaf.next = leaf.next;
        }
        leaf.next = newLeafPageId;
        int mid = leaf.keys.size() / 2;

        newLeaf.keys.addAll(leaf.keys.subList(mid, leaf.keys.size()));
        newLeaf.values.addAll(leaf.values.subList(mid, leaf.values.size()));

        leaf.keys.subList(mid, leaf.keys.size()).clear();
        leaf.values.subList(mid, leaf.values.size()).clear();

        writeNode(leaf, leafPageId);
        writeNode(newLeaf, newLeafPageId);

        K splitKey = newLeaf.keys.get(0);

        bm.unpinPage(newLeafPageId, indexFile);

        return new SplitResult<>(splitKey, newLeafPageId);
    }

    private SplitResult<K> splitInternalNode(BplusTreeNode<K> node, int nodePageId) throws IOException {
        BplusTreeNode<K> newNode = new BplusTreeNode<>(false);
        int newNodePageId = bm.createIndexPage(indexFile, false).getPid();

        int mid = node.keys.size() / 2;

        // System.out.println("Internal Node (before split) keys: " + node.keys);
        // System.out.println("Internal (before split) values: " + node.children);
        // The promoted key is node.keys.get(mid)
        K splitKey = node.keys.get(mid);

        // newNode gets the keys *after* the promoted key
        newNode.keys.addAll(node.keys.subList(mid + 1, node.keys.size()));
        node.keys.subList(mid, node.keys.size()).clear();

        // newNode also gets the children *after* mid
        newNode.children.addAll(node.children.subList(mid + 1, node.children.size()));
        node.children.subList(mid + 1, node.children.size()).clear();

        // System.out.println("NewLeaf (after subList add) keys: " + newNode.keys);
        // System.out.println("NewLeaf (after subList add) values: " +
        // newNode.children);

        // System.out.println("Leaf (after clear) keys: " + node.keys);
        // System.out.println("Leaf (after clear) values: " + node.children);
        // Write both
        writeNode(node, nodePageId);
        writeNode(newNode, newNodePageId);

        // System.out.println("Split key promoted to parent: " + splitKey);
        // System.out.println("Created new leaf pageId: " + newNodePageId);
        // System.out.println("=========================");

        // Return the promoted key + new node
        bm.unpinPage(newNodePageId, indexFile);
        return new SplitResult<>(splitKey, newNodePageId);
    }

    @Override

    public Iterator<Rid> search(K key) {
        List<Rid> matchingRids = new ArrayList<>();
        try {
            // Descend to the appropriate leaf node.
            BplusTreeNode<K> node = readNode(rootPageId);
            while (!node.isLeaf) {
                int i = Collections.binarySearch(node.keys, key);
                if (i < 0) {
                    i = -(i + 1);
                }
                node = readNode(node.children.get(i));
            }

            // Now in a leaf node. Use binary search to find one occurrence.
            int pos = Collections.binarySearch(node.keys, key);
            if (pos < 0) {
                // Key not present.
                return matchingRids.iterator();
            }

            // If duplicates exist, back up to the first occurrence.
            while (pos > 0 && node.keys.get(pos - 1).compareTo(key) == 0) {
                pos--;
            }

            // Collect all matching Rids in this leaf.
            while (true) {
                while (pos < node.keys.size() && node.keys.get(pos).compareTo(key) == 0) {
                    matchingRids.add(node.values.get(pos));
                    pos++;
                }
                // If there is a next leaf, check if its first key equals the search key.
                if (node.next == -1) {
                    break;
                }
                BplusTreeNode<K> nextLeaf = readNode(node.next);
                // If the next leaf's first key is not equal to key, we're done.
                if (nextLeaf.keys.isEmpty() || nextLeaf.keys.get(0).compareTo(key) != 0) {
                    break;
                }
                node = nextLeaf;
                pos = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return matchingRids.iterator();
    }

    /**
     * Performs a range search between startKey and endKey (inclusive) in the B+
     * tree
     * and returns an iterator over the Rid values found.
     */
    @Override
    public Iterator<Rid> rangeSearch(K startKey, K endKey) {
        List<Rid> results = new ArrayList<>();
        try {
            BplusTreeNode<K> node = readNode(rootPageId);
            while (!node.isLeaf) {
                int i = Collections.binarySearch(node.keys, startKey);
                if (i < 0) {
                    i = -(i + 1);
                }
                node = readNode(node.children.get(i));
            }

            int pos = Collections.binarySearch(node.keys, startKey);
            if (pos < 0) {
                pos = -(pos + 1);
            }

            while (node != null) {
                for (; pos < node.keys.size(); pos++) {
                    K currentKey = node.keys.get(pos);
                    if (currentKey.compareTo(endKey) > 0) {
                        return results.iterator();
                    }
                    if (currentKey.compareTo(startKey) >= 0 && currentKey.compareTo(endKey) <= 0) {
                        results.add(node.values.get(pos));
                    }
                }
                if (node.next == -1) {
                    break;
                }
                node = readNode(node.next);
                pos = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results.iterator();
    }

    public void printTree() {
        try {
            printNode(rootPageId, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printNode(int pageId, int level) throws IOException, ClassNotFoundException {
        BplusTreeNode<K> node = readNode(pageId);
        String indent = new String(new char[level]).replace("\0", "    ");

        if (node.isLeaf) {
            System.out.println(indent + "Leaf Node (Page " + pageId + "):");
            System.out.println(indent + "  Keys: " + node.keys);
            System.out.println(indent + "  Values: " + node.values);
            System.out.println(indent + "  Next: " + node.next);
        } else {
            System.out.println(indent + "Internal Node (Page " + pageId + "):");
            System.out.println(indent + "  Keys: " + node.keys);
            System.out.println(indent + "  Children: ");
            for (int i = 0; i < node.children.size(); i++) {
                int childPageId = node.children.get(i);
                System.out.println(indent + "    Child " + i + " (Page " + childPageId + "):");
                printNode(childPageId, level + 2);
            }
        }
    }

    private int maxOrderSize(boolean isLeaf) {
        tableMetaData data = catalog.getTableMetaData(indexFile);
        int KEY_SIZE = catalog.getOffsets(indexFile);
        int PID_SIZE = data.getColumnSize("pid");
        int ROW_COUNT_SIZE = 4;
        int BOOL_SIZE = 1;

        if (isLeaf) {
            int NEXT_LEAF_POINTER = 4;
            int SLOT_ID_SIZE = data.getColumnSize("slotID");
            int ROW_SIZE = KEY_SIZE + PID_SIZE + SLOT_ID_SIZE;
            return (Config.PAGE_SIZE - ROW_COUNT_SIZE - BOOL_SIZE - NEXT_LEAF_POINTER) / ROW_SIZE;
        }

        int ROW_SIZE = KEY_SIZE + PID_SIZE;
        return (Config.PAGE_SIZE - ROW_COUNT_SIZE - BOOL_SIZE) / ROW_SIZE;
    }

}