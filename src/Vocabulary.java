import java.io.*;
/*
 * This class implements a simple list of n-words vocabulary and
 * it is used to replace any token defined "word1 word2" to a word1-word2
 * from a text content
 * 
 * @author Julio Guerra
 */
import java.util.*;
public class Vocabulary {
	public String[] terms 
		  = {"prime numbers","prime number","composite numbers","composite number","prime factorization",
			 "equivalent fractions","equivalent fraction","least common multiple","least common denominator",
			 "greatest common factor","common factors"};
	
	private String separator = "-";
	
	public Vocabulary(String filename, String separator){
		this.separator = separator;
		this.readVocabularyFromFile(filename);
	}
	
	public String changeText(String original){
		original = original.replaceAll("  ", " ");
		original = original.toLowerCase();
		for (int i=0;i<terms.length;i++){
			String term = terms[i].trim();
			String term_r = term.replaceAll(" ", this.separator);
			//System.out.println(term+" : "+term_r);
			original = original.replaceAll(term, term_r);
		}
		return original;
	}
	
	public void readVocabularyFromFile(String filename){
		ArrayList<String> lines = new ArrayList<String>();
		try{
			FileReader fr = new FileReader(filename);
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			while((line=br.readLine())!=null){
				lines.add(line.toString());
			}
			br.close();
			fr.close();
			int n = lines.size();
			this.terms = new String[n];
			for (int i=0;i<n;i++){
				this.terms[i] = lines.get(i).toString();
			
			}
			
		}catch(Exception e){
			
		}
	}
	
	public static void main(String args[]){
		Vocabulary voc = new Vocabulary("data/algebra_nterm_vocabulary.txt","-");
		
		String test = "Prime And Composite Numbers Prime And Composite Numbers Notice that the only factors of 7 are 1 and 7 itself, and that the only factors of 23 are 1 and 23 itself. Prime Number A whole number greater than 1 whose only whole number factors are itself and 1 is called a prime number. The first seven prime numbers are2, 3, 5, 7, 11, 13, and 17The number 1 is not considered to be a prime number, and the number 2 is the first and only even prime number.Many numbers have factors other than themselves and 1. For example, the factors of 28 are 1, 2, 4, 7, 14, and 28 (since each of these whole numbers and only these whole numbers divide into 28 without a remainder). Composite Numbers A whole number that is composed of factors other than itself and 1 is called a composite number. Composite numbers are not prime numbers.Some composite numbers are 4, 6, 8, 10, 12, and 15. ";
		System.out.println(voc.changeText(test));
	}
}
