package org.example;

import Row.Row;
import Utilities.Utilities;
import buffer.BufferManagerImplem;
import buffer.BufferManager;
import operators.Operator;
import operators.projectionOperator.TopProjectionOperator;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class performanceTesting {

    // function to run perf tests based on start and end ranges and bugger size
    public static void runPerformanceTest(String start, String end, int bufferSize) {
        //change this to directory of choice
        String outputDir = "C:\\Users\\HP\\Desktop\\ms\\645\\lab1\\645-Lab-32966720340112693401883534060222\\app\\shreya_perf_op";

        BufferManagerImplem bufferManager = new BufferManagerImplem(bufferSize);
        Operator topProjectionOperator = new TopProjectionOperator();

        topProjectionOperator.open(bufferManager, start, end, false);

        ArrayList<Row> rows = new ArrayList<>();
        Row row;
        while ((row = topProjectionOperator.next()) != null) {
            rows.add(row);
        }
        topProjectionOperator.close();

        // Dump result rows
        String rowOutputFile = outputDir + "\\" + start + "_" + end + "_data.csv";
        Utilities.writeCSV(rows, rowOutputFile);

        // Dump I/O stats
        String ioOutputFile = outputDir + "\\" + start + "_" + end + "_io.csv";
        try (PrintWriter pw = new PrintWriter(new FileWriter(ioOutputFile))) {
            pw.println("start_query,end_query,total_IOs");
            pw.printf("%s,%s,%d\n", start, end, bufferManager.getIOCount());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {


        System.out.println("Starting Caa to Cab, B=100");
        runPerformanceTest("Caa", "Cab", 100);

        System.out.println("Starting W to We, B=100");
        runPerformanceTest("W", "We", 100);

        System.out.println("Starting Ba to Bc, B=100");
        runPerformanceTest("Ba", "Bc", 100);

        System.out.println("Starting Caa to Cab, B=200");
        runPerformanceTest("Caa", "Cab", 200);

        System.out.println("Starting W to We, B=200");
        runPerformanceTest("W", "We", 200);

        System.out.println("Starting Ba to Bc, B=200");
        runPerformanceTest("Ba", "Bc", 200);

    }
}
