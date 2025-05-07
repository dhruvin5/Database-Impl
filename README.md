# 645-Lab-32966720340112693401883534060222

## UNIX DEVICE OR WSL IS RECOMMENDED TO RUN THE PROGRAM

## Dependencies
- Java JDK-17
- Gradle 8.13
- Git Bash


## Query Executor interface
- This is the main interface to interact with the project/query plan.
- All the instructions to use the "run_query" command and the "preprocess" command is in app/src/main/java/query_executer/running_instructions file
  
## Unit tests
- For all the labs the unit tests are in the tests folder in app/src.
- The tests for the LAB3 include checking for one row match, multiple row matches and selctivity operators testing.
- To run the unit tests - gradle clean test
- Please run the test before using application, and delete the associate bin files created from the test.

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
- **To use the performance test, follow the instructions in the section `How To Run Performance test` to build the project and run it. Please note, you can technically use the `Query Executor interface` without building the application.**

## How To Run Performance test:
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

## Correctness testing
- Load data in postgres:
  - Perform base file cleanup using this command in terminal- NOTE: update paths for imdb files and cleaned files in base_file_cleaner.java:
    - ./run_base_file_cleanup
  - load data in postgres by running this command in terminal- NOTE: update cleaned file paths and output result file path(USE RELATIVE PATH HERE) as needed and USE SAME RANGE:
    - ./Load_psql 'Alaa' 'Alab'
  - This will connect to psql, populate the schemas and write query result from psql to csv file
- Paste the following command in terminal to perform correctness test. NOTE: update paths of files in CT.java (USE RELATIVE PATHS FOR BOTH FILES):
    - ./run_correctness_test   

## BONUS PART
- We have also implemented the index access method using the B+ tree.
- To run using the index operator, in caller make the
  - topProjectionOperator.open(bufferManager, "Alaa", "Alab", true) or if you are using the run query command
- To run using the index operator, using the run_query command
  - topProjectionOperator.open(bufferManager, "Alaa", "Alab", true) in app/src/main/java/query_executer/runquery.java
- Setting the boolean to false will default the plan to use a file scan on the movies table



## Exact steps to run the project:

NOTE: All steps to run are also mentioned in design doc(with screenshots code/output), and readme

To use the application using commandline:

Step 1) Navigate to the path of query_executer folder app/src/main/java/query_executer

Step 2) Give execution permissions to all these scripts in the terminal:
        2.1) chmod +x preprocess
        2.2) chmod +x run_query
        2.3) chmod +x run_base_file_cleanup
        2.4) chmod +x Load_psql
        2.5) chmod +x run_correctness_test

Step 3) Run preprocess script to prepare all base Table binary files(Movies, Work, People) for the lab3 code in the terminal. Before running this, please update the file paths for the raw/unprocessed base tsv files for the tables in preprocess.java
        ./preprocess

Step 4) After processing is successful, you will see the bin files created in the dir
        and will see this in terminal output:
        "Preprocessing Completed Successfully! "

Step 5) Update the Query output file name in runquery.java before executing your query and save the JAVA file (optional- but the if the file with same name already exists, output csv file will be overwritten/appended every time you execute a new query.) 


NOTE: To use the index operator (the bonus), please set the boolean to true in `topProjectionOperator` in runquery.java, otherwise it will not use the index operator. To run with simple file scan, set useIndex to false.


Step 6) To run the query, invoke the runquery script in this format in Terminal (update start range, end range, and buffer size as needed):
        ./run_query 'Alaa' 'Alab' 6

Step 7) Perform Correctness Test:

        7.1) Load data in postgres:
             Note: We have sample PostgreSQL outputs in PSQL_Output folders for the ranges (Alaa, Alab), (Ba, Bm), and (Caa, Cab). If you want to use those.
             7.1.1) Perform base file cleanup using this command in terminal- NOTE: update paths for imdb files and cleaned files in base_file_cleaner.java:
             ./run_base_file_cleanup

Note: the cleaned files will be generated in the same directory as your base unprocessed imdb tsv files. Update base path here in the base_file_cleaner.java(path: app/src/main/java/base_file_cleaner.java)
You can also choose to compile and run base_file_cleaner.java separately.


            7.2.2) load data in postgres by running this command in terminal- NOTE: update cleaned file paths and output result file path(try to use relative paths here) as needed and USE SAME RANGE you used to run LAB3 code:
             ./Load_psql 'Alaa' 'Alab'
                This will connect to psql, populate the schemas and write query result from psql to csv file
                
                In case this script does not work your end, Please follow these instructions to create schema in Postgres and load cleaned base files in them. Update paths and ranges as needed. 
                1. Create database Lab3_645;    //creating database
                2. \c lab3_645   //connecting to database
                3. CREATE TABLE movies   (movieId CHAR(9),  title CHAR(30));     //creating table schemas
                4. CREATE TABLE workedon (movieId CHAR(9), personid CHAR(10), category CHAR(20));
                5. CREATE TABLE people   (personid CHAR(10), name CHAR(105));
                //Loading data in tables
                6. \copy Movies(movieId, title) FROM PROGRAM 'tail -n +2 /Users/simranmalik/Desktop/cleaned_movies.tsv' WITH (FORMAT text);			// should COPY 4978131 rows

                7. \copy workedon(movieId, personid, category) FROM PROGRAM 'tail -n +2 /Users/simranmalik/Desktop/cleaned_workedon.tsv' WITH (FORMAT text);	// should COPY 51040986 rows

                8. \copy people(personid,name) FROM PROGRAM 'tail -n +2 /Users/simranmalik/Desktop/cleaned_people.tsv' WITH (FORMAT text);		// should COPY 14358304 rows


                9. \copy (SELECT Movies.title, People.name FROM Movies JOIN WorkedOn ON Movies.movieId = WorkedOn.movieId JOIN People ON WorkedOn.personId = People.personId WHERE Movies.title >= 'Alaa' AND Movies.title <= 'Alab' AND WorkedOn.category = 'director') TO '/Users/simranmalik/Desktop/Updated_PSQL_Caa_Cab_output.csv' WITH (FORMAT csv, HEADER); 	

                //should COPY 25 rows for this range

        7.2) Comparing results
             7.2) Paste the following command in terminal to perform correctness test. NOTE: update paths of both output files in CT.java:
             ./run_correctness_test

        CT. java file compares results from lab3 code and postgres output. Output from Lab3  java code should saved in LAB3_OUTPUT folder, and from Load_psql script in PSQL_Output folder. Update output file paths/file names in line 8,9 of CT.java
        This file compares the tuples and prints the comparison results. You can also choose to compile and run CT.java separately.
