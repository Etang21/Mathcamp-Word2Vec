import Jama.Matrix;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;

import static java.lang.Math.exp;

/**
 * Created by dawsonbyrd on 7/20/17.
 */

//9 one color, 8 another, 1 assasin, 7 bystanders

public class Codenames_UI {

    final static float[] B = new float[]{-3.55106049399243f, 11.8332877194120f};
    final static float t = 0.75f;


    public static Word2VecUtility util = new Word2VecUtility();
    static ArrayList<String> words = new ArrayList<>();
    static ArrayList<String> opp = new ArrayList<>();
    static ArrayList<String> bystanders = new ArrayList<>();
    static String assassin;

    static float count=0; //for progress bar
    static int[] num_subsets;

    static String[] data = new String[404];

    public static void main(String[] args) throws IOException {

        util.getVectors(GameSettings.DATABASE_SIZE);
        while(true) {
            FileWriter fw = new FileWriter("threshold_data.txt", true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);

            words.clear();
            opp.clear();
            bystanders.clear();
            assassin = "";

            if (GameSettings.PLAY_BOARD_FROM_FILE) {
                loadBoardFromFile("board.txt");
            } else {
                loadBoardFromOnline();
            }
            
            num_subsets = new int[words.size() + 1];
            for (int k = 0; k < num_subsets.length; k++) {
                int result = 1;
                for (int i = 0; i < k; i++) {
                    result *= (words.size() - i);
                }
                for (int i = 1; i <= k; i++) {
                    result /= i;
                }
                num_subsets[k] = result;
            }

	        System.out.println("Getting hint...");
	        ArrayList<Hint> maximums = findBestHints();
	        int shift = (maximums.size() > 1) ? 2 : 1;
	        Hint final_hint = maximums.get(maximums.size() - shift);
	        System.out.println();
	        System.out.println("hint: " + final_hint.word + "," + final_hint.s.length);
	
	        Scanner in = new Scanner(System.in);
	        for (int j = 0; j < final_hint.s.length; j++) {
	            System.out.print("pick " + j + ":");
	            String pick = in.nextLine();
	            if (!words.contains(pick)) {
	                System.out.println("\rincorrect!");
	                out.print((j + 1) + "" + 0 + ",");
	                break;
	            }
	            if (j == final_hint.s.length - 1) {
	                System.out.println("\rcorrect!");
	                out.print(final_hint.s.length + "" + 1 + ",");
	            }
	        }
	
	        String[] intended = new String[final_hint.s.length];
	        for (int j = 0; j < intended.length; j++) {
	            intended[j] = words.get(final_hint.s[j]);
	        }
	        System.out.println("intended cards: " + Arrays.toString(intended));
	
	            System.out.print("quit? ");
	            if (!in.nextLine().equals("")) break;
	            System.out.println();
	
	            out.close();

        }
    }
    
    //Scans board from fileName, populates our ArrayLists:
    private static void loadBoardFromFile(String fileName) {
    	try {
	    	File file = new File(fileName);
	        Scanner boardInput = new Scanner(file);

	        words.clear(); opp.clear();
	
	        int num_words = Integer.parseInt(boardInput.nextLine());
	        for(int k=0; k<num_words; k++){
	            String curr = boardInput.next();
	            words.add(curr);}
	
	        int num_opp = Integer.parseInt(boardInput.next());
	        for(int k=0; k<num_opp; k++){
	            String curr = boardInput.next();
	            opp.add(curr);}
	
	        int num_by = Integer.parseInt(boardInput.next());
	        for(int k=0; k<num_by; k++){bystanders.add(boardInput.next());}
	
	        assassin = boardInput.next();
	        boardInput.close();
    	} catch(FileNotFoundException e) {
    		System.out.println("Could not find file " + fileName);
    		System.out.println("Error report: " + e);
    		System.out.println("Did you misspell fileName? Try again.");
    	}
    }

