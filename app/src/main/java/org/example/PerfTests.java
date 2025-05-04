package org.example;

import SystemCatalog.systemCatalog;
import Row.Row;
import buffer.BufferManager;
import Page.Page;
import configs.Config;
import Utilities.Utilities;
import buffer.BufferManagerImplem;

import java.nio.charset.StandardCharsets;

public class PerfTests {

    // Constants for column sizes
    private static final int MOVIE_ID_SIZE = 9;
    private static final int PERSON_ID_SIZE = 10;

    // Step 1: I/O Cost Calculation for Base WorkedOn Table
    public static double calculateWorkedOnIOCost(BufferManager bufferManager, systemCatalog catalog) {
        String workedOnFile = catalog.getTableFile("WorkedOn");
        return (double) Utilities.getNumberOfPages(workedOnFile);
    }

    // Step 2: Calculate WorkedOn selectivity
    public static double calculateWorkedOnSelectivity(BufferManager bufferManager, systemCatalog catalog, String category) {
        String workedOnFile = catalog.getTableFile("WorkedOn");
        int totalRowsWorkedOn = 0;
        int totalRowsCategory = 0;

        int P_work = Utilities.getNumberOfPages(workedOnFile);
        for (int pageId = 0; pageId < P_work; pageId++) {
            Page page = bufferManager.getPage(pageId, workedOnFile);
            for (int rowId = 0; rowId < page.getRowCount(); rowId++) {
                Row row = page.getRow(rowId);
                totalRowsWorkedOn++;
                if (new String(row.category, StandardCharsets.UTF_8).trim().equals(category)) {
                    totalRowsCategory++;
                }
            }
        }
        return (double) totalRowsCategory / totalRowsWorkedOn;
    }

    // Step 3: Calculate I/O cost for materialized WorkedOn table
    public static double calculateMaterializedWorkedOnIOCost(BufferManager bufferManager, systemCatalog catalog, double workedOnSelectivity) {
        String workedOnFile = catalog.getTableFile("WorkedOn");
        int totalRows = getTotalRows(bufferManager, workedOnFile);
        int selectedRows = (int) (workedOnSelectivity * totalRows);

        int materializedRowSize = MOVIE_ID_SIZE + PERSON_ID_SIZE;
        int pageSize = Config.PAGE_SIZE;

        int materializedPages = (int) Math.ceil((double) (selectedRows * materializedRowSize) / pageSize);
        return materializedPages;
    }

    // Step 4: Calculate total I/O cost for WorkedOn table
    public static double calculateTotalWorkedOnIOCost(BufferManager bufferManager, systemCatalog catalog, double workedOnSelectivity) {
        double baseWorkedOnIOCost = calculateWorkedOnIOCost(bufferManager, catalog);
        double materializedWorkedOnIOCost = calculateMaterializedWorkedOnIOCost(bufferManager, catalog, workedOnSelectivity);
        return baseWorkedOnIOCost + 2 * materializedWorkedOnIOCost;
    }

    // Step 5: I/O Cost Calculation for Base Movies Table
    public static double calculateMoviesIOCost(BufferManager bufferManager, systemCatalog catalog) {
        String moviesFile = catalog.getTableFile("Movies");
        return (double) Utilities.getNumberOfPages(moviesFile);
    }

    // Step 6: Calculate Movies Table Selectivity (based on 1st character of title)
    public static double calculateMoviesSelectivity(BufferManager bufferManager, systemCatalog catalog, char start, char end) {
        String moviesFile = catalog.getTableFile("Movies");

        int totalRows = 0;
        int rowsInRange = 0;
        int totalPages = Utilities.getNumberOfPages(moviesFile);

        for (int pageId = 0; pageId < totalPages; pageId++) {
            Page page = bufferManager.getPage(pageId, moviesFile);
            for (int rowId = 0; rowId < page.getRowCount(); rowId++) {
                Row row = page.getRow(rowId);
                String title = new String(row.title, StandardCharsets.UTF_8).trim();
                if (!title.isEmpty()) {
                    char firstChar = title.charAt(0);
                    if (firstChar >= start && firstChar <= end) {
                        rowsInRange++;
                    }
                    totalRows++;
                }
            }
        }
        return totalRows == 0 ? 0.0 : (double) rowsInRange / totalRows;
    }

    // Step 7: Calculate BNL Join I/O Cost between Movies and Materialized WorkedOn
    public static double calculateBNLJoinIOCost(double moviesSelectivity, int totalPagesMovies, int materializedWorkedOnPages, int bufferSize) {
        int C = 2; // Reserved pages for inner scan and output buffer
        int blockSize = (bufferSize - C) / 2;

        // Pages from Movies participating in the join
        int outerPages = (int) Math.ceil(moviesSelectivity * totalPagesMovies);

        // Number of blocks to scan all outer pages
        int numBlocks = (int) Math.ceil((double) outerPages / blockSize);

        // Total cost = outer scan + (inner scan per block)
        return outerPages + numBlocks * materializedWorkedOnPages;
    }


    // Helper to get row count from file
    private static int getTotalRows(BufferManager bufferManager, String fileName) {
        int totalRows = 0;
        int pages = Utilities.getNumberOfPages(fileName);
        for (int pageId = 0; pageId < pages; pageId++) {
            Page page = bufferManager.getPage(pageId, fileName);
            totalRows += page.getRowCount();
        }
        return totalRows;
    }

    // Main method for testing
    public static void main(String[] args) {
        BufferManager bufferManager = new BufferManagerImplem(10);  // Example buffer size
        systemCatalog catalog = new systemCatalog();

        // --- WorkedOn analysis ---
        String category = "director";
        double workedOnSelectivity = calculateWorkedOnSelectivity(bufferManager, catalog, category);
        System.out.println("WorkedOn Selectivity (category = 'director'): " + workedOnSelectivity);

        double totalWorkedOnIOCost = calculateTotalWorkedOnIOCost(bufferManager, catalog, workedOnSelectivity);
        System.out.println("Total I/O Cost for WorkedOn Table: " + totalWorkedOnIOCost);

        // --- Movies analysis ---
        char startRange = 'C';
        char endRange = 'D';

        double moviesSelectivity = calculateMoviesSelectivity(bufferManager, catalog, startRange, endRange);
        System.out.println("Movies Selectivity (title starts between '" + startRange + "' and '" + endRange + "'): " + moviesSelectivity);

        double moviesIOCost = calculateMoviesIOCost(bufferManager, catalog);
        System.out.println("Base I/O Cost for Movies Table: " + moviesIOCost);

        int totalPagesMovies = Utilities.getNumberOfPages(catalog.getTableFile("Movies"));
        int materializedPages = (int) calculateMaterializedWorkedOnIOCost(bufferManager, catalog, workedOnSelectivity);

        double bnlJoinCost = calculateBNLJoinIOCost(moviesSelectivity, totalPagesMovies, materializedPages, 10);
        System.out.println("BNL Join I/O Cost (Movies ⨝ WorkedOn): " + bnlJoinCost);

    }
}
