package org;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import buffer.BufferManagerImplem;
import java.nio.file.*;
import Page.Page;
import Row.Row;
import java.io.IOException;

class BufferManagerImplemTest {

    //Test if the buffer manager is correctly initialized
    @Test void testingCorrectBufferMangerIntialization() {
         
        //Repeatedly create buffer manager with different capacity
        for(int i=1; i <= 5; i++) {
            BufferManagerImplem test = new BufferManagerImplem(i);
            assertNotNull(test, "BufferManagerImplem object is not null");

            //Checks if the buffer capacity, number of free frames, total pages and if the buffer is empty
            assertEquals(i, test.getBufferCapacity(), "Buffer capacity is " + i);
            assertEquals(i, test.getFreeFrames(), "Free frames is " + i);
            assertEquals(0, test.getTotalPages(), "Total pages is 0");
            assertTrue(test.isBufferEmpty(), "Buffer should be empty");

        }
    }

    //Test if the createPage method works correctly by seeing if the buffermanger correctly updates the number of free frames, total pages and if the page is dirty
    @Test void testCreatePage_1() {
        BufferManagerImplem test = new BufferManagerImplem(10);
        
        //Creates upto 10 pages
        for(int i=1; i <= 10; i++) {
            
            //Creates page and checks if it was able to create the page
            Page page = test.createPage();
            assertNotNull(page, "Page is created and not null");

            //Checks if the page is dirty, pin count, total pages and free frames are set correctly (pin count is 1, total pages is i and free frames is 10-i)
            assertEquals(test.getPageMetaData(i-1).getPinCount(), 1, "Pin count should be is 1");
            assertEquals(test.getPageMetaData(i-1).isDirty(), true, "Page is supposed to be dirty");
            assertEquals(i, test.getTotalPages(), "Total pages is " + i);
            assertEquals(10-i, test.getFreeFrames(), "Free frames is " + (10-i));
        }
        // The BufferManager should not be able to create more pages as it has reached its capacity and all current pages are pinned
        Page page = test.createPage();
        assertNull(page, "Should not be able to create more pages");
        assertEquals(10, test.getTotalPages(), "Total pages is " + 10);
        assertEquals(0, test.getFreeFrames(), "Free frames is " + 0);

    }

    //Test if the createPage method works correctly in the case where the buffer manager is full and some pages are unpinned
    @Test void testCreatePage_2() throws IOException {
        //Create temporary disk location
        String PATH = "src/test/java/org/bin/createPageTest.bin";
        Files.deleteIfExists(Paths.get(PATH));
        BufferManagerImplem test = new BufferManagerImplem(10);
        test.updateFile(PATH);

        //Creates upto 10 pages
        for(int i=1; i <= 10; i++) {
            //Creates page and checks if it was able to create the page
            Page page = test.createPage();
            assertNotNull(page, "Page is created and not null");

            //Checks if the page is dirty, pin count, total pages and free frames are set correctly (pin count is 1, total pages is i and free frames is 10-i)
            assertEquals(test.getPageMetaData(i-1).getPinCount(), 1, "Pin count should be is 1");
            assertEquals(test.getPageMetaData(i-1).isDirty(), true, "Page is supposed to be dirty");
            assertEquals(i, test.getTotalPages(), "Total pages is " + i);
            assertEquals(10-i, test.getFreeFrames(), "Free frames is " + (10-i));
        }

        //The Buffermanger unpins the first 3 pages and creates 3 more pages
        for(int i = 1; i < 3; i++) {
            test.unpinPage(i);
            Page page = test.createPage();
            assertNotNull(page, "Should be able to create more pages");

            //Checks if the page is dirty, pin count, total pages and free frames are set correctly (pin count is 1, total pages is 10+i and free frames is 10-i)
            assertEquals(test.getPageMetaData(9+i).getPinCount(), 1, "Pin count should be is 1");
            assertEquals(test.getPageMetaData(9+i).isDirty(), true, "Page is supposed to be dirty");
            assertEquals(10+i, test.getTotalPages(), "Total pages is " + (10+i));
            assertEquals(0, test.getFreeFrames(), "Free frames is " + 0);
            assertEquals(-1, test.existsInCache(i));
        }       
    }


