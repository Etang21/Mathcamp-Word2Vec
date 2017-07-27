import java.io.*;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

/**
 * Created by dawsonbyrd on 7/18/17.
 */
public class SimilarGuess {

    public static Word2VecUtility util = new Word2VecUtility();

    static Object[] keys;
    static Random generator = new Random();

    public static void main(String[] args) throws IOException{

        System.out.println("retrieving word vectors...");
        util.getVectors(100000);

        FileWriter fw = new FileWriter("logistic.txt", true);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter data = new PrintWriter(bw);
        Scanner in = new Scanner(System.in);

        boolean run=true;
        keys = util.vectors.keySet().toArray();


        while(run){
            Random generator = new Random();
            Object[] keys = util.vectors.keySet().toArray();
            String word = (String) keys[generator.nextInt(keys.length)];
            float[] vec = util.vectors.get(word);

            System.out.println("type y/n for words similar to "+word);
            System.out.print("pass:p, stop:s"); String pass = in.nextLine();

            if(pass.equals("p"))continue; if(pass.equals("s"))break;

            int i=0;

            while (i<5) {
                float minsim = 0.4f*generator.nextFloat();
                String curr = within(vec, minsim);
                if(curr==null) continue;
                if(curr.equals(word)) continue;

                float[] curr_vec = util.getVec(curr);
                float sim = util.l2norm(curr_vec, vec);

                System.out.print(curr+":");
                String result = in.next();

                if(result.equals("y")){data.println(sim+",1.0");}
                else{data.println(sim+",0.0");}

                i++;
            }

            in.nextLine();
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
