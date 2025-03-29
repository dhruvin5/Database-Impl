package org.example;

import java.util.Iterator;

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
            BufferManager bufferManager = new BufferManagerImplem(1);

            // Load dataset into the buffer
            Utilities.loadDataset(bufferManager, "C:/Users/bhaga/Downloads/title.basics.tsv");
            System.out.println("PASS: Buffer Manager initialized with buffer size: 10");

            boolean boolValue = true;
            byte byteValue = (byte) (boolValue ? 1 : 0);

            // Create a new page and insert a row into it
            Page page0 = bufferManager.createPage("movie_Id_index.bin");
            page0.insertRow(new Row((byte) 0, "123455555".getBytes(), "3".getBytes(), "4".getBytes()));
            bufferManager.unpinPage(0, "movie_Id_index.bin");
            page0 = null;

            // Create another page and insert a row into it and evicts the first page
            Page page1 = bufferManager.createPage("movie_Id_index.bin");
            page1.insertRow(new Row(byteValue, "123456789".getBytes(), "4".getBytes(), "5".getBytes()));
            bufferManager.unpinPage(1, "movie_Id_index.bin");

            // Retrieve the rows from the pages 1
            Row row2 = page1.getRow(0);
            System.out.println("Row2: " + (row2.booleanValue) + " " + new String(row2.key) + " "
                    + new String(row2.pid) + " " + new String(row2.slotid));

            // Retrieve the rows from the pages 0
            page0 = bufferManager.getPage(0, "movie_Id_index.bin");
            Row row1 = page0.getRow(0);
            System.out.println("Row1: " + (row1.booleanValue) + " " + new String(row1.key) + " "
                    + new String(row1.pid) + " " + new String(row1.slotid));

            bufferManager.unpinPage(0, "movie_Id_index.bin");
            Page moviePage = bufferManager.getPage(4, "movies.bin");
            Row movie_row = moviePage.getRow(0);
            System.out.println(
                    "Movie Row: " + (movie_row.movieId) + " " + new String(movie_row.title));

            System.out.print("Success");

            // // Create two different B+ Trees for movieId and title
            // BplusTreeImplem<Integer> movieIdIndex = new BplusTreeImplem<>("movieId.bin",
            // bufferManager);
            // BplusTreeImplem<String> titleIndex = new BplusTreeImplem<>("title.bin",
            // bufferManager);

            // // Load data from the movie table and populate the B+ tree indexes
            // int currentPageId = 0; // Initialize page ID (loading rows from the pages)

            // while (true) {
            // // Get the current page from the buffer manager
            // // Assuming the second argument is the page type or file name
            // Page p = bufferManager.getPage(currentPageId, "movies.bin"); // Replace with
            // // actual file/page type
            // if (p == null) {
            // break; // No more pages to read
            // }

            // // Iterate through rows in the page
            // int rowId = 0;
            // Row row;
            // while ((row = p.getRow(rowId)) != null) {
            // // Process the row
            // String movieIdStr = new String(row.movieId).trim();
            // String titleStr = new String(row.title).trim();

            // // Parse movieId and create Rid for the row
            // int movieId = Integer.parseInt(movieIdStr);
            // Rid movieRid = new Rid(currentPageId, rowId);

            // // Insert movieId and title into B+ Tree indexes
            // movieIdIndex.insert(movieId, movieRid);
            // titleIndex.insert(titleStr, movieRid);

            // // Move to the next row
            // rowId++;
            // }

            // // After processing the page, move to the next page
            // currentPageId++;
            // }

            // // Sample tests
            // int searchMovieId = 1; // Example movieId for search
            // System.out.println("Searching for movieId: " + searchMovieId);
            // Iterator<Rid> movieIdResults = movieIdIndex.search(searchMovieId);
            // while (movieIdResults.hasNext()) {
            // System.out.println("MovieId Search Result: " + movieIdResults.next());
            // }

            // String searchTitle = "Inception"; // Example title for search
            // System.out.println("Searching for title: " + searchTitle);
            // Iterator<Rid> titleResults = titleIndex.search(searchTitle);
            // while (titleResults.hasNext()) {
            // System.out.println("Title Search Result: " + titleResults.next());
            // }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
