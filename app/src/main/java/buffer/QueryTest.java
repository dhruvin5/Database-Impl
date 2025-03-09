package buffer;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import Page.Page;
import Row.Row;

public class QueryTest {
    public static void main(String[] args) {
        //Step 1: Initialize the BufferManager with a buffer size of 5
        BufferManager bufferManager = new BufferManagerImplem(5); // Using 5 pages buffer size
        Random rand = new Random();

        System.out.println("✅ BufferManager initialized with buffer size: 5");

        //Step 2: Create a page and insert initial rows
        System.out.println("\n✅ Creating a new page...");
        Page p = bufferManager.createPage();
        int append_pid = p.getPid();
        System.out.println("✅ Created Page ID: " + append_pid);

        //Insert some initial rows into the page
        for (int i = 0; i < 3; i++) {
            byte[] movieId = ("tt000000" + i).getBytes(StandardCharsets.UTF_8);
            byte[] title = ("Movie " + i).getBytes(StandardCharsets.UTF_8);
            Row row = new Row(movieId, title);
            p.insertRow(row);
            System.out.println("✅ Inserted row " + i + " (Movie ID: " + new String(movieId).trim() + ")");
        }

        //Unpin the page after insertion
        bufferManager.unpinPage(append_pid);
        System.out.println("✅ Initial rows inserted and page unpinned.");

        //Trigger evictions by creating more pages
        System.out.println("\n✅ Creating additional pages to trigger evictions...");
        for (int i = 0; i < 7; i++) { // Increased the number of pages to 7 to force evictions
            Page tempPage = bufferManager.createPage(); // This should evict pages
            bufferManager.unpinPage(tempPage.getPid());
            System.out.println("✅ Created and unpinned temp page " + tempPage.getPid());
        }

        //Fetch evicted page again
        System.out.println("\n✅ Fetching previously evicted page (ID: " + append_pid + ")...");
        long startTime = System.nanoTime();
        p = bufferManager.getPage(append_pid);
        long endTime = System.nanoTime();
        long evictedLoadTime = endTime - startTime;
        
        if (p == null) {
            System.out.println("❌ Page retrieval failed! It was not properly saved.");
            return;
        } else {
            System.out.println("✅ Page successfully retrieved. Load Time: " + evictedLoadTime + " ns");
            // Print contents of the page after it was re-fetched post initial eviction
        System.out.println("\n✅ Printing contents of the refetched page after eviction:");
        for (int i = 0; i < 3; i++) {
            Row row = p.getRow(i);
            if (row == null) {
                System.out.println("❌ Missing row " + i + " after eviction.");
                return;
            } else {
                System.out.println("✅ Row " + i + " exists: " +
                        new String(row.movieId).trim() + " - " + new String(row.title).trim());
            }
        }
        }
        

        //Mark page as dirty and insert more rows
        System.out.println("\n✅ Marking page as dirty and inserting more rows...");
        bufferManager.markDirty(append_pid);
        for (int i = 3; i < 6; i++) {
            byte[] movieId = ("tt000000" + i).getBytes(StandardCharsets.UTF_8);
            byte[] title = ("Movie " + i).getBytes(StandardCharsets.UTF_8);
            Row row = new Row(movieId, title);
            p.insertRow(row);
            System.out.println("✅ Inserted row " + i + " (Movie ID: " + new String(movieId).trim() + ")");
        }

        // Unpin the page after insertion
        bufferManager.unpinPage(append_pid);

        //Validate all rows after dirty pages and eviction
        System.out.println("\n✅ Validating all rows after eviction and dirty pages...");
        startTime = System.nanoTime();
        p = bufferManager.getPage(append_pid);
        endTime = System.nanoTime();
        long pinnedLoadTime = endTime - startTime;
        
        if (p == null) {
            System.out.println("❌ Failed to retrieve page for validation!");
            return;
        }

        System.out.println("✅ Pinned Page Load Time: " + pinnedLoadTime + " ns");
        
        //Compare load times
        System.out.println("\n✅ Comparing load times...");
        if (pinnedLoadTime < evictedLoadTime) {
            System.out.println("✅ Pinned page loaded faster than evicted page.");
        } else {
            System.out.println("❌ Pinned page did not load faster than evicted page.");
        }
        
        //Check all rows in the page
        for (int i = 0; i < 6; i++) {
            Row row = p.getRow(i);
            if (row == null) {
                System.out.println("❌ Missing row " + i + " after eviction.");
                return;
            } else {
                System.out.println("✅ Row " + i + " exists: " +
                        new String(row.movieId).trim() + " - " + new String(row.title).trim());
            }
        }

        //Ensure that dirty pages are flushed to disk
        System.out.println("\n✅ Ensuring dirty pages are flushed to disk...");
        bufferManager.createPage(); // This triggers eviction and writes dirty pages
        System.out.println("✅ Eviction triggered to flush dirty pages.");

        System.out.println("\n Querying test completed successfully!!!");
    }
}
