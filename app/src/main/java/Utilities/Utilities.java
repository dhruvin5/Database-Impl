package Utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import Page.Page;
import Row.Row;
import Row.movieRow;
import Row.peopleRow;
import Row.workRow;
import buffer.BufferManager;
import configs.Config;

public class Utilities {

    public static void loadDataset(BufferManager bf, String filepath) {
        generalLoadDataset(bf, filepath, "movies.bin",
            columns -> {
                byte[] movieId = toFixedByteArray(columns[0], 9);
                byte[] title   = toFixedByteArray(columns[2], 30);
                return new movieRow(movieId, title);
            });
    }

    public static void loadWorkDataset(BufferManager bf, String filepath) {
        generalLoadDataset(bf, filepath, "work.bin",
            columns -> {
                byte[] movieId  = toFixedByteArray(columns[0], 9);
                byte[] personId = toFixedByteArray(columns[2], 10);
                byte[] category = toFixedByteArray(columns[3], 20);
                return new workRow(movieId, personId, category);
            });
    }

    public static void loadPeopleDataset(BufferManager bf, String filepath) {
        generalLoadDataset(bf, filepath, "people.bin",
            columns -> {
                byte[] personId = toFixedByteArray(columns[0], 10);
                byte[] name     = toFixedByteArray(columns[1], 105);
                return new peopleRow(personId, name);
            });
    }

    public static byte[] toFixedByteArray(String inputString, int length) {
        if (inputString == null) inputString = "";
        if (inputString.length() > length) {
            inputString = inputString.substring(0, length);
        }
        byte[] original = inputString.getBytes(StandardCharsets.UTF_8);
        byte[] fixed    = new byte[length];
        System.arraycopy(original, 0, fixed, 0, Math.min(original.length, length));
        return fixed;
    }

    public static int getNumberOfPages(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("Error: File " + fileName + " does not exist.");
            return -1;
        }
        long fileSize = file.length();
        return (int) Math.ceil((double) fileSize / Config.PAGE_SIZE);
    }

    public static void generalLoadDataset(BufferManager bf,
                                          String filepath,
                                          String binFileName,
                                          createRow rowCreator) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String header = reader.readLine();
            if (header == null) {
                System.out.println("Error: Empty file");
                return;
            }

            int currentPageId = -1;
            boolean pageExists = false;
            String dataLine;

            while ((dataLine = reader.readLine()) != null) {
                String[] cols = dataLine.split("\t", -1);

                // Checking if file name is "movies.bin" or "work.bin"
                if (binFileName.equals("movies.bin") || binFileName.equals("work.bin")) {
                    boolean Invalid_MovieId = false;
                    boolean Invalid_Title   = false;

                    // Flagging tuple with invalid MovieID for skipping (size not equal to 9)
                    if (cols.length < 1 || cols[0].length() != 9) {
                        Invalid_MovieId = true;
                    }

                    // If file name="movies.bin", check if title is valid
                    if (binFileName.equals("movies.bin")) {
                        if (HasInvalidCharacter(cols[2])) {
                            // Flagging tuple with invalid title (with invalid chars) for skipping
                            Invalid_Title = true;
                        }
                    }

                    if (Invalid_MovieId || Invalid_Title) {
                        //Skipping tuples with invalid MovieId or title
                        continue;
                    }
                }

                // Create the row and write it into pages
                Row row = rowCreator.createRow(cols);

                if (!pageExists) {
                    Page newPage = bf.createPage(binFileName);
                    currentPageId = newPage.getPid();
                    bf.unpinPage(currentPageId, binFileName);
                    pageExists = true;
                }

                Page p = bf.getPage(currentPageId, binFileName);
                if (p.isFull()) {
                    bf.unpinPage(currentPageId, binFileName);
                    p = bf.createPage(binFileName);
                    currentPageId = p.getPid();
                }
                p.insertRow(row);
                bf.markDirty(currentPageId, binFileName);
                bf.unpinPage(currentPageId, binFileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // method to flag titles with invalid characters: comma, quote, Non ASCII characters
    private static boolean HasInvalidCharacter(String str) {
        for (char c : str.toCharArray()) {
            if (c == ',' || c == '"' || c == '\'' || c > 127) {
                return true;
            }
        }
        return false;
    }

    public static void writeCSV(ArrayList<Row> output, String fileName) {
        StringBuilder sb = new StringBuilder();
        for (Row row : output) {
            String title = new String(row.title, StandardCharsets.UTF_8)
                               .replace('\0',' ').trim();
            String name  = new String(row.name,  StandardCharsets.UTF_8)
                               .replace('\0',' ').trim();
            title = String.format("%-30s", title);
            name  = String.format("%-105s", name);
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
