import java.io.*;
import java.net.URL;
import java.util.*;

import Jama.Matrix;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import javafx.util.Pair;

import static java.lang.Math.exp;
import static java.lang.Math.max;

/**
 * Created by dawsonbyrd on 7/20/17.
 */

public class Codenames {

    final static float[] B = new float[]{-3.55106049399243f, 11.8332877194120f};
    final static float t = 0.75f;

    final static int[] num_subsets = new int[]{1,8,28,56,70,56,28,8,1};

    static float count=0;

    public static Word2VecUtility util = new Word2VecUtility();
    static ArrayList<String> words = new ArrayList<>();
    static ArrayList<String> opp = new ArrayList<>();
    static ArrayList<Hint> maximums = new ArrayList<>();

    static String[] data = new String[404];

    public static void main(String[] args) throws IOException {

        FileWriter fw = new FileWriter("performance.txt", true);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter out = new PrintWriter(bw);

        System.out.print("\rretrieving word vectors...");
        util.getVectors(100000);
        Scanner input = new Scanner(System.in);

        //found a source online that contains *common* enlgish words that appear in codenames

        URL url = new URL("https://raw.githubusercontent.com/jbowens/codenames/master/assets/original.txt");
        //URL url = new URL("https://raw.githubusercontent.com/jbowens/codenames/master/assets/game-id-words.txt");
        //URL url = new URL("https://raw.githubusercontent.com/jbowens/codenames/master/assets/words.txt");
        Scanner s = new Scanner(url.openStream());

        System.out.print("\rdownloading word set...");
        int i=0;
        while(s.hasNext()){data[i] = s.next().toLowerCase(); i++;}

        Scanner in = new Scanner(System.in);

        while(true) {
            words.clear(); opp.clear(); maximums.clear();

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

//        System.out.println("blue:"+words);
//        System.out.println("red:"+opp);

            for (int j = 0; j < 4; j++) {
                for (int x = 0; x < 3; x++) {
                    System.out.print(cards.get(4 * j + x) + " ");
                }
                System.out.println(cards.get(4 * j + 3));
            }

            for (int k = 2; k <= 8; k++) {
                count = 0;
                maximums.add(new Hint(0, "", new int[]{0}));

                int[] subset = new int[k];
                checkSubsets(8, subset, 0, 0);

                if (maximums.get(k - 2).prob < 0.4) break;
            }

            System.out.println();
            //System.out.println(maximums);

            int shift = (maximums.size() > 1) ? 2 : 1;
            Hint final_hint = maximums.get(maximums.size() - shift);
            System.out.println("hint: " + final_hint.word + "," + final_hint.s.length);

            for (int j = 0; j < final_hint.s.length; j++) {
                System.out.print("pick " + j + ":");
                String pick = in.nextLine();
                if (!words.contains(pick)) {
                    System.out.println("\rincorrect!");
                    out.print(0);break;
                }
                if (j == final_hint.s.length - 1){System.out.println("\rcorrect!"); out.print(1);}
            }

            String[] intendend = new String[final_hint.s.length];

            for (int j = 0; j < intendend.length; j++) {
                intendend[j] = words.get(final_hint.s[j]);
            }

            System.out.println("intended cards: " + Arrays.toString(intendend));

            System.out.print("quit:"); String response = in.nextLine();
            if(response.equals("y")) break;

            System.out.println();
        }

        out.close();
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

            ArrayList<WordScore> canidates = util.wordsCloseTo(average_vec,subset.length+5);

            for(int i=0; i<5; i++){

                float prob = 1.0f;
                float min_prob=1.0f;
                String curr_word = canidates.get(subsetSize+i).word;
                float[] hint =  util.vectors.get(curr_word);

                for(int c: subset){
                    float[] card = util.getVec(words.get(c));
                    float curr_prob = prob(util.cosineSimilarity(hint,card));
                    prob*=curr_prob;
                    if(curr_prob<min_prob) min_prob=curr_prob;
                }

                float max_prob=0;
                for(String s: opp){
                    float curr_prob = prob(util.cosineSimilarity(hint, util.getVec(s)));
                    if(curr_prob>max_prob){max_prob = curr_prob;}
                }

                if(max_prob<0.1 /*t*min_prob*/ && prob > maximums.get(subsetSize-2).prob){
                    int[] k = new int[subsetSize];
                    for(int m=0; m<subsetSize; m++){k[m]=subset[m];}
                    maximums.set(subsetSize-2, new Hint(prob, curr_word, k));
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

