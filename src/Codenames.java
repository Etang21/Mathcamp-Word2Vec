import java.io.File;
import java.io.IOException;
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

    static String[] data = new String[9914];

    public static void main(String[] args) throws IOException {

        //found a source online that contains *common* enlgish words
        URL url = new URL("https://raw.githubusercontent.com/jbowens/codenames/master/assets/game-id-words.txt");
        Scanner s = new Scanner(url.openStream());

        System.out.print("\rretrieving word vectors...");
        util.getVectors(100000);
        Scanner input = new Scanner(System.in);

        System.out.print("\rdownloading word set...");
        int i=0;
        while(s.hasNext()){data[i] = s.next(); i++;}

        System.out.println("\rgenerating game board...");

        int n=0;
        while(n<data.length){
            if(opp.size()==8 && words.size()==8) break;

            double rand = Math.random();
            if(rand<0.002){
                if(util.getVec(data[n])==null)continue;

                if(rand<0.001){if(words.size()<8) words.add(data[n]);}
                else if(opp.size()<8){opp.add(data[n]);}
            }

            if(n==data.length-1)n=0;
            else n++;
        }

        System.out.println("blue:"+words);
        System.out.println("red:"+opp);

        double[][] array = new double[100000][300];
        Iterator it = util.vectors.entrySet().iterator();

        int m=0;
        while (it.hasNext()) {
            Map.Entry<String, float[]> pair = (Map.Entry)it.next();
            for(int j=0; j<300; j++){
                array[m][j] = (double) pair.getValue()[j];
            }
            m++;
        }

        /*Matrix sims = X.times(average);

        double norm = 0; int V = sims.getRowDimension();
        System.out.println(V);
        for(int i=0; i<V; i++){norm += exp(sims.get(i,0));}

        //normalizing cosine similarity vector using sotfmax
        double[] softmax = new double[V];
        for(int i=0; i<V; i++){softmax[i] = exp(sims.get(i,0))/norm;}

        Integer[] indexes = new Integer[V];
        for(int i=0; i<V; i++) indexes[i]=i;

        Arrays.sort(indexes, new Comparator<Integer>(){
            @Override
            public int compare(Integer o1, Integer o2) {
                return Double.compare(softmax[o1], softmax[o2]);}});

        for(int i=1; i<6; i++){
            System.out.println(util.vectors.keySet().toArray()[indexes[V-i]] +","+sims.get(indexes[V-i],0));
        }*/

        for(int k=2; k<=8; k++){
            count=0;
            maximums.add(new Hint(0,"", new int[]{0}));

            int[] subset = new int[k];
            checkSubsets(8, subset, 0,0);

            if(maximums.get(k-2).prob < 0.4) break;
        }

        System.out.println();
        System.out.println(maximums);

        int shift = (maximums.size()>1) ? 2:1;

        System.out.println("choice: " + maximums.get(maximums.size()-shift));

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
            average.timesEquals((double)1/4);

            float[] average_vec = new float[300];

            for(int i=0; i<300; i++){average_vec[i] = (float)average.get(i,0);}

            ArrayList<WordScore> canidates = util.wordsCloseTo(average_vec,subset.length+5);

            for(int i=0; i<5; i++){

                float prob = 1.0f;
                float min_prob=1.0f;
                String curr_word = canidates.get(subset.length+i).word;
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

