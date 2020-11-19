To run the Indexer, queries and to print the trec_eval results:

./run.sh

This script creates a .jar and runs it. It then runs the trec_eval commands

---Running the code from source---

1) You must first git clone the repo:

git clone https://github.com/andrejliadov/Lucene_Experiments_Maven.git

2) Go into the trec_eval directory and build and compile the project:

cd trec_eval
make

3) Go back to the main project directory, grant the shell script execute permission:

cd ..
chmod +x run.sh

4) Execute the run.sh shell script:

./run.sh
