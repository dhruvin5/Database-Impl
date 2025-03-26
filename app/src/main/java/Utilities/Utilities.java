package Utilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import Page.Page;
import Row.Row;
import buffer.*;

public class Utilities {

    // loads the dataset into a disk file
    // takes the bufferManagaer and the filePath as the input.
    public static void loadDataset(BufferManager bf, String filepath) {

        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {

            // initially there is no page
            int currentPageId = -1;
            boolean pageExists = false;

            String dataLine;

            while ((dataLine = reader.readLine()) != null) {

                // split by tab
                String[] cols = dataLine.split("\t");

                // get the idColumn and the title Column
                String idStr = cols[0];
                String titleStr = cols[2];

                // discard if the movieId is not of size 9
                if (idStr.length() != 9) {
                    continue;
                }

                // convert the string to fixed size byte arrays
                byte[] idBytes = toFixedByteArray(idStr, 9);
                byte[] titleBytes = toFixedByteArray(titleStr, 30);

                // create a new Row Object
                Row row = new Row(idBytes, titleBytes);

                // if no page currently, create a page first
                if (!pageExists) {
                    Page newPage = bf.createPage();
                    currentPageId = newPage.getPid();
                    bf.unpinPage(currentPageId);
                    pageExists = true;
                }

                // get the page after creating using the currentPageId
                Page p = bf.getPage(currentPageId);

                // check if the page is already full
                // if full unpin it and tell the buffer to create a new page
                if (p.isFull()) {
                    bf.unpinPage(currentPageId);
                    p = bf.createPage();
                    currentPageId = p.getPid();
                }

                // insert the rows in the page
                p.insertRow(row);
                bf.markDirty(currentPageId);
                bf.unpinPage(currentPageId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

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

}