package org.example;

import Bplus.BplusTreeImplem;
import Bplus.Rid;
import Page.Page;
import Row.Row;
import Utilities.Utilities;
import buffer.BufferManager;
import buffer.BufferManagerImplem;
import org.example.performanceTestingModule;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Caller {
    public static void main(String[] args) {
        try {

            // Delete if files exist
            // Initialize BufferManager with size 10
            BufferManager bufferManager = new BufferManagerImplem(10000);
            Utilities.loadDataset(bufferManager, "c:/Users/bhaga/Downloads/title.basics.tsv"); // Load dataset
            System.out.println("PASS: Buffer Manager initialized with buffer size: 10000");

            // Create two different B+ Trees for movieId and title
            BplusTreeImplem<String> movieIdIndex = new BplusTreeImplem<>("movie_Id_index.bin", bufferManager);
            BplusTreeImplem<String> titleIndex = new BplusTreeImplem<>("title_index.bin", bufferManager);

            System.out.println("PASS: Initialized Empty B+ Tree Indexes");

            // Load data from the movie table and populate the B+ tree indexes
            int currentPageId = 0; // Initialize page ID (loading rows from the pages)

            while (true) {
                // Get the current page from the buffer manager
                // Assuming the second argument is the page type or file name
                Page p = bufferManager.getPage(currentPageId, "movies.bin"); // Replace with actual file/page type
                if (p == null) {
                    break; // No more pages to read
                }

                int rowId = 0;
                Row row;
                while ((row = p.getRow(rowId)) != null) {
                    // Process the row
                    byte[] movieIdStr = row.movieId;
                    byte[] titleStr = row.title;
                    Rid Rid = new Rid(currentPageId, rowId);
                    String movieId = new String(movieIdStr, StandardCharsets.UTF_8);
                    String title = new String(titleStr, StandardCharsets.UTF_8);
                    // movieIdIndex.insert(movieId, Rid);
                    titleIndex.insert(title, Rid);
                    rowId++;
                }
                bufferManager.unpinPage(currentPageId, "movies.bin");
                // After processing the page, move to the next page
                currentPageId++;
            }
            System.out.println("PASS: Loaded data into B+ tree indexes");

            performanceTestingModule testModule = new performanceTestingModule(bufferManager, movieIdIndex, titleIndex);

            // testModule.performanceTesting_MovieID(); // Call the performance testing
            testModule.performanceTesting_Title(); // Call the performance testing

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
