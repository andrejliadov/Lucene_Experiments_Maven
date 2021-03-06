package maven.lucene.search;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class InquirerVSM {

	// the location of the search index
	private static String INDEX_DIRECTORY = "index";
	private static String QUERY_FILE = "corpus/cran.qry";
	private static String RESULTS_FILE = "results/SimpleQueryVSMScoring.txt";
	private static boolean appendToFile = false;

	// Limit the number of search results we get
	private static int MAX_RESULTS = 20;

	public void queryIndexVSM() throws Exception
	{

		System.out.println("Inquiring with VSM Scoring");

		Utils utils = new Utils();
		//Organise and parse the documents
		File file = new File(QUERY_FILE);
		if (!file.exists()){
			System.out.print("File Not Found: 404\n");
		}
		ArrayList<ArrayList<String>> querys = utils.parseQuesrys(file);

		// Open the folder that contains our search index
		Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));

		// create objects to read and search across the index
		DirectoryReader ireader = DirectoryReader.open(directory);
		IndexSearcher isearcher = new IndexSearcher(ireader);
		//Seting the VSM scoring in the searcher
		isearcher.setSimilarity(new ClassicSimilarity());

		//Setup a means of storing results
		File outputFile = new File(RESULTS_FILE);
		FileWriter writer = new FileWriter(outputFile, appendToFile);

		float[] resultArray = new float[1400];
		ArrayList<String> queryTokens = null;
		ArrayList<DocumentScore> scores = null;

		for(int i = 0; i < querys.get(1).size(); i++) {
			queryTokens = utils.tokeniseQuery(querys.get(1).get(i));
			//resultArray = utils.makeQuery(queryTokens, isearcher);
			resultArray = utils.makeSingleQuery(queryTokens, isearcher);
			scores = utils.rankDocumnets(resultArray);
			//scores = utils.normalize(scores);

			for(int j = 0; j < MAX_RESULTS; j++) {
				if (j >= scores.size()) {
					break;
				}

				writer.write(String.valueOf(i + 1) + " Q0 " + (scores.get(j).documentNumber + 1) + " " + (j+1) + " " + scores.get(j).score + " SimpleQueryVSMScoring\n");
			}
		}            

		// close everything we used
		System.out.print("VSM Querys are completed\n\n");
		ireader.close();
		directory.close();
		writer.close();
	}

}
