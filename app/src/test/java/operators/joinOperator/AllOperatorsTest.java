package operators.joinOperator;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import Page.Page;
import Row.Row;
import Row.joinRow1;
import Row.joinRow2;
import buffer.BufferManager;
import operators.Operator;
import operators.selectionOperator.MovieSelectionOperator;
import operators.selectionOperator.WorkSelectionOperator;

public class AllOperatorsTest {

    static class FakePage implements Page {
        private final int pid;
        private final List<Row> rows = new ArrayList<>();

        FakePage(int pid) { this.pid = pid; }
        @Override public int getPid() { return pid; }
        @Override public boolean isFull() { return false; }
        @Override public int getRowCount() { return rows.size(); }
        @Override public Row getRow(int slot) { return rows.get(slot); }
        @Override public int insertRow(Row row) { rows.add(row); return rows.size() - 1; }
        @Override public void setRowCount(int count) { if (count < rows.size()) rows.subList(count, rows.size()).clear(); }
        @Override public byte[] getRows() { return new byte[0]; }
        @Override public void setNextPointer(int next) {}
        @Override public int getNextPointer() { return -1; }
        @Override public boolean getBoolValue() { return false; }
    }

    static class FakeBufferManager extends BufferManager {
        private final int capacity;
        private int nextPid = 0;
        private final Map<Integer, FakePage> pages = new HashMap<>();

        FakeBufferManager(int bufSize) { super(bufSize); this.capacity = bufSize; }
        @Override public int getBufferCapacity() { return capacity; }
        @Override public Page createPage(String fileName) { FakePage p = new FakePage(nextPid++); pages.put(p.getPid(), p); return p; }
        @Override public Page createIndexPage(String fileName, boolean isLeaf) { return createPage(fileName); }
        @Override public void markDirty(int pid, String fn) {}
        @Override public void markUndirty(int pid, String fn) {}
        @Override public void unpinPage(int pid, String fn) {}
        @Override public Page getPage(int pid, String fn) { return pages.get(pid); }
        @Override public void force() {}
        @Override public void clearCache() {}
        @Override public void deleteFile(String fn) {}
    }

    static class FakeOperator implements Operator {
        private final List<Row> rows;
        private int idx = 0;
        FakeOperator(List<Row> rows) { this.rows = rows; }
        @Override public void open(BufferManager bm) {}
        @Override public void open(BufferManager bm, String s, String e, boolean u) {}
        @Override public Row next() { return idx < rows.size() ? rows.get(idx++) : null; }
        @Override public void close() {}
    }

    static class FakeMovieRow extends Row {
        FakeMovieRow(String id, String title) {
            this.movieId = id.getBytes();
            this.title   = title.getBytes();
        }
    }
    static class FakeWorkRow extends Row {
        FakeWorkRow(String movieId, String personId, String category) {
            this.movieId  = movieId.getBytes();
            this.personId = personId.getBytes();
            this.category = category.getBytes();
        }
    }
    static class FakePersonRow extends Row {
        FakePersonRow(String personId, String name) {
            this.personId = personId.getBytes();
            this.name     = name.getBytes();
        }
    }
    static class TestableBNLOperator1 extends BNLOperator1 {
        public void init(BufferManager bm, String fn, Operator out, Operator in) {
            super.initialize(bm, fn, out, in);
        }
    }
    static class TestableBNLOperator2 extends BNLOperator2 {
        public void init(BufferManager bm, String fn, Operator out, Operator in) {
            super.initialize(bm, fn, out, in);
        }
    }

    @Test
    public void testBNLOperator1_singleMatch() {
        FakeBufferManager fbm = new FakeBufferManager(10);
        FakeMovieRow m = new FakeMovieRow("000000001", "Movie Name A");
        FakeWorkRow w  = new FakeWorkRow("000000001", "Person A", "director");
        TestableBNLOperator1 op = new TestableBNLOperator1();
        op.init(fbm, "random", new FakeOperator(List.of(m)), new FakeOperator(List.of(w)));

        Row result = op.next();
        assertNotNull(result);
        assertEquals("000000001", new String(result.movieId));
        assertEquals("Movie Name A", new String(result.title));
        assertEquals("Person A", new String(result.personId));
        assertNull(op.next());
    }

    @Test
    public void testBNLOperator1_multipleMatches() {
        FakeBufferManager fbm = new FakeBufferManager(12);
        FakeMovieRow m = new FakeMovieRow("000000002", "Movie Name B");
        FakeWorkRow w1 = new FakeWorkRow("000000002", "Person B", "director");
        FakeWorkRow w2 = new FakeWorkRow("000000002", "Person C", "director");
        TestableBNLOperator1 op = new TestableBNLOperator1();
        op.init(fbm, "random", new FakeOperator(List.of(m)), new FakeOperator(List.of(w1, w2)));

        Row r1 = op.next();
        Row r2 = op.next();
        assertNotNull(r1); assertNotNull(r2);
        assertEquals("Person B", new String(r1.personId));
        assertEquals("Person C", new String(r2.personId));
        assertNull(op.next());
    }

