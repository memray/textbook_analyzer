import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;
public class BookNode {
	private String title;
	private String docid;
	private String text;
	
	private String raw_content;
	public String getRawContent() {
		return raw_content;
	}

	public void setRawContent(String raw_content) {
		this.raw_content = raw_content;
	}


	private int processed_text_length;
	private int tree_text_length;
	private String comments;
	
	private BookNode parentnode;
	
	public ArrayList<BookNode> children;
	
	private double[] topic_probs;
	private double[] tfidf;
	
	protected DecimalFormat formatter;
     

	public BookNode(String docid, String title, BookNode parent){
		this.docid = docid;
		this.title = title;
		
		this.children = new ArrayList<BookNode>();
		this.text = "";
		setParentNode(parent);
		formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
		formatter.applyPattern("#.###");
	}
	
	public String getTitle(){return title;}
	public String getDocId(){return docid;}
	public String getText(){return text;}
	public int getProcessedTextLength() {
		return processed_text_length;
	}
	public int getTreeTextLength() {
		return tree_text_length;
	}

	public String getComments(){return comments;}
	public double[] getTopicProbabilities() {
		return topic_probs;
	}

	public double[] getTfidf() {
		return tfidf;
	}
	
	public BookNode getParentNode(){return parentnode;}
	
	public void setTitle(String title){this.title = title;}
	public void setDocId(String docid){this.docid = docid;}
	public void setText(String text){this.text = text;}
	public void setProcessedTextLength(int processed_text_length) {
		this.processed_text_length = processed_text_length;
	}
	public void setComments(String comments){this.comments = comments;}
	public void setTopicProbabilities(double[] topic_probs) {
		this.topic_probs = topic_probs;
	}
	public void setTfidf(double[] tfidf) {
		this.tfidf = tfidf;
	}
	public void setParentNode(BookNode parentnode){
		if (parentnode != null) parentnode.add(this);
	}
	
	public void add(BookNode child){
		child.parentnode = this;
		this.children.add(child);
	}
	
	public ArrayList<BookNode> getChildren(){
		return children;
	}
	
	public String toStringWTopic(){
		String r = "("+docid+") "+title+" : ";
		double[] probs = this.getTopicProbabilities();
		if (probs != null && probs.length>0){
			for (int j=0;j<probs.length;j++){
				if (probs[j]>0.01) 
				r += "Topic "+j+": "+formatter.format(probs[j])+", ";
				
			}
		}
		return r;
	}
	
	public String toString(){
		
		return "("+docid+") "+title+" : "+text;
	}
	
	public String toLDAInputString(boolean only_sections_w_text){
		String str_line = docid + "\t" + "no label\t" + text;
		return str_line;
	}
	
	public boolean hasText(){
		if (text == null || text.length()<2) return false;
		return (Math.abs(title.length()-text.length())>150); 
	}
	
	public String getReducedTitle(int count){
		if (count < 5) count = 5;
		if (title.length()>count){
			return title.substring(0,count-3).trim()+"...";
		}
		return title;
	}
	
	
	public boolean hasChildren(){
		if (children != null && children.size()>0) return true;
		return false;
	}
	
	public String treeToString(String offset){
		String r = "";
		r = offset + toString();
		for (BookNode child : children){
			r += "\n" + child.treeToString(offset+"    ");
		}
			
		return r;
	}
	

	
	public String treeToLDAInputString(boolean only_sections_w_text){
		String r = "";
		if (only_sections_w_text){
			if (hasText() || !hasChildren()) r += toLDAInputString(only_sections_w_text)+"\n";
		}else{
			r += toLDAInputString(only_sections_w_text)+"\n";
		}
		
		if (this.hasChildren()){
			for (BookNode child : children){
				r += child.treeToLDAInputString(only_sections_w_text);
			}
		}
			
		return r;
	}
	
	public BookNode searchNodeById(String id){
		if (getDocId().equalsIgnoreCase(id)) return this;
		else{
			if (this.getChildren() != null && this.getChildren().size()>0){
				for(BookNode child : this.getChildren()){
					BookNode bn = child.searchNodeById(id);
					if (bn != null) return bn;
				}
			}
		}
		return null;
	}
	
	// @@@
	// Propagate text sizes to parents
	public int propagateTreeTextSizes(){
		this.tree_text_length = this.processed_text_length;
		if (this.hasChildren()){
			for (BookNode child : children){
				this.tree_text_length += child.propagateTreeTextSizes();
			}
		}
		return this.tree_text_length;
	}

