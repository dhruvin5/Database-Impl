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

    // Tests the initialization of BufferManagerImplem with different buffer capacities
    @Test
    void testingCorrectBufferManagerInitialization() {
        for (int i = 1; i <= 5; i++) {
            BufferManagerImplem bm = new BufferManagerImplem(i);
            assertNotNull(bm, "BufferManagerImplem object should not be null");
            assertEquals(i, bm.getBufferCapacity(), "Buffer capacity should be " + i);
            assertEquals(i, bm.getFreeFrames(), "Free frames should be " + i);
            assertTrue(bm.isBufferEmpty(), "Buffer should be empty initially");
        }
    }

    // Tests creating pages up to the buffer capacity and checking page properties
    @Test
    void testCreatePage_1() {
        try{
        String FILE_NAME = "movie.bin";
        BufferManagerImplem test = new BufferManagerImplem(10);

        for (int i = 1; i <= 10; i++) {
            Page page = test.createPage(FILE_NAME);
            assertNotNull(page, "Page should be created and not null");

            PageMetaData metadata = test.pageInfo.get(FILE_NAME).get(page.getPid());
            assertNotNull(metadata, "Metadata for the page should not be null");

            assertTrue(metadata.isDirty(), "Page should be marked as dirty");

            assertEquals(1, metadata.getPinCount(), "Pin count should be 1");

            assertEquals(10 - i, test.getFreeFrames(), "Free frames should decrease with each page creation");
        }

        Page page = test.createPage(FILE_NAME);
        assertNull(page, "Should not be able to create more pages when buffer is full");
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    // Tests fetching a page that does not exist in the buffer or on disk
    @Test
    void testGetPage_1() throws IOException {
        try {
            String FILE_NAME = "src/test/java/org/bin/getPageTest_1.bin";
            Files.deleteIfExists(Paths.get(FILE_NAME));  

            BufferManagerImplem test = new BufferManagerImplem(10);

            Page page = test.getPage(58, FILE_NAME);  
            assertNull(page, "Fetched page should be null as it does not exist in buffer or on disk.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Tests creating pages up to the buffer capacity and then unpinning some pages
    @Test
    void testCreatePage_2() throws IOException {
        try {
            String FILE_NAME = "src/test/java/org/bin/placeholder.bin";
            Files.deleteIfExists(Paths.get(FILE_NAME));
            
            BufferManagerImplem test = new BufferManagerImplem(10);
    
            for (int i = 1; i <= 10; i++) {
                Page page = test.createPage(FILE_NAME);
                assertNotNull(page, "Page should be created and not null");
    
                PageMetaData metadata = test.pageInfo.get(FILE_NAME).get(page.getPid());
    
                assertTrue(metadata.isDirty(), "Page should be marked as dirty");
    
                assertEquals(1, metadata.getPinCount(), "Pin count should be 1");
    
                assertEquals(10 - i, test.getFreeFrames(), "Free frames should decrease with each page creation");
            }
    
            for (int i = 1; i <= 3; i++) {
                test.unpinPage(i, FILE_NAME);
    
                Page page = test.createPage(FILE_NAME); 
                assertNotNull(page, "Should be able to create more pages");
    
                PageMetaData metadata = test.pageInfo.get(FILE_NAME).get(page.getPid());
                
                assertTrue(metadata.isDirty(), "New page should be marked as dirty");
                assertEquals(1, metadata.getPinCount(), "Pin count should be 1");
    
                assertEquals(10 - 3 - i, test.getFreeFrames(), "Free frames should be correct after creating new pages");
    
                int pageIndex = test.existsInCache(FILE_NAME, page.getPid());
                assertTrue(pageIndex != -1, "Page should be in the LRU cache");
            }
    
            for (int i = 1; i <= 3; i++) {
                int pageIndex = test.existsInCache(FILE_NAME, i);
                assertTrue(pageIndex == -1, "Unpinned pages should have been evicted from cache");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Tests marking a page as dirty and ensuring its state remains consistent
    @Test
    void testMarkDirty_1() {
        try {
            BufferManagerImplem test = new BufferManagerImplem(2);

            String FILE_NAME = "src/test/java/org/bin/placeholder.bin";  
            Page page = test.createPage(FILE_NAME);
            int pageId = page.getPid();

            PageMetaData metadata = test.pageInfo.get(FILE_NAME).get(pageId);
            assertTrue(metadata.isDirty(), "Page should be marked as dirty by default");

            test.markDirty(pageId, FILE_NAME);
            assertTrue(test.pageInfo.get(FILE_NAME).get(pageId).isDirty(), "Page should remain marked as dirty");

            test.markDirty(pageId, FILE_NAME);
            assertTrue(test.pageInfo.get(FILE_NAME).get(pageId).isDirty(), "Page should still be marked as dirty after calling markDirty again");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Tests marking a page as dirty after it has been evicted from the buffer
    @Test
    void testMarkDirty_2() throws IOException {
        try {
            String FILE_NAME = "src/test/java/org/bin/markDirtyTest_2.bin";
            Files.deleteIfExists(Paths.get(FILE_NAME)); 
            
            BufferManagerImplem test = new BufferManagerImplem(2);
            
            Page page1 = test.createPage(FILE_NAME);
            Page page2 = test.createPage(FILE_NAME);
            test.unpinPage(page1.getPid(), FILE_NAME);
            test.unpinPage(page2.getPid(), FILE_NAME);

            Page page3 = test.createPage(FILE_NAME);
            assertEquals(test.existsInCache(FILE_NAME, page1.getPid()), -1, "Page 0 is not in cache");

            Page page1_again = test.getPage(page1.getPid(), FILE_NAME);
            assertNotNull(page1_again, "Page should be fetched from disk");
            assertFalse(test.pageInfo.get(FILE_NAME).get(page1.getPid()).isDirty(), "Page should not be dirty after being evicted");

            test.markDirty(page1.getPid(), FILE_NAME);
            assertTrue(test.pageInfo.get(FILE_NAME).get(page1.getPid()).isDirty(), "Page should be marked as dirty after markDirty");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Tests unpinning a page and its behavior in the buffer pool
    @Test
    void testUnpinPage() {
        try {
            BufferManagerImplem test = new BufferManagerImplem(10); 
            Page page = test.createPage("src/test/java/org/bin/placeholder.bin"); 
            int pageId = page.getPid();

            test.unpinPage(100, "src/test/java/org/bin/placeholder.bin"); 
        
            for (int i = 0; i < 5; i++) {
                test.unpinPage(pageId, "src/test/java/org/bin/placeholder.bin");
                assertEquals(0, test.pageInfo.get("src/test/java/org/bin/placeholder.bin").get(pageId).getPinCount(), "Page pin count is 0 after unpinning");
            }

            for (int i = 0; i < 5; i++) {
                test.getPage(pageId, "src/test/java/org/bin/placeholder.bin");
                assertEquals(i + 1, test.pageInfo.get("src/test/java/org/bin/placeholder.bin").get(pageId).getPinCount(), "Page pin count should be " + (i + 1) + " after accessing");
            }

            for (int i = 0; i < 5; i++) {
                test.unpinPage(pageId, "src/test/java/org/bin/placeholder.bin");
                assertEquals(5 - i - 1, test.pageInfo.get("src/test/java/org/bin/placeholder.bin").get(pageId).getPinCount(), "Page pin count should be " + (5 - i - 1) + " after unpinning");
            }
            
            test.unpinPage(pageId, "src/test/java/org/bin/placeholder.bin");
            test.createPage("src/test/java/org/bin/placeholder.bin");

            test.unpinPage(pageId, "src/test/java/org/bin/placeholder.bin");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
