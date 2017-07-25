import Jama.Matrix;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import static java.lang.Math.exp;

/**
 * Created by dawsonbyrd on 7/20/17.
 */

//9 one color, 8 another, 1 assasin, 7 bystanders

public class Codenames_UI {

    final static float[] B = new float[]{-3.55106049399243f, 11.8332877194120f};
    final static float t = 0.75f;

    final static int[] num_subsets = new int[]{1,8,28,56,70,56,28,8,1};

    static float count=0;

    public static Word2VecUtility util = new Word2VecUtility();
    static ArrayList<String> words = new ArrayList<>();
    static ArrayList<String> opp = new ArrayList<>();
    static ArrayList<String> bystanders = new ArrayList<>();
    static String assassin;

    static ArrayList<Hint> maximums = new ArrayList<>();

    static String[] data = new String[404];

    public static void main(String[] args) throws IOException {

        System.out.println("\rretrieving word vectors...");
        util.getVectors(100000);
        Scanner input = new Scanner(System.in);

        //found a source online that contains *common* enlgish words that appear in codenames

        URL url = new URL("https://raw.githubusercontent.com/jbowens/codenames/master/assets/original.txt");
        //URL url = new URL("https://raw.githubusercontent.com/jbowens/codenames/master/assets/game-id-words.txt");
        //URL url = new URL("https://raw.githubusercontent.com/jbowens/codenames/master/assets/words.txt");
        Scanner s = new Scanner(url.openStream());

        System.out.println("\rdownloading word set...");
        int i=0;

        while(s.hasNext()){data[i] = s.next().toLowerCase(); i++;}

        File file = new File("board.txt");
        Scanner in = new Scanner(file);

        System.out.println("loading board...");
        words.clear(); opp.clear(); maximums.clear();

        int num_words = Integer.parseInt(in.nextLine());
        for(int k=0; k<num_words; k++){
            String curr = in.next();
            words.add(curr);}

        int num_opp = Integer.parseInt(in.next());
        for(int k=0; k<num_opp; k++){
            String curr = in.next();
            opp.add(curr);}

        int num_by = Integer.parseInt(in.next());
        for(int k=0; k<num_by; k++){bystanders.add(in.next());}

        assassin = in.next();

        System.out.println("getting hint...");

        for (int k = 1; k<=num_words; k++) {
            count = 0;
            maximums.add(new Hint(0, "", new int[]{0}));

            int[] subset = new int[k];
            checkSubsets(num_words, subset, 0, 0);

            if (maximums.get(k - 1).prob < 0.4) break;
        }

        System.out.println();
        //System.out.println(maximums);

        int shift = (maximums.size() > 1) ? 2 : 1;
        Hint final_hint = maximums.get(maximums.size() - shift);
        System.out.println("hint: " + final_hint.word + "," + final_hint.s.length);

        String[] intendend = new String[final_hint.s.length];
        for (int j = 0; j < intendend.length; j++) {intendend[j] = words.get(final_hint.s[j]);}

        System.out.println("intended cards: " + Arrays.toString(intendend));
    }

    public static float prob(float sim){
        float arg = B[0] + sim*B[1];
        return 1.0f/(float)(1+exp(-arg));
    }

    static void checkSubsets(int size, int[] subset, int subsetSize, int nextIndex) throws IOException{
        if (subsetSize == subset.length) {

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

            //Screening out all substrings/superstrings:
            ArrayList<String> excluded = new ArrayList<String>(words);
            excluded.addAll(opp);
            String[] paramExcluded = new String[excluded.size()];
            ArrayList<WordScore> candidates = util.wordsCloseTo(average_vec,subset.length+5, excluded.toArray(paramExcluded));

            for(int i=0; i<5; i++){

                float prob = 1.0f;
                float min_prob=1.0f;
                String curr_word = candidates.get(subsetSize+i).word;
                float[] hint =  util.vectors.get(curr_word);

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

                if(ass_prob<0.1 && max_prob_opp<0.2 && max_prob_by<0.3 && prob > maximums.get(subsetSize-1).prob){
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
}

