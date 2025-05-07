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

    // to run the caller to load datasets
     public static void main(String[] args) {
         BufferManager bufferManager = new BufferManagerImplem(100);

         File movieFile = new File("movies.bin");
         File workFile = new File("work.bin");
         File peopleFile = new File("people.bin");

         if (!movieFile.exists()) {
             System.out.println("loading............");
             Utilities.loadDataset(bufferManager,
                     "/Users/Admin/Desktop/645/lab/title.basics.tsv");
         }// Load movies dataset

         if (!workFile.exists())
             Utilities.loadWorkDataset(bufferManager,
                     "/Users/Admin/Desktop/645/lab/title.principals.tsv"); // Load work dataset
         if (!peopleFile.exists())
             Utilities.loadPeopleDataset(bufferManager,
                     "/Users/Admin/Desktop/645/lab/name.basics.tsv"); // Load people dataset

         bufferManager.force();
         bufferManager.clearCache();
         System.out.println("Data loaded successfully.");

         Utilities.createTitleIndex(bufferManager);

         System.out.println("Title index created successfully.");

         bufferManager.force();
         bufferManager.clearCache();

         // Create a top projection operator
         Operator topProjectionOperator = new TopProjectionOperator();
         topProjectionOperator.open(bufferManager, "Alaa", "Alab", true); // Open the operator

         ArrayList<Row> rows = new ArrayList<>();
         Row row = null;
         while ((row = topProjectionOperator.next()) != null) {
             rows.add(row);
         }
         topProjectionOperator.close(); // Close the operator
         Utilities.writeCSV(rows, "output_dg11.csv"); // Write the output to a file

     }


    // Uncomment to run perf tests and find system IOs
    // public static void main(String[] args) {
    //     System.out.println("hi");
    //     performanceTesting.main(args);
    // }

    // Uncomment to find Analytical IOs
    // public static void main(String[] args) {
    //     System.out.println("hi");
    //     AnalyticalIO.printCosts();
    // }
}