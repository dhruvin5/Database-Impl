package org.example;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import Bplus.BplusTreeImplem;
import Bplus.Rid;
import Page.Page;
import Row.Row;
import Utilities.Utilities;
import buffer.BufferManager;
import buffer.BufferManagerImplem;

public class Caller {
    public static void main(String[] args) {
        try {

            // Initialize BufferManager with size 10
            BufferManager bufferManager = new BufferManagerImplem(10);

            // Load dataset into the buffer
            Utilities.loadDataset(bufferManager, "/Users/Admin/Desktop/645/lab/title.basics.tsv");

            bufferManager.force();

            System.out.println("PASS: Buffer Manager initialized with buffer size: 10");

            // Create two different B+ Trees for movieId and title
            int order = 15;
            // BplusTreeImplem<String> movieIdIndex = new
            // BplusTreeImplem<>("movie_Id_index.bin", bufferManager, order);

            System.out.println("PASS: Initialized Movie Index");
            BplusTreeImplem<String> titleIndex = new BplusTreeImplem<>("movie_Id_index.bin", bufferManager);

            //System.out.println("PASS: Initialized Title Index");

            // Load data from the movie table and populate the B+ tree indexes
            int currentPageId = 0; // Initialize page ID (loading rows from the pages)

            while (currentPageId < 1) {
                // Get the current page from the buffer manager
                // Assuming the second argument is the page type or file name
                Page p = bufferManager.getPage(currentPageId, "movies.bin"); // Replace with actual file/page type
                if (p == null) {
                    break; // No more pages to read
                }
                bufferManager.unpinPage(currentPageId, "movies.bin");
                // Iterate through rows in the page
                int rowId = 0;
                Row row;
                while ((row = p.getRow(rowId)) != null) {
                    // Process the row
                    byte[] movieIdStr = row.movieId;
                    byte[] titleStr = row.title;

                    // System.out.println(new String(titleStr, Charset.defaultCharset()));

                    // Parse movieId and create Rid for the row

                    Rid movieRid = new Rid(currentPageId, rowId);

                    // Insert movieId and title into B+ Tree indexes
                    String movieId = new String(movieIdStr, StandardCharsets.UTF_8);

                    // System.out.println(movieId);
                    // movieIdIndex.insert(movieId, movieRid);
                    // System.out.println("INSERTED " + movieId);

                    titleIndex.insert(movieId, movieRid);
                    System.out.println("Inserted key: " + movieId);



                    System.out.println("**********************************");
                    titleIndex.printTree();
                    System.out.println("**********************************");
                    //

                    // Move to the next row
                    rowId++;
                }
                /*
                 * System.out.println("Inserted page: " + currentPageId);
                 * System.out.println("**********************************");
                 * titleIndex.printTree();
                 * System.out.println("**********************************");
                 *
                 * bufferManager.unpinPage(currentPageId,"movies.bin");
                 * // After processing the page, move to the next page
                 */
                currentPageId++;

            }
            bufferManager.force();
            System.out.println("printing whole tree again");
            titleIndex.printTree();

            //single key search for start key
            System.out.println("--------------------------");
            System.out.println("Test 1 single key search ");
            System.out.println("--------------------------");

            String test_search1="tt0000004";

            Iterator<Rid> singleSearchResultsIterator = titleIndex.search(test_search1);
            System.out.println(" single key search " + singleSearchResultsIterator);

            List<Rid> singleSearchResults = new ArrayList<>();
            while (singleSearchResultsIterator.hasNext()) {
                Rid rid = singleSearchResultsIterator.next();
                singleSearchResults.add(rid);
                System.out.println("Found Rid -> PageID: " + rid.getPageId() + ", SlotID: " + rid.getSlotId());
            }

            if (singleSearchResults.isEmpty()) {
                System.out.println("No results found for key: " + test_search1);
            }

            // Fetching the corresponding record from movie.bin file
            for (Rid rid : singleSearchResults) {
                int pageId = rid.getPageId();
                int rowId = rid.getSlotId();

                // Get the page from buffer
                Page page_single_key_search = bufferManager.getPage(pageId, "movies.bin");

                if (page_single_key_search != null) {
                    Row row = page_single_key_search.getRow(rowId);
                    if (row != null) {
                        System.out.println("Found record for " + test_search1 + ":");
                        System.out.println("Movie ID: " + new String(row.movieId, StandardCharsets.UTF_8));
                        System.out.println("Title: " + new String(row.title, StandardCharsets.UTF_8));
                    }
                }
            }

            //single key search for end key
            System.out.println("--------------------------");
            System.out.println("Test 2 single key search ");
            System.out.println("--------------------------");

            String test_search2="tt0000010";
            Iterator<Rid> singleSearchResultsIterator2 = titleIndex.search(test_search2);
            System.out.println(" single key search " + singleSearchResultsIterator2);

            List<Rid> singleSearchResults2 = new ArrayList<>();
            while (singleSearchResultsIterator2.hasNext()) {
                Rid rid2 = singleSearchResultsIterator2.next();
                singleSearchResults2.add(rid2);
                System.out.println("Found Rid -> PageID: " + rid2.getPageId() + ", SlotID: " + rid2.getSlotId());
            }

            if (singleSearchResults2.isEmpty()) {
                System.out.println("No results found for key: " + test_search2);
            }

            // Fetching the corresponding record from movie.bin file
            for (Rid rid2 : singleSearchResults2) {
                int pageId2 = rid2.getPageId();
                int rowId2 = rid2.getSlotId();

                // Get the page from buffer
                Page page_single_key_search2 = bufferManager.getPage(pageId2, "movies.bin");

                if (page_single_key_search2 != null) {
                    Row row2 = page_single_key_search2.getRow(rowId2);
                    if (row2 != null) {
                        System.out.println("Found record for " + test_search2 + ":");
                        System.out.println("Movie ID: " + new String(row2.movieId, StandardCharsets.UTF_8));
                        System.out.println("Title: " + new String(row2.title, StandardCharsets.UTF_8));
                    }
                }
            }


            //testing range search
            System.out.println("-----testing range search-----");
            System.out.println("KEY 1: " + test_search1);//poor peirrot
            System.out.println("KEY 2: " + test_search2);//the arrival of a train
            Iterator<Rid> rangeSearchResultsIterator = titleIndex.rangeSearch(test_search1,test_search2);
            System.out.println(" range key search " + rangeSearchResultsIterator);

            List<Rid> rangeSearchResults = new ArrayList<>();
            while (rangeSearchResultsIterator.hasNext()) {
                Rid rid = rangeSearchResultsIterator.next();
                rangeSearchResults.add(rid);
                System.out.println("Found Rid in range -> PageID: " + rid.getPageId() + ", SlotID: " + rid.getSlotId());
            }

            if (rangeSearchResults.isEmpty()) {
                System.out.println("No results found in range: " + test_search1 + " to " + test_search2);
            }

            for (Rid rid : rangeSearchResults) {
                int pageId = rid.getPageId();
                int rowId = rid.getSlotId();

                Page page_range_key_search = bufferManager.getPage(pageId, "movies.bin");

                if (page_range_key_search != null) {
                    Row row = page_range_key_search.getRow(rowId);
                    if (row != null) {
                        System.out.println("Found record in range " + test_search1 + " to " + test_search2 + ":");
                        System.out.println("Movie ID: " + new String(row.movieId, StandardCharsets.UTF_8));
                        System.out.println("Title: " + new String(row.title, StandardCharsets.UTF_8));
                    }
                }
            }

            //  Edison Kinetoscopic Record of

            //single key search for start key
            System.out.println("--------------------------");
            System.out.println("Test 3 single key search ");
            System.out.println("--------------------------");

            String test_search3="Edison Kinetoscopic Record of ";

            Iterator<Rid> singleSearchResultsIterator3 = titleIndex.search(test_search3);
            System.out.println(" single key search " + singleSearchResultsIterator3);

            List<Rid> singleSearchResults3 = new ArrayList<>();
            while (singleSearchResultsIterator3.hasNext()) {
                Rid rid = singleSearchResultsIterator3.next();
                singleSearchResults3.add(rid);
                System.out.println("Found Rid -> PageID: " + rid.getPageId() + ", SlotID: " + rid.getSlotId());
            }

            if (singleSearchResults3.isEmpty()) {
                System.out.println("No results found for key: " + test_search3);
            }

            // Fetching the corresponding record from movie.bin file
            for (Rid rid : singleSearchResults3) {
                int pageId = rid.getPageId();
                int rowId = rid.getSlotId();

                // Get the page from buffer
                Page page_single_key_search3 = bufferManager.getPage(pageId, "movies.bin");

                if (page_single_key_search3 != null) {
                    Row row = page_single_key_search3.getRow(rowId);
                    if (row != null) {
                        System.out.println("Found record for " + test_search3 + ":");
                        System.out.println("Movie ID: " + new String(row.movieId, StandardCharsets.UTF_8));
                        System.out.println("Title: " + new String(row.title, StandardCharsets.UTF_8));
                    }
                }
            }

            //testing range search3
            System.out.println("-----testing range search 3-----");
            System.out.println("KEY 1: " + test_search3);//edison
            System.out.println("KEY 2: " + test_search1);//poor periot
            Iterator<Rid> rangeSearchResultsIterator3 = titleIndex.rangeSearch(test_search3,test_search1);
            System.out.println(" range key search " + rangeSearchResultsIterator3);

            List<Rid> rangeSearchResults3 = new ArrayList<>();
            while (rangeSearchResultsIterator3.hasNext()) {
                Rid rid = rangeSearchResultsIterator3.next();
                rangeSearchResults3.add(rid);
                System.out.println("Found Rid in range -> PageID: " + rid.getPageId() + ", SlotID: " + rid.getSlotId());
            }

            if (rangeSearchResults3.isEmpty()) {
                System.out.println("No results found in range: " + test_search3 + " to " + test_search1);
            }

            for (Rid rid : rangeSearchResults3) {
                int pageId = rid.getPageId();
                int rowId = rid.getSlotId();

                Page page_range_key_search = bufferManager.getPage(pageId, "movies.bin");

                if (page_range_key_search != null) {
                    Row row = page_range_key_search.getRow(rowId);
                    if (row != null) {
                        System.out.println("Found record in range " + test_search3 + " to " + test_search1 + ":");
                        System.out.println("Movie ID: " + new String(row.movieId, StandardCharsets.UTF_8));
                        System.out.println("Title: " + new String(row.title, StandardCharsets.UTF_8));
                    }
                }
            }

            //System.out.println(" range key search " + titleIndex.rangeSearch(test_search1, test_search2));
            /*
            Iterator<Rid> singleSearchResults = titleIndex.search(test_search1);
            while (singleSearchResults.hasNext()) {
                Rid rid = singleSearchResults.next();
                int pageId = rid.getPageId();
                System.out.println(pageId);
                int rowId = rid.getSlotId();
                System.out.println(pageId);

                // Get the page from buffer
                Page page = bufferManager.getPage(pageId, "movies.bin");

                if (page != null) {
                    Row row = page.getRow(rowId);
                    if (row != null) {
                        System.out.println("Found record for " + test_search1 + ":");
                        System.out.println("Movie ID: " + new String(row.movieId, StandardCharsets.UTF_8));
                        System.out.println("Title: " + new String(row.title, StandardCharsets.UTF_8));
                    }
                }
            }

            Iterator<Rid> rangeSearchResults = titleIndex.rangeSearch(test_search1, test_search2);
            while (rangeSearchResults.hasNext()) {
                Rid rid = rangeSearchResults.next();
                int pageId = rid.getPageId();
                int rowId = rid.getSlotId();

                // Get the page from buffer
                Page page = bufferManager.getPage(pageId, "movies.bin");

                if (page != null) {
                    Row row = page.getRow(rowId);
                    if (row != null) {
                        System.out.println("Found record in range search:");
                        System.out.println("Movie ID: " + new String(row.movieId, StandardCharsets.UTF_8));
                        System.out.println("Title: " + new String(row.title, StandardCharsets.UTF_8));
                    }
                }
            }
            */



            // Sample tests
            /*
             * int searchMovieId = 1; // Example movieId for search
             * System.out.println("Searching for movieId: " + searchMovieId);
             * Iterator<Rid> movieIdResults = movieIdIndex.search(searchMovieId);
             * while (movieIdResults.hasNext()) {
             * System.out.println("MovieId Search Result: " + movieIdResults.next());
             * }
             *
             * String searchTitle = "Inception"; // Example title for search
             * System.out.println("Searching for title: " + searchTitle);
             * Iterator<Rid> titleResults = titleIndex.search(searchTitle);
             * while (titleResults.hasNext()) {
             * System.out.println("Title Search Result: " + titleResults.next());
             * }
             *
             * }
             * }
             */
        } catch (Exception e) {
            e.printStackTrace();

        }

    }
}