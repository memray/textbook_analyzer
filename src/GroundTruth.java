import cc.mallet.util.FileUtils;

import java.io.*;
import java.util.ArrayList;

public class GroundTruth {
	public static final int SIMPLE_RANK = 0; // computes scores of each match by multiplying relevance and confidence
	public static final int SIMPLE_RANK_NORMALIZED = 1; // computes scores of each match by multiplying relevance and confidence, and then divides by the highest score in the list
	
	ArrayList<NodeMap> map;
	NodeRank[] node_ranks;
	
	public GroundTruth(){
		this.map = new ArrayList<NodeMap>();
		node_ranks = null;
	}
	
	
	public NodeMap searchNodeInMap(String node_id){
		NodeMap res = null;
		if (map.size()>0){
			for(NodeMap nodemap : map){
				if(nodemap.getNodeId().equalsIgnoreCase(node_id)){
					return nodemap;
				}
			}
		}
		return res; 
	}
	
	public NodeRank searchNodeInRanks(String node_id){
		NodeRank res = null;
		if (node_ranks.length>0){
			for(NodeRank noderank : node_ranks){
				if(noderank.getNodeId().equalsIgnoreCase(node_id)){
					return noderank;
				}
			}
		}
		return res; 
	}
	public static GroundTruth readMapFiles(String truth_dir, int mode){
		GroundTruth truth = null;
		File dir = new File(truth_dir);
		
		if (dir != null){
			String[] filenames = dir.list();
			if (filenames == null || filenames.length == 0)
                return null;
			else{
				// Creates the ground truth object
				truth = new GroundTruth();

				for (int i=0; i<filenames.length; i++) {
			        String filename = filenames[i];
			        if (!filename.startsWith(".")){
			        	String filename_ne = filename.substring(0,filename.indexOf(".txt"));
			        	int ind = filename_ne.indexOf(".html_");
			        	String user = filename_ne.substring(ind+6,ind+11);
			        	truth.readFromFile(truth_dir+"/"+filename, user);
			        	//System.out.println(user);
			        }
			    }
				truth.computeRanks(mode);
				//System.out.println(book.treeToString(""));
			}
		}
		

		return truth;
	
	}
	
	public void readFromFile(String filename, String user){
		try{
			BigFile file = new BigFile(filename);
			for (String line : file){
				if (line.length()>4){
					String[] nodesstr = line.split(";");
					String node1_id = nodesstr[0];
					//System.out.println(node1_id);
					for(int j=1;j<nodesstr.length;j++){
						if (nodesstr[j].length()>2){
							String[] matchstr = nodesstr[j].split(",");
							String node2_id = matchstr[0];
							//System.out.println("  "+node2_id);
							int r = Integer.parseInt(matchstr[1]);
							int c = Integer.parseInt(matchstr[2]);
							this.addMatch2Map(node1_id, node2_id, user, r, c);
							//System.out.println("  "+node2_id+"("+r+","+c+")");
						}
					}
				}
			}
		}catch(Exception e){
			
		}
	}
	
	public void printMap(){
		String out = "";
		for(NodeMap nodemap : map){
			out += nodemap.getNodeId()+"\n";
			for(Match match : nodemap.matchs){
				out += "  "+match.getNodeId()+"\n";
				int c = match.getCount();
				//System.out.println(c);
				for (int i=0;i<c;i++){
					out += "    "+match.getUsers()[i]+" r:"+match.getR()[i]+" c:"+match.getC()[i]+"\n";
				}
			}
		}
		System.out.println(out);
	}

	public String printMapByLines(){
		String out = "";
		for(NodeMap nodemap : map){
			out += nodemap.getNodeId()+"\t";
			for(Match match : nodemap.matchs){
                int c = match.getCount();
                int r = 0;
                for (int i=0;i<c;i++)
                    r += match.getR()[i];

				out += match.getNodeId()+":"+(float)r/c+",";
			}
			out = out.substring(0, out.length()-1);
			out+="\n";
		}
		return out;
	}

	public void addMatch2Map(String node1, String node2, String user, int r, int c){
		NodeMap nodemap = searchNodeInMap(node1);
		if (nodemap == null){
			nodemap = new NodeMap(node1);
			map.add(nodemap);
		}
		Match match = nodemap.searchMatch(node2);
		if (match==null){
			match = new Match(node2);
			nodemap.matchs.add(match);
		}
		//System.out.println("adding "+node1+" - "+node2+"("+r+","+c+")");
		match.addMatch(user, r, c);
		
	}
	
	// mode: 
	// 0: simple rank
	public void computeRanks(int mode){
		int n1 = map.size();
		this.node_ranks = new NodeRank[n1];
		for(int i=0;i<n1;i++){
			NodeMap nodemap = map.get(i);
			this.node_ranks[i] = new NodeRank(nodemap.getNodeId());
			switch (mode){
				case 0:
					for(Match match: nodemap.matchs){
						String node2 = match.getNodeId();
						double score = 0.0;
						
						for(int j=0;j<match.getCount();j++){
							score += match.getR()[j]*match.getC()[j];
						}
						this.node_ranks[i].addNode(node2, score);
					
					}
					break;
				case 1:
					for(Match match: nodemap.matchs){
						String node2 = match.getNodeId();
						double score = 0.0;
						
						for(int j=0;j<match.getCount();j++){
							score += match.getR()[j]*match.getC()[j];
						}
						this.node_ranks[i].addNode(node2, score);
					}
					this.node_ranks[i].normalizeScores();
					break;
			}
			this.node_ranks[i].orderRank();
		}

	}
	
