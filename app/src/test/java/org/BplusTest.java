package org;

import Bplus.BplusTreeImplem;
import Bplus.Rid;
import buffer.BufferManager;
import buffer.BufferManagerImplem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.nio.charset.StandardCharsets;
import Row.Row;
import Page.Page;
import static org.junit.jupiter.api.Assertions.*;

class BplusTest {

    public BplusTreeImplem<String> titleIndex;
    public BplusTreeImplem<String> movieIdIndex;
    public BufferManager bufferManager;

    @BeforeEach
    void setUp() {
        try {
            bufferManager = new BufferManagerImplem(7);
            titleIndex = new BplusTreeImplem<>("title_index.bin", bufferManager);
            movieIdIndex = new BplusTreeImplem<>("movie_Id_index.bin", bufferManager);
            bufferManager.force();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Test for inserting a movie title and searching it
    @Test
    void testInsertAndSearchTitle() {
        try {
            String movieTitle = "Inception"; 
            byte[] movieTitleBytes = new byte[30];
            byte[] titleBytes = movieTitle.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(titleBytes, 0, movieTitleBytes, 0, titleBytes.length);
            Rid movieRid = new Rid(1, 0);
            titleIndex.insert(new String(movieTitleBytes, StandardCharsets.UTF_8), movieRid);
            Iterator<Rid> result = titleIndex.search(new String(movieTitleBytes, StandardCharsets.UTF_8));
            assertTrue(result.hasNext(), "Title should be found in the index.");
            Rid foundRid = result.next();
            assertEquals(movieRid.toString(), foundRid.toString(), "The Rid from the index should match the inserted Rid.");
            Page page = bufferManager.getPage(foundRid.getPageId(), "movies.bin");
            bufferManager.unpinPage(foundRid.getPageId(),"movies.bin");

            Row rowFromFile = page.getRow(foundRid.getSlotId());
            String titleFromFile = new String(rowFromFile.title, StandardCharsets.UTF_8);
            assertEquals(movieTitle, titleFromFile, "The title retrieved from the index should match the title in the file.");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Test for inserting a movie ID and searching it
    @Test
    void testInsertAndSearchMovieId() {
        try {
            String movieId = "tt1375666"; 
            Rid movieRid = new Rid(2, 0);
            movieIdIndex.insert(movieId, movieRid);
            Iterator<Rid> result = movieIdIndex.search(movieId);
            assertTrue(result.hasNext(), "MovieId should be found in the index.");
            Rid foundRid = result.next();
            assertEquals(movieRid.toString(), foundRid.toString(), "The Rid from the index should match the inserted Rid.");
            Page page = bufferManager.getPage(foundRid.getPageId(), "movies.bin");
            bufferManager.unpinPage(foundRid.getPageId(), "movies.bin");

            Row rowFromFile = page.getRow(foundRid.getSlotId());
            String movieIdFromFile = new String(rowFromFile.movieId, StandardCharsets.UTF_8);
            assertEquals(movieId, movieIdFromFile, "The movieId retrieved from the index should match the movieId in the file.");            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Test for searching a non-existent movie title
    @Test
    void testSearchNonExistentTitle() {
        try {
            Rid ridInception = new Rid(1, 0);
            Rid ridInterstellar = new Rid(2, 0);
            byte[] titleInceptionBytes = new byte[30];
            byte[] titleInterstellarBytes = new byte[30];
            System.arraycopy("Inception".getBytes(StandardCharsets.UTF_8), 0, titleInceptionBytes, 0, "Inception".length());
            System.arraycopy("Interstellar".getBytes(StandardCharsets.UTF_8), 0, titleInterstellarBytes, 0, "Interstellar".length());
            titleIndex.insert(new String(titleInceptionBytes, StandardCharsets.UTF_8), ridInception);
            titleIndex.insert(new String(titleInterstellarBytes, StandardCharsets.UTF_8), ridInterstellar);
            byte[] nonExistentTitleBytes = new byte[30];
            System.arraycopy("Incredibles".getBytes(StandardCharsets.UTF_8), 0, nonExistentTitleBytes, 0, "Incredibles".length());
            Iterator<Rid> result = titleIndex.search(new String(nonExistentTitleBytes, StandardCharsets.UTF_8));
            assertFalse(result.hasNext(), "The search should return no results for a non-existent title.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Test for searching a non-existent movie ID
    @Test
    void testSearchNonExistentMovieId() {
        try {
            movieIdIndex.insert("tt1375666", new Rid(1, 0));
            movieIdIndex.insert("tt0120338", new Rid(2, 0));
            String nonExistentMovieId = "tt9999999";
            Iterator<Rid> result = movieIdIndex.search(nonExistentMovieId);
            assertFalse(result.hasNext(), "The search should return no results for a non-existent movieId.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Test for range search with movie titles
    @Test
    void testRangeSearchTitle() {
        try {
            Rid ridInception = new Rid(1, 0);
            Rid ridInterstellar = new Rid(2, 0);
            Rid ridDarkKnight = new Rid(3, 0);
            byte[] titleInceptionBytes = new byte[30];
            byte[] titleInterstellarBytes = new byte[30];
            byte[] titleDarkKnightBytes = new byte[30];
            System.arraycopy("Inception".getBytes(StandardCharsets.UTF_8), 0, titleInceptionBytes, 0, "Inception".length());
            System.arraycopy("Interstellar".getBytes(StandardCharsets.UTF_8), 0, titleInterstellarBytes, 0, "Interstellar".length());
            System.arraycopy("The Dark Knight".getBytes(StandardCharsets.UTF_8), 0, titleDarkKnightBytes, 0, "The Dark Knight".length());
            titleIndex.insert(new String(titleInceptionBytes, StandardCharsets.UTF_8), ridInception);
            titleIndex.insert(new String(titleInterstellarBytes, StandardCharsets.UTF_8), ridInterstellar);
            titleIndex.insert(new String(titleDarkKnightBytes, StandardCharsets.UTF_8), ridDarkKnight);
            Iterator<Rid> rangeResults = titleIndex.rangeSearch("Inception", "Interstellar");
            int count = 1;
            List<Rid> foundRids = new ArrayList<>();
            Rid rid;
            while (rangeResults.hasNext()) {
                rid = rangeResults.next();
                foundRids.add(rid);
                count++;
            }
            assertEquals(2, count, "Range search should return 2 records within the specified range.");
            assertEquals(ridInception.toString(), foundRids.get(0).toString(), "First Rid should match the inserted one for 'Inception'");
            assertEquals(ridInterstellar.toString(), foundRids.get(1).toString(), "Second Rid should match the inserted one for 'Interstellar'");
            for (int i = 0; i < foundRids.size(); i++) {
                Page page = bufferManager.getPage(foundRids.get(i).getPageId(), "movies.bin");
                bufferManager.unpinPage(foundRids.get(i).getPageId(), "movies.bin");
                Row rowFromFile = page.getRow(foundRids.get(i).getSlotId());
                String titleFromFile = new String(rowFromFile.title, StandardCharsets.UTF_8);
                assertEquals(i == 0 ? "Inception" : "Interstellar", titleFromFile, "The title from the file should match the title in the index.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Test for range search with movie IDs
    @Test
    void testRangeSearchMovieId() {
        try {
            Rid rid1 = new Rid(1, 0);
            Rid rid2 = new Rid(2, 0);
            Rid rid3 = new Rid(3, 0);
            movieIdIndex.insert("tt1375666", rid1);
            movieIdIndex.insert("tt0120338", rid2);
            movieIdIndex.insert("tt0100012", rid3);
            Iterator<Rid> rangeResults = movieIdIndex.rangeSearch("tt0100000", "tt0125000");
            int count = 0;
            List<Rid> foundRids = new ArrayList<>();
            Rid rid;
            while (rangeResults.hasNext()) {
                rid = rangeResults.next();
                foundRids.add(rid);
                count++;
            }
            assertEquals(2, count, "Range search should return 2 records within the specified range.");
            assertEquals(rid3.toString(), foundRids.get(0).toString(), "First Rid should match the inserted one for 'tt1375666'");
            assertEquals(rid2.toString(), foundRids.get(1).toString(), "Second Rid should match the inserted one for 'tt0120338'");
            for (int i = 0; i < foundRids.size(); i++) {
                Page page = bufferManager.getPage(foundRids.get(i).getPageId(), "movies.bin");
                bufferManager.unpinPage(foundRids.get(i).getPageId(), "movies.bin");
                Row rowFromFile = page.getRow(foundRids.get(i).getSlotId());
                String movieIdFromFile = new String(rowFromFile.movieId, StandardCharsets.UTF_8);
                assertEquals(i == 0 ? "tt0100012" : "tt0120338", movieIdFromFile, "The movie ID from the file should match the movie ID in the index.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Test for empty range search with movie titles
    @Test
    void testEmptyRangeSearchTitle() {
        try {
            byte[] startTitleBytes = new byte[30];
            byte[] endTitleBytes = new byte[30];
            System.arraycopy("Harry Potter".getBytes(StandardCharsets.UTF_8), 0, startTitleBytes, 0, "A".length());
            System.arraycopy("Twilight".getBytes(StandardCharsets.UTF_8), 0, endTitleBytes, 0, "B".length());
            titleIndex.insert(new String(startTitleBytes, StandardCharsets.UTF_8), new Rid(1, 0));
            titleIndex.insert(new String(endTitleBytes, StandardCharsets.UTF_8), new Rid(2, 0));
            Iterator<Rid> rangeResults = titleIndex.rangeSearch("A", "B");
            assertFalse(rangeResults.hasNext(), "Range search with no results should return empty.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Test for empty range search with movie IDs
    @Test
    void testEmptyRangeSearchMovieId() {
        try {
            Iterator<Rid> rangeResults = movieIdIndex.rangeSearch("tt0000001", "tt0000010");
            assertFalse(rangeResults.hasNext(), "Range search with no results should return empty.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Test for inserting multiple titles and searching for a specific title
    @Test
    void testInsertMultipleTitlesAndSearch() {
        try {
            byte[] titleInceptionBytes = new byte[30];
            byte[] titleInterstellarBytes = new byte[30];
            byte[] titleDunkirkBytes = new byte[30];
            System.arraycopy("Inception".getBytes(StandardCharsets.UTF_8), 0, titleInceptionBytes, 0, "Inception".length());
            System.arraycopy("Interstellar".getBytes(StandardCharsets.UTF_8), 0, titleInterstellarBytes, 0, "Interstellar".length());
            System.arraycopy("Dunkirk".getBytes(StandardCharsets.UTF_8), 0, titleDunkirkBytes, 0, "Dunkirk".length());
            Rid rid1 = new Rid(1, 0);
            Rid rid2 = new Rid(2, 0);
            Rid rid3 = new Rid(3, 0);
            titleIndex.insert(new String(titleInceptionBytes, StandardCharsets.UTF_8), rid1);
            titleIndex.insert(new String(titleInterstellarBytes, StandardCharsets.UTF_8), rid2);
            titleIndex.insert(new String(titleDunkirkBytes, StandardCharsets.UTF_8), rid3);
            Iterator<Rid> result = titleIndex.search("Interstellar");
            List<Rid> foundRids = new ArrayList<>();
            Rid rid;
            while ((rid = result.next()) != null) {
                foundRids.add(rid);
            }
            assertEquals(1, foundRids.size(), "There should be 1 result for 'Interstellar'.");
            assertEquals(rid2.toString(), foundRids.get(0).toString(), "The Rid should match the inserted one for 'Interstellar'");
            Page page = bufferManager.getPage(foundRids.get(0).getPageId(), "movies.bin");
            bufferManager.unpinPage(foundRids.get(0).getPageId(), "movies.bin");
            Row rowFromFile = page.getRow(foundRids.get(0).getSlotId());
            String titleFromFile = new String(rowFromFile.title, StandardCharsets.UTF_8);
            assertEquals("Interstellar", titleFromFile, "The title retrieved from the index should match the title in the file.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Test for inserting multiple movie IDs and searching for a specific movie ID
    @Test
    void testInsertMultipleMovieIdsAndSearch() {
        try {
            Rid rid1 = new Rid(1, 0);
            Rid rid2 = new Rid(2, 0);
            Rid rid3 = new Rid(3, 0);
            movieIdIndex.insert("tt1375666", rid1);
            movieIdIndex.insert("tt0120338", rid2);
            movieIdIndex.insert("tt0109830", rid3);
            Iterator<Rid> result = movieIdIndex.search("tt0120338");
            List<Rid> foundRids = new ArrayList<>();
            Rid rid;
            while ((rid = result.next()) != null) {
                foundRids.add(rid);
            }
            assertEquals(1, foundRids.size(), "There should be 1 result for 'tt0120338'");
            assertEquals(rid2.toString(), foundRids.get(0).toString(), "The Rid should match the inserted one for 'tt0120338'");
            Page page = bufferManager.getPage(foundRids.get(0).getPageId(), "movies.bin");
            bufferManager.unpinPage(foundRids.get(0).getPageId(), "movies.bin");
            Row rowFromFile = page.getRow(foundRids.get(0).getSlotId());
            String movieIdFromFile = new String(rowFromFile.movieId, StandardCharsets.UTF_8);
            assertEquals("tt0120338", movieIdFromFile, "The movie ID retrieved from the file should match the movie ID in the index.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}