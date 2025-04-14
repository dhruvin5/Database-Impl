package operators.joinOperator;

import operators.Operator;
import operators.tableOperator.PeopleOperator;
import buffer.BufferManager;

import Row.Row;
import Row.joinRow2;
import Page.Page;
import Bplus.Rid;

import java.util.ArrayList;

public class BNLOperator2 extends BNLOperator1 {

    @Override
    public void open(BufferManager bufferManager, String startRange, String endRange, boolean useIndex) {
        initialize(bufferManager, "-2", new BNLOperator1(), new PeopleOperator());
        outer.open(bufferManager, startRange, endRange, useIndex);
        inner.open(bufferManager);
    }

    @Override
    public Row next() {
        if (this.outer == null || this.inner == null) {
            return null;
        }

        if (this.ridIterator != null && this.ridIterator.hasNext()) {
            return getJoinedRow(this.ridIterator.next());
        }

        this.ridIterator = null;
        this.currInnerRow = null;

        while (true) {
            this.currInnerRow = inner.next();
            if (this.currInnerRow == null || this.firstRun) {
                this.firstRun = false;
                if (!fillBlocks("personId"))
                    return null;
                if (this.currInnerRow == null) {
                    this.inner.close();
                    this.inner.open(bufferManager);
                    this.currInnerRow = inner.next();
                }

            }
            String currPersonId = new String(currInnerRow.personId);
            ArrayList<Rid> ridList = ridMap.get(currPersonId);
            if (ridList != null) {
                this.ridIterator = ridList.iterator();
                return getJoinedRow(this.ridIterator.next());
            }
        }
    }

    @Override
    protected Row getJoinedRow(Rid rid) {
        Page currentPage = bufferManager.getPage(rid.getPageId(), fileName);
        Row outerRow = currentPage.getRow(rid.getSlotId());
        this.bufferManager.unpinPage(currentPage.getPid(), fileName);
        return new joinRow2(outerRow.movieId, outerRow.title, outerRow.personId, currInnerRow.name);
    }
}
