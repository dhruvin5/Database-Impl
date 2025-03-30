package org.example;

import Page.Page;
import Row.*;
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

            test1(bufferManager);
            test2(bufferManager);
            test3(bufferManager);
            test4(bufferManager);
            test5(bufferManager);

            System.out.print("Success");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void test1(BufferManager bufferManager) {
        // Get a Movie data page from the buffer manager
        Page moviePage = bufferManager.getPage(4, "movies.bin");
        bufferManager.unpinPage(4, "movies.bin");
        Row movie_row = moviePage.getRow(0);
        System.out.println("Movie Row: " + (movie_row.movieId) + " " + new String(movie_row.title));
    }

    private static void test2(BufferManager bufferManager) {
        // Create a leaf movie id index file
        Page movieid_index_page = bufferManager.createIndexPage("movie_Id_index.bin", true);
        bufferManager.unpinPage(0, "movie_Id_index.bin");
        movieid_index_page.insertRow(new leafRow("123456789".getBytes(), "4".getBytes(), "5".getBytes()));
        movieid_index_page.insertRow(new leafRow("145456788".getBytes(), "64".getBytes(), "2".getBytes()));
        // Get the first row from the movie index page
        Row row = movieid_index_page.getRow(0);
        System.out
                .println("Row: " + new String(row.key) + " " + new String(row.pid) + " " + new String(row.slotid));

        // Get the second row from the movie index page
        Row row1 = movieid_index_page.getRow(1);
        System.out.println(
                "Row: " + new String(row1.key) + " " + new String(row1.pid) + " " + new String(row1.slotid));
    }

    private static void test3(BufferManager bufferManager) {
        bufferManager.createIndexPage("title_index.bin", false);
        bufferManager.unpinPage(0, "title_index.bin");

        Page title_index_page = bufferManager.getPage(0, "title_index.bin");
        bufferManager.unpinPage(0, "title_index.bin");
        int val = title_index_page.insertRow(new leafRow("123456789".getBytes(), "4".getBytes(), "5".getBytes()));
        title_index_page.insertRow(new nonLeafRow("145467883".getBytes(), "14".getBytes()));
        Row title_row = title_index_page.getRow(0);
        System.out.println("Title Row: " + new String(title_row.key) + " " + new String(title_row.pid));
    }

    private static void test4(BufferManager bufferManager) {
        Page page = bufferManager.getPage(0, "movie_Id_index.bin");
        bufferManager.unpinPage(0, "movie_Id_index.bin");
        Row row = page.getRow(0);
        Row row1 = page.getRow(1);
        System.out.println("Row: " + new String(row.key) + " " + new String(row.pid) + " " + new String(row.slotid));
        System.out.println("Row: " + new String(row1.key) + " " + new String(row1.pid) + " " + new String(row1.slotid));

        Page page1 = bufferManager.getPage(0, "title_index.bin");
        bufferManager.unpinPage(0, "title_index.bin");
        Row row2 = page1.getRow(0);
        System.out.println("Row: " + new String(row2.key) + " " + new String(row2.pid));

    }

    private static void test5(BufferManager bufferManager) {
        Page page = bufferManager.getPage(0, "movie_Id_index.bin");
        bufferManager.unpinPage(0, "movie_Id_index.bin");
        // page.setNextPointer(5);
        System.out.println("Next Pointer: " + page.getNextPointer());

        Page page1 = bufferManager.getPage(0, "title_index.bin");
        page1.setNextPointer(6);
        System.out.println("Next Pointer: " + page1.getNextPointer());

    }
}
