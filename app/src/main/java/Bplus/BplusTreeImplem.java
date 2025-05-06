package Bplus;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import Page.Page;
import Row.leafRow;
import Row.nonLeafRow;
import SystemCatalog.systemCatalog;
import SystemCatalog.tableMetaData;
import buffer.BufferManager;
import configs.Config;

public class BplusTreeImplem<K extends Comparable<K>> implements BTree<K, Rid> {
    final String indexFile; // name of the index file corresponding to the index key type
    private int rootPageId;// index page id of the root
    private BufferManager bm;
    private final int order, leafOrder; // Bplus tree degree

    private BplusTreeNode<K> root; // The in-memory root reference

    private final systemCatalog catalog;

    private final int index_info_page_id;

    // saving the result after a node split
    private static class InternalNodeSplit<K> {
        final K splitKey;
        final int newPageId;

        InternalNodeSplit(K key, int pid) {
            this.splitKey = key;
            this.newPageId = pid;
        }
    }

    // initialisation of a new Index Bplus tree
    public BplusTreeImplem(String indexFile, BufferManager bm) throws IOException {
        this.catalog = new systemCatalog();
        this.indexFile = indexFile;
        this.bm = bm;
        this.leafOrder = maxOrderSize(true);
        this.order = maxOrderSize(false);

        // deleting previous index files if they exist
        File file = new File(indexFile);
        if (file.exists()) {
            this.index_info_page_id = 0;

            this.rootPageId = readRootNodeInfo(this.index_info_page_id);

            System.out.println("ROOT PAGE ID: " + this.rootPageId);
        } else {

            // creating a page to store a pointer to the root node
            index_info_page_id = bm.createIndexPage(indexFile, false).getPid();

            // creating root node.
            this.rootPageId = bm.createIndexPage(indexFile, true).getPid();
            BplusTreeNode<K> rootNode = new BplusTreeNode<>(true);
            writeNode(rootNode, rootPageId);

            bm.unpinPage(this.rootPageId, this.indexFile);
            writeRootNodeInfo(index_info_page_id, rootPageId);
            bm.unpinPage(this.index_info_page_id, this.indexFile);
        }

    }

    // storing info of root
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

        byte[] colBytes = Arrays.copyOfRange(data, offset + 30, offset + 34);
        int new_rootPageId = ByteBuffer.wrap(colBytes).getInt();

        bm.unpinPage(pageId, this.indexFile);

