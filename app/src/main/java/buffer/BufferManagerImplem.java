package buffer;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.AbstractMap;

import Page.*;
import configs.Config;
import SystemCatalog.systemCatalog;
import SystemCatalog.tableMetaData;

public class BufferManagerImplem extends BufferManager {

    // for mapping file name and page id to frames
    HashMap<String, HashMap<Integer, Integer>> pageTable;

    // Buffer pool
    Page[] bufferPool;

    // Free Frames
    ArrayList<Integer> freeFrameList;

    // LRU Cache
    LinkedList<AbstractMap.SimpleEntry<String, Integer>> lruCache;

    // maps File name and Page id to metadata
    HashMap<String, HashMap<Integer, PageMetaData>> pageInfo;

    // sytem catalog instance
    private final systemCatalog catalog;

    // Used to maintain the mapping of the current Page Id to the file name - the
    // total number of pages created for each file
    private HashMap<String, Integer> FileToPID;

    int totalPages; // total number of pages created

    // initialize the buffer pool, page table, free frames list, lru cache, page
    // metadata
    public BufferManagerImplem(int bufferSize) {
        super(bufferSize);
        this.bufferPool = new Page[bufferSize];
        this.freeFrameList = new ArrayList<>();
        this.lruCache = new LinkedList<>();
        this.pageTable = new HashMap<>();
        this.pageInfo = new HashMap<>();
        this.FileToPID = new HashMap<>(); // Initialize the file to PID mapping
        this.totalPages = 0;

        this.catalog = new systemCatalog(); // Initialize the catalog instance

        // all frames are free initially
        for (int i = 0; i < bufferSize; i++)
            freeFrameList.add(i);
    }

    // get the column sizes for the file by contacting the system catalog
    private HashMap<String, Integer> getColumnSizes(String FILE_NAME) {

        tableMetaData table = this.catalog.getTableMetaData(FILE_NAME); // Get the table metadata
        ArrayList<String> columnNames = table.getColumnNames(); // Get the table name
        HashMap<String, Integer> columnSizes = new HashMap<>(); // Initialize the

        for (String columnName : columnNames) {
            columnSizes.put(columnName, table.getColumnSize(columnName)); // Get the column size
        }
        if (this.catalog.isIndexFile(FILE_NAME)) {
            columnSizes.put("key", table.getColumnSize(catalog.getIndex(FILE_NAME).getKey()));
        }

        return columnSizes;
    }

    // Create new Page
    Page createAndAllocatePage(int frameIndex, Page page, boolean isPageCreated, String FILE_NAME, boolean isLeaf) {

        if (!isPageCreated) { // Create a new page if doesnt exist

            int newID = this.FileToPID.getOrDefault(FILE_NAME, 0); // Get the current page id for the file
            HashMap<String, Integer> columnSize = getColumnSizes(FILE_NAME); // Get the column sizes for the file
            byte boolValue = (byte) (isLeaf ? 1 : 0); // Convert boolean to byte

            // Create a new page based on the file type
            if (this.catalog.isIndexFile(FILE_NAME) && isLeaf) {
                page = new LeafIndexPageImpl(newID, boolValue, columnSize.get("key"), columnSize.get("pid"),
                        columnSize.get("slotID"));
            } else if (this.catalog.isIndexFile(FILE_NAME) && !isLeaf) {
                page = new NonLeafIndexPage(newID, boolValue, columnSize.get("key"), columnSize.get("pid"));
            } else {
                page = new PageImpl(newID, columnSize.get("movieId"), columnSize.get("title"));
            }

            this.totalPages = this.totalPages + 1;
            this.FileToPID.put(FILE_NAME, newID + 1); // Update the total number of pages created for the file
        }

        // Allocate the page in the buffer pool
        bufferPool[frameIndex] = page;

        // Store the metadata
        PageMetaData metadata = new PageMetaData();
        metadata.incrementPinCount(); // pinning
        if (!isPageCreated) { // set the page dirty only while creating the new page and not while loading it
                              // from the disk
            metadata.setDirtyBit(true); // marking it dirty
        }
        // Store the metadata in the pageInfo map
        if (pageInfo.containsKey(FILE_NAME) && pageInfo.get(FILE_NAME).containsKey(page.getPid())) {
            System.out.println("Error: Page " + page.getPid() + " already exists in the buffer.");
            return null; // Page already exists, cannot create again
        }

        // Map the page id and fileName to its metadata
        HashMap<Integer, PageMetaData> file_pageMetaData = pageInfo.getOrDefault(FILE_NAME, new HashMap<>());
        file_pageMetaData.put(page.getPid(), metadata); // map the page id to its metadata
        pageInfo.put(FILE_NAME, file_pageMetaData); // update the pageInfo map

        // Map the page id to its frame index in the page table
        HashMap<Integer, Integer> file_pageTable = pageTable.getOrDefault(FILE_NAME, new HashMap<>());
        file_pageTable.put(page.getPid(), frameIndex); // map the page id to its frame index
        pageTable.put(FILE_NAME, file_pageTable); // update the page table

        // Add the new page to the LRU cache
        lruCache.addLast(new AbstractMap.SimpleEntry<>(FILE_NAME, page.getPid())); // store the file name and page id in
                                                                                   // the LRU cache

        return page;
    }