	// parent probabilities = sum over children divided by the text size 
	public void propagateTopicAssignment2(){
		if (this.hasChildren()){
			for (BookNode child : children){
				child.propagateTopicAssignment2();
			}
			//this.normalizeProbabilities();
		}
		
		if (parentnode != null && topic_probs != null){
			int n = topic_probs.length;
			if (parentnode.getTopicProbabilities() == null){
				parentnode.setTopicProbabilities(new double[n]);
			}
			
			if (parentnode.getTfidf() == null){
				parentnode.setTfidf(new double[n]);
			}
			
			double[] my_tp = this.getTopicProbabilities();
			double[] parent_tp = parentnode.getTopicProbabilities();
			
			double[] my_tfidf = this.getTfidf();
			double[] parent_tfidf = parentnode.getTfidf();
			
			if (my_tp.length != parent_tp.length || my_tfidf.length != parent_tfidf.length){ return;}
			else{
				// n: number of topics
				for(int i=0;i<n;i++){
					parent_tp[i] += my_tp[i]*(this.getTreeTextLength())/parentnode.getTreeTextLength();
					parent_tfidf[i] += my_tfidf[i]; // topic-freq*idf are aggregated summing all children values to parent
				}
			}
			
		}
		
		//this.normalizeProbabilities();
	}

	// propagates probabilities from children to parent. Parent probability for the 
	// topic t will be the high probability of t among the children. 
	public void propagateTopicAssignment(){
		if (this.hasChildren()){
			for (BookNode child : children){
				child.propagateTopicAssignment();
			}
			this.normalizeProbabilities();
		}
			
		if (parentnode != null && topic_probs != null){
			int n = topic_probs.length;
			if (parentnode.getTopicProbabilities() == null){
				parentnode.setTopicProbabilities(new double[n]);
			}
			
			if (parentnode.getTfidf() == null){
				parentnode.setTfidf(new double[n]);
			}
			
			double[] my_tp = this.getTopicProbabilities();
			double[] parent_tp = parentnode.getTopicProbabilities();
			
			double[] my_tfidf = this.getTfidf();
			double[] parent_tfidf = parentnode.getTfidf();
			
			if (my_tp.length != parent_tp.length || my_tfidf.length != parent_tfidf.length){ return;}
			else{
				// n: number of topics
				for(int i=0;i<n;i++){
					if (parent_tp[i]<my_tp[i]) parent_tp[i] = my_tp[i]; // the higher probability of children propagate 
					
					parent_tfidf[i] += my_tfidf[i]; // topic-freq*idf are aggregated summing all children values to parent
				}
			}
			
		}
	}
	
	public void cutLowProbabilities(double threshold){
		double[] my_tp = this.getTopicProbabilities();
		for(int i=0;i<my_tp.length;i++){
			if (my_tp[i] < threshold){
				my_tp[i] = 0.0001;
			}
		}
	}
	
	public void normalizeProbabilities(){
		double[] my_tp = this.getTopicProbabilities();
		double sum = 0.0;
		for(int i=0;i<my_tp.length;i++){
			sum += my_tp[i];
		}
		for(int i=0;i<my_tp.length;i++){
			my_tp[i] = my_tp[i] / sum;
		}
	}
	
	public int size(){
		int s = 1;
		if (hasChildren()){
			//s += children.size();
			for(BookNode child : children){
				s += child.size();
			}	
		}
		return s;
	}
	
	public BookNode[] toArray(){
		int s = this.size();
		BookNode[] nodes_array = new BookNode[s];
		//BookNode[] nodes_array;
		ArrayList<BookNode> out_array = new ArrayList<BookNode>(s);
		this.toArrayList(out_array);
		int i = 0;
		for(Object out_object : out_array){
			BookNode out_node = (BookNode) out_object;
			nodes_array[i] = out_node;
			i++;
		}
			
		//nodes_array = out_array.toArray();
		//System.out.println(s);
		return nodes_array;
	}
	
	public void toArrayList(ArrayList<BookNode> out_array){
		out_array.add(this);
		for(BookNode child : children){
			child.toArrayList(out_array);
		}
	}

