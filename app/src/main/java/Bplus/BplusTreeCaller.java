package Bplus;

import java.util.Iterator;

import Page.Page;
import Row.movieRow;
import Utilities.Utilities;
import buffer.BufferManager;
import buffer.BufferManagerImplem;

public class BplusTreeCaller {
    public static void main(String[] args) {
        try {
            // Initialize BufferManager with size 10
            BufferManager bufferManager = new BufferManagerImplem(10);

            // Load dataset into the buffer
            Utilities.loadDataset(bufferManager, "/Users/Admin/Downloads/title.basics.tsv");
            System.out.println("PASS: Buffer Manager initialized with buffer size: 10");

            // Create two different B+ Trees for movieId and title
            BplusTreeImplem<Integer> movieIdIndex = new BplusTreeImplem<>("movieIdIndexFile", bufferManager);
            BplusTreeImplem<String> titleIndex = new BplusTreeImplem<>("titleIndexFile", bufferManager);

            // Load data from the movie table and populate the B+ tree indexes
            int currentPageId = 0; // Initialize page ID (loading rows from the pages)

            while (true) {
                // Get the current page from the buffer manager
                // Assuming the second argument is the page type or file name
                Page p = bufferManager.getPage(currentPageId, "movieDataFile"); // Replace with actual file/page type
                if (p == null) {
                    break; // No more pages to read
                }

                // Iterate through rows in the page
                int rowId = 0;
                movieRow row;
                while ((row = p.getRow(rowId)) != null) {
                    // Process the row
                    String movieIdStr = new String(row.movieId).trim();
                    String titleStr = new String(row.title).trim();

                    // Parse movieId and create Rid for the row
                    int movieId = Integer.parseInt(movieIdStr);
                    Rid movieRid = new Rid(currentPageId, rowId);

                    // Insert movieId and title into B+ Tree indexes
                    movieIdIndex.insert(movieId, movieRid);
                    titleIndex.insert(titleStr, movieRid);

                    // Move to the next row
                    rowId++;
                }

                // After processing the page, move to the next page
                currentPageId++;
            }

            // Sample tests
            int searchMovieId = 1; // Example movieId for search
            System.out.println("Searching for movieId: " + searchMovieId);
            Iterator<Rid> movieIdResults = movieIdIndex.search(searchMovieId);
            while (movieIdResults.hasNext()) {
                System.out.println("MovieId Search Result: " + movieIdResults.next());
            }

            String searchTitle = "Inception"; // Example title for search
            System.out.println("Searching for title: " + searchTitle);
            Iterator<Rid> titleResults = titleIndex.search(searchTitle);
            while (titleResults.hasNext()) {
                System.out.println("Title Search Result: " + titleResults.next());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
