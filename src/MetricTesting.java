import static java.lang.Math.exp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class MetricTesting {

	public static void main(String[] args) throws IOException {
		Word2VecUtility util = new Word2VecUtility();
		util.getVectors(100000);
		
		testLotsOfHints(util);
		//testCosMetrics(util);
		
		/*
		//Finding words close to a query, excluding the query
		String query = "novel";
		String[] excluded = {query};
		System.out.println(util.wordsCloseTo(query, 10, excluded));
		
		
		//Print cosine similarity between two words
		util.printCosineSimilarity("cartoon_character", "snowman"); 
		
		//Analogies! If A:B = C:D, then B - A + C should = D.
		float[] vec = util.getVec("king");
		vec = util.addVec(vec, util.getVec("male"), -1);
		vec = util.addVec(vec, util.getVec("female"), 1);
		ArrayList<WordScore> analogy = util.wordsCloseTo(vec, 10);
		System.out.println(analogy.toString());
		*/
	}
	
	public static void testCosProductHints(Word2VecUtility util, ArrayList<String> targets) {
		WordScore bestHint = bestScoringHint(targets, util);
		System.out.println(bestScoringHint(targets, util));
		for(String target: targets) {
			System.out.println("Prob. between " + bestHint.word + ", " + target + " is " + prob(util.cosineSimilarity(util.getVec(bestHint.word), util.getVec(target))));
		}
	}
	
	
	public static void testLotsOfHints(Word2VecUtility util) {
		for(int i=0; i<randomClueTriples.length-1; i+=3) {
			ArrayList<String> targets = new ArrayList<String>(Arrays.asList(randomClueTriples[i], randomClueTriples[i+1], randomClueTriples[i+2]));
			testCosProductHints(util, targets);
			System.out.println();
		}
	}
	
	//Trying distance to 1 idea.
	private static WordScore bestScoringHint(ArrayList<String> targets, Word2VecUtility util) {
		WordScore best = new WordScore("",-999);
		ArrayList<float[]> targetVecs = new ArrayList<float[]>();
		for(String tar: targets) {
			targetVecs.add(util.getVec(tar));
		}
		
		//TODO: Deal with duplicate words here later
		for(Map.Entry<String, float[]> entry: util.vectors.entrySet()) {
			float score = getProbProd(entry.getValue(), targetVecs, util);
			if(score > best.score)
				best = new WordScore(entry.getKey(), score);
		}
		return best;
	}
	
	//Try your "distance to 1" idea: if (x,y) is a probability pair, computes distance from (x,y) to (1,1).
	//This one alters the distance to one so that things even farther out are prioritized - as long as they're even.
		private static float distTo1Altered(float[] clueVec, ArrayList<float[]> targetVecs, Word2VecUtility util) {
			float powerFactor = 2;
			float probSquareSum = 0f;
			for(float[] tarVec: targetVecs) {
				probSquareSum += Math.pow(1-prob(util.cosineSimilarity(clueVec, tarVec)), powerFactor);
			}
			return (float) Math.pow(probSquareSum, 1f/powerFactor) * (-1);
		}

	//Try your "distance to 1" idea: if (x,y) is a probability pair, computes distance from (x,y) to (1,1).
	private static float distTo1(float[] clueVec, ArrayList<float[]> targetVecs, Word2VecUtility util) {
		float probSquareSum = 0f;
		for(float[] tarVec: targetVecs) {
			//Takes the distance to 1 and squares it.
			probSquareSum += Math.pow(1-prob(util.cosineSimilarity(clueVec, tarVec)), 2);
		}
		return (float) Math.pow(probSquareSum, 0.5f) * (-1);
	}
	
	//Sum of sqrts of probabilities?
	private static float sumOfProbSqrts(float[] clueVec, ArrayList<float[]> targetVecs, Word2VecUtility util) {
		float probSqrtSum = 0f;
		for(float[] tarVec: targetVecs) {
			probSqrtSum += Math.pow(prob(util.cosineSimilarity(clueVec, tarVec)), 0.5);
		}
		return probSqrtSum;
	}
		
	
	//Try product of probabilities?
	private static float getProbProd(float[] clueVec, ArrayList<float[]> targetVecs, Word2VecUtility util) {
		float probProd = 1f;
		for(float[] tarVec: targetVecs) {
			probProd *= prob(util.cosineSimilarity(clueVec, tarVec));
		}
		return probProd;
	}
	
	//Try sum of probabilities?
	private static float getProbSum(float[] clueVec, ArrayList<float[]> targetVecs, Word2VecUtility util) {
		float probSum = 0f;
		for(float[] tarVec: targetVecs) {
			probSum += prob(util.cosineSimilarity(clueVec, tarVec));
		}
		return probSum;
	}
	
	//Try sum of square roots? Penalizes those that are close to one and far from the other:
	private static float getCosSqrtSum(float[] clueVec, ArrayList<float[]> targetVecs, Word2VecUtility util) {
		float cosSqrtSum = 0f;
		for(float[] tarVec: targetVecs) {
			cosSqrtSum += Math.pow(util.cosineSimilarity(clueVec, tarVec), 0.5f);
		}
		return cosSqrtSum;
	}
	
	//Returns product of cosine similarities between clueVec and all targetVecs
	private static float getCosProd(float[] clueVec, ArrayList<float[]> targetVecs, Word2VecUtility util) {
		float cosProd = 1f;
		for(float[] tarVec: targetVecs) {
			cosProd *= util.cosineSimilarity(clueVec, tarVec);
		}
		return cosProd;
	}
	
	private static void testCosMetrics(Word2VecUtility util) {
		printCosMetrics(goodClueTriples, 3, util);
	}
	
	private static void printCosMetrics(String[] words, int wordsPerClue, Word2VecUtility util) {
		float cosSumAvg = 0f;
		float cosProdAvg = 0f;
		float probSumAvg = 0f;
		float probProdAvg = 0f;
		
		//Loop through all clues
		for(int i=0; i<words.length; i+=wordsPerClue) {
			float cosSum = 0f;
			float cosProd = 1f;
			float probSum = 0f;
			float probProd = 1f;
			//Loop through all words in a given clue:
			System.out.print("Clue is " + words[i+wordsPerClue-1] + " for: ");
			for(int j=0; j<wordsPerClue-1; j++) {
				System.out.print(words[i+j] + ", ");
				float cosSim = util.cosineSimilarity(util.getVec(words[i+wordsPerClue-1]), util.getVec(words[i+j]));
				cosSum += cosSim;
				cosProd *= cosSim;
				float prob = prob(cosSim);
				probSum += prob;
				probProd *= prob;
				System.out.print("(prob: " + prob + "),");
			}
			System.out.println();
			System.out.println("cosSimSum: " + cosSum + ", cosProd: " + cosProd + ", probSum: " + probSum + ", probProd: " + probProd); 
			System.out.println();
			cosSumAvg += cosSum;
			cosProdAvg += cosProd;
			probSumAvg += probSum;
			probProdAvg += probProd;
		}
		System.out.println();
		float numClues = (float)words.length/wordsPerClue; 
		System.out.println("Across all these clues, the mean values of your metrics are . . . ");
		System.out.print("cosSumAvg: " + cosSumAvg/numClues + " cosProdAvg: " + cosProdAvg/numClues 
				+ " probSumAvg: " + probSumAvg/numClues + " probProdAvg: " + probProdAvg/numClues);
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
	
	//Method for estimating the probability that two words are related, as a function of their cosSim

    final static float[] B = new float[]{-3.55106049399243f, 11.8332877194120f};
    final static float t = 0.75f;
    public static float prob(float sim){
        float arg = B[0] + sim*B[1];
        return 1.0f/(float)(1+exp(-arg));
    }
    

	static String[] randomClueTriples = new String[] {
			"club", "joint", "Michigan",
			"wheelhouse", "crew", "east",
			"jeep", "painting", "apple",
			"fairness", "gorge", "commander",
			"butter", "pretty", "monsoon",
			"albatross", "pineapple", "holes",
			"imagination", "run", "blemish",
			"steal", "tall", "snowman",
			"Nike", "blonde", "quietly",
			"hawk", "alien", "bold",
			"dip", "turbulent", "Asian",
			"ketchup", "weak", "Aaron"
	};
	
	static String[] goodCluePairs = new String[] {"sail", "boat", "key", "lock", "work", "office", "pouch", "kangaroo", "Australia", "outback", "flower", "romantic",
			"honey", "bee", "calendar", "time", "Juliet", "love", "Noah", "ark", "gym", "fit", "tool", "insult"};
			
	static String[] randomWordPairs = new String[] {"vodka", "bear", "cruel", "potato", "albatross", "cold", "help", "Alicia", "north", "key", "part", "warrior", "knelt", "far",
			"normal", "driver", "turbulent", "dragon", "violent", "cute", "fantasy", "May", "bisexual", "horde", "colorful", "trace"};
	
	static String[] goodClueTriples = new String[] {
			"lake", "queen", "Victoria", 
			"outlet", "lamp", "electricity",
			"knowledge", "building", "school",
			"tournament", "knight", "battle",
			"green", "carbon", "photosynthesis",
			"forest", "Lincoln", "log",
			"cool", "Japan", "Siberia",
			"honey", "nuts", "granola",
			"animation", "incredible", "Pixar",
			"anchovies", "Chicago", "pizza",
			"Obama", "eagle", "America",
			"pope", "jewel", "crown",
			"Normandy", "bomb", "war",
			"clear", "bartender", "glass",
			"mean", "whip", "punish"
	};
	

}
