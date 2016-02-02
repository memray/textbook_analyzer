import cc.mallet.topics.TopicModelDiagnostics;
import cc.mallet.topics.TopicModelDiagnostics.TopicScores;


public class LDAJunkTopicRemoval {
	public Collection col;
	
	
	public static double[] weightTopics(Collection col){
		if (col.model_LDA == null) return null;
		int n = col.model_LDA.getNumTopics();
		
		double[] weights = new double[n];
		double[] tokens = new double[n];
		double[] doc_entropy = new double[n];
		double[] word_length = new double[n];
		double[] coherence = new double[n];
		double[] uniform_dist = new double[n];
		double[] corpus_dist = new double[n];
		double[] rank_1_docs = new double[n];
		
		double[] maxmin_weights = new double[2];
		double[] maxmin_tokens = new double[2];
		double[] maxmin_doc_entropy = new double[2];
		double[] maxmin_word_length = new double[2];
		double[] maxmin_coherence = new double[2];
		double[] maxmin_uniform_dist = new double[2];
		double[] maxmin_corpus_dist = new double[2];
		double[] maxmin_rank_1_docs = new double[2];
		
		TopicModelDiagnostics diagnostics = new TopicModelDiagnostics(col.model_LDA, 15);
		//TopicScores SDocEntropy = diagnostics.
		for (int topic = 0; topic < n; topic++) {
			for (TopicScores scores: diagnostics.getScores()) {
				// *tokens, *document_entropy, *uniform_dist, eff_num_words, *corpus_dist, 
				// token-doc-diff, *word-length, word-length-sd, *coherence, *rank_1_docs, 
				// allocation_ratio, allocation_count, 
				String name = scores.name;
				if (name.equalsIgnoreCase("tokens")) {
					tokens[topic] = scores.scores[topic];
					
				}
				if (name.equalsIgnoreCase("document_entropy")) {
					doc_entropy[topic] = scores.scores[topic];
				}
				if (name.equalsIgnoreCase("uniform_dist")) {
					uniform_dist[topic] = scores.scores[topic];
				}
				if (name.equalsIgnoreCase("corpus_dist")) {
					corpus_dist[topic] = scores.scores[topic];
				}
				if (name.equalsIgnoreCase("coherence")) {
					coherence[topic] = scores.scores[topic];
				}
				if (name.equalsIgnoreCase("rank_1_docs")) {
					coherence[topic] = scores.scores[topic];
				}
				if (name.equalsIgnoreCase("word_length")) {
					word_length[topic] = scores.scores[topic];
				}
			}
		}
		
		
		
		return weights;
	}
	public static int maxIndex(double[] array){
		double max = array[0];
		int maxi = 0;
		for (int i=0;i<array.length;i++){
			if (array[i]>max){
				max = array[i];
				maxi = i;
			}
		}
		return maxi;		
	}
}
