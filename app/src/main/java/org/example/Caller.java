package org.example;

import java.io.File;
import java.util.ArrayList;

import Row.Row;
import Utilities.Utilities;
import buffer.BufferManager;
import buffer.BufferManagerImplem;
import operators.Operator;
import operators.projectionOperator.TopProjectionOperator;
import SystemCatalog.systemCatalog;
import org.example.AnalyticalIO;

public class Caller {

    public static void main(String[] args) {
        System.out.println("hi");
        // AnalyticalIO.printCosts();
        performanceTesting.main(args);
    }
    
    // public static void main(String[] args) {
    //     BufferManager bufferManager = new BufferManagerImplem(100);

    //     File movieFile = new File("movies.bin");
    //     File workFile = new File("work.bin");
    //     File peopleFile = new File("people.bin");

    //     if (!movieFile.exists()) {
    //         System.out.println("loading............");
    //         Utilities.loadDataset(bufferManager,
    //                 "/Users/Admin/Desktop/645/lab/title.basics.tsv");
    //     }// Load movies dataset

    //     if (!workFile.exists())
    //         Utilities.loadWorkDataset(bufferManager,
    //                 "/Users/Admin/Desktop/645/lab/title.principals.tsv"); // Load work dataset
    //     if (!peopleFile.exists())
    //         Utilities.loadPeopleDataset(bufferManager,
    //                 "/Users/Admin/Desktop/645/lab/name.basics.tsv"); // Load people dataset

    //     bufferManager.force();
    //     bufferManager.clearCache();
    //     System.out.println("Data loaded successfully.");

    //     Utilities.createTitleIndex(bufferManager);

    //     System.out.println("Title index created successfully.");

    //     bufferManager.force();
    //     bufferManager.clearCache();

    //     // Create a top projection operator
    //     Operator topProjectionOperator = new TopProjectionOperator();
    //     topProjectionOperator.open(bufferManager, "Alaa", "Alab", true); // Open the operator

    //     ArrayList<Row> rows = new ArrayList<>();
    //     Row row = null;
    //     while ((row = topProjectionOperator.next()) != null) {
    //         rows.add(row);
    //     }
    //     topProjectionOperator.close(); // Close the operator
    //     Utilities.writeCSV(rows, "output_dg10.csv"); // Write the output to a file

    // }

    // public static void main(String[] args) {
    //     System.out.println("hi");
    //     BufferManager bufferManager = new BufferManagerImplem(1000);

    //     File movieFile = new File("movies.bin");
    //     File workFile = new File("work.bin");
    //     File peopleFile = new File("people.bin");

    //     if (!movieFile.exists())
    //         Utilities.loadDataset(bufferManager,
    //                 "C:\\Users\\HP\\Desktop\\ms\\645\\lab1\\645-Lab-32966720340112693401883534060222\\app\\src\\main\\java\\title.basics.tsv"); // Load movies dataset

    //     if (!workFile.exists())
    //         Utilities.loadWorkDataset(bufferManager,
    //                 "C:\\Users\\HP\\Desktop\\ms\\645\\lab1\\645-Lab-32966720340112693401883534060222\\app\\src\\main\\java\\title.principals.tsv"); // Load work dataset
    //     if (!peopleFile.exists())
    //         Utilities.loadPeopleDataset(bufferManager,
    //                 "C:\\Users\\HP\\Desktop\\ms\\645\\lab1\\645-Lab-32966720340112693401883534060222\\app\\src\\main\\java\\n" + //
    //                                             "ame.basics.tsv"); // Load people dataset

    //     bufferManager.force();
    //     System.out.println("Data loaded successfully.");

    //     // Create a top projection operator
    //     Operator topProjectionOperator = new TopProjectionOperator();
    //     topProjectionOperator.open(bufferManager, "Caa", "Cab", false); // Open the operator

    //     ArrayList<Row> rows = new ArrayList<>();
    //     Row row = null;
    //     while ((row = topProjectionOperator.next()) != null) {
    //         rows.add(row);
    //     }
    //     topProjectionOperator.close(); // Close the operator
    //     Utilities.writeCSV(rows, "shreya_op.csv"); // Write the output to a file

    //     // Print the actual I/O count incurred during execution
    //     System.out.println("Total I/O operations (System calculated): " + bufferManager.getIOCount());

    // }
}