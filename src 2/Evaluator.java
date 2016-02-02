import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import cc.mallet.util.*;
import cc.mallet.types.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.topics.*;

/*
 * This class evaluates the propagated topic model with the ground truth (manual mapping)
 * 
 * @author Julio Guerra
 */

public class Evaluator {
	public static int nbook1 = 1;
	public static int nbook2 = 2;
	public static boolean using_tfidf = false; // true: compute similarities using topicFreq-idf vector. False: use topic probabilities
	
	public static double[] evaluateNDCG(Collection C, 
										String truth_dir, 
										String mode, 
										int n_book1,
										int n_book2, 
										boolean using_tfidf, 
										boolean is_hlda){
		double[] res = new double[3];
		Evaluator.nbook1 = n_book1;
		Evaluator.nbook2 = n_book2;
		Evaluator.using_tfidf = using_tfidf;
		
		GroundTruth gt0 = GroundTruth.readMapFiles(truth_dir,GroundTruth.SIMPLE_RANK_NORMALIZED);
		if (gt0 != null){
			//gt0.printMap();
			int n = gt0.map.size(); // how many nodes has been mapped
			String [][] modelmap = new String[n][];
			for(int i=0;i<n;i++){
				GroundTruth.NodeMap node_map = gt0.map.get(i);
				String[] modelmatchs = new String[11];
				modelmatchs[0] = node_map.node_id;
				BookNode nodeA = C.searchNodeById(node_map.node_id);
				BookNode[] similar_computed = null;
				// get 10 most similar nodes on book 2 (static variable) and return them ordered
				if (mode == "COSINE"){
					similar_computed = getMostSimilarByCosine(C, nodeA, 10, is_hlda); //
					//System.out.print("\t using COSINE: ");
				}else if (mode == "KL-DIVERGENCE"){
					similar_computed = getMostSimilarByKLDivergence(C, nodeA, 10, is_hlda); // get m+2 most similar nodes to nodeA.
					//System.out.print("\t using KL-DIVERGENCE: ");
				}
				// add each node id to the array result
				int c = 1;
				for (BookNode match_node: similar_computed){
					modelmatchs[c] = match_node.getDocId();
					c++;
				}
				modelmap[i] = modelmatchs;
				
			}
			res[0] = gt0.evaluateMapNDCG(modelmap, 1);
			res[1] = gt0.evaluateMapNDCG(modelmap, 3);
			res[2] = gt0.evaluateMapNDCG(modelmap, 10);
			
		}
		
		
		return res;
	}
	
	// evaluates the model comparing with the ground truth file. For each line in this file, a case is defined as 
	// a nodeA from book 1 mapped to m nodes on the book 2 (nodes nodeB). 
	// Each case-precision (each line) is computed by counting all matches for the m most similar
	// nodes returned by the model (using COSINE and KL_DIVERGENCE) and then dividing by m. 
	// The total precision is computed by summing all case-precisions and dividing by the number of lines
	public static double evaluate(Collection C, String truth_file, String out_file, String mode, int n_book1, int n_book2, boolean using_tfidf, boolean is_hlda){
		Evaluator.nbook1 = n_book1;
		Evaluator.nbook2 = n_book2;
		Evaluator.using_tfidf = using_tfidf;
		String[][] truth = readTruthFile(truth_file); // truth sets nodeA,nodeB1,nodeB2, ... nodeBn
		//printTruth(truth);
		String report_str = "";
		double eval = 0.0;
		if (truth != null){
			int n = truth.length;
			int m = truth[0].length-1;// not counting the first element, who is the nodeA
			//System.out.println(m);
			for (int i=0;i<n;i++){
				// m represent the number of similar nodes in book 2 for the i node mapped
				// the precision will be computed to the m most similar automatic selected nodes
				m = truth[i].length-1;// not counting the first element, who is the nodeA
				BookNode nodeA = C.searchNodeById(truth[i][0]);
				
				if (C.getBookNumber(nodeA) == nbook1){
					BookNode[] similar_computed = null;
					BookNode[] similar_truth = new BookNode[m]; 
					
					//System.out.print("Evaluating for node "+nodeA.getDocId());
					
					// Get the similar nodes to nodeA 
					// Get the similar by cosine
					if (mode == "COSINE"){
						similar_computed = getMostSimilarByCosine(C,nodeA,m, is_hlda); // get m+2 most similar nodes to nodeA.
						//System.out.print("\t using COSINE: ");
					}
					// Get the similar by kl-divergence
					if (mode == "KL-DIVERGENCE"){
						similar_computed = getMostSimilarByKLDivergence(C,nodeA,m, is_hlda); // get m+2 most similar nodes to nodeA.
						//System.out.print("\t using KL-DIVERGENCE: ");
					}
					//System.out.print("\t m: "+similar_computed.length);
					
					report_str += nodeA.getDocId();
					for (int j=0;j<similar_computed.length;j++){
						report_str += "," + similar_computed[j].getDocId();
						//System.out.print(similar_computed[j].getDocId()+" ");
					}
					report_str += "\n";
					// GET the true similar to compare. The nodes are taken from the truth array
					// 
					for(int j=0;j<m;j++){
						similar_truth[j] = C.searchNodeById(truth[i][j+1]);
						//System.out.print(truth[i][j+1]+" ");
					}
					//System.out.println();
					
					// COMPUTE pseudo-precision for nodeA
					double correct = 0.0;
					for (int j=0;j<m;j++){
						int position = getIndex(similar_computed,similar_truth[j]);
						if (position != -1){
							//correct += 1.0/(position+1);
							correct += 1.0;
						}
						
					}
					//System.out.print("\t"+1.0*correct/(m)+"% \n");
					eval += 1.0*correct/(m);
					
				}
				
			} 
			eval = eval/n;
			ModelReporter.writeFile(report_str,out_file);

		}
		return eval;
		
		
	}
	