	// COMPUTES COSINE similarity
	public double cosineSimilarity(double[] A, double[] B, double[] W){
		if (A == null || A.length==0 || B == null || B.length == 0 || A.length != B.length) return 0.0;
		if (W == null || W.length==0 || W.length != A.length) return 0.0;
		
		double AxB = 0.0;
		double sumA2 = 0.0;
		double sumB2 = 0.0;
		
		for(int k=0;k<A.length;k++){
			if (W[k] > 0.001){ // if wi=0, then not consider the topic in the computation
				AxB += W[k]*W[k]*A[k]*B[k];
				sumA2 += W[k]*W[k]*A[k]*A[k];
				sumB2 += W[k]*W[k]*B[k]*B[k];				
			}
		}
		return AxB/(Math.sqrt(sumA2)*Math.sqrt(sumB2));
	}
	
	
	// COMPUTES symmetric KL Divergence
	public double KLDivergenceSimilarity(double[] A, double[] B, double[] W){
		double div = (1.0/2)*(divergence(A,B,W)+divergence(B,A,W));
		//System.out.print(" "+ModelReporter.formatter.format(div));
		return div;
	}
	
	// KL Divergence
	public double divergence(double[] A, double[] B, double[] W){
		if (W == null || W.length==0 || W.length != A.length) return 999999999;
		if (A == null || A.length==0 || B == null || B.length == 0 || A.length != B.length){
			
			System.out.println("   ---> Warning KL div for unpaired arrays!!!");
			return 99999999;
		}
		int n = A.length;
		double d = 0.0;
		
		for (int i=0;i<n;i++){
			if (W[i] > 0.001){ // not consider topics is weight is 0
				if (A[i] < 0.0001) A[i] = 0.0001;
				if (B[i] < 0.0001) B[i] = 0.0001;
				d += (1/W[i])*A[i]*log2(A[i]/B[i]); // weight with wi the contribution of the topic i to the divergence 
			}
		}
		return d;
			
	}
	
	public static double log2(double num){
		return (Math.log(num)/Math.log(2));
	}
	
	
	public double computeSimilarity(BookNode other_node, String mode, boolean using_tfidf, double[] topic_weights, boolean is_hlda, int[] t_levels){
		double[] A0 = null;
		double[] B0 = null;
		double[] A = null;
		double[] B = null;
		double[] W = null;
		
		if (using_tfidf){
			A0 = this.getTfidf();
			B0 = other_node.getTfidf();
			A0 = BookNode.normalizeVector(A);
			B0 = BookNode.normalizeVector(B);
		}else{
			A0 = this.getTopicProbabilities();
			B0 = other_node.getTopicProbabilities();			
		}
		// topic vectors are boosted using topic_weights - 
		// NO!!! It is better to use weight in similarity computation 
		// for consider more or less the topic matches
		
		
		// Similarity or divergence in HLDA should only compare pairs of topics for each
		// level which at least one document has a probability distinct of 0.0 
		// (each document is a single path, so has just one topic with some probability in each level) 
		if (is_hlda && t_levels != null){
			int c = 0;
			double[] tmpA = new double[A0.length];
			double[] tmpB = new double[B0.length];
			double[] tmpW = new double[A0.length];
			for(int i=0;i<A0.length;i++){
				if (A0[i] > 0.001 || B0[i] > 0.001){
					tmpA[c] = A0[i];
					tmpB[c] = B0[i];
					tmpW[c] = topic_weights[i];
					c++;
				}else{}
			}
			//System.out.println(c + "!=" + A0.length);
			// Shorter versions of the arrays only containing nodes having probabilities
			// in at least one of the two comparing nodes
			A = new double[c];
			B = new double[c];
			W = new double[c];
			for(int i=0;i<c;i++){
				A[i] = tmpA[i];
				B[i] = tmpB[i];
				W[i] = tmpW[i];
			}

		}else{
			A = new double[A0.length];
			B = new double[B0.length];
			W = new double[A0.length];
			for(int i=0;i<A0.length;i++){
				//A[i] = topic_weights[i]*A0[i];
				//B[i] = topic_weights[i]*B0[i];
				A[i] = A0[i];
				B[i] = B0[i];
				W[i] = topic_weights[i];
			}			
		}
		
		
		if (mode.equalsIgnoreCase("COSINE")){
			return cosineSimilarity(A,B,W);
		}
		if (mode.equalsIgnoreCase("KL-DIVERGENCE")){
			return KLDivergenceSimilarity(A,B,W);
		}
		
		return -1;
	}
	
	
	public void computeTFIDF(int[] topic_df, int N){
		double[] tp = this.getTopicProbabilities();
		int n = tp.length;
		double smooth = 0.001;
		if (n != topic_df.length) return;
		
		this.tfidf = new double[n];
		for (int i=0;i<n;i++){
			tfidf[i] = tp[i]*processed_text_length*Math.log(1.0*N/(topic_df[i]+smooth));
			if (tfidf[i]<0.02) tfidf[i] = 0.0001;
			//System.out.print(" "+ModelReporter.formatter.format(tfidf[i]));
		}

	}
	
