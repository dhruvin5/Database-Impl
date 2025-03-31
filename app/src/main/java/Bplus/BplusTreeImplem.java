package Bplus;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

import Page.Page;
import Row.leafRow;
import Row.nonLeafRow;
import SystemCatalog.systemCatalog;
import buffer.BufferManager;

public class BplusTreeImplem<K extends Comparable<K>> implements BplusTree<K, Rid> {
    final String indexFile;
    private int rootPageId;
    private BufferManager bm;
    private final int order; // Degree of the B+ Tree
    private final int maxKeys;
    private BplusTreeNode<K> root; // The in-memory root reference

    private final systemCatalog catalog;

    /**
     * Simple helper class carrying the result of a node split:
     * the key to promote up (splitKey) and the pageId of the newly-created node (newPageId).
     */
    private static class SplitResult<K> {
        final K splitKey;
        final int newPageId;

        SplitResult(K key, int pid) {
            this.splitKey = key;
            this.newPageId = pid;
        }
    }

    public BplusTreeImplem(String indexFile, BufferManager bm, int order) throws IOException {
        this.indexFile = indexFile;
        this.bm = bm;
        this.order = order;
        this.maxKeys = order - 1;

        this.catalog = new systemCatalog();
        File file = new File(indexFile);
        if (!file.exists()) {
            // Create a new "leaf" page for the root
            this.rootPageId = bm.createIndexPage(indexFile, true).getPid();
            //System.out.println("RootPage PID:-" + this.rootPageId);

            // Initialize an empty (leaf = false?) node.
            // Typically, if we say "leaf" in B+TreeNode<K>" we do "true" for leaves.
            // But your code used new BplusTreeNode<>(false) as root, so we'll keep that pattern:
            BplusTreeNode<K> rootNode = new BplusTreeNode<>(false);
            writeNode(rootNode, rootPageId);

            bm.unpinPage(this.rootPageId, this.indexFile);
            this.root = rootNode;
        }
        else {
            // If file exists, we read the root from page 0 (as your code suggests).
            this.rootPageId = 0;
            try {
                root = readNode(rootPageId);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // ----------------------------------------------------------------
    // 1) WRITE NODE TO DISK (unchanged)
    // ----------------------------------------------------------------
    private void writeNode(BplusTreeNode<K> node, int pageId) throws IOException {
        Page page = bm.getPage(pageId, indexFile);

        page.setRowCount(0);

        //System.out.println();

        if (node.isLeaf) {
            for (int i = 0; i < node.keys.size(); i++) {
                Rid rowId = node.values.get(i);

                byte[] pid = ByteBuffer.allocate(4).putInt(rowId.pageId).array();
                byte[] slot_id = ByteBuffer.allocate(4).putInt(rowId.slotId).array();
                byte[] keyBytes = serializeKey(node.keys.get(i));

                page.insertRow(new leafRow(keyBytes, pid, slot_id));
            }

           // System.out.println("CHANGING LEAF NODES!!");
            if (node.next != null) {
                page.setNextPointer(node.next);
            }
        } else {
            for (int i = 0; i < node.keys.size(); i++) {

                if(i == 0)
                {
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

    // ----------------------------------------------------------------
    // 2) SERIALIZE KEY (unchanged)
    // ----------------------------------------------------------------
    private byte[] serializeKey(K key) throws IOException {
        byte[] keyBytes = ((String) key).getBytes(StandardCharsets.UTF_8);
        return keyBytes;
    }


    // ----------------------------------------------------------------
    // 3) (Optional) GENERIC SERIALIZATION METHOD (kept as is)
    // ----------------------------------------------------------------
    private byte[] serializeUsingObjectStream(K key) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(key);
            oos.flush();
            return bos.toByteArray();
        }
    }

    // ----------------------------------------------------------------
    // 4) READ NODE FROM DISK (unchanged from your code)
    // ----------------------------------------------------------------
    private BplusTreeNode<K> readNode(int pageId) throws IOException, ClassNotFoundException {
        Page page = bm.getPage(pageId, this.indexFile);
        byte[] data = page.getRows();

       // System.out.println("DATA LENGTH--" + data.length);
        int totalRows = page.getRowCount();

        ArrayList<String> columns = this.catalog.getTableMetaData(this.indexFile).getColumnNames();

        boolean isLeaf = page.getBoolValue();  // your code infers leaf by presence of slot_id, etc.
        BplusTreeNode<K> node = new BplusTreeNode<>(isLeaf);

        int offset = catalog.getPageOffset(isLeaf);

        if(!isLeaf)
        {
            byte[] colBytes = Arrays.copyOfRange(data, offset + 9, offset  + 13);
            int pid = ByteBuffer.wrap(colBytes).getInt();

            node.children.add(pid);
            offset += 13;
            totalRows--;
            columns.remove(columns.size() - 1);
        }
        else {
            byte[] colBytes = Arrays.copyOfRange(data, offset , offset  + 4);
            node.next  = ByteBuffer.wrap(colBytes).getInt();
            offset += 4;
        }

        for (int r = 0; r < totalRows; r++) {
            Map<String, byte[]> columnMap = new HashMap<>();

            for (String columnName : columns) {


                int colSize = this.catalog.getTableMetaData(this.indexFile).getColumnSize(columnName);
                byte[] colBytes = Arrays.copyOfRange(data, offset, offset + colSize);


                offset += colSize;
                columnMap.put(columnName, colBytes);
            }

            byte[] keyBytes = columnMap.get("movieId");
            if (keyBytes != null) {
                K key = (K) new String(keyBytes, StandardCharsets.UTF_8);
               // System.out.println("READING KEY: " + key);
                node.keys.add(key);
            }

            byte[] pageIdBytes = columnMap.get("pid");
            int pid = ByteBuffer.wrap(pageIdBytes).getInt();

            int slotId = -1;
            if(isLeaf)
            {
                byte[] slotIdBytes = columnMap.get("slotID");
                slotId = ByteBuffer.wrap(slotIdBytes).getInt();
                node.values.add(new Rid(pid, slotId));
            }
            else {
                //System.out.println("^^" + pid);
                node.children.add(pid);
            }
        }
        bm.unpinPage(pageId, indexFile);
        return node;
    }

    // ----------------------------------------------------------------
    // 5) Old Helper Method (dataContainsSlotId) - unchanged
    // ----------------------------------------------------------------
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
        return buffer.remaining() > 0 && buffer.getInt() > 0;
    }

    // ----------------------------------------------------------------
    // 6) DESERIALIZE KEY (unchanged, though not used in current code)
    // ----------------------------------------------------------------
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

    // ----------------------------------------------------------------
    // 7) GENERIC DESERIALIZE USING OBJECT STREAM (kept as is)
    // ----------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private K deserializeUsingObjectStream(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (K) ois.readObject();
        }
    }

    // ----------------------------------------------------------------
    // 8) OLD METHODS (commented out, as requested)
    // ----------------------------------------------------------------

    /*
    // public void insertFunc(int rootPageId, K key, Rid rid) {
    //     try {
    //         BplusTreeNode<K> root = readNode(rootPageId);
    //         if(root.isLeaf) {
    //             insertNonFull(rootPageId, key, rid);
    //         } else {
    //             // ...
    //         }
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     }
    // }

    // private void insertNonFull(int pageId, K key, Rid rid) throws IOException, ClassNotFoundException {
    //     // Old approach
    // }

    // private void splitChild(BplusTreeNode<K> parent, int index, BplusTreeNode<K> child) throws IOException {
    //     // Old approach
    // }
    */

    // ----------------------------------------------------------------
    // NEW APPROACH: BUBBLE-UP INSERTION
    // ----------------------------------------------------------------

    /**
     * Inserts a key-value pair into the B+ Tree (public).
     * Uses the new "bubble-up" approach.
     */
    @Override
    public void insert(K key, Rid rid) {
        try {
            // Call our private recursive method
            SplitResult<K> result = insertHelper(rootPageId, key, rid);
            //BplusTreeNode<K> root = readNode(rootPageId);
          //  System.out.println("ROOT keys size--: " + root.keys.size());

            if (result != null) {
                // Root split -> create a new root
                BplusTreeNode<K> newRoot = new BplusTreeNode<>(false);
                int newRootPageId = bm.createPage(indexFile).getPid();

                // root 0
                // 2
                // 0 1

                // 0
                // 2 1

                // swap the page ids

                // newRoot has one key (the promoted key), and two children

                newRoot.children.add(rootPageId);


                newRoot.keys.add(result.splitKey);


                newRoot.children.add(result.newPageId);

               // System.out.println("After split" + newRoot.children.size());

                // Update references
                //root = newRoot;
                rootPageId = newRootPageId;

                //System.out.println("ROOT PAGE ID--: " + rootPageId);
             //  System.out.println("ROOT keys size--: " + root.keys.size());

                // Write new root
                writeNode(newRoot, newRootPageId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Recursively inserts (key, rid) into the subtree at pageId.
     * If a split occurs, returns a (splitKey, newPageId). Otherwise returns null.
     */
    private SplitResult<K> insertHelper(int pageId, K key, Rid rid) throws IOException, ClassNotFoundException {
        BplusTreeNode<K> node = readNode(pageId);

        //System.out.println(pageId + "####" + node.isLeaf);

        //Page page = bm.getPage(pageId, indexFile);
        // If leaf, insert here
        if (node.isLeaf) {
            // Insert in sorted order
            int i = Collections.binarySearch(node.keys, key);
            if (i < 0) i = -(i + 1);


            // Check for split


            node.keys.add(i, key);
            node.values.add(i, rid);

//                System.out.println("NewLeaf (after subList add) keys: " + node.keys);
//                System.out.println("NewLeaf (after subList add) values: " + node.values);

//            byte[] pid = ByteBuffer.allocate(4).putInt(rid.pageId).array();
//            byte[] slot_id = ByteBuffer.allocate(4).putInt(rid.slotId).array();
//
//            byte[] keyBytes = serializeKey(key);
//
//            page.insertRow(new leafRow(keyBytes, pid, slot_id));

           // bm.unpinPage(pageId,indexFile);

            if (node.keys.size() >= order) {
                return splitLeafNode(node, pageId);
            } else {
                writeNode(node, pageId);
                // No split, just write node
                return null;
            }
        } else {
            // Internal node => find child
            int i = Collections.binarySearch(node.keys, key);
            if (i < 0) i = -(i + 1);

            //System.out.println(key + "%%%" + node.children + "%%%" +i);
            SplitResult<K> childSplit = insertHelper(node.children.get(i), key, rid);

            // If childSplit is null => no split
            if (childSplit == null) {
                return null;
            } else {
                // Child has split => insert child's promoted key in this node
                K splitKey = childSplit.splitKey;
                int newPageId = childSplit.newPageId;

                int pos = Collections.binarySearch(node.keys, splitKey);
                if (pos < 0) pos = -(pos + 1);

                node.keys.add(pos, splitKey);
                node.children.add(pos + 1, newPageId);

//                byte[] pid = ByteBuffer.allocate(4).putInt(rid.pageId).array();
//
//
//                byte[] keyBytes = serializeKey(key);
//
//                page.insertRow(new nonLeafRow(keyBytes, pid));

              //  bm.unpinPage(pageId,indexFile);
                // Now check if this internal node overflows
                if (node.keys.size() >= order) {
                    return splitInternalNode(node, pageId);
                } else {
                    writeNode(node, pageId);
                    return null;
                }
            }
        }
    }

    /**
     * Splits a leaf node that has overflowed, creating a new leaf.
     * Returns (splitKey, newLeafPageId) to be promoted.
     */
    private SplitResult<K> splitLeafNode(BplusTreeNode<K> leaf, int leafPageId) throws IOException {

        //System.out.println("Splitting Leaf Node");
        BplusTreeNode<K> newLeaf = new BplusTreeNode<>(true);
        int newLeafPageId = bm.createIndexPage(indexFile, true).getPid();

        // Calculate the 'mid' position where we split

        if(leaf.next == -1)
        {
            newLeaf.next = null;
        }
        else {
            newLeaf.next = leaf.next;
        }
        leaf.next = newLeafPageId;
        int mid = leaf.keys.size() / 2;

//        System.out.println("=== SPLIT LEAF NODE ===");
//        System.out.println("Leaf (before split) keys: " + leaf.keys);
//        System.out.println("Leaf (before split) values: " + leaf.values);

        // Move the second half of (keys, values) to newLeaf
        newLeaf.keys.addAll(leaf.keys.subList(mid, leaf.keys.size()));
        newLeaf.values.addAll(leaf.values.subList(mid, leaf.values.size()));

//        System.out.println("NewLeaf (after subList add) keys: " + newLeaf.keys);
//        System.out.println("NewLeaf (after subList add) values: " + newLeaf.values);

        // Remove them from old leaf
        leaf.keys.subList(mid, leaf.keys.size()).clear();
        leaf.values.subList(mid, leaf.values.size()).clear();

//        System.out.println("Leaf (after clear) keys: " + leaf.keys);
//        System.out.println("Leaf (after clear) values: " + leaf.values);

        // Fix the linked list pointers


        // Write both leaves to disk
        writeNode(leaf, leafPageId);
        writeNode(newLeaf, newLeafPageId);

        // In a B+ Tree, we promote newLeaf's first key
        K splitKey = newLeaf.keys.get(0);

//        System.out.println("Split key promoted to parent: " + splitKey);
//        System.out.println("Created new leaf pageId: " + newLeafPageId);
//        System.out.println("=========================");

        bm.unpinPage(newLeafPageId,indexFile);

        return new SplitResult<>(splitKey, newLeafPageId);
    }

    /**
     * Splits an internal node that has overflowed, creating a new internal node.
     * Returns (splitKey, newNodePageId) for the parent to insert.
     */
    private SplitResult<K> splitInternalNode(BplusTreeNode<K> node, int nodePageId) throws IOException {
        BplusTreeNode<K> newNode = new BplusTreeNode<>(false);
        int newNodePageId = bm.createIndexPage(indexFile, false).getPid();

       // System.out.println("*****************Splitting Internal Node*****************************");
        // Split roughly in half
        int mid = node.keys.size() / 2;

//        System.out.println("Internal Node (before split) keys: " + node.keys);
//        System.out.println("Internal (before split) values: " + node.children);
        // The promoted key is node.keys.get(mid)
        K splitKey = node.keys.get(mid);

        // newNode gets the keys *after* the promoted key
        newNode.keys.addAll(node.keys.subList(mid + 1, node.keys.size()));
        node.keys.subList(mid, node.keys.size()).clear();




        // newNode also gets the children *after* mid
        newNode.children.addAll(node.children.subList(mid + 1, node.children.size()));
        node.children.subList(mid + 1, node.children.size()).clear();

//        System.out.println("NewLeaf (after subList add) keys: " + newNode.keys);
//        System.out.println("NewLeaf (after subList add) values: " + newNode.children);

//        System.out.println("Leaf (after clear) keys: " + node.keys);
//        System.out.println("Leaf (after clear) values: " + node.children);
        // Write both
        writeNode(node, nodePageId);
        writeNode(newNode, newNodePageId);

//        System.out.println("Split key promoted to parent: " + splitKey);
//        System.out.println("Created new leaf pageId: " + newNodePageId);
//        System.out.println("=========================");

        // Return the promoted key + new node
        bm.unpinPage(newNodePageId,indexFile);
        return new SplitResult<>(splitKey, newNodePageId);
    }

    // ----------------------------------------------------------------
    // SEARCH & RANGE SEARCH (unchanged)
    // ----------------------------------------------------------------

    @Override
    public Iterator<Rid> search(K key) {
        List<Rid> matchingRids = new ArrayList<>();
        try {
            BplusTreeNode<K> node = readNode(rootPageId);
            while (!node.isLeaf) {
                int i = Collections.binarySearch(node.keys, key);
                if (i < 0) i = -(i + 1);
                node = readNode(node.children.get(i));
            }

            int i = Collections.binarySearch(node.keys, key);
            if (i >= 0) {
                while (i < node.keys.size() && node.keys.get(i).compareTo(key) == 0) {
                    matchingRids.add(node.values.get(i));
                    i++;
                }

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

    @Override
    public Iterator<Rid> rangeSearch(K startKey, K endKey) {
        List<Rid> results = new ArrayList<>();
        try {
            BplusTreeNode<K> node = readNode(rootPageId);
            while (!node.isLeaf) {
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
                node = (node.next == null) ? null : readNode(node.next);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results.iterator();
    }

    /**
     * Prints the entire B+ tree starting from the root.
     */
    public void printTree() {
        try {
            printNode(rootPageId, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Recursively prints a node (given its pageId) and its children.
     * @param pageId the pageId of the node to print.
     * @param level the current level in the tree (for indentation).
     * @throws IOException
     * @throws ClassNotFoundException
     */
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

}
