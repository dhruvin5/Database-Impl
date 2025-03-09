package Utilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import Page.Page;
import Row.Row;
import buffer.*;

public class Utilities{

    public static void loadDataset(BufferManager bf, String filepath){

        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String dataLine;
            int currentPageId = -1;
            boolean pageExists = false;

            while ((dataLine = reader.readLine()) != null) {
                String[] cols = dataLine.split("\t");

                String movieIdStr = cols[0];
                String titleStr   = cols[2];
                
                
                if (movieIdStr.length() != 9) {
                    continue;
                }
                
                byte[] movieIdBytes = toFixedByteArray(movieIdStr, 9);
                byte[] titleBytes   = toFixedByteArray(titleStr, 30);

                Row row = new Row(movieIdBytes, titleBytes);
                if (!pageExists) {
                    Page newPage = bf.createPage();
                    currentPageId = newPage.getPid();
                    bf.unpinPage(currentPageId);
                    pageExists = true;
                }
                Page p = bf.getPage(currentPageId);

                if (p.isFull()) {
                    bf.unpinPage(currentPageId);
                    p = bf.createPage();
                    currentPageId = p.getPid();
                }

                p.insertRow(row);
                bf.markDirty(currentPageId);
                bf.unpinPage(currentPageId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private static byte[] toFixedByteArray(String input, int length) {
        if (input == null) {
            input = "";
        }
        if (input.length() > length) {
            input = input.substring(0, length);  // truncate
        }
        byte[] src = input.getBytes(StandardCharsets.UTF_8);
        byte[] fixed = new byte[length];
        System.arraycopy(src, 0, fixed, 0, Math.min(src.length, length));
        return fixed;
    }

}