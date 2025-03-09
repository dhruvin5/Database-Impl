package org;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import Page.PageImpl;
import Row.Row;

class PageTest {
    
    //Test if insert row works when adding null row and valid rows
    @Test
    void testInsertRow_1() {
        //creates a page
        PageImpl page = new PageImpl(1);
        
        byte[] movieId = new byte[9];
        byte[] title = new byte[30];
        
        movieId[0] = (byte) 1;
        for (int i = 0; i < 6; i++) {
            title[i] = (byte) "movie1".charAt(i);
        }

        //Test if page can add null row
        assertEquals(page.insertRow(null), -1, "Page cannot add null row"); 
        //Test if page can add row with null movieId and title
        assertEquals(page.insertRow(new Row(null, null)), -1, "Page cannot add null row");
        //Test if page can add row with null movieId
        assertEquals(page.insertRow(new Row(movieId, null)), -1, "Page cannot add null movieId");
        //Test if page can add row with null title
        assertEquals(page.insertRow(new Row(null, title)), -1, "Page cannot add null title");
        //Test if page can add row with valid movieId and title
        assertEquals(page.insertRow(new Row(movieId, title)), 0, "Page can add row");

    }


    // Test if insertingRows works when adding more than one row into the page
    @Test
    void testInsertRow_2() {
        //creates a page
        PageImpl page = new PageImpl(1);

        //Repeatedly adds rows to the page
        for(int i = 0; i < 104; i++) {
            byte[] movieId = new byte[9];
            movieId[0] = (byte) i;
            byte[] title = new byte[30];
            Row row = new Row(movieId, title);
            int rowId = page.insertRow(row);
            assertEquals(i, rowId, "Row ID is't correct");
        }
        //The page is full and should not able to add another row
        byte[] movieId = new byte[9];
        movieId[0] = 104;
        byte[] title = new byte[30];
        int rowId = page.insertRow(new Row(movieId, title));
        assertEquals(-1, rowId, "Page cannot add another row");
    }

    // Test if GetRow works by getting a row that is and not in the page, verifying if the data stored is corrected
    @Test
    void testGetRow() {
        //creates a page
        PageImpl page = new PageImpl(1);
        //Test if page can get row that is not in the page
        assertNull(page.getRow(0), "Row should be null");
        assertNull(page.getRow(-1), "Row should be null");

        //Test if page can get row that is in the page
        byte[] movieId = new byte[9];
        byte[] title = new byte[30];

        movieId[0] = (byte) 1;
        for (int i = 0; i < 6; i++) {
            title[i] = (byte) "movie1".charAt(i);
        }
        
        // adds row to the page
        Row row = new Row(movieId, title);
        page.insertRow(row);

        // retrieves the row
        Row retrievedRow = page.getRow(0);
        assertNotNull(retrievedRow);

        // verifies the data stored in the row is correct
        assertArrayEquals(movieId, retrievedRow.movieId);
        assertArrayEquals(title, retrievedRow.title);
    }

    // Checks if IsFull works by checking the status of the page as we add more rows
    @Test
    void testIsFull() {
        PageImpl page = new PageImpl(1);
        // Adds new rows to the page
        for (int i = 0; i < 104; i++) {
            assertFalse(page.isFull(), "Page should not be full");
            byte[] movieId = new byte[9];
            byte[] title = new byte[30];
            Row row = new Row(movieId, title);
            page.insertRow(row);
        }
        
        // The page is full and should not able to add another row
        assertTrue(page.isFull(), "Page should be full");
        assertEquals(-1, page.insertRow(new Row(new byte[9], new byte[30])), "Page cannot add another row");
    }

    // Checks if the page id can be retrieved
    @Test
    void testGetPid() {
        // Creates a page and checks if the page id is correct
        for(int i = 0; i < 25; i++) {
            PageImpl page = new PageImpl(i);
            assertNotNull(page, "Page object is not null");
            assertEquals(i, page.getPid(), "Page ID should match - Retrieved: " + page.getPid() + " Expected: " + i);
        }
        // Checks if the page id is correct when the page id is negative
        PageImpl page = new PageImpl(-1);
        assertNotNull(page, "Page object is not null");
        assertEquals(-1, page.getPid(), "Page ID should match - Retrieved: " + page.getPid() + " Expected: " + -1);

    }

    // Test GetRows when the page is empty and when the page has no rows
    @Test
    void testGetRows_1() {
        PageImpl page = new PageImpl(1);
        byte[] rows = page.getRows();
        assertNotNull(rows);
        assertEquals(4096, rows.length);

        // Check if all bytes are zero
        for (int i = 0; i < 4096; i++) {
            assertEquals(0, rows[i]);
        }
    }

  
    // Test GetRows when the page has data in the rows
    @Test
    void testGetRows_2() {
        PageImpl page = new PageImpl(1);
        

        byte[] movieId1 = new byte[9];
        byte[] title1 = new byte[30];
        movieId1[0] = 1;
        for (int i = 0; i < 6; i++) {
            title1[i] = (byte) "movie1".charAt(i);
        }

        // adds row1 to the page
        Row row1 = new Row(movieId1, title1);
        page.insertRow(row1);

        byte[] movieId2 = new byte[9];
        byte[] title2 = new byte[30];
        movieId2[0] = 2;
        for (int i = 0; i < 6; i++) {
            title2[i] = (byte) "movie2".charAt(i);
        }

        // adds row2 to the page
        Row row2 = new Row(movieId2, title2);
        page.insertRow(row2);

        // Check the row statistics are correct
        byte[] rows = page.getRows();
        assertNotNull(rows, "Rows should not be null");
        assertEquals(4096, rows.length);
        
        
        //Check if row1 moveId and title are correct in rows
        for (int i = 0; i < 9; i++) {
            assertEquals(movieId1[i], rows[i+4], "Movie ID for row 1 should match");
        }
        for (int i = 0; i < 30; i++) {
            assertEquals(title1[i], rows[9 + i+4], "Title for row 1 should match");
        }

        
        //Check if row2 moveId and title are correct in rows
        for (int i = 0; i < 9; i++) {
            assertEquals(movieId2[i], rows[39 + i+4], "Movie ID for row 2 should match");
        }
        for (int i = 0; i < 30; i++) {
            assertEquals(title2[i], rows[48 + i+4], "Title for row 2 should match");
        }
    }
}
