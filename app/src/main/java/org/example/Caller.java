package org.example;

import java.util.Iterator;

import Bplus.BplusTreeImplem;
import Bplus.Rid;
import Page.Page;
import Row.Row;
import Utilities.Utilities;
import buffer.BufferManager;
import buffer.BufferManagerImplem;

public class Caller {
    public static void main(String[] args) {

        try {

            // Initialize BufferManager with size 10
            BufferManager bufferManager = new BufferManagerImplem(1);

            // Load dataset into the buffer
            Utilities.loadDataset(bufferManager, "C:/Users/bhaga/Downloads/title.basics.tsv");
            System.out.println("PASS: Buffer Manager initialized with buffer size: 10");

            // Get a Movie data page from the buffer manager
            Page moviePage = bufferManager.getPage(4, "movies.bin");
            Row movie_row = moviePage.getRow(0);
            System.out.println(
                    "Movie Row: " + (movie_row.movieId) + " " + new String(movie_row.title));

            bufferManager.unpinPage(4, "movies.bin");

            // Create a leaf movie id index file
            Page movieid_index_page = bufferManager.createIndexPage("movie_Id_index.bin", true);
            movieid_index_page.insertRow(new Row("123456789".getBytes(), "4".getBytes(), "5".getBytes()));
            movieid_index_page.insertRow(new Row("145456788".getBytes(), "64".getBytes(), "2".getBytes()));
            Row row = movieid_index_page.getRow(0);
            System.out
                    .println("Row: " + new String(row.key) + " " + new String(row.pid) + " " + new String(row.slotid));
            Row row1 = movieid_index_page.getRow(1);
            System.out.println(
                    "Row: " + new String(row1.key) + " " + new String(row1.pid) + " " + new String(row1.slotid));

            System.out.print("Success");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