	// get the top m similar nodes to nodeA computing similarity using Cosine Similarity
	public static BookNode[] getMostSimilarByCosine(Collection C, BookNode nodeA, int m, boolean is_hlda){
		BookNode[] r = new BookNode[m];
		
		// level indexes for the topic arrays for hlda 
		int[] t_levels = null;
		if (is_hlda) t_levels = C.hlda_util.topic_levels;
		
		
		// 1. Get the similarity computation for nodeA and all other nodes in the collection
		//    Considers only nodes NOT IN THE SAME BOOK!
		BookNode[] array_nodes = C.toArray();
		int s = array_nodes.length;
		double[] similarity_vector = new double[s];
		similarity_vector[0] = 0; // the first node is always the collection node
		for (int i=1;i<s;i++){
			BookNode nodeB = array_nodes[i];
			
			//if(C.areInTheSameBook(nodeA,nodeB)){
			if(C.getBookNumber(nodeB) != nbook2){
				similarity_vector[i] = 0;
			}else{
				similarity_vector[i] = nodeA.computeSimilarity(nodeB, "COSINE", using_tfidf, C.topic_weights, is_hlda, t_levels);
				
			}
			//System.out.print(", "+similarity_vector[i]);
		}
		//System.out.print(" "+similarity_vector.length+" ");
		// 2. Get the highest m values in similarity_vector and fill array r with the
		//    BookNodes corresponding to the indices 
		int[] highest_indexes = getNHighestIndexes(m, similarity_vector);
		//System.out.print(" "+m);
		double[] highest_values = new double[m]; // not used!!!!
		for(int i=0;i<m;i++) {
			highest_values[i] = similarity_vector[highest_indexes[i]];
			r[i] = array_nodes[highest_indexes[i]];
		}

		return r;		
	}
	
