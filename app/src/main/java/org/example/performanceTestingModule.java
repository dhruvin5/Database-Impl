package org.example;

import Bplus.BplusTreeImplem;
import Bplus.Rid;
import Page.Page;
import Row.Row;
import Utilities.Utilities;
import buffer.BufferManager;
import buffer.BufferManagerImplem;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    public void performanceTesting_MovieID() {
        Random random = new Random();
        List<String> linearScanResult = new ArrayList<>();
        List<String> bPlusTreeResult = new ArrayList<>();

        for (int i = 0; i < 2500; i++) {
            // Generate random range for movieId in the format of tt0000000
            // Assuming movieId is a 7-digit number (e.g., tt0000000)
            int rangeStart = random.nextInt(5500000);
            int rangeEnd = rangeStart + random.nextInt(100);

            // Put in the format of tt0000000
            String rangeStartStr = String.format("tt%07d", rangeStart);
            String rangeEndStr = String.format("tt%07d", rangeEnd);

            // Method 1: Direct scan
            bufferManager.clearCache();
            long startTimeMethod1 = System.nanoTime();
            int numRowsMethod1 = directScan(bufferManager, rangeStartStr, rangeEndStr, false);
            long endTimeMethod1 = System.nanoTime();
            long timeMethod1 = endTimeMethod1 - startTimeMethod1;

            // Method 2: Using B+ Tree index
            bufferManager.clearCache();
            long startTimeMethod2 = System.nanoTime();
            int numRowsMethod2 = bPlusTreeScan(movieIdIndex, rangeStartStr, rangeEndStr);
            long endTimeMethod2 = System.nanoTime();
            long timeMethod2 = endTimeMethod2 - startTimeMethod2;

            if (numRowsMethod1 != numRowsMethod2) {
                System.out.println("ERROR: Mismatch in row counts: " + numRowsMethod1 + " vs " + numRowsMethod2);
            }

            System.out.println("Query: " + i + " Range: " + rangeStartStr + " to " + rangeEndStr);
            // Collect data
            linearScanResult.add((rangeEnd - rangeStart) + " " + timeMethod1 / (double) 1000000);
            bPlusTreeResult.add((rangeEnd - rangeStart) + " " + timeMethod2 / (double) 1000000);
            // Write results to file

        }

        linearScanResult.sort((a, b) -> {
            String[] partsA = a.split(" ");
            String[] partsB = b.split(" ");
            return Integer.compare(Integer.parseInt(partsA[0]), Integer.parseInt(partsB[0]));
        });
        bPlusTreeResult.sort((a, b) -> {
            String[] partsA = a.split(" ");
            String[] partsB = b.split(" ");
            return Integer.compare(Integer.parseInt(partsA[0]), Integer.parseInt(partsB[0]));
        });

        writeResultsToFile("movieid_linearScanResults.txt", linearScanResult);
        writeResultsToFile("movieid_bPlusTreeResults.txt", bPlusTreeResult);
    }

    public void performanceTesting_Title() {
        Random random = new Random();
        List<String> linearScanResult = new ArrayList<>();
        List<String> bPlusTreeResult = new ArrayList<>();

        for (int i = 0; i < 2500; i++) {
            // Generate random range for title
            String rangeStartStr = "A" + random.nextInt(26) + random.nextInt(1000000);
            String rangeEndStr = "Z" + random.nextInt(26) + random.nextInt(1000000);

            // Method 1: Direct scan
            bufferManager.clearCache();
            long startTimeMethod1 = System.nanoTime();
            int numRowsMethod1 = directScan(bufferManager, rangeStartStr, rangeEndStr, true);
            long endTimeMethod1 = System.nanoTime();
            long timeMethod1 = endTimeMethod1 - startTimeMethod1;

            // Method 2: Using B+ Tree index
            bufferManager.clearCache();
            long startTimeMethod2 = System.nanoTime();
            int numRowsMethod2 = bPlusTreeScan(titleIndex, rangeStartStr, rangeEndStr);
            long endTimeMethod2 = System.nanoTime();
            long timeMethod2 = endTimeMethod2 - startTimeMethod2;

            if (numRowsMethod1 != numRowsMethod2) {
                System.out.println("ERROR: Mismatch in row counts: " + numRowsMethod1 + " vs " + numRowsMethod2);
            }

            // Collect data
            linearScanResult
                    .add((rangeEndStr.length() - rangeStartStr.length()) + " " + timeMethod1 / (double) 1000000);
            bPlusTreeResult.add((rangeEndStr.length() - rangeStartStr.length()) + " " + timeMethod2 / (double) 1000000);
        }

        linearScanResult.sort((a, b) -> {
            String[] partsA = a.split(" ");
            String[] partsB = b.split(" ");
            return Integer.compare(Integer.parseInt(partsA[0]), Integer.parseInt(partsB[0]));
        });
        bPlusTreeResult.sort((a, b) -> {
            String[] partsA = a.split(" ");
            String[] partsB = b.split(" ");
            return Integer.compare(Integer.parseInt(partsA[0]), Integer.parseInt(partsB[0]));
        });

        writeResultsToFile("title_linearScanResults.txt", linearScanResult);
        writeResultsToFile("title_bPlusTreeResults.txt", bPlusTreeResult);
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

                if (target.compareTo(rangeEnd) > 0) {
                    foundAll = true;
                    break;
                }
                rowId++;
            }
            manager.unpinPage(currentPageId, "movies.bin");
            currentPageId++;
        }

        return numRows;
    }

    private int bPlusTreeScan(BplusTreeImplem<String> movie_index, String rangeStart, String rangeEnd) {
        int numRows = 0;

        var iterator = movie_index.rangeSearch(rangeStart, rangeEnd);
        while (iterator.hasNext()) {
            Rid rid = iterator.next();
            Page p = bufferManager.getPage(rid.getPageId(), "movies.bin");
            Row row = p.getRow(rid.getSlotId());
            numRows++;
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
}
