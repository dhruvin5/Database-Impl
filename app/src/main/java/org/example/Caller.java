package org.example;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import Bplus.BplusTreeImplem;
import Bplus.Rid;
import Page.Page;
import Row.Row;
import Utilities.Utilities;
import buffer.BufferManager;
import buffer.BufferManagerImplem;

public class Caller {

    // Test C3 : Search on movie id index and movie title index
    public static int testC3(BufferManager bufferManager, BplusTreeImplem<String> index, String searchType) {
        boolean stat = true;

        try {
            Random rand = new Random();
            int pageCount = Utilities.getNumberOfPages("movies.bin");
            int randomPage = rand.nextInt(pageCount);  // Select a random page
            Page p = bufferManager.getPage(randomPage, "movies.bin");

            if (p != null) {
                int totalRowPerPage = p.getRowCount();
                int randomRow = rand.nextInt(totalRowPerPage);  // Select a random row
                Row row = p.getRow(randomRow);

                // Randomly select movieId or title based on the searchType
                String movieId = new String(row.movieId, StandardCharsets.UTF_8);
                String movieTitle = new String(row.title, StandardCharsets.UTF_8);
                String searchKey = searchType.equals("movieId") ? movieId : movieTitle;

                System.out.println("Searching for " + searchType + ": " + searchKey + " from the " + searchType + " index file");
                Iterator<Rid> searchResults = index.search(searchKey);
                List<Rid> searchResultsList = new ArrayList<>();
                Rid r_id;
                while (searchResults.hasNext()) {
                    Rid rid = searchResults.next();
                    searchResultsList.add(rid);
                    System.out.println("Found Rid -> PageID: " + rid.getPageId() + ", SlotID: " + rid.getSlotId() + " from the " + searchType + " index file\nSearching using these in the movies.bin file now...");
                }

                // Fetch and compare the actual movie record from the file
                if (!searchResultsList.isEmpty()) {
                    for (Rid rid : searchResultsList) {
                        int pageId = rid.getPageId();
                        int rowId = rid.getSlotId();
                        Page page_single_key_search = bufferManager.getPage(pageId, "movies.bin");
                        if (page_single_key_search != null) {
                            Row rowFromFile = page_single_key_search.getRow(rowId);
                            if (rowFromFile != null) {
                                String movieIdFromFile = new String(rowFromFile.movieId, StandardCharsets.UTF_8);
                                String titleFromFile = new String(rowFromFile.title, StandardCharsets.UTF_8);
                                System.out.println("Found record for " + searchType + ": " + searchKey + " Movie ID: " + movieIdFromFile + " Title: " + titleFromFile + " from movie.bin");
                                System.out.println("searchKey idx" + searchKey);
                                System.out.println("titleFromFile " + titleFromFile);
                                // Verify the movieId and title match
                                if ((searchType.equals("movieId") && !searchKey.equals(movieIdFromFile)) || (searchType.equals("title") && !searchKey.equals(titleFromFile))) {
                                    stat = false;
                                    System.out.println("Mismatch in data for " + searchType + ": " + searchKey);
                                }
                            }
                        }
                    }
                } else {
                    stat = false;
                    System.out.println("No results found for " + searchType + ": " + searchKey);
                }
            }

            if (stat) {
                return 0;  // Test passes
            } else {
                return 1;  // Test fails
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 1;  // Test fails on exception
        }
    }

    // Test C1 and C2: Create title and movieId indexes
    public static int testC1AndC2(BufferManager bufferManager, BplusTreeImplem<String> titleIndex, BplusTreeImplem<String> movieIdIndex) {
        boolean stat = true;

        try {
            int pageCount = Utilities.getNumberOfPages("movies.bin");
            for (int currPID = 0; currPID < pageCount; currPID++) {
                Page p = bufferManager.getPage(currPID, "movies.bin");
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
                    String movieTitle = new String(titleStr, StandardCharsets.UTF_8);
                    titleIndex.insert(movieTitle, movieRid);
                    String movieId = new String(movieIdStr, StandardCharsets.UTF_8);
                    movieIdIndex.insert(movieId, movieRid);
                }
                bufferManager.unpinPage(currPID, "movies.bin");
            }
            bufferManager.force();
            if (!stat) {
                return 1;  // Test C1 or C2 fails
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 1;  // Test C1 or C2 fails on exception
        }
        return 0;  // Test C1 and C2 pass
    }

    public static void main(String[] args) {
        try {
            // Initialize Buffer Manager
            System.out.println("========================== STARTING TEST: INITIALIZING BUFFER MANAGER ==========================");
            BufferManager bufferManager = new BufferManagerImplem(7);
            Utilities.loadDataset(bufferManager, "/Users/simranmalik/Desktop/title.basics.tsv");
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

            // Running Test C3 for movieId
            System.out.println("========================== STARTING TEST C3 ==========================");
            int r3MovieId = testC3(bufferManager, movieIdIndex, "movieId");
            int r3Title = testC3(bufferManager, titleIndex, "title");
            if (r3MovieId == 1 || r3Title == 1) {
                System.out.println("========================== FAIL TEST C3 ==========================");
            } else {
                System.out.println("========================== PASS TEST C3 ==========================");
            }

        } catch (Exception e) {
            System.err.println("Caught exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
