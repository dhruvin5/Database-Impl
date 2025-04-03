package org;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import buffer.BufferManagerImplem;
import java.nio.file.*;
import Page.Page;
import Row.Row;
import java.io.IOException;
import Page.PageMetaData;

class BufferManagerImplemTest { 
    @Test
    void testingCorrectBufferManagerInitialization() {
        for (int i = 1; i <= 5; i++) {
            BufferManagerImplem bm = new BufferManagerImplem(i);
            assertNotNull(bm, "BufferManagerImplem object should not be null");
            assertEquals(i, bm.getBufferCapacity(), "Buffer capacity should be " + i);
            assertEquals(i, bm.getFreeFrames(), "Free frames should be " + i);
            // assertEquals(0, bm.getTotalPages(), "Total pages should be 0 initially");
            assertTrue(bm.isBufferEmpty(), "Buffer should be empty initially");
        }
    }

    @Test
    void testCreatePage_1() {
        try{
        String FILE_NAME = "movie.bin";
        BufferManagerImplem test = new BufferManagerImplem(10);

        // Create up to 10 pages
        for (int i = 1; i <= 10; i++) {
            Page page = test.createPage(FILE_NAME);
            assertNotNull(page, "Page should be created and not null");

            // Get the metadata for the page
            PageMetaData metadata = test.pageInfo.get(FILE_NAME).get(page.getPid());
            assertNotNull(metadata, "Metadata for the page should not be null");

            // Check if the page is dirty
            assertTrue(metadata.isDirty(), "Page should be marked as dirty");

            // Check pin count (should be 1 after creation)
            assertEquals(1, metadata.getPinCount(), "Pin count should be 1");

            // Free frames should decrease with each page creation
            assertEquals(10 - i, test.getFreeFrames(), "Free frames should decrease with each page creation");
        }

        // Try creating a page after the buffer is full
        Page page = test.createPage(FILE_NAME);
        assertNull(page, "Should not be able to create more pages when buffer is full");
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    void testGetPage_1() throws IOException {
        try {
            // Set up a temporary disk file for simulation
            String FILE_NAME = "src/test/java/org/bin/getPageTest_1.bin";
            Files.deleteIfExists(Paths.get(FILE_NAME));  // Ensure any existing file is deleted

            // Initialize BufferManager with 10 slots
            BufferManagerImplem test = new BufferManagerImplem(10);

            // The manager should not be able to return a page as it has not created any pages with that ID
            // Fetch a page with a specific page ID (58) that does not exist
            Page page = test.getPage(58, FILE_NAME);  // Get page by its ID and the file name

            // The page should not be found in the buffer pool, so it should return null
            assertNull(page, "Fetched page should be null as it does not exist in buffer or on disk.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCreatePage_2() throws IOException {
        try {
            // Create temporary disk location
            String FILE_NAME = "src/test/java/org/bin/placeholder.bin";
            Files.deleteIfExists(Paths.get(FILE_NAME));
            
            // Initialize BufferManager with 10 buffer slots
            BufferManagerImplem test = new BufferManagerImplem(10);
    
            // Create up to 10 pages (buffer full)
            for (int i = 1; i <= 10; i++) {
                Page page = test.createPage(FILE_NAME); // Provide the file name directly
                assertNotNull(page, "Page should be created and not null");
    
                // Get the metadata for the page
                PageMetaData metadata = test.pageInfo.get(FILE_NAME).get(page.getPid());
    
                // Check if the page is dirty
                assertTrue(metadata.isDirty(), "Page should be marked as dirty");
    
                // Check pin count (should be 1 after creation)
                assertEquals(1, metadata.getPinCount(), "Pin count should be 1");
    
                // Free frames should decrease with each page creation
                assertEquals(10 - i, test.getFreeFrames(), "Free frames should decrease with each page creation");
            }
    
            // Unpin the first 3 pages
            for (int i = 1; i <= 3; i++) {
                test.unpinPage(i, FILE_NAME);  // Unpin pages (simulate that pages are available for eviction)
    
                // Create a new page and check if it gets added to the buffer
                Page page = test.createPage(FILE_NAME); // Create page after unpinning
                assertNotNull(page, "Should be able to create more pages");
    
                // Get the metadata for the new page
                PageMetaData metadata = test.pageInfo.get(FILE_NAME).get(page.getPid());
                
                // Check if the page is dirty and pin count is 1
                assertTrue(metadata.isDirty(), "New page should be marked as dirty");
                assertEquals(1, metadata.getPinCount(), "Pin count should be 1");
    
                // Check free frames
                assertEquals(10 - 3 - i, test.getFreeFrames(), "Free frames should be correct after creating new pages");
    
                // Check if the page has been added to the buffer
                int pageIndex = test.existsInCache(FILE_NAME, page.getPid());
                assertTrue(pageIndex != -1, "Page should be in the LRU cache");
            }
    
            // Try to check if the unpinned pages no longer exist in cache after they are evicted
            for (int i = 1; i <= 3; i++) {
                int pageIndex = test.existsInCache(FILE_NAME, i);
                assertTrue(pageIndex == -1, "Unpinned pages should have been evicted from cache");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Test
    void testMarkDirty_1() {
        try {
            // Initialize the BufferManager with 2 buffer slots
            BufferManagerImplem test = new BufferManagerImplem(2);

            // Create a page
            String FILE_NAME = "src/test/java/org/bin/placeholder.bin";  // Use a proper file name
            Page page = test.createPage(FILE_NAME);
            int pageId = page.getPid();

            // Ensure the page is marked as dirty by default
            PageMetaData metadata = test.pageInfo.get(FILE_NAME).get(pageId);
            assertTrue(metadata.isDirty(), "Page should be marked as dirty by default");

            // Mark the page as dirty again (this should have no effect)
            test.markDirty(pageId, FILE_NAME);
            assertTrue(test.pageInfo.get(FILE_NAME).get(pageId).isDirty(), "Page should remain marked as dirty");

            // Now, check if marking dirty on a page that is already dirty does not change the dirty bit
            // The dirty bit should still be true as the page is already dirty
            test.markDirty(pageId, FILE_NAME);
            assertTrue(test.pageInfo.get(FILE_NAME).get(pageId).isDirty(), "Page should still be marked as dirty after calling markDirty again");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testMarkDirty_2() throws IOException {
        try {
            // Create a temporary disk location
            String FILE_NAME = "src/test/java/org/bin/markDirtyTest_2.bin";
            Files.deleteIfExists(Paths.get(FILE_NAME)); // Ensure the file is deleted if it exists
            
            // Initialize BufferManager with 2 buffer slots
            BufferManagerImplem test = new BufferManagerImplem(2);
            
            // Create 2 pages and unpin them
            Page page1 = test.createPage(FILE_NAME);
            Page page2 = test.createPage(FILE_NAME);
            test.unpinPage(page1.getPid(), FILE_NAME);
            test.unpinPage(page2.getPid(), FILE_NAME);

            // Create a new page and check if the first page is evicted
            Page page3 = test.createPage(FILE_NAME);
            assertEquals(test.existsInCache(FILE_NAME, page1.getPid()), -1, "Page 0 is not in cache");

            // Fetch the page from disk (it was evicted) and check if it is not dirty
            Page page1_again = test.getPage(page1.getPid(), FILE_NAME);
            assertNotNull(page1_again, "Page should be fetched from disk");
            assertFalse(test.pageInfo.get(FILE_NAME).get(page1.getPid()).isDirty(), "Page should not be dirty after being evicted");

            // Marking the page as dirty
            test.markDirty(page1.getPid(), FILE_NAME);
            assertTrue(test.pageInfo.get(FILE_NAME).get(page1.getPid()).isDirty(), "Page should be marked as dirty after markDirty");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Test unpinPage method in the case where the page is in buffer pool or in memory
    @Test
    void testUnpinPage() {
        try {
            BufferManagerImplem test = new BufferManagerImplem(10); // Initialize BufferManager with 10 buffer slots
            Page page = test.createPage("src/test/java/org/bin/placeholder.bin"); // Create a new page with a specific file
            int pageId = page.getPid();

            // Checks if unpinning a page that doesn't exist doesn't crash the buffer manager
            test.unpinPage(100, "src/test/java/org/bin/placeholder.bin"); // Pass a page ID that doesn't exist
        
            // Unpin page 5 times to see if the pin count goes down to 0
            for (int i = 0; i < 5; i++) {
                test.unpinPage(pageId, "src/test/java/org/bin/placeholder.bin");
                assertEquals(0, test.pageInfo.get("src/test/java/org/bin/placeholder.bin").get(pageId).getPinCount(), "Page pin count is 0 after unpinning");
            }

            // Pin the page 5 times (simulating accesses to the page)
            for (int i = 0; i < 5; i++) {
                test.getPage(pageId, "src/test/java/org/bin/placeholder.bin");
                assertEquals(i + 1, test.pageInfo.get("src/test/java/org/bin/placeholder.bin").get(pageId).getPinCount(), "Page pin count should be " + (i + 1) + " after accessing");
            }

            // Unpin the page 5 times again to see if the pin count goes down to 0
            for (int i = 0; i < 5; i++) {
                test.unpinPage(pageId, "src/test/java/org/bin/placeholder.bin");
                assertEquals(5 - i - 1, test.pageInfo.get("src/test/java/org/bin/placeholder.bin").get(pageId).getPinCount(), "Page pin count should be " + (5 - i - 1) + " after unpinning");
            }
            
            // Create a new page and evict the current page from the buffer pool
            test.unpinPage(pageId, "src/test/java/org/bin/placeholder.bin");
            test.createPage("src/test/java/org/bin/placeholder.bin");

            // Should not crash the buffer manager, check if unpin works properly for an evicted page
            test.unpinPage(pageId, "src/test/java/org/bin/placeholder.bin");

        } catch (Exception e) {
            e.printStackTrace(); // Print stack trace if any exception occurs
        }
    }


}