    // LRU page eviction if possible
    int evictPage() {
        if (lruCache.isEmpty()) {
            System.out.println("Error: No pages available for eviction.");
            return -1; // No pages to evict
        }

        // Iterating over the LRU cache to find a page with pin count = 0
        for (int i = 0; i < lruCache.size(); i++) {
            String FILE_NAME = lruCache.get(i).getKey(); // get the file name from the LRU cache
            int pageId = lruCache.get(i).getValue(); // get the page id from the LRU cache
            PageMetaData metadata = pageInfo.get(FILE_NAME).get(pageId); // get the metadata for the page

            // Check eviction eligibility
            if (metadata.getPinCount() == 0) {
                int frameIndex = pageTable.get(FILE_NAME).get(pageId); // get the frame index from the page table

                lruCache.remove(i); // Remove it from LRU cache

                // Write dirty page back to disk
                if (metadata.isDirty()) {

                    Page page = bufferPool[frameIndex];
                    writeToDisk(page, FILE_NAME);
                }

                // Remove the evicted page from the buffer pool
                bufferPool[frameIndex] = null;

                // Remove the evicted page's metadata and mapping
                pageInfo.get(FILE_NAME).remove(pageId); // remove the page metadata
                pageTable.get(FILE_NAME).remove(pageId); // remove the page mapping from the page table

                return frameIndex;
            }
        }

        // No evictable pages with pin count = 0 is found
        System.out.println("Error: No evictable page found (all pages are pinned).");
        return -1;
    }

    Page createAndLoadPageHelper(Page page, boolean isPageCreated, String FILE_NAME, boolean isLeaf) {
        // Check for free frames
        if (!freeFrameList.isEmpty()) {
            // remove free frame for allocation
            int frameIndex = freeFrameList.remove(freeFrameList.size() - 1);

            // Create and allocate new page in the buffer pool
            return createAndAllocatePage(frameIndex, page, isPageCreated, FILE_NAME, isLeaf);
        } else {
            // Evict using LRU if possible
            int lruFrameIndex = evictPage();

            if (lruFrameIndex == -1) {
                return null; // all pages are pinned, nothing removed from LRU cache, page creation failed
            }

            // Proceed with creating and allocating new page in the buffer pool
            return createAndAllocatePage(lruFrameIndex, page, isPageCreated, FILE_NAME, isLeaf);
        }
    }

    @Override
    public Page getPage(int pageId, String FILE_NAME) {
        // Check if in the buffer pool
        if (pageTable.containsKey(FILE_NAME) && pageTable.get(FILE_NAME).containsKey(pageId)) {
            int frameIndex = pageTable.get(FILE_NAME).get(pageId); // page frame index
            Page page = bufferPool[frameIndex]; // get the page from the buffer pool

            PageMetaData metadata = pageInfo.get(FILE_NAME).get(pageId); // get the page metadata
            metadata.incrementPinCount(); // increment the pin count

            // Move the page to the end of the LRU cache (most recently used)
            AbstractMap.SimpleEntry<String, Integer> entry = new AbstractMap.SimpleEntry<>(FILE_NAME, pageId);
            lruCache.remove(entry); // remove from current position
            lruCache.addLast(entry); // add to the end

            return page;
        } else { // load page from disk
                 // Check if the page exists on disk
            if (!isPageOnDisk(pageId, FILE_NAME)) {
                System.out.println("Error: Page " + pageId + " does not exist on disk.");
                return null; // Page not found on disk
            }

            // get page from disk
            Page page = getPageFromDisk(pageId, FILE_NAME);

            if (page == null) {
                System.out.println("Error: Failed to load page" + pageId + "from disk.");
                return null; // failed to load page from disk
            }

            boolean isLeaf = ispageLeaf(FILE_NAME, page.getRows());
            // load to buffer
            return createAndLoadPageHelper(page, true, FILE_NAME, isLeaf); // load the page to the buffer pool
        }
    }

    @Override
    public Page createPage(String FILE_NAME) {
        return createAndLoadPageHelper(null, false, FILE_NAME, false);
    }

    public Page createIndexPage(String FILE_NAME, boolean isLeaf) {
        return createAndLoadPageHelper(null, false, FILE_NAME, isLeaf);
    }

