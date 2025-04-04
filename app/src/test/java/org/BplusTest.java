package org;

import Bplus.BplusTreeImplem;
import Bplus.Rid;
import buffer.BufferManager;
import buffer.BufferManagerImplem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @Test
    void testInsertAndSearchTitle() {
        try {
            String movieTitle = "Inception";
            Rid movieRid = new Rid(1, 0);
            titleIndex.insert(movieTitle, movieRid);
            Iterator<Rid> result = titleIndex.search(movieTitle);
            //assertTrue(result.hasNext(), "The key should exist in the index.");
            assertEquals(movieRid, result.next(), "The Rid should match the inserted one.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

     @Test
     void testInsertAndSearchMovieId() {
         try {
             String movieId = "tt1375666";
             Rid movieRid = new Rid(2, 0);
             movieIdIndex.insert(movieId, movieRid);
             Iterator<Rid> result = movieIdIndex.search(movieId);
            assertTrue(result.hasNext(), "The key should exist in the index.");
            // assertEquals(movieRid, result.next(), "The Rid should match the inserted one.");
         } catch (Exception e) {
             e.printStackTrace();
         }
     }

      @Test
      void testSearchNonExistentTitle() {
          try {
              // Insert some data first
              titleIndex.insert("Inception", new Rid(1, 0));
              titleIndex.insert("Interstellar", new Rid(2, 0));

              // Now search for a non-existent title
              String nonExistentTitle = "NonExistentMovie";
              Iterator<Rid> result = titleIndex.search(nonExistentTitle);

              // Assert that no results are found for the non-existent title
              assertFalse(result.hasNext(), "The search should return no results for a non-existent title.");
          } catch (Exception e) {
              e.printStackTrace();
          }
      }

      @Test
      void testSearchNonExistentMovieId() {
          try {
              // Insert some data first
              movieIdIndex.insert("tt1375666", new Rid(1, 0)); // Inception
              movieIdIndex.insert("tt0120338", new Rid(2, 0)); // Titanic

              // Now search for a non-existent movieId
              String nonExistentMovieId = "tt9999999"; // A movie ID that doesn't exist
              Iterator<Rid> result = movieIdIndex.search(nonExistentMovieId);

              // Assert that no results are found for the non-existent movieId
              assertFalse(result.hasNext(), "The search should return no results for a non-existent movieId.");
          } catch (Exception e) {
              e.printStackTrace();
          }
      }

      @Test
      void testRangeSearchTitle() {
          try {
              titleIndex.insert("Inception", new Rid(1, 0));
              titleIndex.insert("Interstellar", new Rid(2, 0));
              titleIndex.insert("The Dark Knight", new Rid(3, 0));

              // Search for a range of titles
              Iterator<Rid> rangeResults = titleIndex.rangeSearch("Inception", "Interstellar");
              int count = 0;
              while (rangeResults.hasNext()) {
                  count++;
                  rangeResults.next();
              }
              assertEquals(2, count, "Range search should return 2 records within the specified range.");
          } catch (Exception e) {
              e.printStackTrace();
          }
      }

      @Test
      void testRangeSearchMovieId() {
          try {
              movieIdIndex.insert("tt1375666", new Rid(1, 0)); // Inception
              movieIdIndex.insert("tt0120338", new Rid(2, 0)); // Titanic
              movieIdIndex.insert("tt0109830", new Rid(3, 0)); // Forrest Gump

              // Range search for movieIds
              Iterator<Rid> rangeResults = movieIdIndex.rangeSearch("tt0100000", "tt0125000");
              int count = 0;
              while (rangeResults.hasNext()) {
                  count++;
                  rangeResults.next();
              }
              assertEquals(2, count, "Range search should return 2 records within the specified range.");
          } catch (Exception e) {
              e.printStackTrace();
          }
      }

      @Test
      void testEmptyRangeSearchTitle() {
          try {
              Iterator<Rid> rangeResults = titleIndex.rangeSearch("A", "B");
              assertFalse(rangeResults.hasNext(), "Range search with no results should return empty.");
          } catch (Exception e) {
              e.printStackTrace();
          }
      }

      @Test
      void testEmptyRangeSearchMovieId() {
          try {
              Iterator<Rid> rangeResults = movieIdIndex.rangeSearch("tt0000001", "tt0000010");
              assertFalse(rangeResults.hasNext(), "Range search with no results should return empty.");
          } catch (Exception e) {
              e.printStackTrace();
          }
      }

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
              assertTrue(result.hasNext(), "Key 'Interstellar' should exist.");
              assertEquals(rid2, result.next(), "The Rid should match the inserted one.");
          } catch (Exception e) {
              e.printStackTrace();
          }
      }

      @Test
      void testInsertMultipleMovieIdsAndSearch() {
          try {
              Rid rid1 = new Rid(1, 0);
              Rid rid2 = new Rid(2, 0);
              Rid rid3 = new Rid(3, 0);

              movieIdIndex.insert("tt1375666", rid1); // Inception
              movieIdIndex.insert("tt0120338", rid2); // Titanic
              movieIdIndex.insert("tt0109830", rid3); // Forrest Gump

              Iterator<Rid> result = movieIdIndex.search("tt0120338");
              assertTrue(result.hasNext(), "Key 'tt0120338' should exist.");
              assertEquals(rid2, result.next(), "The Rid should match the inserted one.");
          } catch (Exception e) {
              e.printStackTrace();
          }
      }
}