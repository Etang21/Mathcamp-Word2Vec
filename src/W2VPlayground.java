import java.util.*;

public class W2VPlayground {
	
	public static void main(String[] args) {
		Word2VecUtility util = new Word2VecUtility();
		util.getVectors(100000);
		
		Scanner s = new Scanner(System.in);		
		System.out.println("Welcome to the Word2Vec playground!");
		boolean stillPlaying = true;
		while(stillPlaying) {
			System.out.println("To find which words are similar to a given word, type \"Synonyms\"");
			System.out.println("To find the similarity between two different words, type \"Similarity\"");
			System.out.println("To have Word2Vec solve analogies, type \"Analogies\"");
			String choice = s.next();
			if(choice.equalsIgnoreCase("Synonyms")) {
				System.out.print("Find words close to: ");
				String query = s.next();
				String[] excluded = {query};
				System.out.println(util.wordsCloseTo(query, 10, excluded));
			}
			else if(choice.equalsIgnoreCase("Similarity")) {
				System.out.println("Finds the cosine similarity between two words. Enter one word, hit return, another word, hit return.");
				String query1 = s.next();
				String query2 = s.next();
				util.printCosineSimilarity(query1, query2); 

			}
			else if(choice.equalsIgnoreCase("Analogies")) {
				System.out.println("A is to B as C is to . . .");
				System.out.print("A: ");
				String A = s.next();
				System.out.print("B: ");
				String B = s.next();
				System.out.print("C: ");
				String C = s.next();
				solveAnalogy(A, B, C, util);
			}
			else {
				System.out.println("Couldn't recgonize that choice. Try again?");
				continue;
			}
		}
	}
	
	//Solves the analogy A is to B as C is to D.
	public static ArrayList<WordScore> solveAnalogy(String A, String B, String C, Word2VecUtility util) {
		//Analogies! If A:B = C:D, then B - A + C should = D.
		float[] vec = util.getVec(B);
		vec = util.addVec(vec, util.getVec(A), -1);
		vec = util.addVec(vec, util.getVec(C), 1);
		String[] excluded = {A, B, C};
		ArrayList<WordScore> solutions = util.wordsCloseTo(vec, 10, excluded);
		System.out.println(solutions.toString());
		return solutions;
	}
	
}
