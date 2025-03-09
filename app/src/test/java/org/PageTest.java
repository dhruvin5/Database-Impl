package org;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import Page.PageImpl;
import Row.Row;

class PageTest {

    @Test
    void testInsertRow() {
        PageImpl page = new PageImpl(1);
        for(int i = 0; i < 104; i++) {
            byte[] movieId = new byte[9];
            movieId[0] = (byte) i;
            byte[] title = new byte[30];
            Row row = new Row(movieId, title);
            int rowId = page.insertRow(row);
            assertEquals(i, rowId, "Row ID is't correct");
        }
        byte[] movieId = new byte[9];
        movieId[0] = 104;
        byte[] title = new byte[30];
        int rowId = page.insertRow(new Row(movieId, title));
        assertEquals(-1, rowId, "Page cannot add another row");
    }

    @Test
    void testGetRow() {
        PageImpl page = new PageImpl(1);
        assertNull(page.getRow(0), "Row should be null");
        assertNull(page.getRow(-1), "Row should be null");

        byte[] movieId = new byte[9];
        byte[] title = new byte[30];

        movieId[0] = (byte) 1;
        for (int i = 0; i < 6; i++) {
            title[i] = (byte) "movie1".charAt(i);
        }
        
        Row row = new Row(movieId, title);
        page.insertRow(row);

        Row retrievedRow = page.getRow(0);
        assertNotNull(retrievedRow);
        assertArrayEquals(movieId, retrievedRow.movieId);
        assertArrayEquals(title, retrievedRow.title);
    }

    @Test
    void testIsFull() {
        PageImpl page = new PageImpl(1);
        for (int i = 0; i < 104; i++) {
            assertFalse(page.isFull(), "Page should not be full");
            byte[] movieId = new byte[9];
            byte[] title = new byte[30];
            Row row = new Row(movieId, title);
            page.insertRow(row);
        }
        assertTrue(page.isFull(), "Page should be full");
    }

    @Test
    void testGetPid() {
        for(int i = 0; i < 25; i++) {
            PageImpl page = new PageImpl(i);
            assertNotNull(page, "Page object is not null");
            assertEquals(i, page.getPid(), "Page ID should match. Retrieved: " + page.getPid() + " Expected: " + i);
        }
    }

    @Test
    void testGetRows_1() {
        PageImpl page = new PageImpl(1);
        byte[] rows = page.getRows();
        assertNotNull(rows);
        assertEquals(4096, rows.length);

        for (int i = 0; i < 4096; i++) {
            assertEquals(0, rows[i]);
        }
    }

  
    @Test
    void testGetRows_2() {
        PageImpl page = new PageImpl(1);
        
        byte[] movieId1 = new byte[9];
        byte[] title1 = new byte[30];
        movieId1[0] = 1;
        for (int i = 0; i < 6; i++) {
            title1[i] = (byte) "movie1".charAt(i);
        }
        Row row1 = new Row(movieId1, title1);
        page.insertRow(row1);

        byte[] movieId2 = new byte[9];
        byte[] title2 = new byte[30];
        movieId2[0] = 2;
        for (int i = 0; i < 6; i++) {
            title2[i] = (byte) "movie2".charAt(i);
        }
        Row row2 = new Row(movieId2, title2);
        page.insertRow(row2);

        byte[] rows = page.getRows();
        assertNotNull(rows, "Rows should not be null");
        assertEquals(4096, rows.length);
        
        
        for (int i = 0; i < 9; i++) {
            assertEquals(movieId1[i], rows[i+4], "Movie ID for row 1 should match");
        }
        for (int i = 0; i < 30; i++) {
            assertEquals(title1[i], rows[9 + i+4], "Title for row 1 should match");
        }

        
        for (int i = 0; i < 9; i++) {
            assertEquals(movieId2[i], rows[39 + i+4], "Movie ID for row 2 should match");
        }
        for (int i = 0; i < 30; i++) {
            assertEquals(title2[i], rows[48 + i+4], "Title for row 2 should match");
        }
    }
}
