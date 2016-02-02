import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.*;
import cc.mallet.util.*;
import cc.mallet.types.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.topics.*;

/* This class contains method for writing files reporting the model
 * 
 * @author Julio Guerra
 */
public class ModelReporter {
	private static String separator = ","; 
	public static DecimalFormat formatter;
	
	static{
		formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
		formatter.applyPattern("#.###");
	}
	
	public static void writeListOfDocs(Collection col, String filename){
		BookNode[] nodes = col.toArray();
		int[] book_indexes = col.getBookIndexes();
		String content = "doc_num,book_num,doc_id,title"+"\n";
		for(int i=0;i<nodes.length;i++){
			content += i+separator+book_indexes[i]+separator+nodes[i].getDocId()+separator+nodes[i].getTitle();
			content += "\n";
		}
		writeFile(content, filename);		
	}
	
	public static void writeListOFTopics(Collection col, String filename, int terms_per_topic){
		Object[][] topic_words = col.model_LDA.getTopWords(terms_per_topic);
		String content = "topic,topic_df,word,word,word..."+"\n";
		int [] topic_df = col.getTopicDf();
		for (int i=0;i<topic_words.length;i++){
			content += i + separator + topic_df[i];
        	for (int j=0;j<topic_words[i].length;j++){
        		content += separator+topic_words[i][j].toString();
        	}
        	content += "\n";
        }
		writeFile(content, filename);
	}

	public static void writeListOfDocsTopicsProb(Collection col, String filename){
		BookNode[] nodes = col.toArray();
		int[] book_indexes = col.getBookIndexes();
		
		try{
			FileWriter fw = new FileWriter(filename);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("doc_num,book_num,doc_id,topic1_prob,...,topicN_prob"+"\n");
			for(int i=0;i<nodes.length;i++){
				bw.write(i+separator+book_indexes[i]+separator+nodes[i].getDocId());
				double[] probs = nodes[i].getTopicProbabilities();
				if (probs != null && probs.length>0){
					for (int j=0;j<probs.length;j++){
						//if (probs[j]>0.01){
							//content += "Topic "+j+": "+formatter.format(probs[j])+separator;
						//}
						
						bw.write(separator + formatter.format(probs[j]));
					}
				}
				bw.write("\n");
			}
			bw.close();
			fw.close();
		}catch(Exception e){
			System.out.println(" ** Error writting the file: "+filename);
		}
	}
	
	
	
	// @@@@
	public static boolean containsIndex(int[] a, int index){
		if (a == null || a.length==0) return false;
		for(int i=0;i<a.length; i++){
			if (a[i] == index) return true;
		}
		return false;
	}
	
	public static int[] createIntArray(int value, int n){
		int[] r = new int[n];
		for(int i=0;i<n; i++){
			r[i] = value;
		}
		return r;
	}
	
	public static int[] getNHighestIndexes(int n, double[] topic_values){
		int[] r = createIntArray(-1,n);
		for(int c=0;c<n;c++){
			double max = 0.0;
			int max_index = 0;
			for(int i=0;i<topic_values.length;i++){
				if(topic_values[i]>max && !containsIndex(r,i)){
					max = topic_values[i];
					max_index = i;
				}
			}
			r[c] = max_index;
		}
		return r;
	}
	

	
	// @@@@
	public static void writeDocsTopicSummary(Collection col, String filename){
		BookNode[] nodes = col.toArray();
		int[] book_indexes = col.getBookIndexes();
		ParallelTopicModel model = col.model_LDA;
		Object[][] topic_words = model.getTopWords(15);
		int [] topic_df = col.getTopicDf();
		formatter.applyPattern("##.#");
		try{
			FileWriter fw = new FileWriter(filename);
			BufferedWriter bw = new BufferedWriter(fw);
			//bw.write("doc_num,book_num,doc_id,topic1_prob,...,topicN_prob"+"\n");
			for(int i=0;i<nodes.length;i++){
				bw.write(i+" - "+book_indexes[i]+"\t(size "+nodes[i].getTreeTextLength()+")\t"+nodes[i].getDocId()+"\t"+nodes[i].getTitle()+"\n");
				double[] probs = nodes[i].getTopicProbabilities();
				if (probs != null && probs.length>0){
					int[] high_topics = getNHighestIndexes(4,probs);
					for (int j=0;j<high_topics.length;j++){
						int itopic = high_topics[j];
						bw.write("\t t: "+itopic+"\t"+formatter.format(probs[itopic]*100)+"%  df: "+ topic_df[itopic] +"  words: ");
						for (int k=0;k<topic_words[itopic].length;k++){
							bw.write(topic_words[itopic][k]+" ");
						}
						bw.write("\n");
					}
				}
				bw.write("\n");
			}
			bw.close();
			fw.close();
		}catch(Exception e){
			System.out.println(" ** Error writting the file: "+filename);
		}
		formatter.applyPattern("#.###");
	}
	
	
	
	public static void writeListOfDocsTopicsTfidf(Collection col, String filename){
		BookNode[] nodes = col.toArray();
		int[] book_indexes = col.getBookIndexes();
		try{
			FileWriter fw = new FileWriter(filename);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("doc_num,book_num,doc_id,topic1_tfidf,...,topicN_tfidf"+"\n");
			for(int i=0;i<nodes.length;i++){
				bw.write(i+separator+book_indexes[i]+separator+nodes[i].getDocId());
				double[] tfidf = nodes[i].getTfidf();
				if (tfidf != null && tfidf.length>0){
					for (int j=0;j<tfidf.length;j++){
						bw.write(separator + formatter.format(tfidf[j]));
					}	
				}
				bw.write("\n");
			}
			bw.close();
			fw.close();
		}catch(Exception e){
			System.out.println(" ** Error writting the file: "+filename);
		}
	}

