package maven.lucene.search;

public class Main {

	public static void main(String[] args) throws Exception {
		//Make an Indexer
		Indexer indexCreator = new Indexer();

		//Make querying objects
		Inquirer inquirer = new Inquirer();
		InquirerBM25 inquirerBM25 = new InquirerBM25();
		InquirerCustomScoring inquirerCustom = new InquirerCustomScoring();
		InquirerVSM inquirerVSM = new InquirerVSM();
		
		//I tried to make a VSM scorer from scratch
		//InquirerCustomVSM inquirerCustomVSM = new InquirerCustomVSM();

		//Make Formatter object
		QrelFormatter formatter = new QrelFormatter();

		//Create and store the index
		indexCreator.makeIndex();

		//Run Queries
		inquirer.queryIndex();
		inquirerBM25.queryIndexBM25();
		inquirerCustom.queryIndexCustomScoring();
		inquirerVSM.queryIndexVSM();
		
		//It was slow and didn't work well
		//inquirerCustomVSM.queryIndexCustomVSM();
		
		System.out.println("All Querying Completed\n");

		//Format The QRELS file for trec_eval
		formatter.formatQrels();
		System.out.println("QRELS have been formatted");
	}

}
