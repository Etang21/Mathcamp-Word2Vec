/**
 * Created by dawsonbyrd on 7/20/17.
 * 
 * This class runs the Codenames board game on a JFrame interface. It provides a computer
 * spymaster to give clues, and allows the user to play solo with the computer. The spymaster
 * is powered by vectors from Word2Vec trained on the Google News corpus.
 */

import org.json.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import static java.lang.Math.exp;


public class Codenames_UI{

    private JFrame mainFrame;

    private JLabel headerLabel;
    private JLabel statusLabel;
    private JPanel controlPanel;

    private JLabel hint;
    private JLabel score_show;

    private JPanel board;
    private JPanel bottom;

    int score=0;

    boolean choosing = false;

    ArrayList<Card> cards = new ArrayList<>();

    public static String MODE = "START";

    static int[] num_subsets;

    public static Word2VecUtility util = new Word2VecUtility();


    //9 one color, 8 another, 1 assassin, 7 bystanders
    ArrayList<String> ourWords = new ArrayList<>();
    ArrayList<String> oppWords = new ArrayList<>();
    ArrayList<String> bystanders = new ArrayList<>();
    String assassin;

    ArrayList<String> clues = new ArrayList<>();

    static GameSettings game_set;
    static float count=0; //for progress bar

    public Codenames_UI(){prepareGUI();}

