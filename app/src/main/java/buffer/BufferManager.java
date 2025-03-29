package buffer;

import Page.*;

public abstract class BufferManager {

    // configurable size of buffer cache.
    final int bufferSize;

    public BufferManager(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * Fetches a page from memory if available; otherwise, loads it from disk.
     * The page is immediately pinned.
     * 
     * @param pageId The ID of the page to fetch.
     * @return The Page object whose content is stored in a frame of the buffer pool
     *         manager.
     */
    public abstract Page getPage(int pageId, String fileName);

    /**
     * Creates a new page.
     * The page is immediately pinned.
     * 
     * @return The Page object whose content is stored in a frame of the buffer pool
     *         manager.
     */
    public abstract Page createPage(String fileName);

    public abstract Page createIndexPage(String fileName, boolean isLeaf);

    /**
     * Marks a page as dirty, indicating it needs to be written to disk before
     * eviction.
     * 
     * @param pageId The ID of the page to mark as dirty.
     */
    public abstract void markDirty(int pageId, String fileName);

    /**
     * Unpins a page in the buffer pool, allowing it to be evicted if necessary.
     * 
     * @param pageId The ID of the page to unpin.
     */
    public abstract void unpinPage(int pageId, String fileName);

    /**
     * Flushes all dirty pages from the buffer pool to disk.
     */
    public abstract void force();

}