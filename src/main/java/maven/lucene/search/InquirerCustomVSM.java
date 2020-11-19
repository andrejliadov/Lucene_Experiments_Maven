package maven.lucene.search;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class InquirerCustomVSM 
{

	// the location of the search index
	private static String INDEX_DIRECTORY = "index";
	private static String QUERY_FILE = "corpus/cran.qry";
	private static String RESULTS_FILE = "results/SimpleQueryCustomVSMScoring.txt";
	private static boolean appendToFile = false;

	public void queryIndexCustomVSM() throws Exception {
		System.out.println("Inquiring with VSM Scoring; This may take a minute");
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
		//Seting the BM25 scoring in the searcher
		isearcher.setSimilarity(new BM25Similarity());

		//Setup a means of storing results
		File outputFile = new File(RESULTS_FILE);
		FileWriter writer = new FileWriter(outputFile, appendToFile);

		ArrayList<String> queryTokens = null;
		ArrayList<DocumentScore> scores = null;

		for(int i = 0; i < querys.get(1).size(); i++) {
			//Create a list to store rankable documets with VSM
			ArrayList<TermVectorListEntry> documentsList = new ArrayList<TermVectorListEntry>(); 
			ArrayList<HashMap<String, Double>> frequenciesList = new ArrayList<HashMap<String, Double>>();
			HashMap<String, Double> queryFrequencies = new HashMap<String, Double>();

			BooleanQuery.Builder query = new BooleanQuery.Builder();
			queryTokens = utils.tokeniseQuery(querys.get(1).get(i));

			//Add all the query terms to a boolean query
			for(int itr = 0; itr < queryTokens.size(); itr++) {
				Query term = new TermQuery(new Term("Text", queryTokens.get(itr)));
				query.add(new BooleanClause(term, BooleanClause.Occur.SHOULD));
			}

			//Carry out the query so that we can access the DocID in the indexer and get the term vector
			TopDocs hits = isearcher.search(query.build(), 1400);
			for (int itr = 0; itr < hits.scoreDocs.length; itr++){
				Terms termVector = isearcher.getIndexReader().getTermVector(hits.scoreDocs[itr].doc, "Text");
				org.apache.lucene.document.Document doc = isearcher.doc(hits.scoreDocs[itr].doc);
				documentsList.add(new TermVectorListEntry(doc, termVector));
			}

			//Return a list of term frequencies vectors for each document that was a hit
			frequenciesList = utils.getFrequenciesList(documentsList, ireader);
			queryFrequencies = utils.getQueryFequencies(queryTokens, ireader);

			//The frequencies need to be turned into vectors and multiplied
			scores = utils.getCosineSimilarityScores(queryFrequencies, frequenciesList);
			Collections.sort(scores);

			for(int j = 0; j < 20; j++) {
				if (j >= scores.size()) {
					break;
				}
				writer.write(String.valueOf(i + 1) + " Q0 " + (scores.get(j).documentNumber + 1) + " " + (j+1) + " " + scores.get(j).score + " SimpleQueryCustomVSMScoring\n");
			}
		}            

		// close everything we used
		System.out.print("Custom VSM Querys are completed\n\n");
		ireader.close();
		directory.close();
		writer.close();
	}

}
