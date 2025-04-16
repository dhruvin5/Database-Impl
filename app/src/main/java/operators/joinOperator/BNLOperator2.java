package operators.joinOperator;

import operators.tableOperator.PeopleOperator;
import buffer.BufferManager;

import Row.Row;
import Row.joinRow2;
import Page.Page;
import Bplus.Rid;

import java.util.ArrayList;

// Represents the Block Nested Loop Join between Movie-Work and People tables
// This class extends the BNLOperator1 as it has similar functionality
public class BNLOperator2 extends BNLOperator1 {

    @Override
    // Initializes the BNL operator
    public void open(BufferManager bufferManager, String startRange, String endRange, boolean useIndex) {
        initialize(bufferManager, "-2", new BNLOperator1(), new PeopleOperator());
        outer.open(bufferManager, startRange, endRange, useIndex);
        inner.open(bufferManager);
    }

    @Override
    // Returns the rows joined on personId on movie-work and people tables
    public Row next() {
        if (this.outer == null || this.inner == null) {
            return null;
        }

        // Unlike the previous BNL operator, this will be used since personID is a
        // foreign key for the movie-work table, therefore not unique. So we need to
        // pick up from the previous join in the hashmap
        if (this.ridIterator != null && this.ridIterator.hasNext()) {
            return getJoinedRow(this.ridIterator.next());
        }

        this.ridIterator = null;
        this.currInnerRow = null;

        // Continously samples rows from the inner operator until it finds a match in
        // the outer operator
        while (true) {
            this.currInnerRow = inner.next(); // Get the next row from the inner operator
            if (this.currInnerRow == null || this.firstRun) {
                this.firstRun = false;
                if (!fillBlocks("personId")) // fills block using inner operator
                    return null; // Returns null if the inner operator has no more data

                // If the inner operator is null, close then open it again to get the next row
                if (this.currInnerRow == null) {
                    this.inner.close();
                    this.inner.open(bufferManager);
                    this.currInnerRow = inner.next();
                }
            }
            // Check if the outer row has a match in the inner row
            String currPersonId = new String(currInnerRow.personId);
            ArrayList<Rid> ridList = ridMap.get(currPersonId);
            if (ridList != null) {
                // If there is a match, get the ridList and create an iterator for it
                this.ridIterator = ridList.iterator();
                return getJoinedRow(this.ridIterator.next());
            }
        }
    }

    @Override
    // Gets data from the outer and inner tables and joins them on the personId
    protected Row getJoinedRow(Rid rid) {
        Page currentPage = bufferManager.getPage(rid.getPageId(), fileName);
        Row outerRow = currentPage.getRow(rid.getSlotId());
        this.bufferManager.unpinPage(currentPage.getPid(), fileName);
        return new joinRow2(outerRow.movieId, outerRow.title, outerRow.personId, currInnerRow.name);
    }
}
