package org;

import Bplus.BplusTreeImplem;
import Bplus.Rid;
import buffer.BufferManager;
import buffer.BufferManagerImplem;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import Page.PageImpl;
import Row.Row;
import Utilities.Utilities;


class BPlusTest {

    public BplusTreeImplem<String> titleIndex;
    public BplusTreeImplem<String> movieIdIndex;
    public BufferManager bufferManager;

    @BeforeEach
    void setUp() {
        try{
            bufferManager = new BufferManagerImplem(7);
            // Utilities.loadDataset(bufferManager, "C:\\Users\\HP\\Desktop\\ms\\645\\lab1\\645-Lab-32966720340112693401883534060222\\app\\src\\title.basics.tsv");
            // bufferManager.force();
            titleIndex = new BplusTreeImplem<>("title_index.bin", bufferManager);
            movieIdIndex = new BplusTreeImplem<>("movie_Id_index.bin", bufferManager);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    void testInsertAndSearchTitle() {
        try{
            // Sample movie data
            String movieTitle = "Inception";
            Rid movieRid = new Rid(1, 0);
            titleIndex.insert(movieTitle, movieRid);
            Iterator<Rid> result = titleIndex.search(movieTitle);
            assertTrue(result.hasNext(), "Title should be found in the index.");
            // assertTrue(result.hasNext(), "Title should be found in the index.");
            // assertEquals(movieRid, result.next(), "The Rid should match the inserted one.");
            // assertEquals(0,0);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    // @Test
    // void testInsertAndSearchMovieId() {
    //     // Sample movieId data
    //     String movieId = "tt1234567";
    //     Rid movieRid = new Rid(2, 0); // Using arbitrary pageId and rowCount for testing

    //     // Insert movie ID into the B+ Tree
    //     movieIdIndex.insert(movieId, movieRid);

    //     // Search for the movie ID
    //     Iterator<Rid> result = movieIdIndex.search(movieId);

    //     // Verify that the correct Rid is returned
    //     assertTrue(result.hasNext(), "Movie ID should be found in the index.");
    //     assertEquals(movieRid, result.next(), "The Rid should match the inserted one.");
    // }

    // @Test
    // void testInsertAndSearchDuplicateTitle() {
    //     // Sample duplicate movie data
    //     String movieTitle = "Inception";
    //     Rid firstRid = new Rid(1, 0);
    //     Rid secondRid = new Rid(1, 1); // Different row in the same page for this example

    //     // Insert the same title with two different RIDs
    //     titleIndex.insert(movieTitle, firstRid);
    //     titleIndex.insert(movieTitle, secondRid);

    //     // Search for the movie title
    //     Iterator<Rid> result = titleIndex.search(movieTitle);

    //     // Verify that both RIDs are found
    //     assertTrue(result.hasNext(), "Title should be found in the index.");
    //     assertEquals(firstRid, result.next(), "The first Rid should match.");
    //     assertTrue(result.hasNext(), "Second Rid should also be found.");
    //     assertEquals(secondRid, result.next(), "The second Rid should match.");
    // }

    // @Test
    // void testSearchNonExistentMovieId() {
    //     // Search for a non-existent movieId
    //     String nonExistentMovieId = "tt9999999";
    //     Iterator<Rid> result = movieIdIndex.search(nonExistentMovieId);

    //     // Verify that no result is found
    //     assertFalse(result.hasNext(), "No results should be found for a non-existent movieId.");
    // }

    // @Test
    // void testSearchNonExistentTitle() {
    //     // Search for a non-existent movie title
    //     String nonExistentTitle = "Unknown Movie";
    //     Iterator<Rid> result = titleIndex.search(nonExistentTitle);

    //     // Verify that no result is found
    //     assertFalse(result.hasNext(), "No results should be found for a non-existent title.");
    // }
}