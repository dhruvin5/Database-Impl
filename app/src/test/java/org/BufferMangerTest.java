package org;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import buffer.BufferManagerImplem;
import java.nio.file.*;
import Page.Page;
import Row.Row;
import java.io.IOException;

class BufferManagerImplemTest {
    @Test void testingCorrectBufferMangerIntialization() {
        BufferManagerImplem test = new BufferManagerImplem(1);
        for(int i=1; i <= 5; i++) {
            test = new BufferManagerImplem(i);
            assertNotNull(test, "BufferManagerImplem object is not null");
            assertEquals(i, test.getBufferCapacity(), "Buffer capacity is " + i);
            assertEquals(i, test.getFreeFrames(), "Free frames is " + i);
            assertEquals(0, test.getTotalPages(), "Total pages is 0");
            assertTrue(test.isBufferEmpty(), "Buffer should be empty");

        }
    }

    @Test void testCreatePage_1() {
        BufferManagerImplem test = new BufferManagerImplem(10);
        for(int i=1; i <= 10; i++) {
            Page page = test.createPage();
            assertNotNull(page, "Page is created and not null");
            assertEquals(test.getPageMetaData(i-1).getPinCount(), 1, "Pin count should be is 1");
            assertEquals(test.getPageMetaData(i-1).isDirty(), true, "Page is supposed to be dirty");
            assertEquals(i, test.getTotalPages(), "Total pages is " + i);
            assertEquals(10-i, test.getFreeFrames(), "Free frames is " + (10-i));
        }
        Page page = test.createPage();
        assertNull(page, "Should not be able to create more pages");
        assertEquals(10, test.getTotalPages(), "Total pages is " + 10);
        assertEquals(0, test.getFreeFrames(), "Free frames is " + 0);

    }


    @Test void testCreatePage_2() throws IOException {
        String PATH = "src/test/java/org/bin/createPageTest.bin";
        Files.deleteIfExists(Paths.get(PATH));
        BufferManagerImplem test = new BufferManagerImplem(10);
        test.updateFile(PATH);
        for(int i=1; i <= 10; i++) {
            Page page = test.createPage();
            assertNotNull(page, "Page is created and not null");
            assertEquals(test.getPageMetaData(i-1).getPinCount(), 1, "Pin count should be is 1");
            assertEquals(test.getPageMetaData(i-1).isDirty(), true, "Page is supposed to be dirty");
            assertEquals(i, test.getTotalPages(), "Total pages is " + i);
            assertEquals(10-i, test.getFreeFrames(), "Free frames is " + (10-i));
        }
        for(int i = 1; i < 3; i++) {
            test.unpinPage(i);
            Page page = test.createPage();
            assertNotNull(page, "Should not be able to create more pages");
            assertEquals(test.getPageMetaData(9+i).getPinCount(), 1, "Pin count should be is 1");
            assertEquals(test.getPageMetaData(9+i).isDirty(), true, "Page is supposed to be dirty");
            assertEquals(10+i, test.getTotalPages(), "Total pages is " + (10+i));
            assertEquals(0, test.getFreeFrames(), "Free frames is " + 0);
            assertEquals(-1, test.existsInCache(i));
        }       
    }


    @Test void testGetPage_1() throws IOException {
        String PATH = "src/test/java/org/bin/getPageTest_1.bin";
        Files.deleteIfExists(Paths.get(PATH));
        BufferManagerImplem test = new BufferManagerImplem(10);
        test.updateFile(PATH);

        Page page = test.getPage(58);
        assertNull(page, "Fetched page is not null");
    }