	public String tree2HTML(String offset, int level, String html_dir, boolean include_input, String book){
		String r = "";
		String html = "";
		if (level==0){
			//r += "<h3>"+this.getTitle()+"</h3>\n";
		}else{
			r += offset+"<li class=\"node\" id=\"li_"+this.getDocId()+"\">\n";
			//r += offset+"<input type=\"hidden\" name=\""+this.getDocId()+"\" id=\""+this.getDocId()+"\" value=\""+this.getDocId()+"\" />\n";
			String title_class = "nodetitle_"+level;
			r += offset+"<div class=\"nodetitle_"+book+"\" id=\"title_"+this.getDocId()+"\">\n";
			r += offset+"<span class=\""+title_class+"\">"+this.getTitle()+"</span>\n";
			if (this.hasText()){
				//r += offset+"<span class=\"node_id\" id=\""+this.getDocId()+"\">"+this.getDocId()+"</span>&nbsp;<button class=\"open_html_content\" onclick=\"showContent(\'pop_"+this.getDocId()+"\');\">html</button>\n";
				r += "&nbsp;<button class=\"open_html_content\" onclick=\"showContent(\'pop_"+this.getDocId()+"\',\'"+this.getDocId()+".html\',\'"+this.getTitle()+"\'); return false;\">html</button>\n";
				if (include_input) r += offset+"<input size=\"25\" type=\"hidden\" class=\"input_node\" name=\""+this.getDocId()+"\" id=\""+this.getDocId()+"\" value=\"\" />\n";
				r += "&nbsp;<span class=\"book_node_id\">["+this.getDocId()+"]</span>\n";
				r += offset+"</div>\n";
				//r += offset+"<div class=\"dialog html_content\" id=\"pop_"+this.getDocId()+"\">\n"+this.getRawContent()+"\n</div>\n";
				r += offset+"<div class=\"dialog html_content\" id=\"pop_"+this.getDocId()+"\"></div>\n";
				html = this.getRawContent();
				ModelReporter.writeFile(html, html_dir+"/"+this.getDocId()+".html");
			}else{
				if (include_input) r += offset+"<input size=\"25\" type=\"hidden\" class=\"input_node\" name=\""+this.getDocId()+"\" id=\""+this.getDocId()+"\" value=\"\" />\n";
				r += "&nbsp;<span class=\"book_node_id\">["+this.getDocId()+"]</span>\n";

				r += offset+"</div>\n";
			}
			
		}
		if (hasChildren()){
			r += offset+"  <ul>\n";
			for(BookNode child : children){
				r += child.tree2HTML(offset+"  ",level+1, html_dir, include_input, book);
			}
			r += offset+"  </ul>\n";
		}
		if (offset.length()>0) r += offset+"</li>\n";
		return r;
	}
	
	public void setUniformTopicDistribution(int n_topics){
		double[] probs = new double[n_topics];
		double[] tfidf = new double[n_topics];
		for(int i=0;i<n_topics; i++){
			probs[i] = 1.0/n_topics;
			tfidf[i] = 1.0/n_topics;
		}
		this.setTopicProbabilities(probs);
		this.setTfidf(tfidf);
	}
	
	public void aggregateContent(){
		BookNode[] treepart = this.toArray();
		String agg_content = this.getText();
		for(int i=0;i<treepart.length;i++){
			agg_content += treepart[i].getText() + " || ";
		}
		this.setText(agg_content);
		if (hasChildren()){
			for(BookNode child : children){
				child.aggregateContent();
			}
		}
	
	}
	
	public static double[] normalizeVector(double[] A){
		int n = A.length;
		double[] res = new double[n];
		double sum = 0.0;
		for(int i=0;i<n;i++){
			sum += A[i];
		}
		for(int i=0;i<n;i++){
			res[i] = A[i] / sum;
		}
		return res;
	}

}
