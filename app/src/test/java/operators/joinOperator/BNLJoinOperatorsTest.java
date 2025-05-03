package operators.joinOperator;

import buffer.BufferManager;
import operators.Operator;
import Row.Row;
import Row.joinRow1;
import Row.joinRow2;
import Page.Page;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

public class BNLJoinOperatorsTest {
    // --- Fake Page matching Page interface ---
    static class FakePage implements Page {
        private final int pid;
        private final List<Row> rows = new ArrayList<>();

        FakePage(int pid) {
            this.pid = pid;
        }

        @Override
        public int getPid() {
            return pid;
        }

        @Override
        public boolean isFull() {
            return false;
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public Row getRow(int slot) {
            return rows.get(slot);
        }

        @Override
        public int insertRow(Row row) {
            rows.add(row);
            return rows.size() - 1;
        }

        @Override
        public void setRowCount(int count) {
            if (count < rows.size()) {
                rows.subList(count, rows.size()).clear();
            }
        }

        @Override
        public byte[] getRows() {
            return new byte[0];
        }

        @Override
        public void setNextPointer(int next) {
            // no-op for tests
        }

        @Override
        public int getNextPointer() {
            return -1;
        }

        @Override
        public boolean getBoolValue() {
            return false;
        }
    }

    // --- Fake BufferManager extending abstract BufferManager ---
    static class FakeBufferManager extends BufferManager {
        private final int capacity;
        private int nextPid = 0;
        private final Map<Integer, FakePage> pages = new HashMap<>();

        FakeBufferManager(int bufferSize) {
            super(bufferSize);
            this.capacity = bufferSize;
        }

        @Override
        public int getBufferCapacity() {
            return capacity;
        }

        @Override
        public Page createPage(String fileName) {
            FakePage p = new FakePage(nextPid++);
            pages.put(p.getPid(), p);
            return p;
        }

        @Override
        public Page createIndexPage(String fileName, boolean isLeaf) {
            return createPage(fileName);
        }

        @Override
        public void markDirty(int pageId, String fileName) {}

        @Override
        public void markUndirty(int pageId, String fileName) {}

        @Override
        public void unpinPage(int pageId, String fileName) {}

        @Override
        public Page getPage(int pageId, String fileName) {
            return pages.get(pageId);
        }

        @Override
        public void force() {
            // no-op for tests
        }

        @Override
        public void clearCache() {
            // no-op for tests
        }

        @Override
        public void deleteFile(String FILE_NAME) {

        }
    }

    // --- Stub Operator and Row implementations ---
    static class StubOperator implements Operator {
        private final List<Row> rows;
        private int idx = 0;

        StubOperator(List<Row> rows) {
            this.rows = rows;
        }

        @Override
        public void open(BufferManager bm) {}

        @Override
        public void open(BufferManager bm, String s, String e, boolean u) {}

        @Override
        public Row next() {
            return idx < rows.size() ? rows.get(idx++) : null;
        }

        @Override
        public void close() {}
    }

    static class StubMovieRow extends Row {
        StubMovieRow(String id, String title) {
            this.movieId = id.getBytes();
            this.title = title.getBytes();
        }
    }

    static class StubWorkRow extends Row {
        StubWorkRow(String movieId, String personId) {
            this.movieId = movieId.getBytes();
            this.personId = personId.getBytes();
        }
    }

    static class StubPersonRow extends Row {
        StubPersonRow(String personId, String name) {
            this.personId = personId.getBytes();
            this.name = name.getBytes();
        }
    }

    // --- Expose initialize for testing ---
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

    // --- Tests for BNLOperator1 ---
    @Test
    public void testBNLOperator1_singleMatch() {
        FakeBufferManager fbm = new FakeBufferManager(10);
        StubMovieRow m = new StubMovieRow("001", "MovieA");
        StubWorkRow w = new StubWorkRow("001", "P1");
        TestableBNLOperator1 op = new TestableBNLOperator1();
        op.init(fbm, "f", new StubOperator(List.of(m)), new StubOperator(List.of(w)));

        Row result = op.next();
        assertNotNull(result);
        assertEquals("001", new String(result.movieId));
        assertEquals("MovieA", new String(result.title));
        assertEquals("P1", new String(result.personId));
        assertNull(op.next());
    }

    @Test
    public void testBNLOperator1_multipleMatches() {
        FakeBufferManager fbm = new FakeBufferManager(12);
        StubMovieRow m = new StubMovieRow("X", "M");
        StubWorkRow w1 = new StubWorkRow("X", "A");
        StubWorkRow w2 = new StubWorkRow("X", "B");
        TestableBNLOperator1 op = new TestableBNLOperator1();
        op.init(fbm, "f", new StubOperator(List.of(m)), new StubOperator(List.of(w1, w2)));

        Row r1 = op.next();
        Row r2 = op.next();
        assertNotNull(r1);
        assertNotNull(r2);
        assertEquals("A", new String(r1.personId));
        assertEquals("B", new String(r2.personId));
        assertNull(op.next());
    }

    @Test
    public void testBNLOperator1_noMatch() {
        FakeBufferManager fbm = new FakeBufferManager(8);
        StubMovieRow m = new StubMovieRow("1", "T");
        StubWorkRow w = new StubWorkRow("2", "P");
        TestableBNLOperator1 op = new TestableBNLOperator1();
        op.init(fbm, "f", new StubOperator(List.of(m)), new StubOperator(List.of(w)));

        assertNull(op.next());
    }

    // --- Test for BNLOperator2 ---
    @Test
    public void testBNLOperator2_joinTwoLevels() {
        FakeBufferManager fbm = new FakeBufferManager(10);
        joinRow1 jw = new joinRow1("M1".getBytes(), "T1".getBytes(), "P1".getBytes());
        StubOperator outer = new StubOperator(List.of(jw));
        StubOperator inner = new StubOperator(List.of(new StubPersonRow("P1", "Alice")));
        TestableBNLOperator2 op2 = new TestableBNLOperator2();
        op2.init(fbm, "f2", outer, inner);

        Row r = op2.next();
        assertNotNull(r);
        assertTrue(r instanceof joinRow2);
        joinRow2 jr2 = (joinRow2) r;
        assertEquals("M1", new String(jr2.movieId));
        assertEquals("T1", new String(jr2.title));
        assertEquals("P1", new String(jr2.personId));
        assertEquals("Alice", new String(jr2.name));
        assertNull(op2.next());
    }
}