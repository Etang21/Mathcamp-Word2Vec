import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Word2VecDriver {

	public static void main(String[] args) throws IOException {
		Word2VecUtility util = new Word2VecUtility();
		util.getVectors(100000);
		
		//Finding words close to a query, excluding the query
		String query = "novel";
		String[] excluded = {query};
		System.out.println(util.wordsCloseTo(query, 10, excluded));
		
		//Print cosine similarity between two words
		util.printCosineSimilarity("horse", "cow"); 
		
		//Analogies! If A:B = C:D, then B - A + C should = D.
		float[] vec = util.getVec("king");
		vec = util.addVec(vec, util.getVec("male"), -1);
		vec = util.addVec(vec, util.getVec("female"), 1);
		ArrayList<WordScore> analogy = util.wordsCloseTo(vec, 10);
		System.out.println(analogy.toString());
	}
	
	//If you want to add a bunch of words via scanner and see what they're close to.
	public static void addABunchOfWords() {
		Word2VecUtility util = new Word2VecUtility();
		util.getVectors(100000);
		
		Scanner in = new Scanner(System.in);
		System.out.println("Words to add? Type 'done' when finished.");
		
		ArrayList<String> words = new ArrayList<String>();
		String input;
		while(true) {
			input = in.next();
			if(input.equals("done")) { break; }
			words.add(input);
		}
		
		float[] vecSum = new float[300];
		for(String w: words) {
			float[] wVec = util.getVec(w);
			vecSum = util.addVec(vecSum, wVec, 1);
		}
		ArrayList<WordScore> wordsNearSum = util.wordsCloseTo(vecSum, 20);
		System.out.println(wordsNearSum.toString());
		in.close();
	}
	

}
