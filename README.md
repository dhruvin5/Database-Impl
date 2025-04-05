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
- If you want to see the performance tests:- In `app\src\org\java\example\Caller.java` please uncomment the lines 288, 289, 290

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
      

