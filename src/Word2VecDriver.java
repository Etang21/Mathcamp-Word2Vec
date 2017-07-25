import java.io.IOException;
import java.util.ArrayList;


public class Word2VecDriver {

	public static void main(String[] args) throws IOException {
		Word2VecUtility util = new Word2VecUtility();
		util.getVectors(100000);
		String[] excluded = {"horse"};
		System.out.println(util.wordsCloseTo("horse", 10, excluded));
		
		/*
		//Test queries to play around with!
		util.printCosineSimilarity("horse", "cow"); 
		String query = "horse";
		ArrayList<WordScore> nearQuery = util.wordsCloseTo(query, 10);
		System.out.println(nearQuery.toString());
		
		//Analogies! If A:B = C:D, then B - A + C should = D.
		float[] vec = util.getVec("king");
		vec = util.addVec(vec, util.getVec("male"), -1);
		vec = util.addVec(vec, util.getVec("female"), 1);
		ArrayList<WordScore> analogy = util.wordsCloseTo(vec, 10);
		System.out.println(analogy.toString());
		*/

	}

}