	// writes a docs x docs matrix containing similarity symmetric matrix
	public static void writeMappingMatrix(Collection col, double[][] map, String filename){
		int s = map.length;
		//System.out.println("Writing map matrix file...");
		try{
			FileWriter fw = new FileWriter(filename);
			BufferedWriter bw = new BufferedWriter(fw);
			for (int i=0;i<s;i++){
				//System.out.println("doc "+i);
	        	//content += i + separator + book_indexes[i] + separator + nodes[i].getDocId();
	        	for (int j=0;j<s-1;j++){
	        		bw.write(map[i][j] + separator);
	        	}
	        	bw.write(formatter.format(map[i][s-1]));
	        	bw.write("\n");
			}
	    }catch(Exception e){
	    	System.out.println(" ** Error writting the file: "+filename);
	    }
		
	}

	public static void writeMappingList(Collection col, double[][] map, String filename){
		BookNode[] nodes = col.toArray();
		int[] book_indexes = col.getBookIndexes();
		
		int s = map.length;
		try{
			FileWriter fw = new FileWriter(filename);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("i,book_i,doc_i,j,book_j,doc_j,similarity_i_j"+"\n");
			for (int i=0;i<s;i++){
	        	
	        	for (int j=0;j<s-1;j++){
	        		if (i!=j && book_indexes[i] != book_indexes[j]){
	        			bw.write(i + separator + book_indexes[i] + separator + nodes[i].getDocId() + separator);
	        			bw.write(j + separator + book_indexes[j] + separator + nodes[j].getDocId() + separator);
	        			bw.write(formatter.format(map[i][j]));
	        			bw.write("\n");
	        		}
	        	}
	        	
		    }
			bw.close();
			fw.close();
		}catch(Exception e){
			System.out.println(" ** Error writting the file: "+filename);
		}
		
	}
	
	public static void writeMappingMostSimilar(Collection col, double[][] map, String filename){
		BookNode[] nodes = col.toArray();
		int[] book_indexes = col.getBookIndexes();
		
		int s = map.length;
		String content = "i,book,doc_i,doc_most_similar,similarity_i_j"+"\n";
		
			
        for (int i=0;i<s;i++){
        	content += i + separator + book_indexes[i] + separator + nodes[i].getDocId();
        	int most_similar = 0;
        	double max_similarity = 0.0;
        	for (int j=0;j<s;j++){
        		// only maps nodes to nodes from other books!
        		if (i!=j && book_indexes[i] != book_indexes[j]){
	        		if (max_similarity<map[i][j]){
	        			max_similarity = map[i][j];
	        			most_similar = j;
	        		}
        			
        		}
        		
        	}
        	content += separator + nodes[most_similar].getDocId() +  separator + formatter.format(max_similarity);
        	content += "\n";
        }
        writeFile(content, filename);
	}
	
	
	public static void writeVocabularyList(Collection col, String filename){
		Alphabet vocabulary = col.model_LDA.getAlphabet();
		int vsize = vocabulary.size();
		System.out.println("   Vocabulary size: "+vsize);
		try{
			FileWriter fw = new FileWriter(filename);
			BufferedWriter bw = new BufferedWriter(fw);
			//for (int i=0;i<vsize;i++){
				//bw.write(vocabulary.lookupObject(i).toString());
				bw.write(vocabulary.toString());
				bw.write("\n");
			//}
			bw.close();
			fw.close();
	    }catch(Exception e){
	    	System.out.println(" ** Error writting the file: "+filename);
	    }
		
	}
	
	public static void writeTopicWordDistribution(double[][] distribution, String filename){
		//String content = "";
		try{
			FileWriter fw = new FileWriter(filename);
			BufferedWriter bw = new BufferedWriter(fw);
			for (int i=0;i<distribution.length;i++){
				bw.write("t"+i);
				for (int j=0;j<distribution[i].length;j++){
					bw.write("\t" + formatter.format(distribution[i][j]));
				}
				bw.write("\n");
			}
			bw.close();
			fw.close();
	    }catch(Exception e){
	    	System.out.println(" ** Error writting the file: "+filename);
	    }
		
	}
	
	
	public static void writeFile(String content, String filename){
		try{
			//FileWriter fw = new FileWriter(filename);
			//BufferedWriter bw = new BufferedWriter(fw);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename),"UTF8"));
			bw.write(content);
			bw.close();
			//fw.close();
		}catch(Exception e){
			System.out.println(" ** Error writting the file: "+filename);
		}
	}
	
	
	public static void writeSimilarityMatrix(double[][] similarities, String filename){
		try{
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename),"UTF8"));
			
			for (int i=0;i<similarities.length;i++){
				for (int j=0;j<similarities[i].length-1;j++){
					bw.write(formatter.format(similarities[i][j])+" ");
				}
				bw.write(formatter.format(similarities[i][similarities[i].length-1]));
				bw.write("\n");
			}
			bw.close();
			//fw.close();
		}catch(Exception e){
			System.out.println(" ** Error writting the file: "+filename);
		}
	}

	public static boolean createDir(String dir){
		File theDir = new File(dir);
		  // if the directory does not exist, create it
		if (!theDir.exists()) {			
			return theDir.mkdir();  
		}else{
			return false;
		}
	}
	
}
