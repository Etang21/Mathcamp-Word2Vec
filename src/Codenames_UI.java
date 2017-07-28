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

    static int[] num_subsets;

    static float count=0; //A progress bar? I think? -Eric

    public static Word2VecUtility util = new Word2VecUtility();
    static ArrayList<String> words = new ArrayList<>();
    static ArrayList<String> opp = new ArrayList<>();
    static ArrayList<String> bystanders = new ArrayList<>();
    static String assassin;

    static ArrayList<Hint> maximums = new ArrayList<>();

    static String[] data = new String[404];

    public static void main(String[] args) throws IOException {

        while(true) {
            FileWriter fw = new FileWriter("threshold_data.txt", true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);

            words.clear(); opp.clear(); bystanders.clear(); assassin=""; maximums.clear();
            util.getVectors(100000);

            //Online functionality is currently broken, so commented out:
            if (GameSettings.PLAY_BOARD_FROM_FILE) {
                loadBoardFromFile("board.txt");
            } else {loadBoardFromOnline();}

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

            System.out.print("\rgetting hint...");
            for (int k = 1; k <= words.size(); k++) { //Loops through all sizes of subsets?
                count = 0;
                maximums.add(new Hint(0, "", new int[]{0}));

                int[] subset = new int[k]; //The chosen words of a subset?
                checkSubsets(words.size(), subset, 0, 0);

                if (maximums.get(k - 1).prob < 0.125) break;
            }

            System.out.println();
            //System.out.println(maximums);

            Scanner in = new Scanner(System.in);
            int shift = (maximums.size() > 1) ? 2 : 1;
            Hint final_hint = maximums.get(maximums.size() - shift);
            System.out.println("hint: " + final_hint.word + "," + final_hint.s.length);

            for (int j = 0; j < final_hint.s.length; j++) {
                System.out.print("pick " + j + ":");
                String pick = in.nextLine();
                if (!words.contains(pick)) {
                    System.out.println("\rincorrect!");
                    out.print((j+1)+""+0+",");
                    break;
                }
                if (j == final_hint.s.length - 1) {
                    System.out.println("\rcorrect!");
                    out.print(final_hint.s.length+""+1+",");
                }
            }

            String[] intended = new String[final_hint.s.length];
            for (int j = 0; j < intended.length; j++) {intended[j] = words.get(final_hint.s[j]);}
            System.out.println("intended cards: " + Arrays.toString(intended));

            System.out.print("quit? "); if(!in.nextLine().equals("")) break;
            System.out.println();

            out.close();

        }
    }
    
    //MARK: Input board methods
    
    //Scans board from fileName, populates our ArrayLists:
    private static void loadBoardFromFile(String fileName) {
    	try {
	    	File file = new File(fileName);
	        Scanner boardInput = new Scanner(file);

	        words.clear(); opp.clear(); maximums.clear();
	
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

    //FIXME: Fix this following function. Not hugely important, though.
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

    public static float prob(float sim){
        float arg = B[0] + sim*B[1];
        return 1.0f/(float)(1+exp(-arg));
    }

    static void checkSubsets(int size, int[] subset, int subsetSize, int nextIndex) throws IOException{
        if (subsetSize == subset.length) {

        	//Averages the words from each index of our subset:
            Matrix average = new Matrix(300,1);
            for(int index : subset){
                double[][] curr = new double[300][1];
                float[] curr_f = util.vectors.get(words.get(index));
                for(int i=0; i<300; i++) curr[i][0]=(double)curr_f[i];
                average.plusEquals(new Matrix(curr));
            }
            average.timesEquals((double)1/subsetSize);
            float[] average_vec = new float[300];
            for(int i=0; i<300; i++){average_vec[i] = (float)average.get(i,0);}

            //Creates an array of the words which we must exclude from our search for hints
            ArrayList<String> excluded = new ArrayList<String>(words);
            excluded.addAll(opp);
            String[] paramExcluded = new String[excluded.size()];
            
            //Finds our candidate hints: the 5 words closest to the average of our subset
            //These five words exclude substrings and superstrings of words on the board.
            ArrayList<WordScore> candidates = util.wordsCloseTo(average_vec, 10, excluded.toArray(paramExcluded));

            //For each of our 5 candidate words, evaluate the probability that we 
            for(int i=0; i<10; i++){
                float prob = 1.0f;
                float min_prob = 1.0f; //used to track card in subset with minimum similarity probability
                
                //Obtain a candidate hint word, with String curr_word and vec hint.
                String curr_word = candidates.get(i).word;
                float[] hint =  util.vectors.get(curr_word);
                
                //For each word in our subset, we check how our candidate is to that word.
                for(int c: subset){
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

                if(ass_prob < GameSettings.ASSASSIN_THRESHOLD && max_prob_opp < GameSettings.OPPONENT_THRESHOLD && max_prob_by < GameSettings.BYSTANDER_THRESHOLD && prob > maximums.get(subsetSize-1).prob){
                    int[] k = new int[subsetSize];
                    for(int m=0; m<subsetSize; m++){k[m]=subset[m];}
                    maximums.set(subsetSize-1, new Hint(prob, curr_word, k));
                }
            }

            count++;
            System.out.print("\r"+"k="+subset.length+":"+(int)Math.ceil(100*(float)count/num_subsets[subsetSize])
                    +"%");

        } else {
            for (int j = nextIndex; j<size; j++) {

                boolean contains = false;

                for(int i=0; i<subsetSize; i++){if(subset[i]==j) contains=true;}
                if(contains) continue;

                subset[subsetSize] = j;
                checkSubsets(size, subset, subsetSize + 1, j + 1);
            }
        }
    }

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

    	static boolean PLAY_BOARD_FROM_FILE = false; //THIS FUNCTIONALITY IS CURRENTLY BROKEN.
    	
    	//If you want the board to print intended cards before or after you guess, or never:
    	
    	//If you want to play the computer and have it update itself:
    }
}

