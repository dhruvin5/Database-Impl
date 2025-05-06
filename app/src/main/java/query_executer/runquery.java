package query_executer;

import java.util.ArrayList;

import Row.Row;
import Utilities.Utilities;
import buffer.BufferManager;
import buffer.BufferManagerImplem;
import operators.Operator;
import operators.projectionOperator.TopProjectionOperator;

public class runquery {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Please use this format to run: runquery <start_range> <end_range> <buffer_size>");
            System.exit(1);
        }
        String startRange = args[0];
        String endRange = args[1];

        int bufferSize;
        try {
            bufferSize = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid buffer_size: " + args[2]);
            System.exit(1);
            return;
        }
        System.err.println("Processing Query Results....");
        // use the user-provided buffer size
        BufferManager bufferManager = new BufferManagerImplem(bufferSize);
        // Create a top projection operator
        Operator topProjectionOperator = new TopProjectionOperator();
        // pass the user's range values
        // FOR TESTING FOR BONUS PART, PLEASE SET useIndex boolean to true
        topProjectionOperator.open(bufferManager, startRange, endRange, false);

        ArrayList<Row> rows = new ArrayList<>();
        Row row = null;
        while ((row = topProjectionOperator.next()) != null) {
            rows.add(row);
        }
        topProjectionOperator.close(); // Close the operator
        String OutDir = "LAB3_OUTPUT"; // output folder
        new java.io.File(OutDir).mkdirs(); // Creating output folder if it doesn't exist
        String outputFileName = OutDir + "/LAB3_OutputFile.csv";// saving result csv in output folder

        // Write the output to a file
        Utilities.writeCSV(rows, outputFileName);

        System.out.println("Query completed: "
                + rows.size() + " Result tuples written to " + outputFileName);
    }

}
