package org;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import Page.Page;
import Page.PageImpl;
import Row.movieRow;
import configs.Config;

import java.nio.ByteBuffer;

class PageTest {

    /**
     * Tests insertion behavior with null rows and valid rows (movieId=9 bytes, title=30 bytes).
     */
    @Test
    void testInsertRow_1() {
        // Create a new PageImpl with offsets for movieId=9, title=30.
        PageImpl page = new PageImpl(1, 9, 30);

        // Prepare valid data
        byte[] validMovieId = new byte[9];
        byte[] validTitle   = new byte[30];
        validMovieId[0] = (byte) 1;
        for (int i = 0; i < 6; i++) {
            validTitle[i] = (byte) "movie1".charAt(i);
        }

        // 1) Insert a null row => should fail
        assertEquals(-1, page.insertRow(null),
            "Inserting null Row object should return -1");

        // 2) Insert row with null movieId and title => fails
        assertEquals(-1, page.insertRow(new movieRow(null, null)),
            "Row with both fields null should return -1");

        // 3) Insert row with valid movieId but null title => fails
        assertEquals(-1, page.insertRow(new movieRow(validMovieId, null)),
            "Row with null title should return -1");

        // 4) Insert row with null movieId but valid title => fails
        assertEquals(-1, page.insertRow(new movieRow(null, validTitle)),
            "Row with null movieId should return -1");

        // 5) Insert a fully valid row => succeeds
        int firstRowId = page.insertRow(new movieRow(validMovieId, validTitle));
        assertEquals(0, firstRowId,
            "The first valid row should be inserted at index 0");
    }

    /**
     * Tests inserting multiple rows until the page is full (with 9+30=39 bytes per row).
     */
    @Test
    void testInsertRow_2() {
        // Construct the page with (movieId=9, title=30).
        PageImpl page = new PageImpl(1, 9, 30);

        // According to your constructor, MAX_ROW_COUNT = (PAGE_SIZE - 4)/39
        int expectedMax = (Config.PAGE_SIZE - 4) / (9 + 30);

        for (int i = 0; i < expectedMax; i++) {
            byte[] id = new byte[9];
            id[0] = (byte) i;
            byte[] title = new byte[30];
            int rowId = page.insertRow(new movieRow(id, title));
            assertEquals(i, rowId, "Inserted row ID should match " + i);
        }

        // Page should now be full
        assertTrue(page.isFull(),
            "Page should be full after inserting " + expectedMax + " rows");

        // Attempt one more
        int failRow = page.insertRow(new movieRow(new byte[9], new byte[30]));
        assertEquals(-1, failRow,
            "Inserting past capacity should return -1");
    }

    /**
     * Tests retrieving rows (valid and invalid indices).
     */
    @Test
    void testGetRow() {
        PageImpl page = new PageImpl(1, 9, 30);

        // No rows => out-of-range => null
        assertNull(page.getRow(0), "Empty page => getRow(0) returns null");
        assertNull(page.getRow(-1), "Negative index => null");
        assertNull(page.getRow(999), "Out of range => null");

        // Insert a row
        byte[] id = new byte[9];
        id[0] = 42;
        byte[] title = new byte[30];
        for (int i = 0; i < 5; i++) {
            title[i] = (byte) "abcde".charAt(i);
        }
        int rowId = page.insertRow(new movieRow(id, title));
        assertEquals(0, rowId, "First inserted row => index 0");

        // Retrieve row 0 => should match
        var row0 = page.getRow(0);
        assertNotNull(row0, "Row 0 should exist after insertion");
        assertArrayEquals(id, row0.movieId, "MovieId should match");
        assertArrayEquals(title, row0.title, "Title should match");

        // Attempt row 1 => doesn't exist => null
        assertNull(page.getRow(1), "We only inserted 1 row => getRow(1) is null");
    }

