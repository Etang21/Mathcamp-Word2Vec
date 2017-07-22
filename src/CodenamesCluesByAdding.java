import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class CodenamesCluesByAdding {
	//Other ideas: take the dot-product, subtract words?, add all nine of them.

	public static void main(String[] args) throws IOException {
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
	
	public static void pairwiseClosest(ArrayList<String> words) { 
		//Takes a bunch of words, searches for words similar to their pairwise sums, returns the best one?
		for(int i=0; i<words.size()-1; i++) {
			for(int j=i+1; j<words.size(); j++) {
				
			}
		}
	}
	
}
