package maven.lucene.search;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class Indexer {

	// Directory where the search index will be saved
	private static String INDEX_DIRECTORY = "index";
	private static String CORPUS_FILE = "corpus/cran.all.1400";
	private Utils utils = new Utils();

	public void makeIndex() throws Exception
	{   

		String currentDirectory = System.getProperty("user.dir");
		System.out.println("The current working directory is " + currentDirectory);
		// Analyzer that is used to process TextField
		StopList stopList = new StopList();
		CharArraySet set = stopList.stopList;
		Analyzer analyzer = new StandardAnalyzer(set);
		analyzer.setVersion(Version.LUCENE_8_6_3);    	    	

		//Organise and parse the documents
		File file = new File(CORPUS_FILE);
		if (!file.exists()){
			System.out.print("File Not Found: 404\n");
		}
		ArrayList<ArrayList<String>> fields = utils.parseDocuments(file);

		// To store an index in memory
		// Directory directory = new RAMDirectory();
		// To store an index on disk        
		Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));      

		// Set up an index writer to add process and save documents to the index
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		IndexWriter iwriter = new IndexWriter(directory, config);

		ArrayList<Document> documnets = new ArrayList<Document>(); 

		// Create a new document
		Document doc = new Document();

		//Create a custom field type
		//I can fine tune indexing options
		FieldType type = new FieldType();
		type.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
		type.setStored(true);
		type.setTokenized(true);
		type.setStoreTermVectors(true);

		for (int i = 0; i < 1400; i++) {
			for (int j = 0; j < fields.size(); j++) {
				//Use switch to get the parsed data under the right heading
				switch(j) {
				case 0:
					doc.add(new TextField("Document_number", fields.get(j).get(i), Field.Store.YES));
					System.out.print("Indexing Document: ");
					System.out.print(fields.get(j).get(i));
					System.out.print("\n");
					break;
				case 1:
					doc.add(new Field("Title", fields.get(j).get(i), type));	        			
					break;
				case 2:
					doc.add(new Field("Author", fields.get(j).get(i), type));
					break;
				case 3:
					doc.add(new Field("Journal", fields.get(j).get(i), type));
					break;
				case 4:
					doc.add(new Field("Text", fields.get(j).get(i), type));
					break;
				}
			}
			// Save the document to the index
			documnets.add(doc);
			doc = new Document();
		}

		iwriter.addDocuments(documnets);

		// Commit changes and close everything
		iwriter.close();
		directory.close();
		System.out.print("Indexing done\n\n");
	}

}
