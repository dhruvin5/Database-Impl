# 645-Lab-32966720340112693401883534060222

## UNIX DEVICE OR WSL IS RECOMMENDED TO RUN THE PROGRAM

## Dependencies
- Java JDK-17
- Gradle 8.13
- Git Bash

## Inserting in B+ Tree
- Currently all the rows are being inserted in the indexes one by one which takes a significant amount of time
- Correctness Tests (C1, C2, C3, C4) can be performed on a smaller subset of data

  ## Performance tests
- To Run the performance tests, please to the following changes first:
  - Make sure you have the .bin files before hand. If you load the dataset as well as part of performance test, it would account for extra IOs and give incorrect result
  - Change the file path in app\src\main\java\operators\selectionOperator\MovieSelectionOperator.java (line 88) to wherever you want to dump the csv file. We use this to dump the number of rows that satisfy the search query range. We suggest that there is a folder app\shreya_perf_op\ you update the path to dump the results inside the folder.
  - Similar to above, do the same in app\src\main\java\operators\selectionOperator\WorkSelectionOperator.java (line 45). This will dump the result of the slection query on worked on table. Note that both the directories need to be the same
- Now to find the System calculated IOs:
  - in app\src\main\java\org\example\Caller.java uncomment the main method that calls: performanceTesting.main(args). Comment out any other main methods.
  - in app\src\main\java\org\example\performanceTesting.java change the path (line 18) to the same directory you used in MovieSelectionOperator.java We suggest that there is a folder app\shreya_perf_op\ you update the path to dump the results inside the folder.
  - Then after doing the above two changes simply run the Caller. It'll dump the stats in app\shreya_perf_op\ or whichever folder you would have specified. To see the System generate IO. See the file that ends in _io.csv. Note that the other .csv files are for internal reference. Reading through it wont give the answer needed.
- Now to find the Analytical IO:
  - in app\src\main\java\org\example\Caller.java uncomment the main method that calls: AnalyticalIO.printCosts(). Comment out any other main methods.
  - in app\src\main\java\org\example\AnalyticalIO.java change the path (lines 32, 39) to the same directory you used in MovieSelectionOperator.java We suggest that there is a folder app\shreya_perf_op\ you update the path to dump the results inside the folder.
  - Then in app\src\main\java\org\example\AnalyticalIO.java change line 96 to modify the values of the start query and end query as per requirement.
  - You can also change the buffer size if needed too by changing lines 98, 100 in app\src\main\java\org\example\AnalyticalIO.java
  - Then after doing the above three changes simply run the Caller. It'll print all the relevant stats on the terminal.


## Changes that Need to be Made
- Download the IMDB dataset (unzipped)
- In `app\src\org\java\example\Caller.java` please update the location of the dataset when calling `Utilities.loadDataset`

## How To Run:
- To Build and Run Unit Tests in the Project:
  - Go to the root directory
  - Enter the Command `gradle clean build` in git bash
    - It will create a jar file and run the unit test, in which the test results will be displayed.
  - To run the executable, go to `build\libs` and use the command `java -jar app.jar` which will run the end-to-end script
    - Please note running the jar file may take time as it will load the IMDB dataset.
- To Just Run Unit Test:
  - Go to the root directory
  - Use the command `gradle clean test` in git bash
    - It will show the results of the unit test
    - A report will be also generated and visible in `index.html` in `app\build\report\tests\test`
- Note: When running the unit test in the CLI, results that have `standard_out` are not unit test results. They are print statements in the buffermanager or page class that are invoked in the unit test. 
      

