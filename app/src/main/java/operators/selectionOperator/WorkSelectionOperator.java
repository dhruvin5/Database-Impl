package operators.selectionOperator;

import buffer.BufferManager;
import operators.Operator;
import operators.tableOperator.WorkOperator;

import java.io.FileWriter;
import java.io.PrintWriter;

import Row.Row;

// Pull data from the work dataset and filter by category = director
public class WorkSelectionOperator implements Operator {
    private Operator workOperator; // Operator Pull data from the work dataset
    private int matchCount = 0;    // Counter for rows where category = director

    public void open(BufferManager bufferManager, String startRange, String endRange, boolean useIndex) {
        open(bufferManager);
    }

    // initializes the workOperator to pull data from the work dataset
    public void open(BufferManager bufferManager) {
        workOperator = new WorkOperator();
        workOperator.open(bufferManager);
    }

    // Operator that returns a row if it is a director catagory
    public Row next() {
        if (workOperator == null) {
            return null;
        }
        Row row;
        while ((row = workOperator.next()) != null) {
            String workName = new String(row.category).trim();
            if (workName.equals("director")) {
                matchCount++;  // Count matching row
                return row;
            }
        }
        return null; // No more rows in the specified range
    }

    public void close() {
        if (workOperator != null) {
            // dumps result for the matching count. For correctness testing, the next line can be commented out and is optional. But we need it for perf testing
            dumpMatchStats("C:\\Users\\HP\\Desktop\\ms\\645\\lab1\\645-Lab-32966720340112693401883534060222\\app\\shreya_perf_op");
            workOperator.close();
            workOperator = null;
        }
    }

    // Dumps analytical match data to a CSV file
    public void dumpMatchStats(String outputDir) {
        String filePath = outputDir + "\\work_director_analytical_match.csv";
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
            pw.println("category,matches");
            pw.printf("director,%d\n", matchCount);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // return the count of matching rows
    public int getMatchCount() {
        return matchCount;
    }

}
