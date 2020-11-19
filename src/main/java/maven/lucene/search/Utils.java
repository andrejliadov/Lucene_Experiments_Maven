package maven.lucene.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

public class Utils {

	public ArrayList<ArrayList<String>> parseQuesrys(File file) throws Exception{
		ArrayList<ArrayList<String>> results = new ArrayList<ArrayList<String>>();

		//Read in the queriescran
		FileInputStream fin = new FileInputStream(file);
		String all = convertStreamToString(fin);
		fin.close();

		String queryNumber = null;
		String text = null;

		ArrayList<String> queryNumbers = new ArrayList<String>();
		ArrayList<String> texts = new ArrayList<String>();

		int index = 0;
		int queryCount = 1;
		while(all != null && index < all.length()) {
			index = all.indexOf(".I");
			if (index+3 < all.indexOf(".W")) {
				queryNumber = String.valueOf(queryCount);
				queryNumbers.add(queryNumber);
				queryCount += 1;
			}

			index = all.indexOf(".W");
			if(Integer.parseInt(queryNumbers.get(queryNumbers.size()-1)) < 225) {
				all = all.substring(index, all.length());
				index = all.indexOf(".W");
				if (index+3 < all.indexOf(".I")) {
					text = all.substring(index+3, all.indexOf(".I")-1);
					texts.add(text);
				}
				else {
					texts.add("");
				}
				all = all.substring(all.indexOf(".I"), all.length());
			} 
			else {
				text = all.substring(index+3, all.length()-1);
				texts.add(text);
				all = null;

				break;
			}
		}

		results.add(queryNumbers); results.add(texts);
		return results;
	}

	public ArrayList<String> tokeniseQuery(String query) throws IOException{
		ArrayList<String> results = new ArrayList<String>();
		StopList stopList = new StopList();
		CharArraySet set = stopList.stopList;
		Analyzer analyzer = new StandardAnalyzer(set);
		analyzer.setVersion(Version.LUCENE_8_6_3);

		TokenStream stream = analyzer.tokenStream("query", query);
		CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);

		try {
			stream.reset();

			//print all tokens until stream is exhausted
			while (stream.incrementToken()) {
				results.add(termAtt.toString());
			}

			stream.end();
		}finally {
			stream.close();
		}