    // Test the GetPage method in the case where the page is not in the buffer pool
    @Test void testGetPage_1() throws IOException {
        String PATH = "src/test/java/org/bin/getPageTest_1.bin";
        Files.deleteIfExists(Paths.get(PATH));
        BufferManagerImplem test = new BufferManagerImplem(10);
        test.updateFile(PATH);

        // The manager should not be able to return a page as it has not created any pages with that id
        Page page = test.getPage(58);
        assertNull(page, "Fetched page is not null");
    }


    // Test the GetPage method in the case where the page is in the buffer pool or in memory
    @Test void testGetPage_2() throws IOException {
        // Updates disk location and creates buffer manager
        String PATH = "src/test/java/org/bin/getPageTest_2.bin";
        Files.deleteIfExists(Paths.get(PATH));
        BufferManagerImplem test = new BufferManagerImplem(3);
        test.updateFile(PATH);

        // Store known movieIds and titles
        byte[][] movieIds = new byte[3][9];
        byte[][] titles = new byte[3][30];

        // Create 3 pages and insert a row into each
        for(int i = 0; i < 3; i++) {
            Page page = test.createPage();
            assertNotNull(page, "Page is created and not null");

            movieIds[i][0] = (byte) i;
            for (int j = 0; j < 6; j++) {
                titles[i][j] = (byte) ("movie"+i).charAt(j);
            }
            //adds movieId and title to the page
            page.insertRow(new Row(movieIds[i], titles[i]));

            // Checks if the page is dirty, pin count, total pages and free frames are set correctly (pin count is 1, total pages is i and free frames is 3-i)
            assertEquals(test.getPageMetaData(i).getPinCount(), 1, "Pin count should be is 1");
            assertEquals(test.getPageMetaData(i).isDirty(), true, "Page is supposed to be dirty");
            assertEquals(i+1, test.getTotalPages(), "Total pages is " + (i+1));
            assertEquals(3-i-1, test.getFreeFrames(), "Free frames is " + (3-i-1));
        }

        // Get the pages that are stored in memory and reduce pin count to 0
        for(int i = 0; i < 3; i++) {
            // Fetches the page from the buffer pool
            Page page = test.getPage(i);
            assertNotNull(page, "Fetched page is not null");
            
            //Check if the pincount has gone up to 2 and the page is still dirty, and still in the cache
            assertEquals(test.getPageMetaData(i).getPinCount(), 2, "Pin count should be is 2");
            assertEquals(test.getPageMetaData(i).isDirty(), true, "Page is supposed");
            assertTrue(test.existsInCache(i) != -1);
           
            // Check if the retrieved page still has the same movieId and title
            assertArrayEquals(movieIds[i], page.getRow(0).movieId);
            assertArrayEquals(titles[i], page.getRow(0).title);

            // Unpin the page to 0
            test.unpinPage(i);
            test.unpinPage(i);
            assertEquals(0, test.getPageMetaData(i).getPinCount(), "Pin count should be is 0");
        }

        // Create 2 more pages and remove the page 0 and 1 from the buffer pool
        for(int i = 3; i < 5; i++) {
            // Returns null as the page has not been created yet
            Page page = test.getPage(i);
            assertNull(page, "Fetched page is null");

            // Creates the page and checks if it was able to create the page
            page = test.createPage();
            assertNotNull(page, "Page is created and not null");
            assertTrue(test.existsInCache(i - 3) == -1, "page " + (i-3) + " should not be in cache");
        }
        
        // Verifying that page 0 and 1 are not in the buffer pool and LRU cache is correct
        assertTrue(test.existsInCache(0) ==-1, "page " + 0 + " should not be in cache");
        assertTrue(test.existsInCache(1) ==-1, "page " + 1 + " should not be in cache");
        assertTrue(test.existsInCache(2) == 0, "page " + 2 + " should be in cache");
        assertTrue(test.existsInCache(3) == 1, "page " + 3 + " should be in cache");
        assertTrue(test.existsInCache(4) == 2, "page " + 4 + " should be in cache");
        assertTrue(test.getPageMetaData(2).getPinCount() == 0, "Pin count should be is 0");
        assertTrue(test.getPageMetaData(3).getPinCount() == 1, "Pin count should be is 1");
        
        // Fetches the page 0 which is in memory and evicts the page 2
        Page page = test.getPage(0);
        
        // Checks if the page is dirty, pin count, total pages and free frames are set correctly (pin count is 1, total pages is 5 and free frames is 2)
        assertNotNull(page, "Fetched page should not be null");
        assertEquals(test.getPageMetaData(0).getPinCount(), 1, "Pin count should be is 1");
        assertEquals(test.getPageMetaData(0).isDirty(), false, "Page isn't supposed to be dirty");
        assertTrue(test.existsInCache(2) == -1, "page " + 2 + " should not be in cache");
        
        // Check if the retrieved page still has the same movieId and title
        assertArrayEquals(movieIds[0], page.getRow(0).movieId);
        assertArrayEquals(titles[0], page.getRow(0).title);
    }
    
        
    // Test if MarkDirty method works correctly by seeing if the buffermanger correctly updates the page as dirty
    @Test void testMarkDirty_1() {
        BufferManagerImplem test = new BufferManagerImplem(2);
        Page page = test.createPage();
        int pageId = page.getPid();

        //Checks if the page is already dirty
        assertTrue(test.getPageMetaData(pageId).isDirty(), "Page is marked as dirty");
       
        //marking dirty should not change the dirty bit since it is already dirty
        test.markDirty(pageId);
        assertTrue(test.getPageMetaData(pageId).isDirty(), "Page is marked as dirty");

    }

