package buffer;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import configs.*;
import Page.*;

public class BufferManagerImplem extends BufferManager{

    private static String DISK_FILE = "imdb.bin";

    // for mapping page id to frames
    HashMap<Integer, Integer> pageTable;

    // Buffer pool
    Page[] bufferPool;

    // Free Frames
    ArrayList<Integer> freeFrameList;

    // LRU Cache
    LinkedList<Integer> lruCache;

    // Page metadata
    HashMap<Integer, PageMetaData> pageInfo;

    int totalPages;

    // initialize the buffer pool, page table, free frames list, lru cache, page metadata
    public BufferManagerImplem(int bufferSize){
        super(bufferSize);
        this.bufferPool = new Page[bufferSize];
        this.freeFrameList = new ArrayList<>();
        this.lruCache = new LinkedList<>();
        this.pageTable = new HashMap<>();
        this.pageInfo = new HashMap<>();
        this.totalPages = 0;

        // all frames are free initially
        for(int i=0;i<bufferSize;i++)
            freeFrameList.add(i);
    }

    // Create new Page
    Page createAndAllocatePage(int frameIndex, Page page, boolean isPageCreated) {

        if(!isPageCreated){ // Create a new page if doesnt exist

            page = new PageImpl(this.totalPages);
            this.totalPages = this.totalPages + 1;
        }

        // Allocate the page in the buffer pool
        bufferPool[frameIndex] = page;

        // Store the metadata
        PageMetaData metadata = new PageMetaData();
        metadata.incrementPinCount(); // pinning
        if(!isPageCreated){ // set the page dirty only while creating the new page and not while loading it from the disk
            metadata.setDirtyBit(true); // marking it dirty
        }
        pageInfo.put(page.getPid(), metadata);

        // Map page id to frame index
        pageTable.put(page.getPid(), frameIndex);

        // Add the new page to the LRU cache
        lruCache.addLast(page.getPid());

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
            int pageId = lruCache.get(i);
            PageMetaData metadata = pageInfo.get(pageId);

            // Check eviction eligibility
            if (metadata.getPinCount() == 0) {
                int frameIndex = pageTable.get(pageId);

                lruCache.remove(i); // Remove it from LRU cache

                // Write dirty page back to disk
                if (metadata.isDirty()) {

                    Page page = bufferPool[frameIndex];
                    writeToDisk(page);
                }

                // Remove the evicted page from the buffer pool
                bufferPool[frameIndex] = null;

                // Remove the evicted page's metadata and mapping
                pageInfo.remove(pageId);
                pageTable.remove(pageId);

                return frameIndex;
            }
        }

        // No evictable pages with pin count = 0 is found
        System.out.println("Error: No evictable page found (all pages are pinned).");
        return -1;
    }

    Page createAndLoadPageHelper(Page page, boolean isPageCreated){
        // Check for free frames
        if (!freeFrameList.isEmpty()) {
            // remove free frame for allocation
            int frameIndex = freeFrameList.remove(freeFrameList.size() - 1);

            // Create and allocate new page in the buffer pool
            return createAndAllocatePage(frameIndex, page, isPageCreated);
        }
        else {
            // Evict using LRU if possible
            int lruFrameIndex = evictPage();

            if(lruFrameIndex == -1){
                return null; // all pages are pinned, nothing removed from LRU cache, page creation failed
            }

            // Proceed with creating and allocating new page in the buffer pool
            return createAndAllocatePage(lruFrameIndex, page, isPageCreated);
        }
    }




    @Override
    public Page getPage(int pageId) {
        // Check if in the buffer pool
        if (pageTable.containsKey(pageId)) {
            int frameIndex = pageTable.get(pageId);
            Page page = bufferPool[frameIndex];

            // increment the pin count
            PageMetaData metadata = pageInfo.get(pageId);
            metadata.incrementPinCount();

            // Move the page to the end of the LRU cache (most recently used)
            lruCache.remove((Integer) pageId);
            lruCache.addLast(pageId);

            return page;
        }
        else { // load page from disk
            // Check if the page exists on disk
            if (isPageOnDisk(pageId)) {
                System.out.println("Error: Page " + pageId + " does not exist on disk.");
                return null; // Page not found on disk
            }

            //get page from disk
            Page page = getPageFromDisk(pageId);
            if (page == null) {
                System.out.println("Error: Failed to load page" + pageId + "from disk.");
                return null; // failed to load page from disk
            }

            //load to buffer
            return createAndLoadPageHelper(page, true);
        }
    }

    @Override
    public Page createPage() {
        return createAndLoadPageHelper(null, false);
    }

    @Override
    public void markDirty(int pageId) {
        PageMetaData metadata = pageInfo.get(pageId);

        if (metadata != null) {
            // Page is in the buffer, marking it as dirty
            metadata.setDirtyBit(true);
            System.out.println("Page " + pageId + " is marked as dirty.");
        } else {
            System.out.println("Error: Page " + pageId + " not found in buffer.");
        }
    }

    @Override
    public void unpinPage(int pageId) {
        PageMetaData metadata = pageInfo.get(pageId);

        if (metadata != null) { // page exists in buffer pool
            if (metadata.getPinCount() > 0) {
                metadata.decrementPinCount();
            }
            else { // Pin count is already 0
                System.out.println("Error: Page " + pageId + " already has pin count 0. Cannot unpin further.");
            }
        }
        else { // Page not in the buffer pool
            System.out.println("Error: Page " + pageId + " not found in buffer.");
        }
    }

    private Page getPageFromDisk(int pageId){
        try (RandomAccessFile fileReader = new RandomAccessFile(DISK_FILE, "r")) {
            long offset = (long) pageId * Config.PAGE_SIZE;
            if (offset >= fileReader.length()) {
                return null;
            }
            fileReader.seek(offset);
            byte[] buffer = new byte[Config.PAGE_SIZE];
            fileReader.readFully(buffer);

            Page page = new PageImpl(pageId, buffer);
            return page;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    private void writeToDisk(Page page) {
        int pageId = page.getPid();
        try (RandomAccessFile fileWriter = new RandomAccessFile(DISK_FILE, "rw")) {
            long offset = (long) pageId * Config.PAGE_SIZE;
            fileWriter.seek(offset);
            fileWriter.write(page.getRows(), 0, Config.PAGE_SIZE);
            System.out.println("Wrote page " + pageId + " to disk.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isPageOnDisk(int pageId) {
        if(pageId < this.totalPages)
        {
            return true;
        }
        return false;
    }

    
    public int getBufferCapacity() {
        return this.bufferPool.length;
    }

    public int getFreeFrames() {
        return this.freeFrameList.size();
    }

    public int getLRUCacheSize() {
        return this.lruCache.size();
    }

    public int getPagesInBuffer() {
        return this.pageTable.size();
    }

    public int getTotalPages() {
        return this.totalPages;
    }

    public boolean isBufferEmpty(){
        for(int i=0;i<this.bufferPool.length;i++){
            if(this.bufferPool[i] != null){
                return false;
            }
        }
        return true;
    }
    
    public int existsInCache(int pageId) {
        return lruCache.indexOf(pageId);
    }

    public PageMetaData getPageMetaData(int pageId) {
        return pageInfo.get(pageId);
    }
    
    public void updateFile(String fileName) {
        DISK_FILE = fileName;
    }
    
}
