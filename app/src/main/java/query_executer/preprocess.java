package query_executer;

import java.io.File;

import Utilities.Utilities;
import buffer.BufferManager;
import buffer.BufferManagerImplem;

public class preprocess {
    public static void main(String[] args) {
        BufferManager bufferManager = new BufferManagerImplem(1000);

        File movieFile = new File("movies.bin");
        File workFile = new File("work.bin");
        File peopleFile = new File("people.bin");
        File title_index = new File("title_index.bin");

        if (!movieFile.exists())
            Utilities.loadDataset(bufferManager,
                    "/Users/simranmalik/Desktop/title.basics.tsv"); // Load movies dataset

        if (!workFile.exists())
            Utilities.loadWorkDataset(bufferManager,
                    "/Users/simranmalik/Desktop/title.principals.tsv"); // Load work dataset
        if (!peopleFile.exists())
            Utilities.loadPeopleDataset(bufferManager,
                    "/Users/simranmalik/Desktop/name.basics.tsv"); // Load people dataset

        if (!title_index.exists())
            Utilities.createTitleIndex(bufferManager); // Creating Bplus index on title

        bufferManager.force();
        System.out.println("Data loaded into Tables, Title Index built.");
        System.out.println("Preprocessing Completed Successfully!");
    }
}
