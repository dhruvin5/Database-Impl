package org.example;

import Bplus.BplusTreeImplem;
import Bplus.Rid;
import Page.Page;
import Row.Row;
import Utilities.Utilities;
import buffer.BufferManager;
import buffer.BufferManagerImplem;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Caller {
    public static void main(String[] args) {
        try {

            // Initialize BufferManager with size 10
            BufferManager bufferManager = new BufferManagerImplem(500);

            // Load dataset into the buffer
            Utilities.loadDataset(bufferManager, "/Users/Admin/Desktop/645/lab/title.basics.tsv");

            bufferManager.force();

            System.out.println("PASS: Buffer Manager initialized with buffer size: 10");

            // Create two different B+ Trees for movieId and title
             BplusTreeImplem<String> movieIdIndex = new BplusTreeImplem<>("movie_Id_index.bin", bufferManager);

            System.out.println("PASS: Initialized Movie Index");

            //BplusTreeImplem<String> titleIndex = new BplusTreeImplem<>("title_index.bin", bufferManager);

            // System.out.println("PASS: Initialized Title Index");

            // Load data from the movie table and populate the B+ tree indexes
            int currentPageId = 0; // Initialize page ID (loading rows from the pages)

            while (true) {
                // Get the current page from the buffer manager
                // Assuming the second argument is the page type or file name
                Page p = bufferManager.getPage(currentPageId, "movies.bin"); // Replace with actual file/page type
                if (p == null) {
                    break; // No more pages to read
                }

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
                     movieIdIndex.insert(movieId, movieRid);
                    // System.out.println("INSERTED " + movieId);

                  //  titleIndex.insert(movieId, movieRid);

                    //

                    // Move to the next row
                    rowId++;
                }



                bufferManager.unpinPage(currentPageId, "movies.bin");
                // After processing the page, move to the next page
                currentPageId++;
            }
          //  System.out.println("Inserted page: " + currentPageId);
//            System.out.println("**********************************");
//            movieIdIndex.printTree();
//            System.out.println("**********************************");
            movieIdIndex.pinLevels(2);
            movieIdIndex.unPinLevels(2);

            bufferManager.force();

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
             */ } catch (Exception e) {
            e.printStackTrace();

        }

    }
}