    //Scans board from online, then prints to console
    private static void loadBoardFromOnline() throws IOException {
    	URL url = new URL("https://raw.githubusercontent.com/jbowens/codenames/master/assets/original.txt");
        //URL url = new URL("https://raw.githubusercontent.com/jbowens/codenames/master/assets/game-id-words.txt");
        //URL url = new URL("https://raw.githubusercontent.com/jbowens/codenames/master/assets/words.txt");
        Scanner s = new Scanner(url.openStream());

        System.out.print("\rdownloading vocabulary...");
        int i = 0;
        while(s.hasNext()){data[i] = s.next().toLowerCase(); i++;}

        int word_size=0;
        if(Math.random()<0.5){word_size=8;}
        else{word_size=9;}

        System.out.println("\rgenerating game board...");
        int n = 0;
        while (n < data.length) {
            if (opp.size() == 17-word_size && words.size() == word_size && bystanders.size()==7 && !assassin.equals(""))
                break;

            double rand = Math.random();
            if (rand < 0.1) {
                if (util.getVec(data[n]) == null) continue;
                if (data[n].length() < 3) continue;
                if (rand < 0.025) {
                    if (words.size() < word_size) words.add(data[n]);
                } else if (rand<0.05){
                    if(opp.size() < 17-word_size) opp.add(data[n]);
                } else if(rand<0.075) if(bystanders.size()<7) bystanders.add(data[n]);

                else if(assassin.equals(""))assassin = data[n];
            }

            if (n == data.length - 1) {
                n = 0;
                opp.clear();
                words.clear();
            } else n++;
        }

        ArrayList<String> cards = new ArrayList<>();
        cards.addAll(words);
        cards.addAll(opp);
        cards.addAll(bystanders);
        cards.add(assassin);
        Collections.shuffle(cards);

        for (int j = 0; j < 5; j++) {
            for (int x = 0; x < 4; x++) {
                System.out.print(cards.get(5 * j + x) + " ");
            }
            System.out.println(cards.get(5 * j + 4));
        }
        s.close();
    }

    //Returns ArrayList of best hints, where position i stores the best hint for subsets of size i.
    private static ArrayList<Hint> findBestHints() throws IOException {
        ArrayList<Hint> bestHints = new ArrayList<>(words.size());
    	//Loop through all possible sizes (k) of subsets:
        for (int k=1; k<=words.size(); k++) {
            count = 0;
            bestHints.add(new Hint(0, "", new int[]{0}));
            
            int[] subset = new int[k];
            checkSubsets(subset, 0, 0, bestHints); //Updates bestHints inside method.
            if (bestHints.get(k - 1).prob < GameSettings.SEARCH_CUTOFF) 
            	return bestHints;
        }
        return bestHints;
    }

    //Updates maximums to store the best hint for the the subset size of currIndicesSubset size.
    static void checkSubsets(int[] currIndicesSubset, int currSubsetSize, int nextIndex, ArrayList<Hint> maximums) throws IOException{
        if (currSubsetSize == currIndicesSubset.length) { //If our subset array contains subsetSize (target) # words
        	//Gets a list of all target words to guess:
        	ArrayList<String> targetWords = new ArrayList<String>();
        	for(int index: currIndicesSubset) {
        		targetWords.add(words.get(index));
        	}

            //Creates an array of the words which we must exclude from our search for hints
            ArrayList<String> excluded = new ArrayList<String>(words);
            excluded.addAll(opp);
            
            //NOTE: This bestHintForAllWords is the key method: take a list of words and strings to exclude, return the best hint. Can be modified.
            Hint bestHint = bestHintForAllWordsIn(targetWords, excluded);
            if(bestHint.prob > maximums.get(currIndicesSubset.length-1).prob) {
            	maximums.set(currIndicesSubset.length-1, bestHint);
            }
            
            //Update progress bar:
            count++;
            System.out.print("\r"+"k="+currIndicesSubset.length+":"+(int)Math.ceil(100*(float)count/num_subsets[currSubsetSize]) +"%");

        } else {
        	//If we're here, our "subset" doesn't have enough elements yet, so we add more.
            for (int j = nextIndex; j<words.size(); j++) {

                boolean contains = false;

                for(int i=0; i<currSubsetSize; i++){if(currIndicesSubset[i]==j) contains=true;}
                if(contains) continue;

                currIndicesSubset[currSubsetSize] = j;
                checkSubsets(currIndicesSubset, currSubsetSize + 1, j + 1, maximums);
            }
        }
    }
    
