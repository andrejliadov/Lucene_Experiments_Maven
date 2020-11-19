package maven.lucene.search;

public class DocumentScore implements Comparable<DocumentScore>{
	public int documentNumber;
	public float score;

	DocumentScore(int num, float s){
		documentNumber = num;
		score = s;
	}

	public int compareTo(DocumentScore doc) {		
		return Float.compare(doc.score, this.score);
	}
}
