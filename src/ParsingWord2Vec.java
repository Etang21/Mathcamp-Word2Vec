import java.io.*;
import java.nio.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.math.*;

/* Things that might be good to add!
1. add and subtract vectors.
2. Add a "progress bar" to show where we are in processing the file when searching it.
3. To calibrate that progress bar, scroll through the whole file and print every 5,000th word. Print time as well. Post results to groupchat.
4. Find some way to consolidate all the buffered input lines
5. Rename all the cosine distance names to cosine similarity
6. Condense all the damned IOExceptions by actually using try-catch
7. Fix the terrible array update methods in the wordsToCloseTo
 */

public class ParsingWord2Vec {
	
	public static void main(String[] args) throws IOException {
		//printCosineSimilarity("Obama", "banana"); 
		
		String query = "halloween";
		ArrayList<WordDistance> nearQuery = wordsCloseTo(query, 10, 5000);
		System.out.println(nearQuery.toString());
	}
	
	public static float[] getVec(String word) throws IOException {
		//TODO: Record runtime and word position here. Have a "verbose" default variable.
		//TODO: If we reach the end of a file without finding a word, return null?
		BufferedInputStream bufferedInput = new BufferedInputStream(new FileInputStream("GoogleNews-vectors-negative300.bin"));
		bufferedInput.skip(12);
		String nextWord = readWord(bufferedInput);
		while(!nextWord.equals(word)) {
			bufferedInput.skip(1200);
			nextWord = readWord(bufferedInput);
		}
		return readVector(bufferedInput);
	}
	
	public static ArrayList<WordDistance> wordsCloseTo(String targetWord, int numResults, int numWordsToSearch) throws IOException {
		//TODO: rename the helper class to similarity, not distance.
		//TODO: Returns a list of word and similarity data, with closest ones at 0-index
		float[] targetVec = getVec(targetWord);
		System.out.println("Found " + targetWord);
		
		ArrayList<WordDistance> results = new ArrayList<WordDistance>(numResults);
		for(int i=0; i<numResults; i++) {
			results.add(new WordDistance("", -1));
		}
		
		BufferedInputStream bufferedInput = new BufferedInputStream(new FileInputStream("GoogleNews-vectors-negative300.bin"));
		bufferedInput.skip(12);
		for(int searchIndex = 0; searchIndex < numWordsToSearch; searchIndex++) {
			String nextWord = readWord(bufferedInput);
			float[] nextVec = readVector(bufferedInput);
			float cosSimilarity = cosineDistance(nextVec, targetVec);
			
			//TODO: Fix this! This is the slowest possible way to update our array of 10! Just check against bottom element first, or use the fact that it's sorted.
			for(int i=0; i<numResults; i++) {
				if(cosSimilarity > results.get(i).distance) {
					results.add(i, new WordDistance(nextWord, cosSimilarity));
					results.remove(numResults);
					i = numResults;
				}
			}
		}
		return results;
	}
	
	public static float printCosineSimilarity(String word1, String word2) throws IOException {
		float[] firstVec = getVec(word1);
		System.out.println("\"" + word1 + "\" vec is " + Arrays.toString(firstVec));
		float[] secondVec = getVec(word2);
		System.out.println("\"" + word2 + "\" vec is " + Arrays.toString(secondVec));
		float cosSim = cosineDistance(firstVec, secondVec);
		System.out.println("Cosine distance between " + word1 + " and " + word2 + ": " + cosSim);
		return cosSim;
	}
	
	public static float cosineDistance(float[] vec1, float[] vec2) {
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
	
	private static String readWord(BufferedInputStream bufferedInput) throws IOException {
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
	
	private static float[] readVector(BufferedInputStream bufferedInput) throws IOException {
		byte[] vectorBytes = new byte[1200];
		bufferedInput.read(vectorBytes);
		
		float[] vector = new float[300];
		for(int i=0; i<1200; i+=4) {
			byte[] fourBytes = {vectorBytes[i], vectorBytes[i+1], vectorBytes[i+2], vectorBytes[i+3]};
			vector[i/4] = ByteBuffer.wrap(fourBytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
		}
		return vector;
	}
}


class WordDistance { //Used to find nearby words in wordsCloseToString()
	public String word;
	public float distance;
	public WordDistance(String word, float dist) {
		this.word = word;
		distance = dist;
	}
	
	public String toString() {
		return "\"" + word + "\":" + distance;
	}
}