	// get the top m similar nodes to nodeA computing similarity using 1/KL-Divergence
	public static BookNode[] getMostSimilarByKLDivergence(Collection C, BookNode nodeA, int m, boolean is_hlda){
		BookNode[] r = new BookNode[m];
		
		// level indexes for the topic arrays for hlda 
		int[] t_levels = null;
		if (is_hlda) t_levels = C.hlda_util.topic_levels;
		
		// 1. Get the similarity computation for nodeA and all other nodes in the collection
		//    Considers only nodes NOT IN THE SAME BOOK!
		BookNode[] array_nodes = C.toArray();
		int s = array_nodes.length;
		double[] divergence_vector = new double[s];
		double[] similarity_vector = new double[s];
		similarity_vector[0] = 0.0; // the first node is always the collection node
		//double max_divergence = 0.0;
		for (int i=1;i<s;i++){
			BookNode nodeB = array_nodes[i];
			//if(C.areInTheSameBook(nodeA,nodeB)){
			if(C.getBookNumber(nodeB) != nbook2){
				similarity_vector[i] = 0.0;
				
			}else{
				divergence_vector[i] = nodeA.computeSimilarity(nodeB, "KL-DIVERGENCE", using_tfidf, C.topic_weights, is_hlda, t_levels);
				//if ()
				similarity_vector[i] = 1.0/divergence_vector[i];
				//if (max_divergence<divergence_vector[i]) max_divergence = divergence_vector[i];
			}
			//System.out.print(", "+similarity_vector[i]);
		}
		//System.out.print(" "+similarity_vector.length+" ");
		// 2. Get the highest m values in similarity_vector and fill array r with the
		//    BookNodes corresponding to the indices 
		int[] highest_indexes = getNHighestIndexes(m, similarity_vector);
		//System.out.print(" "+m);
		double[] highest_values = new double[m]; // not used!!!!
		for(int i=0;i<m;i++) {
			highest_values[i] = similarity_vector[highest_indexes[i]];
			r[i] = array_nodes[highest_indexes[i]];
		}

		return r;		
	}
	
	public static void printTruth(String[][] truth){
		for (int i=0;i<truth.length;i++){
			System.out.print(truth[i][0]);
			for(int j=1;j<truth[i].length;j++){
				System.out.print("," + truth[i][j]);
			}
			System.out.println();
		}
	}
	
	public static String[][] readTruthFile(String truth_file){
		ArrayList<String> lines = new ArrayList<String>();
		String[][] r = null;
		try{
			FileReader fr = new FileReader(truth_file);
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			while((line=br.readLine())!=null){
				lines.add(line.toString());
			}
			br.close();
			fr.close();
			int n = lines.size();
			//int m = lines.get(0).toString().split(",").length;
			r = new String[n][];
			for (int i=0;i<n;i++){
				String[] item = lines.get(i).toString().split(",");
				int m = item.length;
				r[i] = new String[m];
				for(int j=0;j<m;j++){
					r[i][j] = item[j].trim();
				}
			}
			
		}catch(Exception e){
			
		}
		return r;
	}
	
	
	public static int[] getNHighestIndexes(int n, double[] values){
		int[] r = createIntArray(0,n);
		for(int c=0;c<n;c++){
			double max = 0.0;
			int max_index = 0;
			for(int i=0;i<values.length;i++){
				if(values[i]>max && !containsIndex(r,i)){
					max = values[i];
					max_index = i;
				}
			}
			r[c] = max_index;
		}
		return r;
	}
	
	public static int[] createIntArray(int value, int n){
		int[] r = new int[n];
		for(int i=0;i<n; i++){
			r[i] = value;
		}
		return r;
	}
	
	// @@@@
	public static boolean containsIndex(int[] a, int index){
		if (a == null || a.length==0) return false;
		for(int i=0;i<a.length; i++){
			if (a[i] == index) return true;
		}
		return false;
	}
	
	public static boolean containsBookNode(BookNode[] a, BookNode b){
		if (a == null || a.length==0) return false;
		for(int i=0;i<a.length; i++){
			if (a[i] == b) return true;
		}
		return false;
	}
	
	public static int getIndex(BookNode[] a, BookNode b){
		if (a == null || a.length==0) return -1;
		for(int i=0;i<a.length; i++){
			if (a[i] == b) return i;
		}
		return -1;
	}
	
/*	
	public static double computeKLDistance(double[] topic1, double[] topic2){
		double kl = 0.0;
		// first some checking
		if (topic1 == null || topic2 == null) return 99999999999.0;
		
		if (topic1.length != topic2.length) return 99999999999.0;
		
		int k = topic1.length;
		if (k == 0) return 99999999999.0;
		
		for (int i=0;i<k;i++){
			kl += (topic1[i]*log2(1.0*topic1[i]/topic2[i]))/2.0;
			kl += (topic2[i]*log2(1.0*topic2[i]/topic1[i]))/2.0;
		}
		
			
		return kl;
	}
*/	
	
	public static double log2(double num){
		return (Math.log(num)/Math.log(2));
	}
}