    private void prepareGUI(){
        mainFrame = new JFrame("Codenames");
        mainFrame.setSize(700,500);
        mainFrame.setLayout(new GridLayout(3, 1));
        mainFrame.setResizable(false);

        mainFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent){
                System.exit(0);
            }
        });
        headerLabel = new JLabel("", JLabel.CENTER);
        headerLabel.setFont(new Font("Tahoma", Font.BOLD, 35));
        statusLabel = new JLabel("word2vec project group - Mathcamp 2017",JLabel.CENTER);

        hint = new JLabel("", JLabel.CENTER);
        score_show = new JLabel("<html><div style='text-align: center;'> <b>score: </b>0</div></html>");

        bottom = new JPanel();
        bottom.setLayout(new FlowLayout());
        bottom.setPreferredSize(new Dimension(700,30));
        bottom.setMaximumSize(bottom.getPreferredSize());
        bottom.setOpaque(true);
        bottom.setBackground(new Color(204,243,255));

        bottom.add(score_show);
        bottom.add(hint);

        controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());
        controlPanel.setFont(new Font("Tahoma", Font.PLAIN, 12));

        board = new JPanel();
        board.setLayout(new GridLayout(5,5));

        mainFrame.add(headerLabel);
        mainFrame.add(controlPanel);
        mainFrame.add(statusLabel);
        mainFrame.setVisible(true);
    }

    /** Run main menu with all menu options. */
    private void start(){
        headerLabel.setText("Codenames AI");

        JButton play = new JButton("Play");
        JButton settings = new JButton("Settings");
        JButton save = new JButton("Save");
        JButton train =  new JButton("Train");
        save.setHorizontalTextPosition(SwingConstants.LEFT);

        play.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mainFrame.getContentPane().removeAll();
                mainFrame.setLayout(new BoxLayout(mainFrame.getContentPane(), BoxLayout.PAGE_AXIS));
                mainFrame.add(board);
                mainFrame.add(bottom);

                mainFrame.setVisible(true);
                mainFrame.validate();
                mainFrame.repaint();

                MODE="GAME";
                Thread run_game = new Thread(new Runnable() {
                    @Override
                    public void run(){try{game();}catch (IOException e){e.printStackTrace();}}
                });
                run_game.start();
            }
        });
        settings.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                statusLabel.setText("Submit Button clicked.");
            }
        });
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                statusLabel.setText("Cancel Button clicked.");
            }
        });

        train.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainFrame.setVisible(false);

                MODE="TRAIN";
                Thread run_game = new Thread(new Runnable() {
                    @Override
                    public void run() {try{train();}catch (IOException e){e.printStackTrace();}}
                });
                run_game.start();
            }
        });
        controlPanel.add(play);
        controlPanel.add(train);
        controlPanel.add(settings);
        controlPanel.add(save);

        mainFrame.setVisible(true);
    }

    /** Start the Codenames game with computer spymaster. */
    private void game() throws IOException{
    		game_set = loadGameSettings();
        util.getVectors(game_set.DATABASE_SIZE);
        boolean start_game = true;

        while(true) {
            if(start_game) {
                start_game=false;
                loadInitialUI();
            }

            num_subsets = new int[ourWords.size() + 1];
            for (int k = 0; k < num_subsets.length; k++) {
                int result = 1;
                for (int i = 0; i < k; i++) {
                    result *= (ourWords.size() - i);
                }
                for (int i = 1; i <= k; i++) {
                    result /= i;
                }
                num_subsets[k] = result;
            }

            //Get array of best hints, select the second largest one:
            ArrayList<Hint> maximums = findBestHints();
            int shift = (maximums.size() > 1) ? 2 : 1;
            Hint final_hint = maximums.get(maximums.size() - shift);

            //Store intended words:
            String[] intended = new String[final_hint.targetIndices.length];
            for (int i = 0; i < final_hint.targetIndices.length; i++) {
                intended[i] = ourWords.get(final_hint.targetIndices[i]);
            }
            
            //Print clue:
            clues.add(final_hint.word);
            hint.setText("<html><div style='text-align: center;'> <b>hint: </b>" + final_hint.word + "," + final_hint
                    .targetIndices.length + "</div></html>");

            //User chooses cards:
            for (int j = 0; j < final_hint.targetIndices.length; j++) {
                String pick="";
                choosing = true;
                while(choosing){
                    for(Card c : cards){
                        if(c.state==1){
                            pick = c.getText();
                            c.state=2;
                            choosing = false;
                            break;
                        }
                    }
                }
                if (!ourWords.contains(pick)) {
                    System.out.println("incorrect!");
                    if(pick.equals(assassin))assassin="";

                    bystanders.remove(pick);
                    oppWords.remove(pick);
                    break;
                }
                ourWords.remove(pick);
                if (j == final_hint.targetIndices.length - 1) {
                    System.out.println("correct!");
                }
                score++;
                score_show.setText("<html><div style='text-align: center;'> <b>score: </b>" + score + "</div></html>");
            }
            
            System.out.println("intended cards: " + Arrays.toString(intended));
        }
    }
    
    private void loadInitialUI() throws IOException {
        cards.clear();
        ourWords.clear();
        oppWords.clear();
        bystanders.clear();
        assassin = "";

        if (game_set.PLAY_BOARD_FROM_FILE) {
            loadBoardFromFile("board.txt");
        } else {loadBoardFromOnline();}

        board.removeAll();
        board.revalidate();
        board.repaint();

        for(String s : ourWords) cards.add(new Card(s, "ours"));
        for(String s : oppWords) cards.add(new Card(s, "opp"));
        for(String s : bystanders) cards.add(new Card(s, "by"));
        cards.add(new Card(assassin, "assn"));

        Collections.shuffle(cards);

        for(int m=0; m<25; m++){board.add(cards.get(m));}
        mainFrame.setVisible(true);
        mainFrame.validate();
        mainFrame.repaint();
    }

    /** Loads game settings from file data. */
    private GameSettings loadGameSettings() throws IOException {
    		int[] search_para = new int[]{100000,10};
        File file = new File("game_settings.txt");
        Scanner settings = new Scanner(new FileInputStream(file));
        float[] param = new float[5];
        for(int i=0; i<5; i++) {
        		param[i] = settings.nextFloat();
        	}
        settings.close();
        return new GameSettings(param, search_para, false);
    }
    
    public static void main(String[] args) throws IOException {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Codenames_UI  display = new Codenames_UI();
                display.start();
            }
        });
    }

    /**Scans board from fileName, populates our ArrayLists with word data */
    private void loadBoardFromFile(String fileName) {
    	try {
	    	File file = new File(fileName);
	        Scanner boardInput = new Scanner(file);

            int num_words = Integer.parseInt(boardInput.nextLine());
	        for(int k=0; k<num_words; k++){
	            String curr = boardInput.next();
	            ourWords.add(curr);}

	        int num_opp = Integer.parseInt(boardInput.next());
	        for(int k=0; k<num_opp; k++){
	            String curr = boardInput.next();
	            oppWords.add(curr);}

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

    /** Scans board from online list of random Codenames words, populates our ArrayLists with words. */
    private ArrayList<String> loadBoardFromOnline() throws IOException {
    		URL url = new URL("https://raw.githubusercontent.com/jbowens/codenames/master/assets/original.txt");
        URLConnection con = url.openConnection();
        con.setConnectTimeout(5000);
        con.setReadTimeout(20000);
        Scanner s = new Scanner(con.getInputStream());

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
            if (oppWords.size() == 17-word_size && ourWords.size() == word_size && bystanders.size()==7 && !assassin.equals(""))
                break;

            double rand = Math.random();
            if (rand < 0.1) {
                if (util.getVec(data[n]) == null) continue;
                if (data[n].length() < 3) continue;
                if (rand < 0.025) {
                    if (ourWords.size() < word_size) ourWords.add(data[n]);
                } else if (rand<0.05){
                    if(oppWords.size() < 17-word_size) oppWords.add(data[n]);
                } else if(rand<0.075) if(bystanders.size()<7) bystanders.add(data[n]);

                else if(assassin.equals(""))assassin = data[n];
            }

            if (n == data.length - 1) {
                n = 0;
                oppWords.clear();
                ourWords.clear();
                bystanders.clear();
                assassin="";
            } else n++;
        }
        ArrayList<String> card_set = new ArrayList<String>();
        card_set.addAll(ourWords);
        card_set.addAll(oppWords);
        card_set.addAll(bystanders);
        card_set.add(assassin);
        Collections.shuffle(card_set);

        s.close();

        return card_set;
    }

    /** Returns the estimated probability that the user will consider two words similar, given their
     * cosine similarity. */
    public float prob(float sim) {
        float arg = game_set.B[0] + sim * game_set.B[1];
        return 1.0f / (float) (1 + exp(-arg));
    }

    /** Returns ArrayList of best hints for current board. Position i stores the best hint for subsets of size i. */
    private ArrayList<Hint> findBestHints() {
        ArrayList<Hint> bestHints = new ArrayList<>(ourWords.size());
        ArrayList<String> excluded = excludedWords();
        
        //Loop through all possible sizes of subsets
        for (int k=1; k<=ourWords.size(); k++) {
            count = 0;
            bestHints.add(bestHintForNumWords(k, excluded));
            if (bestHints.get(k - 1).prob < Math.pow(game_set.SEARCH_CUTOFF,k)) {
            		return bestHints; //Cut off search if too improbable
            }
        }
        return bestHints;
    }
    
    /** Returns the best hint that clues for numWords number of words in ourWords. Excludes words in excluded.
     * Also updates progress bar in the UI. */
    private Hint bestHintForNumWords(int numWords, ArrayList<String> excluded) {
    		ArrayList<int[]> indexSubsets = allIndexSubsetsOfSize(numWords);
    		Hint bestHint = new Hint(0, "", new int[]{0});
    		for (int[] indices : indexSubsets) {
    			Hint candidateHint = bestHintForIndices(indices, excluded);
    			if (candidateHint.prob > bestHint.prob) {
    				bestHint = candidateHint;
    			}
    			
    			//Update progress bar:
            count++;
            hint.setText("\r"+"k="+ numWords +":"+(int)Math.ceil(100*(float)count/num_subsets[numWords]) + "%");
    		}
    		return bestHint;
    }
    
    /** Returns ArrayList of subsets of indices into ourWords, representing all subsets of ourWords of the given size. */
    private ArrayList<int[]> allIndexSubsetsOfSize(int size) {
    		ArrayList<int[]> allSubsets = new ArrayList<int[]>();
    		int[] indices = new int[size];
    		allIndexSubsetsHelper(indices, size, allSubsets);
    		return allSubsets;
    }
    
    /** Helper function to generate all subsets of the indices of ourWords.
     * @param currIndices The current, possibly partial, subset
     * @param numChoicesLeft The number of indices we still need to add to the subset
     * @param allSubsets The final ArrayList of subsets which we add to
     */
    private void allIndexSubsetsHelper(int[] currIndices, int numChoicesLeft, ArrayList<int[]> allSubsets) {
    		if (numChoicesLeft == 0) { //Base case: subset full
    			allSubsets.add(currIndices.clone());
    		} else { //Recursive case: Pick remaining elements, starting from our previous pick
	    		int prevChoice = 0;
	    		if (numChoicesLeft != currIndices.length) {
	    			prevChoice = currIndices[currIndices.length - numChoicesLeft - 1];
	    		}
	    		for (int j = prevChoice + 1; j < ourWords.size(); j++) {
	    			currIndices[currIndices.length - numChoicesLeft] = j;
	    			allIndexSubsetsHelper(currIndices, numChoicesLeft - 1, allSubsets);
	    			currIndices[currIndices.length - numChoicesLeft] = 0;
	        }
    		}
    }
   
    /** Returns the best hint to clue for all words in ourWords at the targetIndices.
     *  Excludes substrings/superstrings of excluded.*/
    private Hint bestHintForIndices(int[] targetIndices, ArrayList<String> excluded) {
	    	ArrayList<String> targetWords = new ArrayList<String>();
	    	for(int index: targetIndices) {
	    		targetWords.add(ourWords.get(index));
	    	}
	    	return bestHintForWords(targetWords, excluded);
    }
    
    /** Returns the best hint to clue for all words in targetWords, excluding substrings/superstrings of excluded.*/
    private Hint bestHintForWords(ArrayList<String> targetWords, ArrayList<String> excluded) {
        //Finds our candidate hints: the words closest to the sum/average of our target words. Empirically works best.
        float[] avgVec = averageVectorFor(targetWords);
        ArrayList<WordScore> candidates = 
        		util.wordsCloseTo(avgVec, game_set.NUM_CANDIDATES, excluded.toArray(new String[excluded.size()]));

        //For all candidates, evaluate distance to target words and opponent words. Update bestHint if best.
        Hint bestHint = new Hint(-1, "", null);
        for (int i = 0; i < game_set.NUM_CANDIDATES; i++) {
            String curr_word = candidates.get(i).word;
            float[] hint = util.vectors.get(curr_word);
            float hintProb = hintProbability(hint, targetWords, game_set);

            if (hintProb > bestHint.prob && isSafeHint(hint, game_set)) {
                int[] indices = indicesOfTargets(targetWords);
                bestHint = new Hint(hintProb, curr_word, indices);
            }
        }
        return bestHint;
    }
    
	/** Returns an array of the words which we must exclude from our search for hints */
    private ArrayList<String> excludedWords() {
        ArrayList<String> excluded = new ArrayList<String>(ourWords);
        excluded.addAll(oppWords);
        excluded.addAll(ourWords);
        excluded.addAll(bystanders);
        excluded.addAll(clues);
        return excluded;
    }
    
    /** Returns an array containing the indices in words of our ArrayList of target words
     * 	Precondition: words contains all words in targetWords */
    private int[] indicesOfTargets(ArrayList<String> targetWords) {
    		int[] indices = new int[targetWords.size()];
        for (int j = 0; j < targetWords.size(); j++) {
            indices[j] = ourWords.indexOf(targetWords.get(j));
        }
        return indices;
    }
    
    /** Returns component-wise average of all vectors for words in targetWords */
    private float[] averageVectorFor(ArrayList<String> targetWords) {
    		//Note: Could normalize these before averaging. Or just sum - no need to average?
	    	float[] avgVec = new float[300];
        for (String targetWord : targetWords) {
            float[] nextVec = util.getVec(targetWord);
            for (int i = 0; i < 300; i++) {
                avgVec[i] += nextVec[i];
            }
        }
        for (int i = 0; i < 300; i++) {
            avgVec[i] *= 1f / (float) (targetWords.size());
        }
        return avgVec;
    }

    /** Returns true if our hint is a safe distance away from opponents, bystanders, and assassin.
     *  Thresholds for safety are defined in the GameSettings we pass in. */
    private boolean isSafeHint(float[] hint, GameSettings game_set) {
        for (String s : oppWords) {
            if (prob(util.cosineSimilarity(hint, util.getVec(s))) > game_set.OPPONENT_THRESHOLD) {
            		return false;
            }
        }

        for (String s : bystanders) {
	        	if (prob(util.cosineSimilarity(hint, util.getVec(s))) > game_set.BYSTANDER_THRESHOLD) {
	        		return false;
	        }
        }
        return assassin.equals("") || prob(util.cosineSimilarity(hint, util.getVec(assassin))) < game_set.ASSASSIN_THRESHOLD;
    }
    
    /** Returns the estimated probability that user guesses all the words in targetWords from hint.
     * Returns 0 if any estimated probability is less than GameSettings minimum probability threshold. */
    private float hintProbability(float[] hint, ArrayList<String> targetWords, GameSettings game_set) {
        float prob = 1.0f;
    		for (String targetWord : targetWords) {
            float[] targetVec = util.getVec(targetWord);
            float curr_prob = prob(util.cosineSimilarity(hint, targetVec));
            if (curr_prob < game_set.MIN_PROB) {
        			return 0f;
            }
            prob *= curr_prob;
        }
    		return prob;
    }
    
    /** Used to train our Codenames spymaster to improve over time. Incomplete/broken feature. */
    private void train() throws IOException{

        int[] search_para = new int[]{100000,10};
        TrainState ts = new TrainState("training_settings.txt");

        game_set = new GameSettings(ts.curr_settings, search_para, false);

        util.getVectors(game_set.DATABASE_SIZE);

        while(true){
            int param = (ts.it%(10*ts.n))/(2*ts.n);

            if(ts.it%(2*ts.n)==0 && ts.it>0){
                ts.curr_settings[param-1]-= ts.adjustments[param];

                ts.gradient[(ts.it%(10*ts.n))/(2*ts.n)-1] = 0.05f*(ts.score[1]-ts.score[0])/ts.adjustments[(ts
                        .it%(10*ts.n))/(2*ts.n)-1];
                System.out.println("current parameter:"+(ts.it%(10*ts.n))/(2*ts.n));
                System.out.println(Arrays.toString(ts.gradient));

                ts.score[0]=0; ts.score[1]=0;
            }

            if(ts.it%(10*ts.n)==0){
                for(int i=0; i<5;i++){ts.curr_settings[i]+=ts.alpha*ts.gradient[i];}
                System.out.println(Arrays.toString(ts.curr_settings));
            }

            if(ts.it%ts.n==0){

                if((ts.it%(2*ts.n))/(ts.n)==1){ts.curr_settings[param] += 2*ts.adjustments[param];}
                else{ts.curr_settings[param] -= ts.adjustments[param];}

                System.out.println("current settings:"+Arrays.toString(ts.curr_settings));

                game_set = new GameSettings(ts.curr_settings,search_para, false);
            }

            System.out.println(ts.it%ts.n+1 + "/"+ts.n);
            System.out.println(game_set.SEARCH_CUTOFF + "," + game_set.MIN_PROB);

            util.getVectors(game_set.DATABASE_SIZE);

            ourWords.clear();
            oppWords.clear();
            bystanders.clear();
            assassin = "";

            System.out.println("GA");

            ArrayList<String> card_set = new ArrayList<>();

            if (game_set.PLAY_BOARD_FROM_FILE) {
                loadBoardFromFile("board.txt");
            } else {card_set=loadBoardFromOnline();}

            for (int j = 0; j < 5; j++) {
                for (int x = 0; x < 4; x++) {
                    System.out.print(card_set.get(5 * j + x) + " ");
                }
                System.out.println(card_set.get(5 * j + 4));
            }

            num_subsets = new int[ourWords.size() + 1];
            for (int k = 0; k < num_subsets.length; k++) {
                int result = 1;
                for (int i = 0; i < k; i++) {
                    result *= (ourWords.size() - i);
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
            System.out.println("hint: " + final_hint.word + "," + final_hint.targetIndices.length);

            String[] intended = new String[final_hint.targetIndices.length];
            for (int j = 0; j < intended.length; j++) {
                intended[j] = ourWords.get(final_hint.targetIndices[j]);
            }
            System.out.println("intended cards: " + Arrays.toString(intended));
            System.out.println("ours: " + ourWords);
            System.out.println("opp: " + oppWords);

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
            in.close();
        }
    }
    
    class Card extends JButton{
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		String type;
        int state = 0;

        public Card(String word, String type) {
            super(word); this.type = type;
            this.setFont(new Font("Tahoma", Font.BOLD, 16));

            this.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(choosing==true) {
                        Card source = (Card) e.getSource();
                        source.setOpaque(true);
                        source.setBorderPainted(false);

                        if (state != 2) {state = 1;}
                        switch (source.type) {
                            case "ours":
                                source.setForeground(Color.white);
                                source.setBackground(new Color(31, 150, 124));
                                break;
                            case "opp":
                                source.setForeground(Color.white);
                                source.setBackground(new Color(158, 30, 44));
                                break;
                            case "by":
                                source.setForeground(Color.white);
                                source.setBackground(new Color(160, 132, 19));
                                break;
                            case "assn":
                                source.setForeground(Color.white);
                                source.setBackground(new Color(119, 71, 107));
                                break;
                        }
                    }
                }
            });
        }
    }
}

