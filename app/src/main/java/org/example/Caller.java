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
        BufferManager bufferManager = new BufferManagerImplem(1000);

        File movieFile = new File("movies.bin");
        File workFile = new File("work.bin");
        File peopleFile = new File("people.bin");

        if (!movieFile.exists())
            Utilities.loadDataset(bufferManager,
                    "/Users/simranmalik/Desktop/title.basics.tsv"); // Load movies dataset

        if (!workFile.exists())
            Utilities.loadWorkDataset(bufferManager,
                    "/Users/simranmalik/Desktop/title.principals.tsv"); // Load work dataset
        if (!peopleFile.exists())
            Utilities.loadPeopleDataset(bufferManager,
                    "/Users/simranmalik/Desktop/name.basics.tsv"); // Load people dataset

        bufferManager.force();
        System.out.println("Data loaded successfully.");

        // Create a top projection operator
        Operator topProjectionOperator = new TopProjectionOperator();
        topProjectionOperator.open(bufferManager, "Alaa", "Alab", false); // Open the operator

        ArrayList<Row> rows = new ArrayList<>();
        Row row = null;
        while ((row = topProjectionOperator.next()) != null) {
            rows.add(row);
        }
        topProjectionOperator.close(); // Close the operator
        Utilities.writeCSV(rows, "output_sim3.csv"); // Write the output to a file

    }
}