		analyzer.close();
		return results;
	}

	public float[] makeQuery(ArrayList<String> queryTokens, IndexSearcher isearcher) throws IOException {
		//Create a data-structure that accumulates the score for every document
		float[] documentScoreCounter = new float[1400];        	

		//Make a seperate query for every word and combine the scores
		//Search the text fields of the index first  	        	
		for(int i = 0; i < queryTokens.size(); i++) {
			Query term = new TermQuery(new Term("Text", queryTokens.get(i)));
			BooleanQuery.Builder query = new BooleanQuery.Builder();
			query.add(new BooleanClause(term, BooleanClause.Occur.SHOULD));

			ScoreDoc[] hits = isearcher.search(query.build(), 20).scoreDocs;        		
			for (int j = 0; j < hits.length; j++){
				Document hitDoc = isearcher.doc(hits[j].doc);
				int docNum = Integer.parseInt(hitDoc.get("Document_number"));
				documentScoreCounter[docNum-1] += hits[j].score;	               
			}
		}

		return documentScoreCounter;
	}

	public float[] makeSingleQuery(ArrayList<String> queryTokens, IndexSearcher isearcher) throws IOException {
		//Create a data-structure that accumulates the score for every document
		float[] documentScoreCounter = new float[1400];
		BooleanQuery.Builder query = new BooleanQuery.Builder();

		//Make a single Query for all the relevant terms in a query
		//The score for the sinle query will be taken as the final score
		for(int i = 0; i < queryTokens.size(); i++) {
			Query term = new TermQuery(new Term("Text", queryTokens.get(i)));
			query.add(new BooleanClause(term, BooleanClause.Occur.SHOULD));
		}

		//A query has been built with all the query terms added
		ScoreDoc[] hits = isearcher.search(query.build(), 20).scoreDocs;        		
		for (int j = 0; j < hits.length; j++){
			Document hitDoc = isearcher.doc(hits[j].doc);
			int docNum = Integer.parseInt(hitDoc.get("Document_number"));
			documentScoreCounter[docNum-1] += hits[j].score;	               
		}

		return documentScoreCounter;
	}

	public ArrayList<DocumentScore> rankDocumnets(float[] documentScores) {
		ArrayList<DocumentScore> docScores = new ArrayList<DocumentScore>();

		for(int i = 0; i < documentScores.length; i++) {
			if(documentScores[i] > 0.1) {
				DocumentScore score = new DocumentScore(i, documentScores[i]);
				docScores.add(score);
			}
		}

		Collections.sort(docScores);
		return docScores;
	}

	//We need term frequency and a means of representing the 
	public ArrayList<HashMap<String, Double>> getFrequenciesList(ArrayList<TermVectorListEntry> termVectors, DirectoryReader ireader) throws IOException {

		ArrayList<HashMap<String, Double>> frequenciesList = new ArrayList<HashMap<String, Double>>();

		for(int itr = 0; itr < termVectors.size(); itr++) {
			//This hash table will store the frequncies for each document
			HashMap<String, Double> frequencies = new HashMap<String, Double>();
			// get terms vectors for one document and one field
			Terms termVector = termVectors.get(itr).termVector;

			if (termVector != null && termVector.size() > 0) {
				// access the terms for this field
				TermsEnum termsEnum = termVector.iterator(); 
				BytesRef term = null;
				PostingsEnum postings = null;

				// explore the terms for this field
				while ((term = termsEnum.next()) != null) {    				
					// get the term frequency in the document 
					postings = termsEnum.postings(postings, PostingsEnum.FREQS);
					if (postings.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
						String text = term.utf8ToString();
						double termFreq = postings.freq();

						//calculate inverse document frequency
						Term termInstance = new Term("Text", text);
						double docFreq = ireader.docFreq(termInstance);
						double inverseDocFreq = Math.log((1+ireader.maxDoc())/(1+docFreq));

						//Calculate and store TFIDF weights
						double TfidfWeight = termFreq * inverseDocFreq;
						frequencies.put(text, TfidfWeight);
					}
				}
			}
			frequenciesList.add(frequencies);
		}
		return frequenciesList;
	}

	public HashMap<String, Double> getQueryFequencies(ArrayList<String> queryTokens, DirectoryReader ireader) throws IOException{
		HashMap<String, Double> queryFrequencies = new HashMap<String, Double>();

		for(int i = 0; i < queryTokens.size();i++) {
			if(!queryFrequencies.containsKey(queryTokens.get(i))) {
				queryFrequencies.put(queryTokens.get(i), 1.0);
			}
			else {
				queryFrequencies.put(queryTokens.get(i), queryFrequencies.get(queryTokens.get(i)) + 1);
			}
		}

		//Calculate the TF-IDF wieghts for every query term
		for(String term : queryFrequencies.keySet()) {
			Term termInstance = new Term("Text", term);
			double docFreq = ireader.docFreq(termInstance);
			double termFreq = queryFrequencies.get(term);
			double inverseDocFreq = Math.log((1+ireader.maxDoc())/(1+docFreq));

			//Calculate and store TFIDF weights
			double TfidfWeight = termFreq * inverseDocFreq;
			queryFrequencies.put(term, TfidfWeight);
		}

		return queryFrequencies;
	}

	public ArrayList<Double> makeVector(HashMap<String, Double> frequencies) {
		ArrayList<Double> vector = new ArrayList<Double>();

		for(String term : frequencies.keySet()) {
			vector.add(frequencies.get(term));
		}

		return vector;
	}

	public ArrayList<DocumentScore> getCosineSimilarityScores(HashMap<String, Double> queryFrequencies, ArrayList<HashMap<String, Double>> frequenciesList) {

		ArrayList<DocumentScore> result = new ArrayList<DocumentScore>();
		ArrayList<Double> queryVector = makeVector(queryFrequencies);
		double score = 0;

		for(int i = 0; i < frequenciesList.size();i++) {
			HashMap<String, Double> documentFrequencies = new HashMap<String, Double>();
			for(String term : queryFrequencies.keySet()) {
				if(frequenciesList.get(i).containsKey(term)) {
					documentFrequencies.put(term, frequenciesList.get(i).get(term));
				}
				else {
					documentFrequencies.put(term, 0.0);
				}
			}

			ArrayList<Double> documentVector = makeVector(documentFrequencies);
			score = dot(queryVector, documentVector);
			score = score / (norm(queryVector)*norm(documentVector));
			DocumentScore scoreElement = new DocumentScore(i, (float) score);

			result.add(scoreElement);
		}

		return result;
	}

	public double norm(ArrayList<Double> queryVector) {
		double norm = 0.01;
		double sumOfSquares = 0;

		for(int i = 0; i < queryVector.size(); i++) {
			sumOfSquares += Math.pow(queryVector.get(i), 2);
		}

		norm = Math.sqrt(sumOfSquares);

		return norm;
	}

	public double dot(ArrayList<Double> queryVector, ArrayList<Double> documentVector) {

		double result = 0;

		for (int i = 0; i < queryVector.size(); i++) {
			result += queryVector.get(i)*documentVector.get(i);
		}

		return result;
	}

	public float mapScore(float score, float start, float end) {
		float result = 5;

		float inputStart = start;
		float inputEnd = end;
		float outputStart = 0;
		float outputEnd = 1;

		if(score > inputEnd) {
			result = (float) 0.99;
			return result;
		}

		result = (score - inputStart) / (inputEnd - inputStart) * (outputEnd - outputStart) + outputStart;

		return result;
	}

	public static String readFileAsString(String fileName) {
		String text = "";
		try {
			text = new String(Files.readAllBytes(Paths.get(fileName)));
		} catch (IOException e) {
			e.printStackTrace();
		}

		return text;
	}

	public String convertStreamToString(InputStream is) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			sb.append(line).append("\n");
		}
		reader.close();
		return sb.toString();
	}

	//Split the Cranfield Collection into 1400 separate files    
	public ArrayList<ArrayList<String>> parseDocuments(File file) throws Exception {
		//Read in the corpus
		FileInputStream fin = new FileInputStream(file);
		String all = convertStreamToString(fin);
		fin.close();

		String documentNumber = null;
		String title = null;
		String author = null;
		String journal = null;
		String text = null;

		//Create fields for all of the doc nums, titles, journals and text fields via parsing
		ArrayList<String> documentNumbers = new ArrayList<String>();
		ArrayList<String> titles = new ArrayList<String>();
		ArrayList<String> authors = new ArrayList<String>();
		ArrayList<String> journals = new ArrayList<String>();
		ArrayList<String> texts = new ArrayList<String>();

		int index = 0;
		while(all != null && index < all.length()) {
			//The -1 is to remove '\n'
			//The +3 is to get past ".I\n" etc...
			index = all.indexOf(".I");
			if (index+3 < all.indexOf(".T")) {
				documentNumber = all.substring(index+3, all.indexOf(".T")-1);
				documentNumbers.add(documentNumber);
			}
			else {
				documentNumbers.add("");
			}

			index = all.indexOf(".T");
			if (index+3 < all.indexOf(".A")) {
				title = all.substring(index + 3, all.indexOf(".A")-1);
				titles.add(title);
			}
			else {
				titles.add("");
			}

			index = all.indexOf(".A");
			if (index+3 < all.indexOf(".B")) {
				author = all.substring(index + 3, all.indexOf(".B")-1);
				authors.add(author);
			}
			else {
				authors.add("");
			}

			index = all.indexOf(".B");
			if (index+3 < all.indexOf(".W")) {
				journal = all.substring(index+3, all.indexOf(".W")-1);
				journals.add(journal);
			}
			else {
				journals.add("");
			}

			index = all.indexOf(".W");
			if(Integer.parseInt(documentNumbers.get(documentNumbers.size()-1)) < 1400) {
				all = all.substring(index, all.length());
				index = all.indexOf(".W");
				if (index+3 < all.indexOf(".I")) {
					text = all.substring(index+3, all.indexOf(".I")-1);
					texts.add(text);
				}
				else {
					texts.add("");
				}
				all = all.substring(all.indexOf(".I"), all.length());
			} 
			else {
				text = all.substring(index+3, all.length()-1);
				texts.add(text);
				all = null;

				break;
			}
		}


		//Return the results in a 2D vector
		ArrayList<ArrayList<String>> results = new ArrayList<ArrayList<String>>();
		results.add(documentNumbers); results.add(titles); results.add(authors); results.add(journals); results.add(texts);

		return results;
	}

	public ArrayList<DocumentScore> normalize(ArrayList<DocumentScore> scores) {
		DocumentScore max = Collections.max(scores);
		float maxScore = max.score;

		for(int i = 0; i < scores.size(); i++) {
			float normVal = scores.get(i).score/maxScore;
			scores.set(i, new DocumentScore(i, normVal));
		}

		return scores;
	}

}
