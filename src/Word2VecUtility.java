import java.io.*;
import java.nio.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.*;

/* Things that might be good to add!
1. Add a "progress bar" to show where we are in processing the file when searching it.
2. To calibrate that progress bar, scroll through the whole file and print every 5,000th word. Print time as well.
3. Find some way to consolidate all the buffered input lines
5. Require users to populate vectors HashMap up front by making it part of the constructor
6. Make methods fail more gracefully if the given word isn't in the dictionary
 */

public class Word2VecUtility {

	public HashMap<String, float[]> vectors = new HashMap<>();

	public void getVectors(int numsearch) {
		try {
			BufferedInputStream bufferedInput = new BufferedInputStream(new FileInputStream("GoogleNews-vectors-negative300.bin"));
			bufferedInput.skip(12);
			int read=0;
			while(bufferedInput.available()>0 && read<numsearch) {
				String word = readWord(bufferedInput);
				float[] vec = readVector(bufferedInput);
				vectors.put(word,vec);
				read++;
			}
			bufferedInput.close();
		} catch (IOException e) {
			System.out.println("Error opening the buffered input stream to read vectors: " + e);
			System.out.println("So make sure the file referenced in the getVectors method of Word2VecUtility is correct. Bye!");
			return;
		}
	}

	public float[] getVec(String word){return vectors.get(word);} //Maybe do this ignoring case?

	/**
	 * Following three methods are overloads of wordsCloseTo().
	 * All find the closest words to the target word or vector.
	 */
	public ArrayList<WordScore> wordsCloseTo(String targetWord, int numResults) {
		return wordsCloseTo(targetWord, numResults, new String[0]);
	}
	
	public ArrayList<WordScore> wordsCloseTo(String targetWord, int numResults, String[] excluded) {
		//Returns words close to target, but excludes any superstring or substring of that word.
		float[] targetVec = getVec(targetWord);
		System.out.println("Found " + targetWord);
		return wordsCloseTo(targetVec, numResults, excluded);
	}

	public ArrayList<WordScore> wordsCloseTo(float[] targetVec, int numResults)  {
		return wordsCloseTo(targetVec, numResults, new String[0]);
	}
	
	/**
	 * Searches our dictionary to find the closest words to the target vector, measured by cosine similarity.
	 * @param targetVec Target, as a 300-component vector
	 * @param numResults Number of results to return
	 * @param excluded All words to be excluded from search
	 * @return Sorted ArrayList of most similar words, with cosine similarity as scores.
	 */
	public ArrayList<WordScore> wordsCloseTo(float[] targetVec, int numResults, String[] excluded) {
		ArrayList<WordScore> results = new ArrayList<WordScore>(numResults);
		for(int i=0; i<numResults; i++) {
			results.add(new WordScore("", -1.0f));
		}
		
		for(String word : vectors.keySet()) {
			float[] vec = vectors.get(word);
			float cosSimilarity = cosineSimilarity(vec, targetVec);
			
			if(cosSimilarity<results.get(numResults-1).score || isExcluded(word, excluded)) {
				continue; //Screen out low-similarity and excluded words
			}
			
			WordScore wordSim = new WordScore(word, cosSimilarity);
			results.add(wordSim);
			Collections.sort(results);
			Collections.reverse(results);
			results.remove(numResults);
		}
		return results;
	}
	
	private static boolean isExcluded(String word, String[] excluded) {
		for(String ex: excluded) { //Screen out excluded words
			if(isSubstring(word, ex)) { 
				return true;
			}
		}
		return false;
	}

	public static boolean isSubstring(String clue, String word) {
        if (clue.toLowerCase().indexOf(word.toLowerCase()) != -1) return true;
        if (word.toLowerCase().indexOf(clue.toLowerCase()) != -1) return true;
        return false;
    }

	//Find closest word to targetVec which is in set
	public ArrayList<WordScore> wordsCloseTo(float[] targetVec, String[] set, int numResults)  {		
		ArrayList<WordScore> results = new ArrayList<WordScore>(numResults);
		for(int i=0; i<numResults; i++) {
			results.add(new WordScore("", -1.0f));
		}

		for(String s : set){
			float[] nextVec = vectors.get(s);
			if(nextVec==null) continue;
			float cosSimilarity = cosineSimilarity(nextVec, targetVec);

			if(cosSimilarity<results.get(numResults-1).score){continue;}

			WordScore next = new WordScore(s, cosSimilarity);
			int position = Collections.binarySearch(results, next, new Comparator<WordScore>() {
				@Override
				public int compare(WordScore o1, WordScore o2) {
					return -Float.compare(o1.score, o2.score);}});

			results.add(position < 0 ? -position - 1 : position, next);
			results.remove(numResults);
		}
		return results;
	}

	public float printCosineSimilarity(String word1, String word2)  {
		float[] firstVec = getVec(word1);
		float[] secondVec = getVec(word2);
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

	public float l2norm(float[] a, float[] b){
		double sum=0;
		if(a.length!=b.length) throw new IllegalArgumentException("vectors must be of the same length");
		for(int i=0; i<a.length; i++){sum+=(a[i]-b[i])*(a[i]-b[i]);}
		return (float)Math.sqrt(sum);
	}
	
	public float euclideanDistanceUnnormalized(float[] a, float[] b){
		double sum=0;
		for(int i=0; i<a.length; i++){sum+=(a[i]-b[i])*(a[i]-b[i]);}
		return (float)Math.sqrt(sum);
	}
	
	public float printEuclideanDistance(String word1, String word2) {
		float[] firstVec = getVec(word1);
		float[] secondVec = getVec(word2);
		float eucDist = euclideanDistanceUnnormalized(firstVec, secondVec);
		System.out.println("Euclidean distance between " + word1 + " and " + word2 + ": " + eucDist);
		return eucDist;
	}
}


class WordScore implements Comparable<WordScore> {
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
	
	@Override
	public int compareTo(WordScore other) {
		return Float.compare(this.score, other.score);
	}
}