    @Test void testGetPage_2() throws IOException {
        String PATH = "src/test/java/org/bin/getPageTest_2.bin";
        Files.deleteIfExists(Paths.get(PATH)); 
        BufferManagerImplem test = new BufferManagerImplem(3);
        test.updateFile(PATH);

        byte[][] movieIds = new byte[3][9];
        byte[][] titles = new byte[3][30];
        for(int i = 0; i < 3; i++) {
            Page page = test.createPage();
            
            assertNotNull(page, "Page is created and not null");

            movieIds[i][0] = (byte) 1;
            for (int j = 0; j < 6; j++) {
                titles[i][j] = (byte) ("movie"+i).charAt(j);
            }
            page.insertRow(new Row(movieIds[i], titles[i]));

            
            assertEquals(test.getPageMetaData(i).getPinCount(), 1, "Pin count should be is 1");
            assertEquals(test.getPageMetaData(i).isDirty(), true, "Page is supposed to be dirty");
            assertEquals(i+1, test.getTotalPages(), "Total pages is " + (i+1));
            assertEquals(3-i-1, test.getFreeFrames(), "Free frames is " + (3-i-1));
        }

        for(int i = 0; i < 3; i++) {
            Page page = test.getPage(i);
            assertNotNull(page, "Fetched page is not null");
            assertEquals(test.getPageMetaData(i).getPinCount(), 2, "Pin count should be is 1");
            assertEquals(test.getPageMetaData(i).isDirty(), true, "Page is supposed");
            assertTrue(test.existsInCache(i) != -1);
           
            assertArrayEquals(movieIds[i], page.getRow(0).movieId);
            assertArrayEquals(titles[i], page.getRow(0).title);
            test.unpinPage(i);
            test.unpinPage(i);
            assertEquals(0, test.getPageMetaData(i).getPinCount(), "Pin count should be is 0");
        }

        for(int i = 3; i < 5; i++) {
            Page page = test.getPage(i);
            assertNull(page, "Fetched page is null");
            page = test.createPage();
            assertNotNull(page, "Page is created and not null");
            assertTrue(test.existsInCache(i - 3) == -1, "page " + (i-3) + " should not be in cache");
        }
        
        assertTrue(test.existsInCache(0) ==-1, "page " + 0 + " should not be in cache");
        assertTrue(test.existsInCache(1) ==-1, "page " + 1 + " should not be in cache");
        assertTrue(test.existsInCache(2) == 0, "page " + 2 + " should be in cache");
        assertTrue(test.existsInCache(3) == 1, "page " + 2 + " should be in cache");
        assertTrue(test.existsInCache(4) == 2, "page " + 2 + " should be in cache");
        assertTrue(test.getPageMetaData(2).getPinCount() == 0, "Pin count should be is 0");
        assertTrue(test.getPageMetaData(3).getPinCount() == 1, "Pin count should be is 1");
        
        
        
        Page page = test.getPage(0);
        
        assertNotNull(page, "Fetched page should not be null");
        assertEquals(test.getPageMetaData(0).getPinCount(), 1, "Pin count should be is 1");
        assertEquals(test.getPageMetaData(0).isDirty(), false, "Page isn't supposed to be dirty");
        assertTrue(test.existsInCache(2) == -1, "page " + 2 + " should not be in cache");
        assertArrayEquals(movieIds[0], page.getRow(0).movieId);
        assertArrayEquals(titles[0], page.getRow(0).title);
    }
    
        

    @Test void testMarkDirty_1() {
        BufferManagerImplem test = new BufferManagerImplem(2);
        Page page = test.createPage();
        int pageId = page.getPid();
        assertTrue(test.getPageMetaData(pageId).isDirty(), "Page is marked as dirty");
        test.markDirty(pageId);
        assertTrue(test.getPageMetaData(pageId).isDirty(), "Page is marked as dirty");
    }

    @Test void testMarkDirty_2() throws IOException {
        String PATH = "src/test/java/org/bin/markDirtyTest_2.bin";
        Files.deleteIfExists(Paths.get(PATH)); 
        BufferManagerImplem test = new BufferManagerImplem(2);
        test.updateFile(PATH);
       
        Page page = test.createPage();
       
        byte[] movieId = new byte[9];
        byte[] title = new byte[30];
        
        int pageId = page.getPid();
        
    }


    @Test void testUnpinPage() {
        BufferManagerImplem test = new BufferManagerImplem(10);
        Page page = test.createPage();
        int pageId = page.getPid();


        for (int i = 0; i < 5; i++) {
            test.unpinPage(pageId);
            assertEquals(0, test.getPageMetaData(pageId).getPinCount(), "Page pin count is 0 after unpinning");
        }

        for (int i = 0; i < 5; i++) {
            test.getPage(pageId);
            assertEquals(i+1, test.getPageMetaData(pageId).getPinCount(), "Page pin count is 0 after unpinning");
        }

        for (int i = 0; i < 5; i++) {
            test.unpinPage(pageId);
            assertEquals(5-i-1, test.getPageMetaData(pageId).getPinCount(), "Page pin count is 0 after unpinning");
        }
    }
}