	public void printMapStats(String domain){
		int n1 = map.size();
		this.node_ranks = new NodeRank[n1];
		String output = "Domain: "+domain;
		
		output += " - TOTAL: "+n1+" nodes in the mapping.";
		String nodes_list = "node,matches,users,nagree\n";
		int totalmatchs = 0;
		for(int i=0;i<n1;i++){
			NodeMap nodemap = map.get(i);
			int nusers = 0;
			
			int nagree = 0;
			int ndisagree = 0;
			int nnodesb = nodemap.matchs.size(); 
			for(Match match: nodemap.matchs){
				String node2 = match.getNodeId();
				int nmatchs = match.getCount();
				if (nusers<nmatchs) nusers=nmatchs;
				if (nmatchs>1) nagree++;
				else ndisagree++;
				totalmatchs++;
				//for(int j=0;j<nmatchs;j++){
					
					//if (match.users.length>1) nagree++;
					
				//}
				
			
			}
			nodes_list += nodemap.getNodeId()+","+nnodesb+","+nusers+","+nagree+"\n";
			
		}
		System.out.println(output+"\n"+nodes_list);
	}
	// automap contains each automatically mapped node ids from book 1 with a list of node ids from book 2.
	// p s the parameter fro nDCG
	public double evaluateMapNDCG(String[][] automap, int p){
		int n1 = automap.length;
		int total = 0;
		double avg_ndcg = 0.0;
		for (int i=0;i<n1;i++){
			
			String nodeA = automap[i][0];
			//System.out.println("-----------------------------------------------");
			//System.out.print("EVAL NODE: " + nodeA + " ");
			int n2 = automap[i].length;
			if (n2 > 1){
				String[] nodes_matched = new String[n2-1];
				for(int j=1;j<n2;j++){
					nodes_matched[j-1] = automap[i][j];
				}
				NodeRank node_rank = this.searchNodeInRanks(nodeA);
				if (node_rank != null){
					//System.out.println("(found in ground truth!)\nTruth:");
					//System.out.println(node_rank.toString());
					//System.out.println("Mapped by model:");
					//for(int k=0;k<nodes_matched.length;k++){
					//	System.out.println("    " + nodes_matched[k]);
					//}
					double ndcg = node_rank.computeNDCG(p, nodes_matched);
					//double ndcgPow2 = node_rank.computeNDCGPow2(p, nodes_matched);
					//System.out.println("\nnDCG(" + p + ")       = " + ndcg);
					//System.out.println("pow 2 nDCG(" + p + ") = " + ndcgPow2);
					//System.out.println();
					avg_ndcg += ndcg;
					total++;
				}
			}
		}
		//System.out.println("total nodes evaluated in book 1: " + total);
		return (avg_ndcg / total);
		
	}
	
	
	public static void main(String args[]){
		
/*		String[][] automap = {
					{"book1_ch01_2","book2_chapter1_1_1","book2_chapter1_2_2","book2_chapter2_2_2"},
					{"book1_ch01_2_2","book2_chapter1_3_1","book2_chapter1_6_1","book2_chapter1_2_2"},
					{"book1_ch01_3","book2_chapter1_1_3","book2_chapter1_1_5"},
					{"book1_ch01_3_2","book2_chapter5_6_2"},
					{"book1_ch01_3_3","book2_chapter5_1_1","book2_chapter1_1_5"},
					{"book1_ch01_4","book2_chapter1_3","book2_chapter1_4_1"}
				};*/
//		GroundTruth gt0 = GroundTruth.readMapFiles("data/algebra/truth",SIMPLE_RANK_NORMALIZED);
        String filePath = "/home/memray/Project/textbook_analyzer/data/julio_data/information_retrieval/expert_mapping/truth";
        GroundTruth gt0 = GroundTruth.readMapFiles(filePath,SIMPLE_RANK_NORMALIZED);
		System.out.println(gt0.printMapByLines());

        String outputPath = "/home/memray/Project/textbook_analyzer/data/ir_groundtruth.txt";
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputPath), false)));
            writer.write(gt0.printMapByLines());
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
//		gt0.printMapStats("Algebra");
//		gt0.printMapStats("Information Retrieval");
//		gt0.printMap();
		//double avg_ndcg = gt0.evaluateMapNDCG(automap, 10);
		//System.out.println("AVG nDCG : " + avg_ndcg);
		
		
		//gt0.printMap();
		//NodeRank[] rank = gt0.getRank(0);
		//for (int i=0;i<rank.length;i++){
		//	System.out.println(rank[i].toString());
			
		//}
		
	}
	
	// inner classes
	public class NodeMap{
		String node_id; // the node from book 2
		ArrayList<Match> matchs;
		public NodeMap(String node_id){
			this.node_id = node_id;
			matchs = new ArrayList<Match>();
		}
		public String getNodeId() {
			return node_id;
		}
		public void setNodeId(String node_id) {
			this.node_id = node_id;
		}
		
		public Match searchMatch(String node_id){
			Match res = null;
			if (matchs.size()>0){
				for(Match match : matchs){
					if(match.getNodeId().equalsIgnoreCase(node_id)){
						return match;
					}
				}
			}
			return res; 
		}
		
	}
	
	public class Match{
		String node_id; // the node from book 2
		int[] r; // relevance
		int[] c; // confidence
		String[] users;
		int count;
		public Match(String node_id){
			this.node_id = node_id;
			r = new int[20];
			c = new int[20];
			users = new String[20];
			count = 0;
		}
		
		public void addMatch(String user, int r, int c){
			this.r[count] = r;
			this.c[count] = c;
			this.users[count] = user;
			count++;
		}

		public int[] getR() {
			return r;
		}

		public int[] getC() {
			return c;
		}

		public String[] getUsers() {
			return users;
		}

		public int getCount() {
			return count;
		}

		public String getNodeId() {
			return node_id;
		}
		public void setNodeId(String node_id) {
			this.node_id = node_id;
		}

	}
	
	public class NodeRank{
		String node_id; // the node from book 1
		double[] scores;
		String[] nodes;
		int count = 0;
		public NodeRank(String nodeid){
			this.node_id = nodeid; 
			scores = new double[200];
			nodes = new String[200];
		}
		
		public void addNode(String nodeid, double score){
			scores[count] = score;
			nodes[count] = nodeid;
			count++;
		}
		
		public void normalizeScores(){
			double max_score = 0.0;
			for(int i=0;i<count;i++){
				if(max_score < scores[i]) max_score = scores[i];
			}
			for(int i=0;i<count;i++){
				scores[i] = scores[i] / max_score;
			}
		}
		
		// simple bubble sort for scores
		public void orderRank(){
			this.trimArrays();
			for(int i=0;i<count;i++){
				for(int j=0;j<count-1;j++){
					if (scores[j]<scores[j+1]){
						double tmpscore = scores[j];
						String tmpnode = nodes[j];
						scores[j] = scores[j+1];
						nodes[j] = nodes[j+1];
						scores[j+1] = tmpscore;
						nodes[j+1] = tmpnode;
					}
				}
			}
		}

		private void trimArrays(){
			double[] newscores = new double[count];
			String[] newnodes = new String[count];
			for(int i=0;i<count;i++){
				newscores[i] = scores[i];
				newnodes[i] = nodes[i];
			}
			scores = newscores;
			nodes = newnodes;
		}
		
		public String getNodeId() {
			return node_id;
		}

		public int getCount() {
			return count;
		}

		public double[] getScores(){
			return scores; 
		}
		public String[] getNodes(){
			return nodes;
		}
		public String toString(){
			String r = this.getNodeId() + "   iDCG(1):" + this.getIDCG(100) + "   iDCG(2):" + this.getIDCGPow2(100) + "\n";
			for(int i=0;i<count;i++){
				r += "    " + scores[i] + " : " + nodes[i]  + "\n";
			}	
			return r;
		}
		
		public double getIDCG(int p){
			double res = 0.0;
			int n = p;
			if (count<p) n = count;
			res += scores[0];
			for (int i=1;i<n;i++){
				res += (scores[i]) / log2(i+1);
			}
			return res;
		}
		public double getIDCGPow2(int p){
			double res = 0.0;
			int n = p;
			if (count<p) n = count;
			
			for (int i=0;i<n;i++){
				res += (Math.pow(2, scores[i])-1) / log2(i+2);
			}
			return res;
		}
		
		// nodes_matched should be ordered by relevance
		public double computeNDCG(int p, String[] nodes_matched){
			double dcg = 0.0;
			//int n = p;
			//if (count<p) n = count;
			if (nodes_matched.length<p) p=nodes_matched.length;
			dcg += getScoreForNode(nodes_matched[0]);
			for(int i=1;i<p;i++){
				String node = nodes_matched[i];
				dcg += getScoreForNode(node)/log2(i+1);
			}
			double idcg = this.getIDCG(p);
			if (idcg == 0.0) return 0.0;
			return dcg / idcg;
		}

		public double computeNDCGPow2(int p, String[] nodes_matched){
			double dcg = 0.0;
			if (nodes_matched.length<p) p=nodes_matched.length;
			for(int i=0;i<p;i++){
				String node = nodes_matched[i];
				dcg += (Math.pow(2,getScoreForNode(node))-1)/log2(i+2);
			}
			double idcg = this.getIDCGPow2(p);
			if (idcg == 0.0) return 0.0;
			return dcg / idcg;
		}

		public double getScoreForNode(String node){
			for(int i=0;i<count;i++){
				if (nodes[i].equalsIgnoreCase(node)) return scores[i];
			}
			return 0.0;
		}
		
	} // end class NodeRank
	
	public static double log2(double num){
		return (Math.log(num)/Math.log(2));
	}
}
