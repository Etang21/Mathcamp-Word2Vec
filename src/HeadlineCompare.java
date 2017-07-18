import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.lang.*;

/**
 * Created by dawsonbyrd on 7/16/17.
 
 TODO: [1] make it so that input headline doesn't have to be from set in file
       [2] have headlines automically come from RSS feeds of major news outlets instead of text file
       [3] idk there is probably alot of other cool things to do
 */

public class HeadlineCompare {

    public static Word2VecUtility util = new Word2VecUtility();

    public static void main(String[] args) throws IOException{
        File file = new File("headlines.txt");
        Scanner lines = new Scanner(file);
        HashMap<String, float[]> headlines = new HashMap<>(10);

        System.out.println("retrieving word vectors...");
        util.getVectors(100000);
        System.out.println("computing headline averages...");

        while(lines.hasNext()){

            String line = lines.nextLine();

            String[] words = line.split("\\s+");
            for (int i = 0; i < words.length; i++) {
                words[i] = words[i].replaceAll("[^\\w]", "");
                words[i] = words[i].toLowerCase();
            }

            float[] average = new float[300];
            int num_words = 0;

            for(String word : words){
                float[] curr = util.getVec(word);

                if(curr!=null) {
                    for (int i = 0; i < 300; i++) {
                        average[i] += curr[i];
                    }
                    num_words++;
                }
            }

            if(num_words>1) {
                for (int i = 0; i < 300; i++) {average[i]/=(float)num_words;}
                headlines.put(line, average);
            }
        }
        System.out.println(headlines);
        lines.close();
        Scanner in = new Scanner(System.in);

        while(true) {
            System.out.println("Choose headline: ");

            String headline = in.nextLine();

            float[] target = headlines.get(headline);

            System.out.println("finding matches... ");

            ArrayList<String> results = new ArrayList<String>(10);
            ArrayList<Float> similarities = new ArrayList<>(10);

            for(int i=0; i<10; i++){results.add(""); similarities.add(-1.0f);}

            Iterator it = headlines.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, float[]> pair = (Map.Entry)it.next();

                float sim = util.cosineSimilarity(pair.getValue(), target);
                if(sim <similarities.get(9)){it.remove(); continue;}

                int position = Collections.binarySearch(similarities, sim, (o1, o2) -> -Float.compare(o1, o2));

                similarities.add(position < 0 ? -position - 1 : position, sim);
                results.add(position < 0 ? -position - 1 : position, pair.getKey());

                similarities.remove(10); results.remove(10);

                // avoids a ConcurrentModificationException
                it.remove();
            }

            for(int i=0; i<10; i++){
                System.out.println("["+(i+1)+"] " + similarities.get(i) + ": " + results.get(i));
            }
        }

    }
}
