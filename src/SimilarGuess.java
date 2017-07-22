import java.io.*;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

/**
 * Created by dawsonbyrd on 7/18/17.
 */
public class SimilarGuess {

    public static Word2VecUtility util = new Word2VecUtility();

    public static void main(String[] args) throws IOException{

        System.out.println("retrieving word vectors...");
        util.getVectors(100000);

        FileWriter fw = new FileWriter("logistic_data.txt", true);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter data = new PrintWriter(bw);
        Scanner in = new Scanner(System.in);

        boolean run=true;

        while(run){
            Random generator = new Random();
            Object[] keys = util.vectors.keySet().toArray();
            String word = (String) keys[generator.nextInt(keys.length)];
            float[] vec = util.vectors.get(word);

            System.out.println("type y/n for words similar to "+word);
            System.out.print("pass:p, stop:s"); String pass = in.nextLine();

            if(pass.equals("p"))continue; if(pass.equals("s"))break;

            int i=0;
            int j=0;
            while(i<12){
                String curr = (String) keys[generator.nextInt(keys.length)];
                float[] curr_vec = util.vectors.get(curr);
                float sim = util.cosineSimilarity(vec, curr_vec);

                if(sim==1.0f) continue;

                if(j==99000) i=12;

                if(i<3){if(sim>0.25)continue;}
                if(i>=3 && i<8){if(sim>0.5)continue; if(sim<0.25) continue;}
                if(i>=8 && i<10) {if(sim>0.5)continue; if(sim<0.4){j++; continue;} j=0;}
                if(i>=10){i=12; if(sim<0.5){j++; continue;} j=0;}

                System.out.print(curr+","+sim+":");
                String result = in.next();

                if(result.equals("y")){data.println(sim+",1.0");}
                else{data.println(sim+",0.0");}

                i++;
            }
            in.nextLine();
        }

        data.close();
    }

}
