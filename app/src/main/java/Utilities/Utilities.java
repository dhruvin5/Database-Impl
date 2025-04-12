package Utilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;

import Page.Page;
import Row.Row;
import Row.movieRow;
import Row.peopleRow;
import Row.workRow;
import buffer.BufferManager;
import configs.Config;

public class Utilities {

    // loads the dataset into a disk file
    // takes the bufferManagaer and the filePath as the input.
    public static void loadDataset(BufferManager bf, String filepath) {
        generalLoadDataset(bf, filepath, "movies.bin",
                columns -> {
                    byte[] movieId = toFixedByteArray(columns[0], 9);
                    byte[] title = toFixedByteArray(columns[2], 30);
                    return new movieRow(movieId, title);
                });
    }

    public static void loadWorkDataset(BufferManager bf, String filepath) {
        generalLoadDataset(bf, filepath, "work.bin",
                columns -> {
                    byte[] movieId = toFixedByteArray(columns[0], 9);
                    byte[] personId = toFixedByteArray(columns[2], 9);
                    byte[] category = toFixedByteArray(columns[3], 20);
                    return new workRow(movieId, personId, category);
                });
    }

    public static void loadPeopleDataset(BufferManager bf, String filepath) {
        generalLoadDataset(bf, filepath, "people.bin",
                columns -> {
                    byte[] personId = toFixedByteArray(columns[0], 9);
                    byte[] name = toFixedByteArray(columns[1], 105);
                    return new peopleRow(personId, name);
                });
    }

    // Utility to convert a string to a fixed byte array of a particular length
    public static byte[] toFixedByteArray(String inputString, int length) {
        if (inputString == null) {
            inputString = "";
        }
        if (inputString.length() > length) {
            inputString = inputString.substring(0, length); // truncate
        }

        // get the bytes from the string
        byte[] originalString = inputString.getBytes(StandardCharsets.UTF_8);

        byte[] fixedByteArray = new byte[length];

        // min truncates if the size is greaterthan the specified length
        System.arraycopy(originalString, 0, fixedByteArray, 0, Math.min(originalString.length, length));

        return fixedByteArray;
    }

    public static int getNumberOfPages(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("Error: File " + fileName + " does not exist.");
            return -1;
        }
        long fileSize = file.length(); // Get file size in bytes
        return (int) Math.ceil((double) fileSize / Config.PAGE_SIZE);
    }

    public static void generalLoadDataset(BufferManager bf, String filepath, String binFileName,
            createRow createRow) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            int currentPageId = -1;
            boolean pageExists = false;
            int count = 0;

            String dataLine = reader.readLine(); // Skip the first line if needed
            if (dataLine == null) {
                System.out.println("Error: Empty file");
                return;
            }
            while ((dataLine = reader.readLine()) != null) {
                String[] cols = dataLine.split("\t");

                // Validate column lengths for specific files
                if (binFileName.equals("movies.bin") && cols[0].length() != 9) {
                    continue;
                } else if (binFileName.equals("work.bin") && cols[0].length() != 9 && cols[2].length() != 9) {
                    continue;
                }

                // Create a new Row object using the provided RowCreator
                Row row = createRow.createRow(cols);
                // Create a new page if none exists
                if (!pageExists) {
                    Page newPage = bf.createPage(binFileName);
                    currentPageId = newPage.getPid();
                    bf.unpinPage(currentPageId, binFileName);
                    pageExists = true;
                }

                // Get the page and insert the row
                Page p = bf.getPage(currentPageId, binFileName);
                if (p.isFull()) {
                    bf.unpinPage(currentPageId, binFileName);
                    p = bf.createPage(binFileName);
                    currentPageId = p.getPid();
                }

                p.insertRow(row);
                bf.markDirty(currentPageId, binFileName);
                bf.unpinPage(currentPageId, binFileName);
                count++;
                if (count % 20000 == 0) {
                    System.out.println("Inserted " + count + " rows into " + binFileName);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeCSV(ArrayList<Row> output, String fileName) {
        // Takes rows (title and name) and writes to a csv file
        StringBuilder sb = new StringBuilder();
        for (Row row : output) {
            String title = new String(row.title, StandardCharsets.UTF_8).replace('\0', ' ');
            String name = new String(row.name, StandardCharsets.UTF_8).replace('\0', ' ');
            sb.append(title).append(",").append(name).append("\n");
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FunctionalInterface
    public interface createRow {
        Row createRow(String[] columns);
    }

}