    // Test the MarkDirty method in the case where the page in the buffer pool
    @Test void testMarkDirty_2() throws IOException {
        // Create temporary disk location
        String PATH = "src/test/java/org/bin/markDirtyTest_2.bin";
        Files.deleteIfExists(Paths.get(PATH)); 
        BufferManagerImplem test = new BufferManagerImplem(2);
        test.updateFile(PATH);
       
        // Create 2 pages and unpin them
        Page page1 = test.createPage();
        Page page2 = test.createPage();
        test.unpinPage(0);
        test.unpinPage(1);

        //Checks if the page and evicted page are not dirty
        Page page3 = test.createPage();
        assertEquals(test.existsInCache(0), -1, "Page 0 is not in cache");
        
        //Gets a page from disk and check if it is not dirty
        Page page1_again = test.getPage(0);
        assertNotNull(page1_again);
        assertFalse(test.getPageMetaData(0).isDirty(), "Page is not marked as dirty");
        
        //Marking dirty should change the dirty bit
        test.markDirty(0);
        assertTrue(test.getPageMetaData(0).isDirty(), "Page is marked as dirty");   
    }


    // Test unpinPage method in the case where the page is in buffer pool or in memory
    @Test void testUnpinPage() {
        BufferManagerImplem test = new BufferManagerImplem(10);
        Page page = test.createPage();
        int pageId = page.getPid();


        //Checks if unpinning page that doesnt exist doesnt crash the buffer manager
        test.unpinPage(100);
      
        //Unpin page 5 times to see if the pin count is still 0
        for (int i = 0; i < 5; i++) {
            test.unpinPage(pageId);
            assertEquals(0, test.getPageMetaData(pageId).getPinCount(), "Page pin count is 0 after unpinning");
        }

        //Pin page 5 times
        for (int i = 0; i < 5; i++) {
            test.getPage(pageId);
            assertEquals(i+1, test.getPageMetaData(pageId).getPinCount(), "Page pin count is 0 after unpinning");
        }

        //Unpin page 5 times to see if the pin count goes down to 0
        for (int i = 0; i < 5; i++) {
            test.unpinPage(pageId);
            assertEquals(5-i-1, test.getPageMetaData(pageId).getPinCount(), "Page pin count is 0 after unpinning");
        }
        
        //Creates a new page and evicts the current page
        test.unpinPage(pageId);
        test.createPage();

        //Should not crash the buffer manager
        test.unpinPage(pageId);
    }
}
