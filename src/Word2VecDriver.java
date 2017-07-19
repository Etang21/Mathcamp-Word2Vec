import java.io.IOException;
import java.util.ArrayList;


public class Word2VecDriver {

	public static void main(String[] args) throws IOException {
		Word2VecUtility util = new Word2VecUtility();
		util.getVectors(100000);
		//Test queries to play around with!
		util.printCosineSimilarity("Cuba", "Mexico"); 
		String query = "philosophy";
		ArrayList<WordScore> nearQuery = util.wordsCloseTo(query, 10);
		System.out.println(nearQuery.toString());
	}

}
