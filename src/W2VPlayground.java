import java.util.*;

public class W2VPlayground {
	
	public static void main(String[] args) {
		Word2VecUtility util = new Word2VecUtility();
		util.getVectors(100000);
		
		Scanner s = new Scanner(System.in);		
		System.out.println("Welcome to the Word2Vec playground!");
		System.out.println();
		
		boolean stillPlaying = true;
		while(stillPlaying) {
			System.out.println("(Syn)onyms  (Sim)ilarity  (Ana)nalogies");
			String choice = s.next();
			if(choice.equalsIgnoreCase("Syn")) {
				runSynonyms(s, util);
			} else if(choice.equalsIgnoreCase("Sim")) {
				runSimilarity(s, util);
			} else if(choice.equalsIgnoreCase("Ana")) {
				runAnalogies(s, util);
			} else if(choice.equalsIgnoreCase("Exit")) {
				stillPlaying = false;
			} else {
				System.out.println("Couldn't recgonize that choice. Try again?");
				continue;
			}
		}
		s.close();
	}
	
	private static void runSynonyms(Scanner s, Word2VecUtility util) {
		System.out.print("Find words close to: ");
		String query = s.next();
		String[] excluded = {query};
		try {
			System.out.println(util.wordsCloseTo(query, 10, excluded));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	private static void runSimilarity(Scanner s, Word2VecUtility util) {
		System.out.println("Finds the cosine similarity between two words. Enter one word, hit return, another word, hit return.");
		String query1 = s.next();
		String query2 = s.next();
		try {
			util.printCosineSimilarity(query1, query2); 
		} catch (WordNotFoundException e) {
			System.out.println("WordNotFoundException: " + e.getMessage());
		}
	}
	
	private static void runAnalogies(Scanner s, Word2VecUtility util) {
		System.out.println("A is to B as C is to . . .");
		System.out.print("A: ");
		String A = s.next();
		System.out.print("B: ");
		String B = s.next();
		System.out.print("C: ");
		String C = s.next();
		try {
			solveAnalogy(A, B, C, util);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	/** Solves analogies, A is to B as C is to D. */
	public static ArrayList<WordScore> solveAnalogy(String A, String B, String C, Word2VecUtility util) throws WordNotFoundException {
		//If A:B = C:D, then B - A + C ~= D.
		float[] aVec = util.getVec(A);
		float[] bVec = util.getVec(B);
		float[] cVec = util.getVec(C);
		if (aVec == null) {
			throw new WordNotFoundException(A);
		} else if (bVec == null) {
			throw new WordNotFoundException(B);
		} else if (cVec == null) {
			throw new WordNotFoundException(C);
		}
		
		float[] targetVec = util.addVec(bVec, aVec, -1);
		targetVec = util.addVec(targetVec, cVec, 1);
		String[] excluded = {A, B, C};
		ArrayList<WordScore> solutions = util.wordsCloseTo(targetVec, 10, excluded);
		System.out.println(solutions.toString());
		return solutions;
	}
	
}