    @Override
    public void markDirty(int pageId, String FILE_NAME) {
        PageMetaData metadata = pageInfo.getOrDefault(FILE_NAME, new HashMap<>()).get(pageId);

        if (metadata != null) {
            // Page is in the buffer, marking it as dirty
            metadata.setDirtyBit(true);
            // System.out.println("Page " + pageId + " is marked as dirty.");
        } else {
            System.out.println("Error: Page " + pageId + " not found in buffer.");
        }
    }

    @Override
    public void unpinPage(int pageId, String FILE_NAME) {
        PageMetaData metadata = pageInfo.getOrDefault(FILE_NAME, new HashMap<>()).get(pageId);

        if (metadata != null) { // page exists in buffer pool
            if (metadata.getPinCount() > 0) {
                metadata.decrementPinCount();
            } else { // Pin count is already 0
                System.out.println("Error: Page " + pageId + " already has pin count 0. Cannot unpin further.");
            }
        } else { // Page not in the buffer pool
            System.out.println("Error: Page " + pageId + " not found in buffer.");
        }
    }

    // get Page from the disk
    private Page getPageFromDisk(int pageId, String FILE_NAME) {

        // using random access file to go to that location and read 4KB of data
        // get the file name from the pid id
        try (RandomAccessFile fileReader = new RandomAccessFile(FILE_NAME, "r")) {
            long offset = (long) pageId * Config.PAGE_SIZE;
            if (offset >= fileReader.length()) {
                return null;
            }

            // goes to the offset
            fileReader.seek(offset);
            byte[] buffer = new byte[Config.PAGE_SIZE];

            // reads 4KB of data
            fileReader.readFully(buffer);

            // gets the columns needed for the page
            HashMap<String, Integer> columnSize = getColumnSizes(FILE_NAME);

            // check if the page is an index page or a data page
            if (this.catalog.isIndexFile(FILE_NAME)) {
                boolean isLeaf = ispageLeaf(FILE_NAME, buffer); // check if the page is a leaf page
                if (isLeaf) {
                    return new LeafIndexPageImpl(pageId, buffer, columnSize.get("key"), columnSize.get("pid"),
                            columnSize.get("slotID"));
                } else {
                    // a non-leaf page
                    return new NonLeafIndexPage(pageId, buffer, columnSize.get("key"), columnSize.get("pid"));
                }
            } else {
                // a regular data page
                return new PageImpl(pageId, buffer, columnSize.get("movieId"), columnSize.get("title"));
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // write to the disk
    private void writeToDisk(Page page, String FILE_NAME) {

        // get the page Id
        int pageId = page.getPid();

        // open the disk file in read write mode
        try (RandomAccessFile fileWriter = new RandomAccessFile(FILE_NAME, "rw")) {
            long offset = (long) pageId * Config.PAGE_SIZE;

            // go to the offset
            fileWriter.seek(offset);

            // overwrite at that offset
            fileWriter.write(page.getRows(), 0, Config.PAGE_SIZE);
            // System.out.println("Wrote page " + pageId + " to disk.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // check if the page exist in a file
    private boolean isPageOnDisk(int pageId, String FILE_NAME) {
        if (pageId < this.FileToPID.getOrDefault(FILE_NAME, -1)) {
            return true;
        }
        return false;
    }

    // get the buffer capacity
    public int getBufferCapacity() {
        return this.bufferPool.length;
    }

    // get the number of free frames in the buffer pool
    public int getFreeFrames() {
        return this.freeFrameList.size();
    }

    // get the size of the LRU cache
    public int getLRUCacheSize() {
        return this.lruCache.size();
    }

    // check if the buffer is empty
    public boolean isBufferEmpty() {
        for (int i = 0; i < this.bufferPool.length; i++) {
            if (this.bufferPool[i] != null) {
                return false;
            }
        }
        return true;
    }

    // flush all dirty pages to disk
    public void force() {
        // Iterate through all pages in the buffer pool using pageTable
        for (String FILE_NAME : pageTable.keySet()) {
            for (int pageId : pageTable.get(FILE_NAME).keySet()) {
                PageMetaData metadata = pageInfo.get(FILE_NAME).get(pageId);
                if (metadata.isDirty()) {
                    Page page = bufferPool[pageTable.get(FILE_NAME).get(pageId)];
                    writeToDisk(page, FILE_NAME);
                    metadata.setDirtyBit(false); // reset the dirty bit after writing to disk
                }
            }
        }
    }

    // check if the index page is a leaf page
    private boolean ispageLeaf(String FILE_NAME, byte[] buffer) {
        int is_Leaf_Offset = 0;
        return this.catalog.isIndexFile(FILE_NAME) && (buffer[is_Leaf_Offset] == 1);
    }

}
