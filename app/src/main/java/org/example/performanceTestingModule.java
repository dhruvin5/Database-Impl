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
        List<String> linearScanResult = new ArrayList<>(1000);
        List<String> bPlusTreeResult = new ArrayList<>(1000);

        for (int i = 0; i < 1000; i++) {
            // Generate random range for movieId in the format of tt0000000
            int rangeStart = random.nextInt(5500000);
            int rangeEnd = rangeStart + random.nextInt(100);

            String rangeStartStr = String.format("tt%07d", rangeStart);
            String rangeEndStr = String.format("tt%07d", rangeEnd);

            // Method 1: Direct scan
            bufferManager.clearCache();
            long startTimeMethod1 = System.nanoTime();
            int numRowsMethod1 = directScan(bufferManager, rangeStartStr, rangeEndStr, false);
            long timeMethod1 = System.nanoTime() - startTimeMethod1;

            // Method 2: Using B+ Tree index
            bufferManager.clearCache();
            long startTimeMethod2 = System.nanoTime();
            int numRowsMethod2 = bPlusTreeScan(movieIdIndex, rangeStartStr, rangeEndStr);
            long timeMethod2 = System.nanoTime() - startTimeMethod2;

            if (numRowsMethod1 != numRowsMethod2) {
                System.out.println("ERROR: Mismatch in row counts: " + numRowsMethod1 + " vs " + numRowsMethod2);
            }

            System.out.println("Query: " + i + " Range: " + rangeStartStr + " to " + rangeEndStr);

            // Collect data
            linearScanResult.add((rangeEnd - rangeStart) + " " + (timeMethod1 / 1000000.0));
            bPlusTreeResult.add((rangeEnd - rangeStart) + " " + (timeMethod2 / 1000000.0));
        }

        // Sort results
        linearScanResult.sort((a, b) -> Integer.compare(
                Integer.parseInt(a.split(" ")[0]),
                Integer.parseInt(b.split(" ")[0])));
        bPlusTreeResult.sort((a, b) -> Integer.compare(
                Integer.parseInt(a.split(" ")[0]),
                Integer.parseInt(b.split(" ")[0])));

        // Write results to file
        writeResultsToFile("movieid_linearScanResults.txt", linearScanResult);
        writeResultsToFile("movieid_bPlusTreeResults.txt", bPlusTreeResult);
    }

    public void performanceTesting_Title() {
        Random random = new Random();
        List<String> linearScanResult = new ArrayList<>();
        List<String> bPlusTreeResult = new ArrayList<>();

        for (int i = 0; i < 2500; i++) {
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
            int numRowsMethod1 = directScan(bufferManager, rangeStartStr, rangeEndStr, true);
            long endTimeMethod1 = System.nanoTime();
            long timeMethod1 = endTimeMethod1 - startTimeMethod1;
            System.out.println("Direct Scan Time: " + timeMethod1 / 1000000.0 + " Found " + numRowsMethod1 + " rows");

            // Method 2: Using B+ Tree index
            System.out.println("B+ Tree Scan");
            bufferManager.clearCache();
            long startTimeMethod2 = System.nanoTime();
            int numRowsMethod2 = bPlusTreeScan(titleIndex, rangeStartStr, rangeEndStr);
            long endTimeMethod2 = System.nanoTime();
            long timeMethod2 = endTimeMethod2 - startTimeMethod2;
            System.out.println("B+ Tree Scan Time: " + timeMethod2 / 1000000.0);

            if (numRowsMethod1 != numRowsMethod2) {
                System.out.println("ERROR: Mismatch in row counts: " + numRowsMethod1 + " vs " + numRowsMethod2);
            }

            // Collect data
            linearScanResult.add(numRowsMethod1 + " " + timeMethod1 / 1000000.0);
            bPlusTreeResult.add(numRowsMethod2 + " " + timeMethod2 / 1000000.0);

            if (i % 100 == 0) {
                linearScanResult.sort((a, b) -> Integer.compare(
                        Integer.parseInt(a.substring(0, a.indexOf(' '))),
                        Integer.parseInt(b.substring(0, b.indexOf(' ')))));
                bPlusTreeResult.sort((a, b) -> Integer.compare(
                        Integer.parseInt(a.substring(0, a.indexOf(' '))),
                        Integer.parseInt(b.substring(0, b.indexOf(' ')))));

                writeResultsToFile("title_linearScanResults.txt", linearScanResult);
                writeResultsToFile("title_bPlusTreeResults.txt", bPlusTreeResult);
            }
        }

        linearScanResult.sort((a, b) -> Integer.compare(
                Integer.parseInt(a.substring(0, a.indexOf(' '))),
                Integer.parseInt(b.substring(0, b.indexOf(' ')))));
        bPlusTreeResult.sort((a, b) -> Integer.compare(
                Integer.parseInt(a.substring(0, a.indexOf(' '))),
                Integer.parseInt(b.substring(0, b.indexOf(' ')))));

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

                if (target.compareTo(rangeEnd) > 0 && !isTitle) {
                    foundAll = true;
                    break;
                }
                rowId++;
            }
            manager.unpinPage(currentPageId, "movies.bin");
            // System.out.println("Page ID: " + currentPageId);
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