/**
 * word: the word we are using as our hint
 * targetIndices: the indexes of the target words in ourWords which we are clueing for
 * prob: estimated probability that the user thinks all those words are similar to our hint word.
 */
class Hint {
    float prob;
    String word;
    int[] targetIndices;

    public Hint(float prob, String word, int[] k){
        this.prob = prob;
        this.word = word;
        this.targetIndices = k;
    }

    public String toString() {
        return "\"" + word + "\":" + prob + ":" + Arrays.toString(targetIndices);
    }
}

/** Used for training to improve Codenames spymaster. Incomplete/broken feature. */
class TrainState {

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

//Constants to adjust game settings:
class GameSettings {

    final float[] B = new float[]{-3.55106049399243f, 11.8332877194120f};

    //Probability below which you stop searching for clues:
    float SEARCH_CUTOFF = 0.6f;
    float MIN_PROB = 0.6f;

    //Probabilities above which you avoid clues:
    float ASSASSIN_THRESHOLD = 0.20f;
    float OPPONENT_THRESHOLD = 0.25f;
    float BYSTANDER_THRESHOLD = 0.3f;

    //Play a board from board.txt (true), or play a random online board (false):
    boolean PLAY_BOARD_FROM_FILE = false;

    //Number of words to search from the Word2Vec database:
    int DATABASE_SIZE = 25000;

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