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

    static float count=0; //A progress bar? I think? -Eric
    final static int[] num_subsets = new int[]{1,8,28,56,70,56,28,8,1}; //TODO: Change this hardcoding. Stores number of subsets of size i at index i.

    static String[] data = new String[404];

    public static void main(String[] args) throws IOException {

        System.out.println("\rRetrieving word vectors...");
        util.getVectors(GameSettings.DATABASE_SIZE);

        System.out.println("Loading board...");
        loadBoardFromFile("board.txt");
        
        if (GameSettings.PLAY_BOARD_FROM_FILE) { loadBoardFromFile("board.txt"); }
        else { loadBoardFromOnline(); }

        System.out.println("Getting hint...");
        ArrayList<Hint> maximums = findBestHints();

        System.out.println();
        //System.out.println(maximums);

        int shift = (maximums.size() > 1) ? 2 : 1;
        Hint final_hint = maximums.get(maximums.size() - shift);
        System.out.println("hint: " + final_hint.word + "," + final_hint.s.length);

        String[] intendend = new String[final_hint.s.length];
        for (int j = 0; j < intendend.length; j++) {intendend[j] = words.get(final_hint.s[j]);}
        System.out.println("intended cards: " + Arrays.toString(intendend));
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

        System.out.print("\rdownloading word set...");
        int i = 0;
        while(s.hasNext()){data[i] = s.next().toLowerCase(); i++;}


        words.clear(); opp.clear();
        System.out.println("\rgenerating game board...");

        int n = 0;
        while (n < data.length) {
            if (opp.size() == 8 && words.size() == 8) break;

            double rand = Math.random();
            if (rand < 0.05) {
                if (util.getVec(data[n]) == null) continue;
                if (data[n].length() < 3) continue;

                if (rand < 0.025) {
                    if (words.size() < 8) words.add(data[n]);
                } else if (opp.size() < 8) {
                    opp.add(data[n]);
                }
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
        Collections.shuffle(cards);

        for (int j = 0; j < 4; j++) {
            for (int x = 0; x < 3; x++) {
                System.out.print(cards.get(4 * j + x) + " ");
            }
            System.out.println(cards.get(4 * j + 3));
        }
        s.close();
    }

    //Returns ArrayList of best hints, where position i stores the best hint for subsets of size i.
    private static ArrayList<Hint> findBestHints() throws IOException {
        ArrayList<Hint> bestHints = new ArrayList<>();
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
    
    
    public static float prob(float sim){
        float arg = B[0] + sim*B[1];
        return 1.0f/(float)(1+exp(-arg));
    }

    static void checkSubsets(int[] currIndicesSubset, int currSubsetSize, int nextIndex, ArrayList<Hint> maximums) throws IOException{
        if (currSubsetSize == currIndicesSubset.length) { //If our subset array contains subsetSize (target) # words

        	//Averages the words from each index of our subset:
            Matrix average = new Matrix(300,1);
            for(int index : currIndicesSubset){
                double[][] curr = new double[300][1];
                float[] curr_f = util.vectors.get(words.get(index));
                for(int i=0; i<300; i++) curr[i][0]=(double)curr_f[i];
                average.plusEquals(new Matrix(curr));
            }
            average.timesEquals((double)1/currSubsetSize);
            float[] average_vec = new float[300];
            for(int i=0; i<300; i++){average_vec[i] = (float)average.get(i,0);}

            //Creates an array of the words which we must exclude from our search for hints
            ArrayList<String> excluded = new ArrayList<String>(words);
            excluded.addAll(opp);
            String[] paramExcluded = new String[excluded.size()];
            
            //Finds our candidate hints: the words closest to the average of our subset, excluding substrings and superstrings of words on the board.
            ArrayList<WordScore> candidates = util.wordsCloseTo(average_vec, GameSettings.NUM_CANDIDATES, excluded.toArray(paramExcluded));

            //For each of our 5 candidate words, evaluate the probability that we 
            //TODO: Let us change how many candidate words we try to find
            for(int i=0; i<GameSettings.NUM_CANDIDATES; i++){
                float prob = 1.0f;
                float min_prob = 1.0f;
                
                //Obtain a candidate hint word, with String curr_word and vec hint. //Why do we skip over some words?
                String curr_word = candidates.get(i).word;
                float[] hint =  util.vectors.get(curr_word);
                
                //For each word in our subset, we check how close our candidate is to that word.
                for(int c: currIndicesSubset){
                    float[] card = util.getVec(words.get(c));
                    float curr_prob = prob(util.cosineSimilarity(hint,card));
                    prob*=curr_prob;
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

                //If this hint meets constraints and beats all probabilities so far for hints of its size (stored in maximums), add it to maximums.
                if(ass_prob < GameSettings.ASSASSIN_THRESHOLD && max_prob_opp < GameSettings.OPPONENT_THRESHOLD && max_prob_by < GameSettings.BYSTANDER_THRESHOLD && prob > maximums.get(currSubsetSize-1).prob){
                    //Make a duplicate copy of subset, called k, and add that to maximums.
                	int[] k = new int[currSubsetSize];
                    for(int m=0; m<currSubsetSize; m++){k[m]=currIndicesSubset[m];}
                    maximums.set(currSubsetSize-1, new Hint(prob, curr_word, k));
                }
            }

            //Update progress bar:
            count++;
            System.out.print("\r"+"k="+currIndicesSubset.length+":"+(int)Math.ceil(100*(float)count/num_subsets[currSubsetSize])
                    +"%");

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
    	static float SEARCH_CUTOFF = 0.125f;
    	
    	//Probabilities above which you avoid clues:
    	static float ASSASSIN_THRESHOLD = 0.3f;
    	static float OPPONENT_THRESHOLD = 0.35f;
    	static float BYSTANDER_THRESHOLD = 0.4f;
    	
    	//Play a board from board.txt (true), or play a random online board (false):
    	static boolean PLAY_BOARD_FROM_FILE = true;
    	
    	//Number of words to search from the Word2Vec database:
    	static int DATABASE_SIZE = 100000;
    	
    	//Number of candidate hints to generate for each subset of words (then checks all these candidates), default 5:
    	static int NUM_CANDIDATES = 5;
    	
    	//If you want the board to print intended cards before or after you guess, or never:
    	
    	//If you want to play the computer and have it update itself:
    }
}

