package org.example;

import Utilities.Utilities;
import buffer.BufferManager;
import buffer.BufferManagerImplem;
import operators.Operator;
import operators.tableOperator.MovieOperator;
import operators.tableOperator.PeopleOperator;
import operators.tableOperator.WorkOperator;
import operators.selectionOperator.MovieSelectionOperator;
import operators.selectionOperator.WorkSelectionOperator;
import operators.projectionOperator.WorkProjectionOperator;
import operators.joinOperator.BNLOperator1;
import operators.joinOperator.BNLOperator2;
import operators.projectionOperator.TopProjectionOperator;

import java.util.ArrayList;

import Row.Row;

public class Caller {
    public static void main(String[] args) {
        BufferManager bufferManager = new BufferManagerImplem(10);
        // Utilities.loadDataset(bufferManager,
        // "C:/Users/bhaga/Downloads/title.basics.tsv"); // Load movies dataset
        // Utilities.loadWorkDataset(bufferManager,
        // "C:/Users/bhaga/Downloads/title.principals.tsv"); // Load work dataset
        // Utilities.loadPeopleDataset(bufferManager,
        // "C:/Users/bhaga/Downloads/name.basics.tsv"); // Load people dataset

        // Create a top projection operator
        TopProjectionOperator topProjectionOperator = new TopProjectionOperator();
        topProjectionOperator.open(bufferManager, "A", "Z"); // Open the operator with a range
        ArrayList<Row> rows = new ArrayList<>();
        Row row = null;
        while ((row = topProjectionOperator.next()) != null) {
            rows.add(row);
        }
        topProjectionOperator.close(); // Close the operator
        Utilities.writeCSV(rows, "output.csv"); // Write the output to a file
    }
}