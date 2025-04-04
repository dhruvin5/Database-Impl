package org.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import Bplus.BplusTreeImplem;
import Bplus.Rid;
import Page.Page;
import Row.Row;
import buffer.BufferManager;

public class performanceTestingModule {

    private BufferManager bufferManager;
    private BplusTreeImplem<String> movieIdIndex;
    private BplusTreeImplem<String> titleIndex;

    public performanceTestingModule(BufferManager bufferManager, BplusTreeImplem<String> movieIdIndex,
            BplusTreeImplem<String> titleIndex) {
        this.bufferManager = bufferManager;
        this.movieIdIndex = movieIdIndex;
        this.titleIndex = titleIndex;
    }

    // Performs performance testing for the movieId
    public void performanceTesting_MovieID(boolean bonusQuestion) {
        // Used to store the results of the linear scan and B+ tree scan
        Random random = new Random();
        List<String> linearScanResult = new ArrayList<>(1000);
        List<String> bPlusTreeResult = new ArrayList<>(1000);

        // Generate 1000 random queries and test the performance of both methods
        for (int i = 0; i < 1000; i++) {

            // Generate random range for movieId in the format of tt0000000
            int rangeStart = random.nextInt(5500000);
            int rangeEnd = rangeStart + random.nextInt(45000);

            String rangeStartStr = String.format("tt%07d", rangeStart);
            String rangeEndStr = String.format("tt%07d", rangeEnd);

            // Method 1: Direct scan
            System.out.println("Direct Scan");
            bufferManager.clearCache();
            long startTimeMethod1 = System.nanoTime();
            int numRowsMethod1 = directScan(bufferManager, rangeStartStr, rangeEndStr, false);
            long timeMethod1 = System.nanoTime() - startTimeMethod1;
            System.out.println("Direct Scan Time: " + timeMethod1 / 1000000.0 + " Found "
                    + numRowsMethod1 + " rows");

            // Method 2: Using B+ Tree index
            System.out.println("B+ Tree Scan");
            bufferManager.clearCache();

            if (bonusQuestion)
                this.movieIdIndex.pinLevels(2);

            long startTimeMethod2 = System.nanoTime();
            int numRowsMethod2 = bPlusTreeScan(movieIdIndex, rangeStartStr, rangeEndStr, false);
            long timeMethod2 = System.nanoTime() - startTimeMethod2;
            System.out.println("B+ Tree Scan Time: " + timeMethod2 / 1000000.0);

            // Check if the number of rows found by both methods is the same
            if (numRowsMethod1 != numRowsMethod2) {
                System.out.println("ERROR: Mismatch in row counts: " + numRowsMethod1 + " vs " + numRowsMethod2);
            }

            System.out.println("Query: " + i + " Range: " + rangeStartStr + " to " + rangeEndStr);

            // Collect data
            linearScanResult.add((numRowsMethod1) + " " + (timeMethod1 / 1000000.0));
            bPlusTreeResult.add((numRowsMethod2) + " " + (timeMethod2 / 1000000.0));

            // Saves the results to a file every 500 iterations
            if (i % 500 == 0) {
                writeResultsToFile("bonus_movieid_linearScanResults.txt", linearScanResult);
                writeResultsToFile("bonus_movieid_bPlusTreeResults.txt", bPlusTreeResult);
            }
        }

        // Write results to file
        writeResultsToFile("bonus_movieid_linearScanResults.txt", linearScanResult);
        writeResultsToFile("bonus_movieid_bPlusTreeResults.txt", bPlusTreeResult);
    }

