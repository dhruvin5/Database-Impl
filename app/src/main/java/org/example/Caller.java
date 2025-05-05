package org.example;

import java.io.File;
import java.util.ArrayList;

import Row.Row;
import Utilities.Utilities;
import buffer.BufferManager;
import buffer.BufferManagerImplem;
import operators.Operator;
import operators.projectionOperator.TopProjectionOperator;

public class Caller {

    public static void main(String[] args) {
        System.out.println("hi");
        performanceTesting.main(args);
    }

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