package org;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import Page.Page;
import Page.PageImpl;
import Row.movieRow;
import configs.Config;

import java.nio.ByteBuffer;

class PageTest {

    // Test the insertion behavior with null and valid rows.
    @Test
    void testInsertRow_1() {
        PageImpl page = new PageImpl(1, 9, 30);

        byte[] validMovieId = new byte[9];
        byte[] validTitle   = new byte[30];
        validMovieId[0] = (byte) 1;
        for (int i = 0; i < 6; i++) {
            validTitle[i] = (byte) "movie1".charAt(i);
        }

        assertEquals(-1, page.insertRow(null));
        assertEquals(-1, page.insertRow(new movieRow(null, null)));
        assertEquals(-1, page.insertRow(new movieRow(validMovieId, null)));
        assertEquals(-1, page.insertRow(new movieRow(null, validTitle)));

        int firstRowId = page.insertRow(new movieRow(validMovieId, validTitle));
        assertEquals(0, firstRowId);
    }

    // Test inserting multiple rows until the page is full.
    @Test
    void testInsertRow_2() {
        PageImpl page = new PageImpl(1, 9, 30);

        int expectedMax = (Config.PAGE_SIZE - 4) / (9 + 30);

        for (int i = 0; i < expectedMax; i++) {
            byte[] id = new byte[9];
            id[0] = (byte) i;
            byte[] title = new byte[30];
            int rowId = page.insertRow(new movieRow(id, title));
            assertEquals(i, rowId);
        }

        assertTrue(page.isFull());

        int failRow = page.insertRow(new movieRow(new byte[9], new byte[30]));
        assertEquals(-1, failRow);
    }

    // Test retrieving rows for valid and invalid indices.
    @Test
    void testGetRow() {
        PageImpl page = new PageImpl(1, 9, 30);

        assertNull(page.getRow(0));
        assertNull(page.getRow(-1));
        assertNull(page.getRow(999));

        byte[] id = new byte[9];
        id[0] = 42;
        byte[] title = new byte[30];
        for (int i = 0; i < 5; i++) {
            title[i] = (byte) "abcde".charAt(i);
        }
        int rowId = page.insertRow(new movieRow(id, title));
        assertEquals(0, rowId);

        var row0 = page.getRow(0);
        assertNotNull(row0);
        assertArrayEquals(id, row0.movieId);
        assertArrayEquals(title, row0.title);

        assertNull(page.getRow(1));
    }

    // Test progressively inserting rows until the page is reported full.
    @Test
    void testIsFull() {
        PageImpl page = new PageImpl(2, 9, 30);
        int maxRows = (Config.PAGE_SIZE - 4) / (9 + 30);

        for (int i = 0; i < maxRows; i++) {
            assertFalse(page.isFull());
            page.insertRow(new movieRow(new byte[9], new byte[30]));
        }

        assertTrue(page.isFull());

        assertEquals(-1, page.insertRow(new movieRow(new byte[9], new byte[30])));
    }

    // Test if getPid() returns the correct pageId.
    @Test
    void testGetPid() {
        for (int i = 0; i < 5; i++) {
            PageImpl page = new PageImpl(i, 9, 30);
            assertEquals(i, page.getPid());
        }

        PageImpl negPage = new PageImpl(-99, 9, 30);
        assertEquals(-99, negPage.getPid());
    }

    // Test that getRows() returns a zero-initialized byte array if no rows have been inserted.
    @Test
    void testGetRows_1() {
        PageImpl page = new PageImpl(5, 9, 30);
        byte[] raw = page.getRows();
        assertNotNull(raw);
        assertEquals(Config.PAGE_SIZE, raw.length);

        for (byte b : raw) {
            assertEquals(0, b);
        }
    }

    // Test inserting rows and then verifying row data in the raw byte array.
    @Test
    void testGetRows_2() {
        PageImpl page = new PageImpl(10, 9, 30);

        byte[] id1 = new byte[9];
        id1[0] = 1;
        byte[] title1 = new byte[30];
        for (int i = 0; i < 6; i++) {
            title1[i] = (byte) "movie1".charAt(i);
        }
        page.insertRow(new movieRow(id1, title1));

        byte[] id2 = new byte[9];
        id2[0] = 2;
        byte[] title2 = new byte[30];
        for (int i = 0; i < 6; i++) {
            title2[i] = (byte) "movie2".charAt(i);
        }
        page.insertRow(new movieRow(id2, title2));

        byte[] raw = page.getRows();
        assertNotNull(raw);
        assertEquals(Config.PAGE_SIZE, raw.length);

        int storedCount = ByteBuffer.wrap(raw, 0, 4).getInt();
        assertEquals(2, storedCount);

        int rowSize = 9 + 30;
        int row0Start = 4;

        for (int i = 0; i < 9; i++) {
            assertEquals(id1[i], raw[row0Start + i]);
        }
        for (int i = 0; i < 30; i++) {
            assertEquals(title1[i], raw[row0Start + 9 + i]);
        }

        int row1Start = row0Start + rowSize;

        for (int i = 0; i < 9; i++) {
            assertEquals(id2[i], raw[row1Start + i]);
        }
        for (int i = 0; i < 30; i++) {
            assertEquals(title2[i], raw[row1Start + 9 + i]);
        }
    }
}
