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
import Row.Row;

public class Caller {
    public static void main(String[] args) {
        BufferManager bufferManager = new BufferManagerImplem(10);
        Utilities.loadDataset(bufferManager, "C:/Users/bhaga/Downloads/title.basics.tsv"); // Load movies dataset
        Utilities.loadWorkDataset(bufferManager,
                "C:/Users/bhaga/Downloads/title.principals.tsv"); // Load work dataset
        Utilities.loadPeopleDataset(bufferManager,
                "C:/Users/bhaga/Downloads/name.basics.tsv"); // Load people dataset

        Operator movieSelect = new MovieSelectionOperator();
        movieSelect.open(bufferManager, "A", "B"); // Select movies with title between A and B

        Operator workSelect = new WorkSelectionOperator();
        workSelect.open(bufferManager);

        Row row = null;
        while ((row = workSelect.next()) != null) {
            System.out
                    .println(new String(row.movieId) + " " + new String(row.category) + " " + new String(row.personId));
        }

        workSelect.close();
        // movieSelect.close();

    }
}