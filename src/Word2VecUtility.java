import java.io.*;
import java.nio.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.math.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.*;

/* Things that might be good to add!
1. Add a "progress bar" to show where we are in processing the file when searching it.
2. To calibrate that progress bar, scroll through the whole file and print every 5,000th word. Print time as well.
Post results to groupchat.
3. Find some way to consolidate all the buffered input lines
4. Condense all the damned IOExceptions by actually using try-catch
5. Require users to populate vectors HashMap up front by making it part of the constructor
6. Make methods more robust if they don't find a given word
 */

public class Word2VecUtility {

	public HashMap<String, float[]> vectors = new HashMap<>();

	public void getVectors(int numsearch) throws IOException {
		//TODO: Record runtime and word position here. Have a "verbose" default variable.
		//TODO: If we reach the end of a file without finding a word, return null?
		BufferedInputStream bufferedInput = new BufferedInputStream(new FileInputStream("GoogleNews-vectors-negative300.bin"));
		bufferedInput.skip(12);

		int read=0;
		while(bufferedInput.available()>0 && read<numsearch) {
			String word = readWord(bufferedInput);
			float[] vec = readVector(bufferedInput);
			vectors.put(word,vec);
			read++;
		}
	}

	public float[] getVec(String word){return vectors.get(word);} //Maybe do this ignoring case?

	public ArrayList<WordScore> wordsCloseTo(String targetWord, int numResults) throws IOException {
		float[] targetVec = getVec(targetWord);
		System.out.println("Found " + targetWord);
		return wordsCloseTo(targetVec, numResults);
	}
	
	public ArrayList<WordScore> wordsCloseTo(float[] targetVec, int numResults) throws IOException {
		ArrayList<WordScore> results = new ArrayList<WordScore>(numResults);
		for(int i=0; i<numResults; i++) {results.add(new WordScore("", -1.0f));}

		Iterator it = vectors.entrySet().iterator();

		while(it.hasNext()){
			Map.Entry<String, float[]> pair = (Map.Entry)it.next();
			
			String nextWord = pair.getKey();
			float[] nextVec = pair.getValue();
			
			float cosSimilarity = cosineSimilarity(nextVec, targetVec);

			if(cosSimilarity<results.get(numResults-1).score){it.remove(); continue;}

			WordScore next = new WordScore(nextWord, cosSimilarity);
			int position = Collections.binarySearch(results, next, new Comparator<WordScore>() {
				@Override
				public int compare(WordScore o1, WordScore o2) {
					return -Float.compare(o1.score, o2.score);}});

			results.add(position < 0 ? -position - 1 : position, next);
			results.remove(numResults);

			it.remove();

		}
		return results;
	}

	public float printCosineSimilarity(String word1, String word2) throws IOException {
		float[] firstVec = getVec(word1);
		System.out.println("\"" + word1 + "\" vec is " + Arrays.toString(firstVec));
		float[] secondVec = getVec(word2);
		System.out.println("\"" + word2 + "\" vec is " + Arrays.toString(secondVec));
		float cosSim = cosineSimilarity(firstVec, secondVec);
		System.out.println("Cosine similarity between " + word1 + " and " + word2 + ": " + cosSim);
		return cosSim;
	}

	public float cosineSimilarity(float[] vec1, float[] vec2) {
		//Cosine similarity is defined as (A . B) / (|A|*|B|), measures similarity between two vecs
		//Could use map and reduce funcs to make this code more concise.
		float dotProd = 0.0f;
		float norm1 = 0.0f;
		float norm2 = 0.0f;

		for(int i=0; i<Math.min(vec1.length, vec2.length); i++) {
			dotProd += vec1[i]*vec2[i];
			norm1 += Math.pow(vec1[i], 2);
			norm2 += Math.pow(vec2[i], 2);
		}

		return (float) (dotProd/(Math.sqrt(norm1)*Math.sqrt(norm2)));
	}

	private String readWord(BufferedInputStream bufferedInput) throws IOException {
		//Could optimize this by checking if a byte is a space character directly, instead of casting?
		//Could optimize this by reading individual bytes instead of arrays
		String word = "";
		byte[] letter = new byte[1];
		bufferedInput.read(letter);
		while (Charset.defaultCharset().decode(ByteBuffer.wrap(letter)).charAt(0) != ' ') {
			word += (char)(letter[0]);
			letter = new byte[1];
			bufferedInput.read(letter);
		}
		return word;
	}

	private float[] readVector(BufferedInputStream bufferedInput) throws IOException {
		byte[] vectorBytes = new byte[1200];
		bufferedInput.read(vectorBytes);

		float[] vector = new float[300];
		for(int i=0; i<1200; i+=4) {
			byte[] fourBytes = {vectorBytes[i], vectorBytes[i+1], vectorBytes[i+2], vectorBytes[i+3]};
			vector[i/4] = ByteBuffer.wrap(fourBytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
		}
		return vector;
	}

	public float[] addVec(float[] a, float[] b, int scale){
		float[] result = new float[300];
		for(int i=0; i<300; i++){result[i] = a[i]+scale*b[i];}
		return result;
	}
}


class WordScore {
	//A helper class which stores a word and associated "score"
	public String word;
	public float score;
	public WordScore(String word, float score) {
		this.word = word;
		this.score = score;
	}
	public String toString() {
		return "\"" + word + "\": " + score;
	}
}