    // Performs performance testing for the title
    public void performanceTesting_Title(boolean bonusQuestion) {
        // Used to store the results of the linear scan and B+ tree scan
        Random random = new Random();
        List<String> linearScanResult = new ArrayList<>(1000);
        List<String> bPlusTreeResult = new ArrayList<>(1000);

        // Generate 1000 random queries and test the performance of both methods
        for (int i = 0; i < 1000; i++) {

            // Generate random range for title
            String rangeStartStr = generateRandomString("");
            String firstLetter = rangeStartStr.substring(0, 1);

            // 50% chance for the first letter to be the next letter if it is not Z or Y
            if (firstLetter.toLowerCase() != "z" && random.nextBoolean()) {
                firstLetter = (char) (firstLetter.charAt(0) + random.nextInt(2)) + "";
            }
            String rangeEndStr = generateRandomString(firstLetter);
            if (rangeEndStr.compareTo(rangeStartStr) <= 0) {
                rangeEndStr = rangeStartStr + "Z"; // Ensure rangeEndStr is greater
            }

            System.out.println("Query: " + i + " Range: " + rangeStartStr + " to " + rangeEndStr);

            // Method 1: Direct scan
            System.out.println("Direct Scan");
            bufferManager.clearCache();
            long startTimeMethod1 = System.nanoTime();
            int numRowsMethod1 = directScan(bufferManager, rangeStartStr, rangeEndStr,
                    true);
            long endTimeMethod1 = System.nanoTime();
            long timeMethod1 = endTimeMethod1 - startTimeMethod1;
            System.out.println("Direct Scan Time: " + timeMethod1 / 1000000.0 + " Found "
                    + numRowsMethod1 + " rows");

            // Method 2: Using B+ Tree index
            System.out.println("B+ Tree Scan");
            bufferManager.clearCache();

            if (bonusQuestion)
                this.movieIdIndex.pinLevels(2);

            long startTimeMethod2 = System.nanoTime();
            int numRowsMethod2 = bPlusTreeScan(titleIndex, rangeStartStr, rangeEndStr, true);
            long endTimeMethod2 = System.nanoTime();
            long timeMethod2 = endTimeMethod2 - startTimeMethod2;
            System.out.println("B+ Tree Scan Time: " + timeMethod2 / 1000000.0);

            // Check if the number of rows found by both methods is the same
            if (numRowsMethod1 != numRowsMethod2) {
                System.out.println("ERROR: Mismatch in row counts: " + numRowsMethod1 + "vs" + numRowsMethod2);
            }

            // Collect data
            linearScanResult.add(numRowsMethod1 + " " + timeMethod1 / 1000000.0);
            bPlusTreeResult.add(numRowsMethod2 + " " + timeMethod2 / 1000000.0);

            // Saves the results to a file every 100 iterations
            if (i % 100 == 0) {
                writeResultsToFile("bonus_title_linearScanResults.txt", linearScanResult);
                writeResultsToFile("bonous_title_bPlusTreeResults.txt", bPlusTreeResult);
            }
        }

        // Write results to file
        writeResultsToFile("bonus_title_linearScanResults.txt", linearScanResult);
        writeResultsToFile("bonus_title_bPlusTreeResults.txt", bPlusTreeResult);
    }

    private int directScan(BufferManager manager, String rangeStart, String rangeEnd, boolean isTitle) {
        int numRows = 0;
        int currentPageId = 0;
        boolean foundAll = false;

        while (!foundAll) {
            Page page = manager.getPage(currentPageId, "movies.bin");
            if (page == null) {
                break;
            }

            int rowId = 0;
            Row row;
            while ((row = page.getRow(rowId)) != null) {
                String target = isTitle ? new String(row.title, StandardCharsets.UTF_8).trim()
                        : new String(row.movieId, StandardCharsets.UTF_8).trim();

                if (target.compareTo(rangeStart) >= 0 && target.compareTo(rangeEnd) <= 0) {
                    numRows++;
                }
                rowId++;
            }
            manager.unpinPage(currentPageId, "movies.bin");
            // System.out.println("Page ID: " + currentPageId);
            currentPageId++;
        }

        return numRows;
    }

    // Method to perform B+ tree scan
    private int bPlusTreeScan(BplusTreeImplem<String> movie_index, String rangeStart, String rangeEnd,
            boolean isTitle) {

        // Iterate through the B+ tree and count the number of rows in the range
        int numRows = 0;
        var iterator = movie_index.rangeSearch(rangeStart, rangeEnd);
        while (iterator.hasNext()) {
            Rid rid = iterator.next(); // Get the Rid object from the iterator
            Page p = bufferManager.getPage(rid.getPageId(), "movies.bin"); // Get the page from the buffer manager
            Row row = p.getRow(rid.getSlotId()); // Get the row from the page

            String target = isTitle ? new String(row.title, StandardCharsets.UTF_8).trim()
                    : new String(row.movieId, StandardCharsets.UTF_8).trim();

            if (target.compareTo(rangeStart) >= 0 && target.compareTo(rangeEnd) <= 0) { // Check if the target is within
                                                                                        // the range
                numRows++;
            }
            bufferManager.unpinPage(rid.getPageId(), "movies.bin");
        }
        return numRows;
    }

    // Method to write the results to a file
    private void writeResultsToFile(String filePath, List<String> results) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String result : results) {
                writer.write(result);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to generate a random string with a given prefix
    private String generateRandomString(String prefix) {
        Random random = new Random();
        int length = 5 + random.nextInt(6);
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        for (int i = 0; i < length; i++) {
            char c = (char) ('A' + random.nextInt(26));
            sb.append(c);
        }

        return sb.toString();
    }
}
