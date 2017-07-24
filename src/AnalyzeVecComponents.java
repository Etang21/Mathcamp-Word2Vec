import java.io.IOException;
import java.util.*;

public class AnalyzeVecComponents {

	public static void main(String[] args) throws IOException {
		printMaxComponentsFor(10, 10, 1000);
	}
	
	public static void printMaxComponentsFor(int numComponents, int numMaxElements, int numWordsToSearch) throws IOException {
		ArrayList<PriorityQueue<WordScore>> componentMaximums = getMaxComponentsFor(numComponents, numMaxElements, numWordsToSearch);
		for(int comp=0; comp<componentMaximums.size(); comp++) {
			System.out.println("For component " + comp + ", the words with maximum score are: " + componentMaximums.get(comp));
			System.out.println();
		}
	}
	
	private static ArrayList<PriorityQueue<WordScore>> getMaxComponentsFor(int numComponents, int numMaxElements, int numWordsToSearch) throws IOException { //Returns numMaximumElements of words for the first numComponents of all vectors. Searches numWordsToSearch words.
		Word2VecUtility util = new Word2VecUtility();
		util.getVectors(numWordsToSearch);
		ArrayList<PriorityQueue<WordScore>> componentMaximums = new ArrayList<PriorityQueue<WordScore>>();//Arraylist of component maximums.
		for(int comp=0; comp<numComponents; comp++) { //Add numComponents worth of PQs
			PriorityQueue<WordScore> initMaximums = new PriorityQueue<WordScore>(); //Initialize maximums array.
			//Uses default comparator established in WordScore class.
			for(int i=0; i<numMaxElements; i++) { //Initialize all priority queues with numMaxElements
				initMaximums.add(new WordScore("", -1.0f));
			}
			componentMaximums.add(initMaximums); 
		}
		
		for(String s: util.vectors.keySet()) { //Loop through all words we have
			float[] sVec = util.getVec(s);
			WordVec wordVec = new WordVec(s, sVec);
			for(int comp=0; comp<componentMaximums.size(); comp++) { //Loop through all components in that word
				updateMaxWords(componentMaximums.get(comp), comp, wordVec);
			}
		}
		
		return componentMaximums;
	}
	
	//For a given word, wordVec, component, and arrayList, go through and find max/min
	private static void updateMaxWords(PriorityQueue<WordScore> maxScores, int component, WordVec wordVec) {
		float minMaxScore = maxScores.peek().score; //The smallest score of the maximal components we have so far.
		if(wordVec.vec[component] > minMaxScore) { //Could pass in a WordScore instead of a wordVec and component index. Later run the comparator on this! That'll work. Can get PQ's comparator.
			maxScores.poll();
			maxScores.add(new WordScore(wordVec.word, wordVec.vec[component]));
		}
	}
}

class WordVec {
	//A helper class which stores a word and associated "score"
	public String word;
	public float[] vec;
	public WordVec(String word, float[] vec) {
		this.word = word;
		this.vec = vec;
	}
	
	public String fullString() {
		return "\"" + word + "\": " + vec;
	}
	
	public String toString() {
		return word;
	}
}
