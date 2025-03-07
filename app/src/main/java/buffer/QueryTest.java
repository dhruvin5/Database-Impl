package buffer;


import java.nio.charset.StandardCharsets;
import java.util.Random;

import Page.Page;
import Row.Row;

public class QueryTest {
    public static void main(String[] args) {
        BufferManager bufferManager = new BufferManagerImplem(3); // Small buffer for forced evictions
        Random rand = new Random();

        // Step 1: Create a page and insert rows
        System.out.println("✅ Creating a new page...");
        Page p = bufferManager.createPage();
        int append_pid = p.getPid();
        System.out.println("✅ Created Page ID: " + append_pid);

        // Insert some initial rows
        for (int i = 0; i < 3; i++) {
            byte[] movieId = ("tt000000" + i).getBytes(StandardCharsets.UTF_8);
            byte[] title = ("Movie " + i).getBytes(StandardCharsets.UTF_8);
            Row row = new Row(movieId, title);
            p.insertRow(row);
        }
        bufferManager.unpinPage(append_pid);
        System.out.println("✅ Initial rows inserted and page unpinned.");

        // Step 2: Trigger evictions by creating more pages
        for (int i = 0; i < 5; i++) {
            Page tempPage = bufferManager.createPage(); // This should evict pages
            bufferManager.unpinPage(tempPage.getPid());
        }

        // Step 3: Fetch evicted page again
        p = bufferManager.getPage(append_pid);
        if (p == null) {
            System.out.println("❌ Page retrieval failed! It was not properly saved.");
            return;
        } else {
            System.out.println("✅ Page successfully retrieved.");
        }

        // Step 4: Mark as dirty & insert more rows
        bufferManager.markDirty(append_pid);
        for (int i = 3; i < 6; i++) {
            byte[] movieId = ("tt000000" + i).getBytes(StandardCharsets.UTF_8);
            byte[] title = ("Movie " + i).getBytes(StandardCharsets.UTF_8);
            Row row = new Row(movieId, title);
            p.insertRow(row);
        }
        bufferManager.unpinPage(append_pid);

        // Step 5: Validate all rows
        p = bufferManager.getPage(append_pid);
        if (p == null) {
            System.out.println("❌ Failed to retrieve page for validation!");
            return;
        }

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

        System.out.println("🎉 Querying test completed successfully!");
    }
}
