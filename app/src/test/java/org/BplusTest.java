package org;



import Bplus.BplusTreeImplem;
import Bplus.Rid;
import buffer.BufferManager;
import buffer.BufferManagerImplem;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import Page.PageImpl;
import Row.Row;
import Utilities.Utilities;


class BplusTest {

    public BplusTreeImplem<String> titleIndex;
    public BplusTreeImplem<String> movieIdIndex;
    public BufferManager bufferManager;

    @BeforeEach
    void setUp() {
        try{
            bufferManager = new BufferManagerImplem(7);
            //Utilities.loadDataset(bufferManager, "C:\\Users\\HP\\Desktop\\ms\\645\\lab1\\645-Lab-32966720340112693401883534060222\\app\\src\\title.basics.tsv");
            // bufferManager.force();
            titleIndex = new BplusTreeImplem<>("title_index.bin", bufferManager);
            movieIdIndex = new BplusTreeImplem<>("movie_Id_index.bin", bufferManager);
            bufferManager.force();
        } catch (Exception e){
            e.printStackTrace();
        }
    }


    @Test
    void testInsertAndSearchTitle() {
        try{
            String movieTitle = "Inception";
            Rid movieRid = new Rid(1, 0);
            titleIndex.insert(movieTitle, movieRid);
            Iterator<Rid> result = titleIndex.search(movieTitle);
            assertEquals(movieRid, result.next(), "The Rid should match the inserted one.");
            //assertEquals(movieRid, result.next(), "The Rid should match the inserted one.");
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}