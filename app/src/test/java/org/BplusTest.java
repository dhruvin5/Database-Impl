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
            Rid movieRid = new Rid(1, 0);
            titleIndex.insert(movieTitle, movieRid);
            Iterator<Rid> result = titleIndex.search(movieTitle);
            assertEquals(movieRid, result.next(), "The Rid should match the inserted one.");
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
            assertEquals(movieRid.getPageId(), result.next().getPageId(), "The Rid should match the inserted one.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Test for searching a non-existent movie title
    @Test
    void testSearchNonExistentTitle() {
        try {
            titleIndex.insert("Inception", new Rid(1, 0));
            titleIndex.insert("Interstellar", new Rid(2, 0));
            String nonExistentTitle = "Incredibles";
            Iterator<Rid> result = titleIndex.search(nonExistentTitle);
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
            titleIndex.insert("Inception", new Rid(1, 0));
            titleIndex.insert("Interstellar", new Rid(2, 0));
            titleIndex.insert("The Dark Knight", new Rid(3, 0));
            Iterator<Rid> rangeResults = titleIndex.rangeSearch("Inception", "Interstellar");
            int count = 0;
            List<Rid> foundRids = new ArrayList<>();
            Rid rid;
            while ((rid = rangeResults.next()) != null) {
                foundRids.add(rid);
                count++;
            }
            assertEquals(2, count, "Range search should return 2 records within the specified range.");
            assertEquals(new Rid(1, 0), foundRids.get(0), "First Rid should match the inserted one for 'Inception'");
            assertEquals(new Rid(2, 0), foundRids.get(1), "Second Rid should match the inserted one for 'Interstellar'");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Test for range search with movie IDs
    @Test
    void testRangeSearchMovieId() {
        try {
            movieIdIndex.insert("tt1375666", new Rid(1, 0)); // Inception
            movieIdIndex.insert("tt0120338", new Rid(2, 0)); // Titanic
            movieIdIndex.insert("tt0109830", new Rid(3, 0)); // Forrest Gump
            Iterator<Rid> rangeResults = movieIdIndex.rangeSearch("tt0100000", "tt0125000");
            int count = 0;
            List<Rid> foundRids = new ArrayList<>();
            Rid rid;
            while ((rid = rangeResults.next()) != null) {  // Use next() and check if it returns null
                foundRids.add(rid);
                count++;
            }
            assertEquals(2, count, "Range search should return 2 records within the specified range.");
            assertEquals(new Rid(1, 0), foundRids.get(0), "First Rid should match the inserted one for 'tt1375666'");
            assertEquals(new Rid(2, 0), foundRids.get(1), "Second Rid should match the inserted one for 'tt0120338'");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Test for empty range search with movie titles
    @Test
    void testEmptyRangeSearchTitle() {
        try {
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
            Rid rid1 = new Rid(1, 0);
            Rid rid2 = new Rid(2, 0);
            Rid rid3 = new Rid(3, 0);
            titleIndex.insert("Inception", rid1);
            titleIndex.insert("Interstellar", rid2);
            titleIndex.insert("Dunkirk", rid3);
            Iterator<Rid> result = titleIndex.search("Interstellar");
            List<Rid> foundRids = new ArrayList<>();
            Rid rid;
            while ((rid = result.next()) != null) {  // Use next() and check if it returns null
                foundRids.add(rid);
            }
            assertEquals(1, foundRids.size(), "There should be 1 result for 'Interstellar'.");
            assertEquals(rid2, foundRids.get(0), "The Rid should match the inserted one for 'Interstellar'");

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
            assertEquals(rid2, foundRids.get(0), "The Rid should match the inserted one for 'tt0120338'");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}