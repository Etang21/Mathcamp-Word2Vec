import Jama.Matrix;
import org.json.*;

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

    static int[] num_subsets;

    public static Word2VecUtility util = new Word2VecUtility();

    static ArrayList<String> words = new ArrayList<>();
    static ArrayList<String> opp = new ArrayList<>();
    static ArrayList<String> bystanders = new ArrayList<>();
    static String assassin;

    static GameSettings game_set;
    static float count=0; //for progress bar

    public static void main(String[] args) throws IOException {

        int[] search_para = new int[]{100000,10};
        TrainState ts = new TrainState("training_settings.txt");

        game_set = new GameSettings(ts.curr_settings, search_para, false);

        util.getVectors(game_set.DATABASE_SIZE);

        while(true){
            if(ts.it%(2*ts.n)==0 && ts.it>0){
                ts.gradient[(ts.it%(10*ts.n))/(2*ts.n)-1] = 0.05f*(ts.score[1]-ts.score[0])/ts.adjustments[(ts
                        .it%(10*ts.n))/(2*ts.n)-1];
                System.out.println("current parameter:"+(ts.it%(10*ts.n))/(2*ts.n));
                System.out.println(Arrays.toString(ts.gradient));
            }

            if(ts.it%(10*ts.n)==0){
                for(int i=0; i<5;i++){ts.curr_settings[i]+=ts.alpha*ts.gradient[i];}
                System.out.println(Arrays.toString(ts.curr_settings));
            }

            if(ts.it%ts.n==0){
                int param = (ts.it%(10*ts.n))/(2*ts.n);

                if((ts.it%(2*ts.n))/(ts.n)==1){ts.curr_settings[param] += ts.adjustments[param];}
                else{ts.curr_settings[param] -= ts.adjustments[param];}

                System.out.println("current settings:"+Arrays.toString(ts.curr_settings));

                game_set = new GameSettings(ts.curr_settings,search_para, false);

                if((ts.it%(2*ts.n))/(ts.n)==1){ts.curr_settings[param] -= ts.adjustments[param];}
                else{ts.curr_settings[param] += ts.adjustments[param];}
            }

            System.out.println(ts.it%ts.n+1 + "/"+ts.n);

//            FileWriter fw = new FileWriter("threshold_data.txt", true);
//            BufferedWriter bw = new BufferedWriter(fw);
//            PrintWriter out = new PrintWriter(bw);

            util.getVectors(game_set.DATABASE_SIZE);

            words.clear();
            opp.clear();
            bystanders.clear();
            assassin = "";

            if (game_set.PLAY_BOARD_FROM_FILE) {
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
            ArrayList<Hint> maximums = findBestHints();
            System.out.println();
            Scanner in = new Scanner(System.in);
            int shift = (maximums.size() > 1) ? 2 : 1;
            Hint final_hint = maximums.get(maximums.size() - shift);
            System.out.println("hint: " + final_hint.word + "," + final_hint.s.length);

            for (int j = 0; j < final_hint.s.length; j++) {
                System.out.print("pick " + j + ":");
                String pick = in.nextLine();
                if (!words.contains(pick)) {
                    System.out.println("incorrect!");
                    //score[(it%40)/20]+=(j-1);
                    //out.print((j) + "" + 0 + ",");
                    break;
                }
                if (j == final_hint.s.length - 1) {
                    System.out.println("correct!");
                    //score[(it%40)/20]+=(final_hint.s.length);
                    //out.print(final_hint.s.length + "" + 1 + ",");
                }
            }

            String[] intended = new String[final_hint.s.length];
            for (int j = 0; j < intended.length; j++) {
                intended[j] = words.get(final_hint.s[j]);
            }
            System.out.println("intended cards: " + Arrays.toString(intended));

            System.out.print("score increase:"); int inc = Integer.parseInt(in.nextLine());

            if(inc!=-2) {
                ts.score[(ts.it % (2 * ts.n)) / ts.n] += inc;

                System.out.println(ts.score[(ts.it % (2 * ts.n)) / ts.n] / (ts.it % ts.n + 1));
                ts.it++;
            }

            System.out.print("quit? ");
            if (!in.nextLine().equals("")){
                ts.save_settings("training_settings.txt");
                break;
            }

            System.out.println();
            //out.close();
        }
    }

    //MARK: Input board methods

    //Scans board from fileName, populates our ArrayLists:
    private static void loadBoardFromFile(String fileName) {
    	try {
	    	File file = new File(fileName);
	        Scanner boardInput = new Scanner(file);

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

        String[] data = new String[404];

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
                bystanders.clear();
                assassin="";
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

    public static float prob(float sim) {
        float arg = game_set.B[0] + sim * game_set.B[1];
        return 1.0f / (float) (1 + exp(-arg));
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
            if (bestHints.get(k - 1).prob < game_set.SEARCH_CUTOFF)
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
        ArrayList<WordScore> candidates = util.wordsCloseTo(avgVec, game_set.NUM_CANDIDATES, excluded.toArray(new
                String[excluded.size()]));

        //For all candidates, evaluate probability of hitting target words, and update bestHint if it's good:
        Hint bestHint = new Hint(-1, "", null);
        for(int i=0; i<game_set.NUM_CANDIDATES; i++){
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
            if(ass_prob < game_set.ASSASSIN_THRESHOLD && max_prob_opp < game_set.OPPONENT_THRESHOLD && max_prob_by <
                    game_set.BYSTANDER_THRESHOLD && prob > bestHint.prob) {
                //Convert our list of words to a list of indices, then return.
            	int[] indices = new int[targetWords.size()];
            	for(int j=0; j<targetWords.size(); j++) { indices[j] = words.indexOf(targetWords.get(j)); }
            	bestHint = new Hint(prob, curr_word, indices);
            }
        }
        return bestHint;
    }
}

class Hint{
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

class TrainState{

    String jsonString;

    float alpha=0.5f;
    float[] gradient = new float[5];
    float[] curr_settings = new float[]{0.55f, 0.4f,0.2f, 0.25f,0.3f};
    float[] adjustments = new float[]{0.03f, 0.03f, 0.025f, 0.025f, 0.025f};

    int n=10;
    float[] score = new float[2];
    int it = 0;

    public TrainState(String file){load_settings(file);}

    private void load_settings(String file){
        try {
            File json = new File(file);
            Scanner load = new Scanner(json);
            jsonString ="";

            while(load.hasNext()){jsonString+=load.next();}
            System.out.println(jsonString);

            JSONObject settings = new JSONObject(jsonString);
            alpha = (float)settings.getDouble("alpha");

            for(int i=0; i<5; i++){
                gradient[i] = (float)settings.getJSONArray("gradient").getDouble(i);
                curr_settings[i] = (float)settings.getJSONArray("curr_settings").getDouble(i);
                adjustments[i] = (float)settings.getJSONArray("adjust").getDouble(i);
            }
            n = (int)settings.getInt("samp_size");
            it = (int)settings.getInt("it");

            score[0] = settings.getJSONArray("score").getInt(0);
            score[1] = settings.getJSONArray("score").getInt(1);

        }catch(FileNotFoundException e){
            System.out.println(e);
            System.out.println("loading default settings");
        }
    }

    void save_settings(String file) {
        JSONObject settings = new JSONObject(jsonString);
        settings.put("it", it);
        settings.getJSONArray("score").put(0, score[0]);
        settings.getJSONArray("score").put(1, score[1]);

        for (int i = 0; i < 5; i++) {
            settings.getJSONArray("curr_settings").put(i, curr_settings[i]);
            settings.getJSONArray("adjust").put(i, adjustments[i]);
            settings.getJSONArray("gradient").put(i, gradient[i]);
        }

        try {
            PrintWriter pw = new PrintWriter(new FileOutputStream(file, false));
            pw.write(settings.toString());
            pw.close();
        } catch (FileNotFoundException e) {
            System.out.println(e);
        }
    }
}

//Constants you can use to adjust game settings:
class GameSettings {

    final float[] B = new float[]{-3.55106049399243f, 11.8332877194120f};

    //Probability below which you stop searching for clues:
    float SEARCH_CUTOFF = 0.55f;
    float MIN_PROB = 0.4f;

    //Probabilities above which you avoid clues:
    float ASSASSIN_THRESHOLD = 0.20f;
    float OPPONENT_THRESHOLD = 0.25f;
    float BYSTANDER_THRESHOLD = 0.3f;

    //Play a board from board.txt (true), or play a random online board (false):
    boolean PLAY_BOARD_FROM_FILE = false;

    //Number of words to search from the Word2Vec database:
    int DATABASE_SIZE = 100000;

    //Number of candidate hints to generate for each subset of words (then checks all these candidates), default 5:
    int NUM_CANDIDATES = 10;

    //If you want the board to print intended cards before or after you guess, or never:

    //If you want to play the computer and have it update itself:

    public GameSettings(float[] param, int[] search_size, boolean mode){
        SEARCH_CUTOFF=param[0];
        MIN_PROB=param[1];
        ASSASSIN_THRESHOLD=param[2];
        OPPONENT_THRESHOLD=param[3];
        BYSTANDER_THRESHOLD=param[4];

        DATABASE_SIZE = search_size[0];
        NUM_CANDIDATES = search_size[1];

        PLAY_BOARD_FROM_FILE = mode;
    }

    public GameSettings(){}
}
