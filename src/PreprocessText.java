import java.io.*;
/*import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;
*/
import org.tartarus.snowball.*;
import org.tartarus.snowball.ext.*;

/*
 * Contains simple text processing of methods including stemming and character filtering
 * @author Julio Guerra
 */
public class PreprocessText {
	static String filtered_symbols = "\"\'.,;:<>á"; 
	
	public static String stem(String text){
		String processed = "";
		try{
			Class stemClass = Class.forName("org.tartarus.snowball.ext.englishStemmer");
			SnowballStemmer stemmer = (SnowballStemmer) stemClass.newInstance();
			String[] words = text.toLowerCase().split(" ");
			for (int i=0;i<words.length;i++){
				stemmer.setCurrent(words[i]);
				stemmer.stem();
				processed += stemmer.getCurrent()+" ";
				
			}
			
			  		
			
		}catch(Exception e){
		}		
		return processed;
	}
	
	public static String filterCharacters(String s){
		String filtered = "";
		for(int i=0;i<s.length();i++){
			if (filtered_symbols.indexOf(s.charAt(i)) == -1 ){
				filtered += s.charAt(i);
			}
		}
		return filtered;
	}
	
	// This method pre-process text to every book node in the Collection object
	// including repeating the titles of the nodes, stemming and tokenizing n-words terms 
	// defined in the vocabulary object
	public static void preprocessTree(Collection col, boolean repeattitles, boolean stemming, Vocabulary vocabulary){
		BookNode[] array_nodes = col.toArray();
		String processed = "";
		for(int i=0;i<array_nodes.length;i++){
			processed = array_nodes[i].getText();
			if (repeattitles) processed = array_nodes[i].getTitle() + " " + array_nodes[i].getTitle() + " " + processed;		
			if (vocabulary != null) processed = vocabulary.changeText(processed);
			if (stemming) processed = stem(processed);
			array_nodes[i].setText(processed);
		}
	}
	
}
