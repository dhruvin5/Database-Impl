package Page;

import Row.Row;

public interface Page {
    /**
     * Fetches a row from the page by its row ID.
     * 
     * @param rowId The ID of the row to retrieve.
     * @return The Row object containing the requested data.
     */
    Row getRow(int rowId);

    /**
     * Inserts a new row into the page.
     * 
     * @param row The Row object containing the data to insert.
     * @return The row ID of the inserted row, or -1 if the page is full
     */
    int insertRow(Row row);

    /**
     * Check if the page is full.
     * 
     * @return true if the page is full, false otherwise
     */
    boolean isFull();

    public byte[] getRows();

    /**
     * Returns the page id
     * 
     * @return page id of this page
     */
    int getPid();

    /**
     * Sets the next pointer of the page.
     * 
     * @param nextPointer The next pointer to set.
     */
    public void setNextPointer(int nextPointer);

    public int getRowCount();

    public boolean getBoolValue();

    public void setRowCount(int count);
    /**
     * Gets the next pointer of the page.
     * 
     * @return The next pointer of the page.
     */
    public int getNextPointer();

}
