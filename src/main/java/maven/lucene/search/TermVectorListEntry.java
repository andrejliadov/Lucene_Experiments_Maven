package maven.lucene.search;

import org.apache.lucene.index.Terms;

public class TermVectorListEntry {
	
	public Terms termVector;
	public org.apache.lucene.document.Document doc;
	
	TermVectorListEntry(org.apache.lucene.document.Document d, Terms termVec){
		termVector = termVec;
		doc = d;
	}

}