        return new_rootPageId;
    }

    static String stripAccents(String s) {
        // 1) decompose: "á" → "a\u0301"
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        // 2) remove all combining marks (the \p{M} class)
        return n.replaceAll("\\p{M}", "");
    }

    // writing serialised node data to disk index file
    private void writeNode(BplusTreeNode<K> node, int pageId) throws IOException {
        Page page = bm.getPage(pageId, indexFile);

        page.setRowCount(0);

        if (node.isLeaf) {
            for (int i = 0; i < node.keys.size(); i++) {
                // writing leaf node
                Rid rowId = node.values.get(i);

                byte[] pid = ByteBuffer.allocate(4).putInt(rowId.pageId).array();
                byte[] slot_id = ByteBuffer.allocate(4).putInt(rowId.slotId).array();
                byte[] keyBytes = serializeKey(node.keys.get(i));

                page.insertRow(new leafRow(keyBytes, pid, slot_id));
            }
            // updating next of leaf if it exists
            if (node.next != null) {
                page.setNextPointer(node.next);
            }
        } else {
            // writing internal node
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

    // serialising the key
    private byte[] serializeKey(K key) throws IOException {
        byte[] keyBytes = ((String) key).getBytes(StandardCharsets.UTF_8);
        return keyBytes;
    }

    // reading node from disk index file
    private BplusTreeNode<K> readNode(int pageId) throws IOException, ClassNotFoundException {
        Page page = bm.getPage(pageId, this.indexFile);
        byte[] data = page.getRows();

        int totalRows = page.getRowCount();

        ArrayList<String> columns = this.catalog.getTableMetaData(this.indexFile).getColumnNames();

        boolean isLeaf = page.getBoolValue();
        BplusTreeNode<K> node = new BplusTreeNode<>(isLeaf);

        int offset = catalog.getPageOffset(isLeaf);
        // reading metadata of internal node page and updating child pointers
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
        }
        // reading next field for leaf node
        else {
            byte[] colBytes = Arrays.copyOfRange(data, offset, offset + 4);
            node.next = ByteBuffer.wrap(colBytes).getInt();
            offset += 4;
        }
        // reading all rows/records in node
        for (int r = 0; r < totalRows; r++) {
            Map<String, byte[]> columnMapping = new HashMap<>();

            for (String columnName : columns) {

                int colSize = this.catalog.getTableMetaData(this.indexFile).getColumnSize(columnName);
                byte[] colBytes = Arrays.copyOfRange(data, offset, offset + colSize);
                offset += colSize;
                columnMapping.put(columnName, colBytes);
            }
            byte[] keyBytes = columnMapping.get(catalog.getIndex(indexFile).getKey());
            if (keyBytes != null) {
                K key = (K) new String(keyBytes, StandardCharsets.UTF_8);
                node.keys.add(key);
            }
            byte[] pageIdBytes = columnMapping.get("pid");
            int pid = ByteBuffer.wrap(pageIdBytes).getInt();
            if (isLeaf) {
                byte[] slotIdBytes = columnMapping.get("slotID");
                int slotId = ByteBuffer.wrap(slotIdBytes).getInt();
                node.values.add(new Rid(pid, slotId));
            } else {
                node.children.add(pid);
            }
        }
        bm.unpinPage(pageId, indexFile);
        return node;
    }

    // inserting records into nodes of Bplus tree
    @Override
    public void insert(K key, Rid rid) {
        try {
            InternalNodeSplit<K> result = insertRecursiveFunc(rootPageId, key, rid);
            // if split occurs, creating a new internal node
            if (result != null) {
                BplusTreeNode<K> newRoot = new BplusTreeNode<>(false);
                int newRootPageId = bm.createPage(indexFile).getPid();
                newRoot.children.add(rootPageId); // updating left child pageID
                newRoot.keys.add(result.splitKey); // key on which split is happening
                newRoot.children.add(result.newPageId); // updating right child pageID
                rootPageId = newRootPageId; // updating root
                writeRootNodeInfo(index_info_page_id, newRootPageId);
                writeNode(newRoot, newRootPageId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // method for recursive splits for insertion
    private InternalNodeSplit<K> insertRecursiveFunc(int pageId, K key, Rid rid)
            throws IOException, ClassNotFoundException {
        BplusTreeNode<K> node = readNode(pageId);
        if (node.isLeaf) {
            int i = binarySearch(node.keys, key, true);
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
            int i = binarySearch(node.keys, key, true);// getting insertion point for key
            InternalNodeSplit<K> childSplit = insertRecursiveFunc(node.children.get(i), key, rid);
            if (childSplit == null) {
                return null;
            } else {
                K splitKey = childSplit.splitKey;
                int newPageId = childSplit.newPageId;

                int positionToInsert = binarySearch(node.keys, splitKey, true); // if (pos < 0) pos = -(pos + 1);
                node.keys.add(positionToInsert, splitKey);
                node.children.add(positionToInsert + 1, newPageId);
                if (node.keys.size() >= this.order) {
                    return splitInternalNode(node, pageId);
                } else {
                    writeNode(node, pageId);
                    return null;
                }
            }
        }
    }

    // method for splitting leaf into 2,and returns promoted the median key, with
    // corresponding page id to next internal node
    private InternalNodeSplit<K> splitLeafNode(BplusTreeNode<K> leaf, int leafPageId) throws IOException {
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

        return new InternalNodeSplit<>(splitKey, newLeafPageId);
    }

    // method for splitting an internal node and returning promoted the median key,
    // with corresponding page id
    private InternalNodeSplit<K> splitInternalNode(BplusTreeNode<K> node, int nodePageId) throws IOException {
        BplusTreeNode<K> newNode = new BplusTreeNode<>(false);
        int newNodePageId = bm.createIndexPage(indexFile, false).getPid();
        int mid = node.keys.size() / 2;
        // The promoted key is node.keys.get(mid)
        K splitKey = node.keys.get(mid);
        // newNode gets the keys after the promoted key
        newNode.keys.addAll(node.keys.subList(mid + 1, node.keys.size()));
        node.keys.subList(mid, node.keys.size()).clear();
        // newNode also gets the children after mid
        newNode.children.addAll(node.children.subList(mid + 1, node.children.size()));
        node.children.subList(mid + 1, node.children.size()).clear();
        // Write both
        writeNode(node, nodePageId);
        writeNode(newNode, newNodePageId);
        // Return the promoted key + new node
        bm.unpinPage(newNodePageId, indexFile);
        return new InternalNodeSplit<>(splitKey, newNodePageId);
    }

    // method for single key search. returns all matching records.
    @Override
    public Iterator<Rid> search(K key) {
        List<Rid> matchingRids = new ArrayList<>();
        try {
            BplusTreeNode<K> node = readNode(rootPageId);
            // Traverse to the correct leaf node
            while (!node.isLeaf) {
                int i = binarySearch(node.keys, key, true);

                // If the search key is found in the given set of keys then we traverse to right
                // child.
                if (i < node.keys.size() && node.keys.get(i).compareTo(key) == 0) {
                    i++;
                }

                // System.out.println("Binary search result key: " + key + " ind " + i);

                int childPageId = node.children.get(i);
                // System.out.println("Descending to child page: " + childPageId);
                node = readNode(childPageId);
            }

            // Now, 'node' is the correct leaf node.
            // System.out.println("Reached leaf node with keys: " + node.keys);
            // Perform binary search within the leaf node. checking if key is found in the
            // leaf node.
            int position = binarySearch(node.keys, key, false);
            // If the key does not exist, exit early.
            if (position < 0) {
                // System.out.println("Key not found in leaf.");
                return matchingRids.iterator();
            }

            // Move back to the first occurrence of the key.
            while (position > 0 && node.keys.get(position - 1).compareTo(key) == 0) {
                position--;
            }

            // Scan forward to collect all matching Rids.
            // System.out.println("Starting scan at position: " + pos);
            while (position < node.keys.size() && node.keys.get(position).compareTo(key) == 0) {
                // System.out.println("Found matching Rid: " + node.values.get(pos));
                matchingRids.add(node.values.get(position));
                position++;
            }

            // Continue to the next leaf node if necessary.
            while (node.next != -1) {
                node = readNode(node.next);
                if (node.keys.isEmpty() || node.keys.get(0).compareTo(key) != 0) {
                    break;
                }

                position = 0;
                while (position < node.keys.size() && node.keys.get(position).compareTo(key) == 0) {
                    // System.out.println("Found matching Rid in next leaf: " +
                    // node.values.get(pos));
                    matchingRids.add(node.values.get(position));
                    position++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return matchingRids.iterator();
    }

    // method for range key search. Returns all records in the requested range
    @Override
    public Iterator<Rid> rangeSearch(K startKey, K endKey) {

        boolean isSwapped = false;
        if (startKey.compareTo(endKey) > 0) {
            K temp = startKey;
            startKey = endKey;
            endKey = temp;
            isSwapped = true;

        }

        List<Rid> results = new ArrayList<>();
        try {
            BplusTreeNode<K> node = readNode(rootPageId);
            // Traverse to the first leaf node that might contain startKey (using search
            // logic).
            while (!node.isLeaf) {
                int i = binarySearch(node.keys, startKey, true);
                // System.out.println("Binary search result: " + i);

                if (i < node.keys.size() && node.keys.get(i).compareTo(startKey) == 0) {
                    i++;
                }
                int childPageId = node.children.get(i);
                node = readNode(childPageId);
            }
            // Find the starting position within the leaf node.
            int pos = binarySearch(node.keys, startKey, true);
            // Scan through leaf nodes until endKey is exceeded.
            while (node != null) {
                // System.out.println("Scanning leaf node: " + node.keys);
                for (; pos < node.keys.size(); pos++) {
                    K currentKey = node.keys.get(pos);
                    String cur = stripAccents(currentKey.toString()).toLowerCase(Locale.ROOT).trim();
                    String lo = stripAccents(startKey.toString()).toLowerCase(Locale.ROOT).trim();
                    String hi = stripAccents(endKey.toString()).toLowerCase(Locale.ROOT).trim();

                    // Modified line 424 logic
                    if (cur.compareTo(hi) > 0) {
                        if (isSwapped) {
                            List<Rid> reversedResults = new ArrayList<>(results);
                            Collections.reverse(reversedResults);
                            return reversedResults.iterator();
                        }
                        return results.iterator();
                    }

                    // Modified line 435 logic
                    if (cur.compareTo(lo) >= 0 && cur.compareTo(hi) <= 0) {
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
        if (isSwapped) {
            List<Rid> reversedResults = new ArrayList<>(results);
            Collections.reverse(reversedResults);
            return reversedResults.iterator();
        }
        return results.iterator();
    }

    // method for printing the bplus tree. traversal starts from root.
    public void printTree() {
        try {
            printNode(rootPageId, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // method for pinning requested level of bplus tree
    public void pinLevels(int level) {
        try {
            pinFirst2LevelsHelper(rootPageId, level, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // method for unpinning requested level of bplus tree
    public void unPinLevels(int level) {
        try {
            unPinFirst2LevelsHelper(rootPageId, level, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // method for pinning first 2 levels of bplus tree
    private void pinFirst2LevelsHelper(int pageId, int level, int currLevel)
            throws IOException, ClassNotFoundException {

        if (currLevel < level) {
            BplusTreeNode<K> node = readNode(pageId);
            for (int i = 0; i < node.children.size(); i++) {
                int childPageId = node.children.get(i);
                pinFirst2LevelsHelper(childPageId, level, currLevel + 1);
            }
        }
        bm.getPage(pageId, this.indexFile);
    }

    // method for pinning first 2 levels of bplus tree
    private void unPinFirst2LevelsHelper(int pageId, int level, int currLevel)
            throws IOException, ClassNotFoundException {

        if (currLevel < level) {
            BplusTreeNode<K> node = readNode(pageId);
            for (int i = 0; i < node.children.size(); i++) {
                int childPageId = node.children.get(i);
                unPinFirst2LevelsHelper(childPageId, level, currLevel + 1);
            }
        }

        bm.unpinPage(pageId, indexFile);
    }

    // method to print the contents of a node(both internal and leaf)
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

    // binary search method.
    // when boolean isReturnInsertionPoint is set to true, returns the correct
    // insertion point of key
    // when set to false, returns offset of key if found, else returns -1 if key not
    // found
    private static <T extends Comparable<T>> int binarySearch(List<T> sortedList, T key,
            boolean isReturnInsertionPoint) {
        int low = 0;
        int high = sortedList.size() - 1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            int comparison = key.compareTo(sortedList.get(mid));

            if (comparison == 0) {
                return mid;
            } else if (comparison < 0) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }

        if (isReturnInsertionPoint) {
            return low;
        }
        return -1;
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