import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import page.Page;
import page.PageImpl;
import buffer.*;

public class Utilities{
    // Loads the buffer manager with the imdb dataset
    private static final String DISK_FILE = "imdb.bin";
    private static final int PAGE_SIZE = 4096;

    public static void loadDataset(BufferManager bf, String filepath){

        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line;
            int currentPageId = -1;
            boolean pageExists = false;

            while ((line = br.readLine()) != null) {
                String[] cols = line.split("\t");
                // Safety check
                if (cols.length < 3) continue;

                String movieIdStr = cols[0];
                String titleStr   = cols[2];

                byte[] movieIdBytes = toFixedByteArray(movieIdStr, 9);
                byte[] titleBytes   = toFixedByteArray(titleStr, 30);

                Row row = new Row(movieIdBytes, titleBytes);

                // If we haven't created or found a "current page" yet, do so
                if (!pageExists) {
                    // The last page might exist if the file isn't empty.
                    // For a simple approach, just always create a brand-new page:
                    Page newPage = bf.createPage();  // pinned automatically
                    currentPageId = newPage.getPid();
                    // Unpin so we can re-fetch it normally
                    bf.unpinPage(currentPageId);
                    pageExists = true;
                }

                // Now fetch the current page
                Page p = bf.getPage(currentPageId); // pinned
                if (p.isFull()) {
                    // It's full, so create a new one
                    bf.unpinPage(currentPageId);

                    Page nextPage = bf.createPage();
                    currentPageId = nextPage.getPid();
                    nextPage.insertRow(row);
                    bf.markDirty(currentPageId);
                    bf.unpinPage(currentPageId);
                } else {
                    // Insert into existing page
                    p.insertRow(row);
                    bf.markDirty(currentPageId);
                    bf.unpinPage(currentPageId);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public static Page getPageFromDisk(int pageId){
        try (RandomAccessFile raf = new RandomAccessFile(DB_FILE_PATH, "r")) {
            long offset = (long) pageId * PAGE_SIZE;
            if (offset >= raf.length()) {
                // The page doesn't exist on disk (beyond EOF).
                return null;
            }
            raf.seek(offset);
            byte[] buffer = new byte[PAGE_SIZE];
            raf.readFully(buffer);

            Page page = new PageImpl(pageId, buffer);
            return page;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    public static void writeToDisk(Page page) {

    }

    public static boolean isPageOnDisk(int pageId) {
    }

}