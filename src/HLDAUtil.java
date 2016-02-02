import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;

import cc.mallet.util.*;
import cc.mallet.types.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.topics.*;
import cc.mallet.topics.HierarchicalLDA.NCRPNode;

import java.util.*;
import java.util.logging.Level;
import java.util.regex.*;
import java.io.*;

public class HLDAUtil {
	public HierarchicalLDAInferencer inferencer = null;
	public int[] topic_ids = null;
	public int[] topic_levels = null;
	
	private int c = 0;
	
	public HLDAUtil(HierarchicalLDAInferencer inferencer){
		this.inferencer = inferencer;
		int n = countTopics();
		//System.out.println("n topics: "+n);
		topic_ids = new int[n];
		topic_levels = new int[n];
		c = 0;
		NCRPNode root = inferencer.rootNode;
		fillTopicIdsArray(root,0);
		//System.out.println("topics ids: "+n);
		//printArray(topic_ids);
	}
	
	// get the topic distribution among the used topics
	public double[] getTopicDistribution(NCRPNode leave, int[] levels){
		double[] td = new double[topic_ids.length];
			
		NCRPNode node = leave;
		int numLevels = inferencer.numLevels;
		int totalNodes = inferencer.totalNodes;
		double alpha = inferencer.alpha;
		
		NCRPNode[] path = new NCRPNode[numLevels];
		for (int level = numLevels - 1; level >= 0; level--) {
			path[level] = node;
			node = node.parent;
		}
		 
		//double[] result = new double[totalNodes];
		 
		int[] levelCounts = new int[numLevels];
		for (int i = 0; i < levels.length; i++) {
			levelCounts[ levels[i] ]++;
		}
		 
		double sum =0.0;
		for (int level=0; level < numLevels; level++) {	 
			sum+=(alpha + levelCounts[level]);
		}
		 
		//for (int level=0; level < numLevels; level++) {	 
		//	result[ path[level].nodeID ] = (double)(alpha + levelCounts[level]/sum) ;
		//}
		 
		for(int i=0; i < numLevels; i++){
			//out.append("Level:" + i + ",");
			int index = getTopicIndexById(path[i].nodeID);
			double prob = (alpha + levelCounts[i])/sum;
			td[index] = prob;
		}
		
		return td;
	}
	
	// use the document index (int) from the the training set of instances
	public double[] getTopicDistribution(int doc_index){
		
		NCRPNode leave = inferencer.golbalDocumentLeaves[doc_index];
		int[] levels = inferencer.globalLevels[doc_index];
		
		return getTopicDistribution(leave, levels);
	}
	
	public double[] getSampledDistribution(Instance instance, int numIterations, int thinning, int burnIn) {
		
		//chceck instance
		if (! (instance.getData() instanceof FeatureSequence)) {
			throw new IllegalArgumentException("Input must be a FeatureSequence");
		}		
		
		NCRPNode node;
		inferencer.fs = (FeatureSequence) instance.getData();
		int docLength = inferencer.fs.size();
		
		inferencer.localLevels = new int[docLength];
		NCRPNode[] path = new NCRPNode[inferencer.numLevels];
		
	    //initialize
		//1.generate path
		path[0] = inferencer.rootNode;
		for (int level = 1; level < inferencer.numLevels; level++) {
			path[level] = path[level-1].selectExisting();
		}
		inferencer.leaveNode = path[inferencer.numLevels-1];
		
		//2. randomly put tokens to different levels
		for (int token=0; token < docLength; token++) {
			int type = inferencer.fs.getIndexAtPosition(token);
			
			//ignore words otside dctionary
			if (type >= inferencer.numTypes) {
				//System.out.println("type:" + type + "ignored."); 
				continue; 
			}
			
			inferencer.localLevels[token] = inferencer.random.nextInt(inferencer.numLevels);
			node = path[ inferencer.localLevels[token] ];
			node.totalTokens++;
			node.typeCounts[type]++;			
		}	
		
		//for each iteration
		for (int iteration = 1; iteration <= numIterations; iteration++) {
			//1.sample path
			inferencer.samplePath();
			//2.sampe topics
			inferencer.sampleTopics();
		}
		
		
		return getTopicDistribution(inferencer.leaveNode, inferencer.localLevels);
	}
	
	
	
	public int getTopicIndexById(int id){
		for (int i=0;i<topic_ids.length;i++){
			if (id == topic_ids[i]) return i;
		}
		return -1;
	}
	
	 public String trainDataToString(InstanceList instances) {
		String r = "";
		for(int i=0 ; i<instances.size(); i++){
			double[] td = getTopicDistribution(i);
			r += (String)instances.get(i).getName()+": ";
			for (int j=0;j<td.length;j++){
				r  += ModelReporter.formatter.format(td[j]) + ", ";
			}
			r += "\n";
		}
		return r;
	 }

	 public String testDataToString(InstanceList instances) {
		String r = "";
		for(int i=0 ; i<instances.size(); i++){
			Instance instance = instances.get(i);
			double[] td = getSampledDistribution(instance, 500, 100, 100);
			r += (String)instances.get(i).getName()+": ";
			for (int j=0;j<td.length;j++){
				r  += ModelReporter.formatter.format(td[j]) + ", ";
			}
			r += "\n";
		}
		return r;
	 }
	
	public void fillTopicIdsArray(NCRPNode node, int level){
		topic_ids[c] = node.nodeID;
		topic_levels[c] = level;
		c++;
		for (NCRPNode child: node.children) {
			fillTopicIdsArray(child, level+1);
		}
	}
	
	public void printArray(int[] arr){
		for (int i=0;i<arr.length;i++){
			System.out.print(arr[i]+", ");
		}
		System.out.println();
	}
	
	public int countTopics(){
		int res = 0;
		NCRPNode root = inferencer.rootNode;
		if (root != null){
			res = 1;
			res += countChildren(root);
			
		}
		return res;
	}
	
	public int countChildren(NCRPNode node){
		int r = 0;
		for (NCRPNode child: node.children) {
			r++;
			r += countChildren(child);
		}
		return r;
	}
	
	// creates an array of weights. Each weight correspond to a topic and all topic in a level have the same weight
	// the method is created to weight topics differently regarding their levels
	// in the tree. 
	public double[] getTopicWeights(double[] level_weights){
		int n = topic_levels.length;
		int m = level_weights.length;
		double[] tw = new double[n];
		for (int i=0;i<n;i++){
			int level = topic_levels[i];
			if (level < m){
				tw[i] = level_weights[level];
				//System.out.println(tw[i]);
			}
		}
		return tw;
	}
	
    public String printTopicNodes(int nwords) {
    	String out = "level,id,total_tokens,customers,words\n" + printTopicNodes(this.inferencer.rootNode, 0, nwords);
		return out;
    }

    public String printTopicNodes(NCRPNode node, int indent, int nwords) {
		String out = "";
		for (int i=0; i<indent; i++) {
			out += "  ";
		}

		out += node.level + "," + node.nodeID + "," + node.totalTokens + "," + node.customers + ",";
		out += node.getTopWords(nwords) + "\n";
	
		for (NCRPNode child: node.children) {
			out += printTopicNodes(child, indent + 1, nwords);
		}
		return out;
    }	
	
}
