import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.Scanner;

/**
 * Created by dawsonbyrd on 7/19/17.
 */
public class SoftmaxData {

    public static Word2VecUtility util = new Word2VecUtility();

    static Object[] keys;
    static Random generator = new Random();

    public static void main(String[] args) throws IOException {

        System.out.println("retrieving word vectors...");
        util.getVectors(100000);

        FileWriter fw = new FileWriter("softmax.txt", true);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter data = new PrintWriter(bw);
        Scanner in = new Scanner(System.in);

        boolean run = true;
        keys = util.vectors.keySet().toArray();

        while(run)
        {
            String word = (String) keys[generator.nextInt(keys.length)];
            float[] hint = util.vectors.get(word);

            System.out.println("hint: " + word);
            System.out.print("pass:p, stop:s ");
            String pass = in.nextLine();

            if (pass.equals("p")) continue;
            if (pass.equals("s")) break;

            float[] sims = new float[25];

            int i=0;
            while (i<5) {
                int j=0;
                while(j<5){
                    float minsim = 0.3f*generator.nextFloat();
                    String curr = within(hint, minsim);
                    if(curr==null) continue;
                    if(curr.equals(word)) continue;

                    float[] curr_vec = util.getVec(curr);
                    sims[5*i+j] = util.cosineSimilarity(hint, curr_vec);

                    if(j==4){System.out.println("["+(5*i+j+1)+"] "+curr+" ");}
                    else{System.out.print("["+(5*i+j+1)+"] "+curr+" ");}
                    j++;
                }
                i++;
            }

            System.out.println("choose closet word: ");
            String choice = in.nextLine();
            int index = Integer.parseInt(choice)-1;
            float closet = sims[index];

            Arrays.sort(sims);
            int rank = Arrays.binarySearch(sims, closet)+1;

            for(int k=0; k<25; k++){data.print(sims[k]+",");}
            data.println(rank);
        }

        data.close();
    }

    static String within(float[] target, float sim){
        int j=0;
        while(j<100000){
            String word = (String) keys[generator.nextInt(keys.length)];
            float[] vec = util.vectors.get(word);

            if(util.cosineSimilarity(target,vec)>=sim) return word;
        }

        return null;
    }
}
