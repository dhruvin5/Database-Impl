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
import Row.Row;

public class Caller {
    public static void main(String[] args) {
        BufferManager bufferManager = new BufferManagerImplem(10);
        Utilities.loadDataset(bufferManager, "C:/Users/bhaga/Downloads/title.basics.tsv"); // Load movies dataset
        Utilities.loadWorkDataset(bufferManager, "C:/Users/bhaga/Downloads/title.principals.tsv"); // Load work dataset
        Utilities.loadPeopleDataset(bufferManager, "C:/Users/bhaga/Downloads/name.basics.tsv"); // Load people dataset

        // Create a work projection operator
        WorkProjectionOperator workProjectionOperator = new WorkProjectionOperator();
        workProjectionOperator.open(bufferManager); // Open the projection operator with the specified range

        Row row = null;
        int count = 0;
        while ((row = workProjectionOperator.next()) != null) {
            count++;
            System.out.println("MoiveId" + " " + new String(row.movieId) + " " + "PersonId" + " "
                    + new String(row.personId) + " " + count);
        }

        while ((row = workProjectionOperator.next()) != null) {
            count++;
            System.out.println("MoiveId" + " " + new String(row.movieId) + " " + "PersonId" + " "
                    + new String(row.personId) + " " + count);
        }

    }
}