    /**
     * Tests progressively inserting rows until the page is reported full.
     */
    @Test
    void testIsFull() {
        PageImpl page = new PageImpl(2, 9, 30);
        int maxRows = (Config.PAGE_SIZE - 4) / (9 + 30);

        for (int i = 0; i < maxRows; i++) {
            assertFalse(page.isFull(), "Should not be full at iteration i=" + i);
            page.insertRow(new movieRow(new byte[9], new byte[30]));
        }

        assertTrue(page.isFull(), 
            "Now it should be full after " + maxRows + " inserts");

        // Insert one more => fail
        assertEquals(-1, page.insertRow(new movieRow(new byte[9], new byte[30])),
            "Extra insert beyond capacity => -1");
    }

    /**
     * Checks that getPid() returns the pageId we gave to the constructor.
     */
    @Test
    void testGetPid() {
        // Try some sample page IDs
        for (int i = 0; i < 5; i++) {
            PageImpl page = new PageImpl(i, 9, 30);
            assertEquals(i, page.getPid(),
                "page.getPid() should match the constructor argument " + i);
        }

        // Negative ID
        PageImpl negPage = new PageImpl(-99, 9, 30);
        assertEquals(-99, negPage.getPid(), 
            "Should handle negative IDs as well");
    }

    /**
     * If we haven't inserted anything, the raw bytes are all zero 
     * (including the first 4 for rowCount).
     */
    @Test
    void testGetRows_1() {
        PageImpl page = new PageImpl(5, 9, 30);
        byte[] raw = page.getRows();
        assertNotNull(raw, "getRows() must not return null");
        assertEquals(Config.PAGE_SIZE, raw.length, 
            "Should match the 4KB (or config-based) page size");

        // The entire array, including [0..3] for rowCount=0, should be zero
        for (byte b : raw) {
            assertEquals(0, b, "No data inserted => all zeros in raw bytes");
        }
    }

    /**
     * Insert two rows, then verify rowCount and row data in raw byte array.
     */
    @Test
    void testGetRows_2() {
        PageImpl page = new PageImpl(10, 9, 30);

        // Insert row1
        byte[] id1 = new byte[9];
        id1[0] = 1;
        byte[] title1 = new byte[30];
        for (int i = 0; i < 6; i++) {
            title1[i] = (byte) "movie1".charAt(i);
        }
        page.insertRow(new movieRow(id1, title1));

        // Insert row2
        byte[] id2 = new byte[9];
        id2[0] = 2;
        byte[] title2 = new byte[30];
        for (int i = 0; i < 6; i++) {
            title2[i] = (byte) "movie2".charAt(i);
        }
        page.insertRow(new movieRow(id2, title2));

        byte[] raw = page.getRows();
        assertNotNull(raw, "Raw page data must not be null");
        assertEquals(Config.PAGE_SIZE, raw.length, 
            "Must be full page size");

        // The first 4 bytes => integer rowCount
        int storedCount = ByteBuffer.wrap(raw, 0, 4).getInt();
        assertEquals(2, storedCount, "We inserted exactly 2 rows");

        // row0 => offset=4 => 9 bytes of movieId + 30 bytes of title => 39 total
        int rowSize = 9 + 30;
        int row0Start = 4;

        // Check row0 movieId
        for (int i = 0; i < 9; i++) {
            assertEquals(id1[i], raw[row0Start + i],
                         "row0's movieId mismatch at i=" + i);
        }
        // Check row0 title
        for (int i = 0; i < 30; i++) {
            assertEquals(title1[i], raw[row0Start + 9 + i],
                         "row0's title mismatch at i=" + i);
        }

        // row1 => offset=4+39=43
        int row1Start = row0Start + rowSize;

        // Check row1 movieId
        for (int i = 0; i < 9; i++) {
            assertEquals(id2[i], raw[row1Start + i],
                         "row1's movieId mismatch at i=" + i);
        }
        // Check row1 title
        for (int i = 0; i < 30; i++) {
            assertEquals(title2[i], raw[row1Start + 9 + i],
                         "row1's title mismatch at i=" + i);
        }
    }
}