    //Returns the best hint to clue all words in targetWords, excluding substrings of excluded.
    //TODO: Pass in an array of Strings to the following method, not an arrayList.
    private static Hint bestHintForAllWordsIn(ArrayList<String> targetWords, ArrayList<String> excluded) {
    	
    	//Adds the vectors of all words in targetWords:
    	float[] avgVec = new float[300];
    	for(String targetWord: targetWords) {
    		float[] nextVec = util.getVec(targetWord);
    		for(int i=0; i<300; i++) { avgVec[i] += nextVec[i]; }
    	}
    	for(int i=0; i<300; i++) {
    		avgVec[i] *= 1f/(float)(targetWords.size());
    	}
        
        //Finds our candidate hints: the words closest to the averageVec, excluding substrings and superstrings of words on the board.
        ArrayList<WordScore> candidates = util.wordsCloseTo(avgVec, GameSettings.NUM_CANDIDATES, excluded.toArray(new String[excluded.size()]));

        //For all candidates, evaluate probability of hitting target words, and update bestHint if it's good:
        Hint bestHint = new Hint(-1, "", null);
        for(int i=0; i<GameSettings.NUM_CANDIDATES; i++){
            float prob = 1.0f;
            float min_prob = 1.0f;
            
            //Obtain a candidate hint word, with String curr_word and vec hint.
            String curr_word = candidates.get(i).word;
            float[] hint =  util.vectors.get(curr_word);
            
            //For each word in our subset, we check how close our candidate is to that word.
            for(String targetWord: targetWords){
                float[] targetVec = util.getVec(targetWord);
                float curr_prob = prob(util.cosineSimilarity(hint, targetVec));
                prob *= curr_prob;
                if(curr_prob<min_prob) min_prob=curr_prob;
            }

            float max_prob_opp=0; float max_prob_by=0;

            for(String s: opp){
                float curr_prob = prob(util.cosineSimilarity(hint, util.getVec(s)));
                if(curr_prob>max_prob_opp){max_prob_opp = curr_prob;}}

            for(String s: bystanders){
                float curr_prob = prob(util.cosineSimilarity(hint, util.getVec(s)));
                if(curr_prob>max_prob_opp){max_prob_by = curr_prob;}}

            float ass_prob = prob(util.cosineSimilarity(hint, util.getVec(assassin)));

            //If this hint meets constraints and is best, replace bestHint.
            if(ass_prob < GameSettings.ASSASSIN_THRESHOLD && max_prob_opp < GameSettings.OPPONENT_THRESHOLD && max_prob_by < GameSettings.BYSTANDER_THRESHOLD && prob > bestHint.prob) {
                //Convert our list of words to a list of indices, then return.
            	int[] indices = new int[targetWords.size()];
            	for(int j=0; j<targetWords.size(); j++) { indices[j] = words.indexOf(targetWords.get(j)); }
            	bestHint = new Hint(prob, curr_word, indices);
            }
        }
        return bestHint;
    }
    
    public static float prob(float sim){
        float arg = B[0] + sim*B[1];
        return 1.0f/(float)(1+exp(-arg));
    }

    //Stores a given hint:
    static class Hint{
        float prob;
        String word;
        int[] s;

        public Hint(float prob, String word, int[] k){
            this.prob = prob;
            this.word = word;
            this.s = k;
        }

        public String toString() {
            return "\"" + word + "\":" + prob + ":" + Arrays.toString(s);
        }
    }
    
    //Constants you can use to adjust game settings:
    static class GameSettings {
    	//Probability below which you stop searching for clues:
    	static float SEARCH_CUTOFF = 0.5f;
    	
    	//Probabilities above which you avoid clues:
    	static float ASSASSIN_THRESHOLD = 0.3f;
    	static float OPPONENT_THRESHOLD = 0.35f;
    	static float BYSTANDER_THRESHOLD = 0.4f;
    	
    	//Play a board from board.txt (true), or play a random online board (false):
    	static boolean PLAY_BOARD_FROM_FILE = true;
    	
    	//Number of words to search from the Word2Vec database:
    	static int DATABASE_SIZE = 100000;
    	
    	//Number of candidate hints to generate for each subset of words (then checks all these candidates), default 5:
    	static int NUM_CANDIDATES = 10;

    	//If you want the board to print intended cards before or after you guess, or never:
    	
    	//If you want to play the computer and have it update itself:
    }
}

