package org.example;

import Bplus.BplusTreeImplem;
import Bplus.Rid;
import Page.Page;
import Row.Row;
import Utilities.Utilities;
import buffer.BufferManager;
import buffer.BufferManagerImplem;
import org.example.performanceTestingModule;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Caller {
    // Test C4 Range Query Search on movie id index and movie title index
    public static int testC4(BufferManager bufferManager, BplusTreeImplem<String> index, String searchType) {
        System.out.println("\n\n==================== TESTING USING " + searchType + " INDEX====================\n");
        boolean stat = true;
        try {
            // create random page
            Random rand = new Random();
            int pageCount = Utilities.getNumberOfPages("movies.bin");
            int randomPage = rand.nextInt(pageCount);
            Page p = bufferManager.getPage(randomPage, "movies.bin");
            bufferManager.unpinPage(randomPage,"movies.bin");
            if (p != null) {
                //create two random rows
                int randomRow1 = rand.nextInt(p.getRowCount());
                int randomRow2;
                do {
                    randomRow2 = rand.nextInt(p.getRowCount());
                } while (randomRow1 == randomRow2);
    
                Row row1 = p.getRow(randomRow1);
                Row row2 = p.getRow(randomRow2);
    
                String movieId1 = new String(row1.movieId, StandardCharsets.UTF_8);
                String movieTitle1 = new String(row1.title, StandardCharsets.UTF_8);
                String movieId2 = new String(row2.movieId, StandardCharsets.UTF_8);
                String movieTitle2 = new String(row2.title, StandardCharsets.UTF_8);

                // make movieID1/movieTitle1 lexicographically smaller than movieID2/movieTitle2
                if (searchType.equals("movieId")) {
                    if (movieId1.compareTo(movieId2) > 0) {
                        String tempMovieId = movieId1;
                        movieId1 = movieId2;
                        movieId2 = tempMovieId;
                        
                        String tempMovieTitle = movieTitle1;
                        movieTitle1 = movieTitle2;
                        movieTitle2 = tempMovieTitle;
                    }
                } else if (searchType.equals("title")) {
                    if (movieTitle1.compareTo(movieTitle2) > 0) {
                        String tempMovieId = movieId1;
                        movieId1 = movieId2;
                        movieId2 = tempMovieId;
                        String tempMovieTitle = movieTitle1;
                        movieTitle1 = movieTitle2;
                        movieTitle2 = tempMovieTitle;
                    }
                }

                // Perform Range Query Search using index files
                System.out.println("########### Performing range search from: " + (searchType.equals("movieId") ? movieId1 : movieTitle1)
                                   + " to " + (searchType.equals("movieId") ? movieId2 : movieTitle2) + " for " + searchType + " ###########");
                Iterator<Rid> rangeResults;
                if (searchType.equals("movieId")) {
                    rangeResults = index.rangeSearch(movieId1, movieId2);
                } else {
                    rangeResults = index.rangeSearch(movieTitle1, movieTitle2);
                }
    
                List<Rid> searchResultsList = new ArrayList<>();
                while (rangeResults.hasNext()) {
                    Rid rid = rangeResults.next();
                    searchResultsList.add(rid);
                    System.out.println("Found Rid -> PageID: " + rid.getPageId() + ", SlotID: " + rid.getSlotId() + " from the " + searchType + " index.");
                }
    
                // System.out.println("movieId1 "+ movieId1+ " movieId2 "+ movieId2);
                // System.out.println("movieTitle1 "+ movieTitle1+ " movieTitle2 "+ movieTitle2);

                // Compare obtained results from indexes with actual movie.bin 
                System.out.println("\n********* Mapping the obtained PageIDs and SlotIDs to movie.bin to compare results *********\n");
                if (!searchResultsList.isEmpty()) {
                    for (Rid rid : searchResultsList) {
                        int pageId = rid.getPageId();
                        int rowId = rid.getSlotId();
                        Page pageFromFile = bufferManager.getPage(pageId, "movies.bin");
                        bufferManager.unpinPage(pageId,"movies.bin");
                        if (pageFromFile != null) {
                            Row rowFromFile = pageFromFile.getRow(rowId);
                            if (rowFromFile != null) {
                                String movieIdFromFile = new String(rowFromFile.movieId, StandardCharsets.UTF_8);
                                String titleFromFile = new String(rowFromFile.title, StandardCharsets.UTF_8);
                                System.out.println("movieIdFromFile: "+movieIdFromFile+" titleFromFile: "+titleFromFile);
                                if ( searchType.equals("movieId") && (movieIdFromFile.compareTo(movieId1) < 0 || movieIdFromFile.compareTo(movieId2) > 0) ){
                                    stat=false;
                                    System.out.println("movie id" + movieIdFromFile + " obtained is out of range.");
                                }
                                else if( searchType.equals("title") && (titleFromFile.compareTo(movieTitle1) < 0 || titleFromFile.compareTo(movieTitle2) > 0) ) {
                                    stat = false;
                                    System.out.println("title " + titleFromFile + " obtained is out of range");
                                }
                            }
                        }
                    }
                    if(stat){
                        if(searchType == "movieId")
                            System.out.println("\n********* Using " + searchType + " index, All above values lie in valid range of " + movieId1 + " to " + movieId2 + " *********");
                        else
                            System.out.println("\n********* Using " + searchType + " index, All above values lie in valid range of " + movieTitle1 + " to " + movieTitle2 + " *********");
                    }
                } else {
                    stat = false;
                    System.out.println("No results found for the range: " + (searchType.equals("movieId") ? movieId1 : movieTitle1)
                                       + " to " + (searchType.equals("movieId") ? movieId2 : movieTitle2));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            stat = false;
        }
    
        return stat ? 0 : 1;
    }    

    // Test C3 : Single Query Search on movie id index and movie title index
    public static int testC3(BufferManager bufferManager, BplusTreeImplem<String> index, String searchType) {
        boolean stat = true;
        try {
            // create random page
            Random rand = new Random();
            int pageCount = Utilities.getNumberOfPages("movies.bin");
            int randomPage = rand.nextInt(pageCount);
            Page p = bufferManager.getPage(randomPage, "movies.bin");
            bufferManager.unpinPage(randomPage,"movies.bin");
            if (p != null) {
                // create random record
                int totalRowPerPage = p.getRowCount();
                int randomRow = rand.nextInt(totalRowPerPage);
                Row row = p.getRow(randomRow);
                String movieId = new String(row.movieId, StandardCharsets.UTF_8);
                String movieTitle = new String(row.title, StandardCharsets.UTF_8);
                String searchKey = searchType.equals("movieId") ? movieId : movieTitle;
                // do single key search on index
                System.out.println("Searching for " + searchType + ": " + searchKey + " from the " + searchType + " index file");
                Iterator<Rid> searchResults = index.search(searchKey);
                List<Rid> searchResultsList = new ArrayList<>();
                Rid r_id;
                while (searchResults.hasNext()) {
                    Rid rid = searchResults.next();
                    searchResultsList.add(rid);
                    System.out.println("Found Rid -> PageID: " + rid.getPageId() + ", SlotID: " + rid.getSlotId() + " from the " + searchType + " index file\nSearching using these in the movies.bin file now...");
                }

                // Compare obtained result from index with actual movie.bin
                if (!searchResultsList.isEmpty()) {
                    for (Rid rid : searchResultsList) {
                        int pageId = rid.getPageId();
                        int rowId = rid.getSlotId();
                        Page page_single_key_search = bufferManager.getPage(pageId, "movies.bin");
                        bufferManager.unpinPage(pageId, "movies.bin");
                        if (page_single_key_search != null) {
                            Row rowFromFile = page_single_key_search.getRow(rowId);
                            if (rowFromFile != null) {
                                String movieIdFromFile = new String(rowFromFile.movieId, StandardCharsets.UTF_8);
                                String titleFromFile = new String(rowFromFile.title, StandardCharsets.UTF_8);
                                System.out.println("Found record for " + searchType + ": " + searchKey + " Movie ID: " + movieIdFromFile + " Title: " + titleFromFile + " from movie.bin");
                                System.out.println("searchKey idx" + searchKey);
                                System.out.println("titleFromFile " + titleFromFile);
                                if ((searchType.equals("movieId") && !searchKey.equals(movieIdFromFile)) || (searchType.equals("title") && !searchKey.equals(titleFromFile))) {
                                    stat = false;
                                    System.out.println("Mismatch in data for " + searchType + ": " + searchKey);
                                }
                            }
                        }
                       // bufferManager.unpinPage(pageId, "movies.bin");
                    }
                } else {
                    stat = false;
                    System.out.println("No results found for " + searchType + ": " + searchKey);
                }
            }
            if (stat) {
                return 0;
            } else {
                return 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }

    // Test C1 and C2: Create title and movieId indexes
    public static int testC1AndC2(BufferManager bufferManager, BplusTreeImplem<String> titleIndex, BplusTreeImplem<String> movieIdIndex) {
        boolean stat = true;
        try {
            int pageCount = Utilities.getNumberOfPages("movies.bin");
            for (int currPID = 0; currPID < pageCount; currPID++) {
                Page p = bufferManager.getPage(currPID, "movies.bin");
                bufferManager.unpinPage(currPID, "movies.bin");
                if (p == null) {
                    stat = false;
                    break;
                }
                int totalRowPerPage = p.getRowCount();
                for (int rowCount = 0; rowCount < totalRowPerPage; rowCount++) {
                    Row row = p.getRow(rowCount);
                    if (row == null) {
                        stat = false;
                        break;
                    }
                    byte[] movieIdStr = row.movieId;
                    byte[] titleStr = row.title;
                    Rid movieRid = new Rid(currPID, rowCount);
                    // Create index on movie title
                    String movieTitle = new String(titleStr, StandardCharsets.UTF_8);
                    titleIndex.insert(movieTitle, movieRid);
                    // Create index on movie id
                    String movieId = new String(movieIdStr, StandardCharsets.UTF_8);
                    movieIdIndex.insert(movieId, movieRid);
                }

            }
            bufferManager.force();
            if (!stat) {
                return 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
        return 0;
    }

    public static void main(String[] args) {
        try {
            // Initialize Buffer Manager
            System.out.println("========================== STARTING TEST: INITIALIZING BUFFER MANAGER ==========================");
            BufferManager bufferManager = new BufferManagerImplem(1000);
            Utilities.loadDataset(bufferManager, "/Users/Admin/Desktop/645/lab/title.basics.tsv");
            bufferManager.force();
            System.out.println("========================== PASS TEST: INITIALIZING BUFFER MANAGER ==========================");

  
            BplusTreeImplem<String> titleIndex = new BplusTreeImplem<>("title_index.bin", bufferManager);
            BplusTreeImplem<String> movieIdIndex = new BplusTreeImplem<>("movie_Id_index.bin", bufferManager);

            // Running Test C1 and C2 (combined)
            System.out.println("========================== STARTING TEST C1 & C2 ==========================");
            int r1 = testC1AndC2(bufferManager, titleIndex, movieIdIndex);
            if (r1 == 1) {
                System.out.println("========================== FAIL TEST C1 & C2 ==========================");
            } else {
                System.out.println("Created Index on Movie Title Successfully...");
                System.out.println("Created Index on Movie ID Successfully...");
                System.out.println("========================== PASS TEST C1 & C2 ==========================");
            }

            // Running Test C3
            System.out.println("========================== STARTING TEST C3 ==========================");
            int r3MovieId = testC3(bufferManager, movieIdIndex, "movieId");
            int r3Title = testC3(bufferManager, titleIndex, "title");
            if (r3MovieId == 1 || r3Title == 1) {
                System.out.println("========================== FAIL TEST C3 ==========================");
            } else {
                System.out.println("========================== PASS TEST C3 ==========================");
            }

            // Running Test C4
            System.out.println("========================== STARTING TEST C4 ==========================");
            int r4MovieId = testC4(bufferManager, movieIdIndex, "movieId");
            int r4Title = testC4(bufferManager, titleIndex, "title");
            if (r4Title == 1){ 
                System.out.println("========================== FAIL TEST C4 ==========================");
            } else {
                System.out.println("========================== PASS TEST C4 ==========================");
            }

            // Performance Testing
            System.out.println("========================== STARTING PERFORMANCE TESTS ==========================");
           // performanceTestingModule performance_test = new performanceTestingModule(bufferManager, movieIdIndex, titleIndex);
           // performance_test.performanceTesting_MovieID(false);
           // performance_test.performanceTesting_Title(false);

        } catch (Exception e) {
            System.err.println("Caught exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}