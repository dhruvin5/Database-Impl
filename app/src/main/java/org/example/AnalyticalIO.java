package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import configs.Config;
import Utilities.Utilities;

public class AnalyticalIO {

    private static final int MOVIE_ID_SIZE = 9;
    private static final int PERSON_ID_SIZE = 10;

    // Returns base I/O cost for the movies table
    public static int getBaseMovieIO() {
        return Utilities.getNumberOfPages("movies.bin");
    }

    // Returns base I/O cost for the people table
    public static int getBasePeopleIO() {
        return Utilities.getNumberOfPages("people.bin");
    }

    // Returns base I/O cost for the work table
    public static int getBaseWorkedOnIO() {
        return Utilities.getNumberOfPages("work.bin");
    }

    // return matching number of rows where category = director after scanning through all the data i.e. worked on selectivity
    public static int getMaterializedSelectivity() {
        String path = "C:\\Users\\HP\\Desktop\\ms\\645\\lab1\\645-Lab-32966720340112693401883534060222\\app\\shreya_perf_op\\work_director_analytical_match.csv";
        return readMatchCountFromCSV(path);
    }

    // return movies selectivity that fall in range of start and end queries
    public static int getRangeSelectionSelectivity(String start, String end) {
        String fileName = start + "_" + end + "_data.csv";
        String path = "C:\\Users\\HP\\Desktop\\ms\\645\\lab1\\645-Lab-32966720340112693401883534060222\\app\\shreya_perf_op\\" + fileName;
        return countDataRowsInCSV(path);
    }

    // helper to count non-header rows in csv files
    private static int countDataRowsInCSV(String filePath) {
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            br.readLine();
            while (br.readLine() != null) {
                count++;
            }
        } catch (IOException e) {
            System.err.println("Error reading data rows from: " + filePath);
            e.printStackTrace();
        }
        return count;
    }

    // helper to count matching rows in csv files
    private static int readMatchCountFromCSV(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            br.readLine();
            String line = br.readLine();
            if (line != null) {
                String[] parts = line.split(",");
                return Integer.parseInt(parts[parts.length - 1]); // last column is match count
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error reading match count from: " + filePath);
            e.printStackTrace();
        }
        return -1;
    }

    // Computes materialized I/O cost based on number of rows
    public static int getMaterializedIO(int materializedRows) {
        int rowSize = MOVIE_ID_SIZE + PERSON_ID_SIZE;
        if (materializedRows <= 0) {
            return 0;
        }
        int rowsPerPage = Config.PAGE_SIZE / rowSize;
        int materializedPages = (int) Math.ceil((double) materializedRows / rowsPerPage);
        return materializedPages;
    }

    // Calculates cost of first block nested loop join
    public static int getBNL1Cost(int m, int n, int o, int ms, int B) {
        int N = (B-4)/2;
        int bs = (Config.PAGE_SIZE - 4) / 39;
        double passes = Math.ceil(1 + Math.ceil(((double) ms / bs) / N));
        return m + n + (int) passes * o;
    }
    
    // Estimates BNL1 join selectivity
    public static int getBNL1Selectivity(int ms, int ws) {
        return (int) (((double) ms * ws) / Math.max(ms, ws));
    }

    // Calculates cost of second block nested loop join
    public static int getBNL2Cost(int t, int p, int B) {
        int N = (B - 4) / 2;
        int r = (Config.PAGE_SIZE - 4)/49;
        double passes = Math.ceil(((double) t / r) / N);
        return (int) (passes * p);
    }

    // Sums up BNL1 and BNL2 costs to get total plan cost
    public static int getTotalPlanCost(int bnl1Cost, int bnl2Cost){
        return (int)(bnl1Cost + bnl2Cost);
    }

    // Prints out various I/O and cost metrics for the analytical plan
    public static void printCosts() {
        int moviePages = getBaseMovieIO();
        int peoplePages = getBasePeopleIO();
        int workPages = getBaseWorkedOnIO();
        int materializedRows = AnalyticalIO.getMaterializedSelectivity();
        int titleRangeRows = AnalyticalIO.getRangeSelectionSelectivity("W", "We");
        int materializedPages = getMaterializedIO(materializedRows);
        int bnl1Cost = getBNL1Cost(moviePages, workPages, materializedPages, titleRangeRows, 200);
        int bnl1Selectivity = getBNL1Selectivity(titleRangeRows, materializedRows);
        int bnl2Cost = getBNL2Cost(bnl1Selectivity, peoplePages, 200);
        int totalPlanCost = getTotalPlanCost(bnl1Cost, bnl2Cost);

        System.out.println("Movies Base IO Cost: " + moviePages);
        System.out.println("People Base IO Cost: " + peoplePages);
        System.out.println("WorkedOn Base IO Cost: " + workPages);
        System.out.println("Materialized Selectivity: " + materializedRows);
        System.out.println("Movies Selectivity: " + titleRangeRows);
        System.out.println("Materialized I/O Cost: " + materializedPages);
        System.out.println("Total IO Cost for Base and Materialized Tables: " + (moviePages+peoplePages+workPages+materializedPages));
        System.out.println("Cost Upto First BNL Join: "+ bnl1Cost);
        System.out.println("BNL1 Selectivity "+ bnl1Selectivity);
        System.out.println("BNL2 Cost: "+ bnl2Cost);
        System.out.println("Total Plan Cost: " + totalPlanCost);

    }
}
