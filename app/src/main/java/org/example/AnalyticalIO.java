package org.example;

import SystemCatalog.systemCatalog;
import Row.Row;
import buffer.BufferManager;
import Page.Page;
import configs.Config;
import Utilities.Utilities;
import buffer.BufferManagerImplem;

import java.nio.charset.StandardCharsets;

public class AnalyticalIO {

    private static final int MOVIE_ID_SIZE = 9;
    private static final int PERSON_ID_SIZE = 10;

    // Helper to hold stats
    private static class TableStats {
        int totalRows;
        int matchingRows;

        TableStats(int total, int match) {
            this.totalRows = total;
            this.matchingRows = match;
        }

        double getSelectivity() {
            return totalRows == 0 ? 0.0 : (double) matchingRows / totalRows;
        }
    }

    private static TableStats analyzeWorkedOn(BufferManager bm, String file, String category) {
        int total = 0, matched = 0;
        int pages = Utilities.getNumberOfPages(file);
        for (int p = 0; p < pages; p++) {
            Page page = bm.getPage(p, file);
            for (int r = 0; r < page.getRowCount(); r++) {
                Row row = page.getRow(r);
                total++;
                if (new String(row.category, StandardCharsets.UTF_8).trim().equals(category)) matched++;
            }
        }
        return new TableStats(total, matched);
    }

    private static TableStats analyzeMovies(BufferManager bm, String file, char start, char end) {
        int total = 0, matched = 0;
        int pages = Utilities.getNumberOfPages(file);
        for (int p = 0; p < pages; p++) {
            Page page = bm.getPage(p, file);
            for (int r = 0; r < page.getRowCount(); r++) {
                Row row = page.getRow(r);
                String title = new String(row.title, StandardCharsets.UTF_8).trim();
                if (!title.isEmpty()) {
                    char first = title.charAt(0);
                    if (first >= start && first <= end) matched++;
                    total++;
                }
            }
        }
        return new TableStats(total, matched);
    }

    private static int calculateMaterializedPages(int selectedRows) {
        int rowSize = MOVIE_ID_SIZE + PERSON_ID_SIZE;
        return (int) Math.ceil((double) selectedRows * rowSize / Config.PAGE_SIZE);
    }

    public static double calculateBNLJoinIOCost(int outerPages, int innerPages, int bufferSize) {
        int blockSize = (bufferSize - 2) / 2;
        int numBlocks = (int) Math.ceil((double) outerPages / blockSize);
        return outerPages + (numBlocks * innerPages);
    }

    public static void main(String[] args) {
        BufferManager bm = new BufferManagerImplem(10);
        systemCatalog catalog = new systemCatalog();

        // WorkedOn
        String workedOnFile = catalog.getTableFile("WorkedOn");
        TableStats workedStats = analyzeWorkedOn(bm, workedOnFile, "director");
        double workedSelectivity = workedStats.getSelectivity();
        int materializedPages = calculateMaterializedPages((int) (workedStats.totalRows * workedSelectivity));
        double totalWorkedCost = Utilities.getNumberOfPages(workedOnFile) + 2 * materializedPages;
        System.out.println("WorkedOn Selectivity: " + workedSelectivity);
        System.out.println("WorkedOn Total IO Cost: " + totalWorkedCost);

        // Movies
        String moviesFile = catalog.getTableFile("Movies");
        TableStats movieStats = analyzeMovies(bm, moviesFile, 'C', 'D');
        double movieSelectivity = movieStats.getSelectivity();
        int moviePages = Utilities.getNumberOfPages(moviesFile);
        int outerPages = (int) Math.ceil(movieSelectivity * moviePages);
        System.out.println("Movies Selectivity: " + movieSelectivity);
        System.out.println("Movies Base IO Cost: " + moviePages);

        // Join
        double joinCost = calculateBNLJoinIOCost(outerPages, materializedPages, 10);
        System.out.println("BNL Join IO Cost: " + joinCost);
    }
}