    @Test
    public void testBNLOperator1_multipleRowMatches() {
        FakeBufferManager fbm = new FakeBufferManager(12);
        FakeMovieRow m = new FakeMovieRow("000000002", "Movie Name B");
        FakeWorkRow w1 = new FakeWorkRow("000000002", "Person B", "director");
        FakeWorkRow w2 = new FakeWorkRow("000000002", "Person C", "director");
        FakeMovieRow m1 = new FakeMovieRow("000000009", "Movie Name H");
        FakeWorkRow w3 = new FakeWorkRow("000000009", "Person H", "director");
        TestableBNLOperator1 op = new TestableBNLOperator1();
        op.init(fbm, "random", new FakeOperator(List.of(m,m1)), new FakeOperator(List.of(w1, w2,w3)));

        Row r1 = op.next();
        Row r2 = op.next();
        Row r3 = op.next();

        assertNotNull(r1); assertNotNull(r2);
        assertNotNull(r3);
        assertEquals("Person B", new String(r1.personId));
        assertEquals("Person C", new String(r2.personId));
        assertEquals("Person H", new String(r3.personId));
        assertNull(op.next());
    }

    @Test
    public void testBNLOperator1_noMatch() {
        FakeBufferManager fbm = new FakeBufferManager(8);
        FakeMovieRow m = new FakeMovieRow("000000003", "Movie Name D");
        FakeWorkRow w  = new FakeWorkRow("000000004", "Person D", "director");
        TestableBNLOperator1 op = new TestableBNLOperator1();
        op.init(fbm, "random", new FakeOperator(List.of(m)), new FakeOperator(List.of(w)));
        assertNull(op.next());
    }

    @Test
    public void testBNLOperator2_joinTwoLevels() {
        FakeBufferManager fbm = new FakeBufferManager(10);
        joinRow1 jw = new joinRow1("000000005".getBytes(), "Movie Name E".getBytes(), "Person E".getBytes());
        FakeOperator outer = new FakeOperator(List.of(jw));
        FakeOperator inner = new FakeOperator(List.of(new FakePersonRow("Person E", "Dhruvin")));
        TestableBNLOperator2 op2 = new TestableBNLOperator2();
        op2.init(fbm, "f2", outer, inner);

        Row r = op2.next();
        assertNotNull(r);
        assertTrue(r instanceof joinRow2);
        joinRow2 jr2 = (joinRow2) r;
        assertEquals("000000005", new String(jr2.movieId));
        assertEquals("Movie Name E", new String(jr2.title));
        assertEquals("Person E", new String(jr2.personId));
        assertEquals("Dhruvin", new String(jr2.name));
        assertNull(op2.next());
    }

    @Test
    public void testMovieSelectionOperator_range() {
        FakeBufferManager fbm = new FakeBufferManager(5);
        FakePage page = (FakePage)fbm.createPage("test-movies.bin");
        page.insertRow(new FakeMovieRow("","Alpha"));
        page.insertRow(new FakeMovieRow("","Beta-1"));
        page.insertRow(new FakeMovieRow("","Beta-2"));
        page.insertRow(new FakeMovieRow("","Beta-3"));
        page.insertRow(new FakeMovieRow("","Gamma"));
        MovieSelectionOperator selectionMovie = new MovieSelectionOperator();
        selectionMovie.open(fbm, "Beta", "Gamma", false);

        List<String> got = new ArrayList<>();
        Row r;
        while ((r = selectionMovie.next()) != null) {
            got.add(new String(r.title));
        }
        selectionMovie.close();

        assertEquals(List.of("Beta-1", "Beta-2","Beta-3","Gamma"), got);
    }

    @Test
    public void testWorkSelectionOperator_filtersDirector() {
        FakeBufferManager fbm = new FakeBufferManager(5);
        FakePage page = (FakePage)fbm.createPage("test-work.bin");
        FakeWorkRow d = new FakeWorkRow("000000006", "Person F","director");
        FakeWorkRow a = new FakeWorkRow("000000007", "Person G","actor");
        page.insertRow(d);
        page.insertRow(a);

        WorkSelectionOperator selectionWork = new WorkSelectionOperator();
        selectionWork.open(fbm);
        Row r = selectionWork.next();
        selectionWork.close();

        assertNotNull(r);
        assertEquals("director", new String(r.category));
        assertNull(selectionWork.next());
    }
}
