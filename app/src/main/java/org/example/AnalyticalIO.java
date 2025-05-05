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

    // Returns base I/O cost for the work (title.principals) table
    public static int getBaseWorkedOnIO() {
        return Utilities.getNumberOfPages("work.bin");
    }

    // return matching number of rows where category = director after scanning through all the data
    public static int getMaterializedSelectivity() {
        String path = "C:\\Users\\HP\\Desktop\\ms\\645\\lab1\\645-Lab-32966720340112693401883534060222\\app\\shreya_perf_op\\work_director_analytical_match.csv";
        return readMatchCountFromCSV(path);
    }

    // return matching number of rows in range after scanning through all the data
    public static int getRangeSelectionSelectivity(String start, String end) {
        String fileName = start + "_" + end + "_analytical_match.csv";
        String path = "C:\\Users\\HP\\Desktop\\ms\\645\\lab1\\645-Lab-32966720340112693401883534060222\\app\\shreya_perf_op\\" + fileName;
        return readMatchCountFromCSV(path);
    }

    private static int readMatchCountFromCSV(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            br.readLine(); // skip header
            String line = br.readLine();
            if (line != null) {
                String[] parts = line.split(",");
                return Integer.parseInt(parts[parts.length - 1]); // last column is match count
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error reading match count from: " + filePath);
            e.printStackTrace();
        }
        return -1; // indicate error
    }

    public static int getMaterializedIO(int materializedRows) {
        int rowSize = MOVIE_ID_SIZE + PERSON_ID_SIZE;
        if (materializedRows <= 0) {
            return 0;
        }
        int rowsPerPage = Config.PAGE_SIZE / rowSize;
        int materializedPages = (int) Math.ceil((double) materializedRows / rowsPerPage);
        return materializedPages;
    }
    

    // Optional helper if you want to print total directly
    public static void printBaseIOCosts() {
        int moviePages = getBaseMovieIO();
        int peoplePages = getBasePeopleIO();
        int workPages = getBaseWorkedOnIO();
        int materializedRows = AnalyticalIO.getMaterializedSelectivity();
        int titleRangeRows = AnalyticalIO.getRangeSelectionSelectivity("Caa", "Cab");
        int materializedPages = getMaterializedIO(materializedRows);

        System.out.println("Movies Base IO Cost: " + moviePages);
        System.out.println("People Base IO Cost: " + peoplePages);
        System.out.println("WorkedOn Base IO Cost: " + workPages);
        System.out.println("Materialized Selectivity: " + materializedRows);
        System.out.println("Movies Selectivity: " + titleRangeRows);
        System.out.println("Materialized I/O Cost: " + materializedPages);
        System.out.println("Total IO Cost for Base and Materialized Tables: " + (moviePages+peoplePages+workPages+materializedPages));
    }
}
