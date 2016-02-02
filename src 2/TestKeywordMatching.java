import java.text.DecimalFormat;
import java.util.Locale;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version; 

import java.io.IOException;
import java.io.Reader;

public class TestKeywordMatching {
	private static DecimalFormat formatter;
	private static String domain_folder = "information_retrieval";// algebra
	private static String domain = "Information Retrieval";// Algebra
	private static String domainID = "ir";// algebra
	static String condition = "condition_keyword"; // the folder to save the output
	static boolean aggregatecontent = true; // Set to true for keyword matching
	static boolean using_vocabulary = false;  // set to true to tokenize n-word defined in the file vocabulary_file
	static boolean stemming = false; // set to true to perform stemming
	static boolean repeat_titles = true; // set to true to repeat titles in nodes as part of the text 
	static String truthdir = "data/"+domain_folder+"/truth"; // ground truth file containing mapping between books 1 and 2
	static String vocabulary_file = "data/algebra_nterm_vocabulary.txt"; // file containing a list of n-word tokens to consider
	static boolean addotherbooks = false;
	
	// index objects
	static IndexAnalyzer indexanalyzer;
	static Directory indexdir;
	static IndexWriterConfig indexconfig;
	static IndexWriter indexwriter;
	
	static{
		formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
		formatter.applyPattern("#.####");
	}
	// MAIN
	public static void main(String[] args) throws IOException, ParseException{
		
		String eval_ndcg_cosine_1 = "";
		String eval_ndcg_cosine_3 = "";
		String eval_ndcg_cosine_10 = "";
		String eval_ndcg_divergence_1 = "";
		String eval_ndcg_divergence_3 = "";
		String eval_ndcg_divergence_10 = "";
		String eval_string = "";
		
		try{
			
			// 1. CREATE THE COLLECTION OBJECT AND FILL IT WITH THE BOOKS
			Collection col = new Collection("Algebra","algebra_collection");
			// stemming here is managed by the lucene indexer. so put false for stemming in preprocessing
			RunModel.fillCollection(col,domainID,domain_folder,addotherbooks,using_vocabulary,repeat_titles,false,vocabulary_file,aggregatecontent);
			
			
			// 2. INDEX Book 2 with LUCENE
			setUpIndex();
			System.out.print("Indexing Book 2...");
			indexBook(col, 2);
			System.out.println("Ok");
			// 7. QUERY For each Book 1 node in the ground truth and compute NDCG 
			
			double[] ndcgs = evaluateNDCG(col, truthdir);
			
			System.out.println("NDCG(1): "+ formatter.format(ndcgs[0]));
			System.out.println("NDCG(3): "+ formatter.format(ndcgs[1]));
			System.out.println("NDCG(10): "+ formatter.format(ndcgs[2]));
			// 8. SUMMARIZE 
			
			//ModelReporter.writeFile(eval_cosine, "data/output/"+condition+"/eval_COSINE.txt");
			//ModelReporter.writeFile(eval_divergence, "data/output/"+condition+"/eval_DIVERGENCE.txt");

	        // Write the output

			//ModelReporter.writeVocabularyList(col, "data/output/"+condition+"/alphabet.txt");
			

		}catch(Exception e){
			System.out.println(e.getMessage());
		}
	}
	
	
	
	public static BookNode[] getBook(Collection col, int booknumber){
		BookNode book = col.children.get(booknumber-1);
		
		BookNode[] nodes = book.toArray();
		return nodes;
		
	}
	
	public static void setUpIndex() throws IOException, ParseException {
		indexanalyzer = new IndexAnalyzer();
		indexdir = new RAMDirectory();
		indexconfig = new IndexWriterConfig(Version.LUCENE_35, indexanalyzer);
		BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
		
	}
	
	public static void indexBook(Collection col, int booknumber) throws IOException{
		BookNode[] book = getBook(col, booknumber);
		indexwriter = new IndexWriter(indexdir, indexconfig);
		if (book != null){
			// starting in 1 -> not considering the book (top node)
			for(int i=1;i<book.length;i++){
				addDoc(indexwriter, book[i].getDocId(), book[i].getTitle(), book[i].getText());
			}
		}
		indexwriter.close();
	}
	
	private static void addDoc(IndexWriter w, String docid, String title, String text) throws IOException {
	    Document doc = new Document();
	    doc.add(new Field("docid", docid, Field.Store.YES, Field.Index.NOT_ANALYZED));
	    doc.add(new Field("title", title, Field.Store.YES, Field.Index.ANALYZED));
	    doc.add(new Field("text", text, Field.Store.YES, Field.Index.ANALYZED));
	    w.addDocument(doc);
	}
	
	private static String[] queryIndex(BookNode node,int m) throws IOException, ParseException{
		String[] results = null;
		String querystr = node.getText();
		Query q = new QueryParser(Version.LUCENE_35, "text", indexanalyzer).parse(QueryParser.escape(querystr));
		int hitsPerPage = 10;
	    IndexReader reader = IndexReader.open(indexdir);
	    IndexSearcher searcher = new IndexSearcher(reader);
	    TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
	    searcher.search(q, collector);
	    ScoreDoc[] hits = collector.topDocs().scoreDocs;
	    
	    //System.out.println("Found " + hits.length + " hits.");
	    int n = m;
	    if (hits.length<n) n=hits.length;
	    results = new String[n];
	    for(int i=0;i<n;++i) {
	      int docId = hits[i].doc;
	      Document d = searcher.doc(docId);
	      results[i] = d.get("docid");
	     // System.out.println((i + 1) + ". " + d.get("title"));
	    }

	    searcher.close();
	    return results;
	}
	
	public static int getIndex(String[] a, String b){
		if (a == null || a.length==0) return -1;
		for(int i=0;i<a.length; i++){
			if (a[i].equalsIgnoreCase(b)) return i;
		}
		return -1;
	}
	
	public static double[] evaluateNDCG(Collection C, String truth_dir) throws IOException, ParseException{
		double[] res = new double[3];
		
		GroundTruth gt0 = GroundTruth.readMapFiles(truth_dir,GroundTruth.SIMPLE_RANK_NORMALIZED);
		if (gt0 != null){
			//gt0.printMap();
			int n = gt0.map.size(); // how many nodes has been mapped
			String [][] modelmap = new String[n][];
			for(int i=0;i<n;i++){
				GroundTruth.NodeMap node_map = gt0.map.get(i);
				String[] modelmatchs = new String[11];
				modelmatchs[0] = node_map.node_id;
				String[] results = null;
				BookNode nodeA = C.searchNodeById(node_map.node_id);
				//BookNode[] similar_computed = null;
				results = queryIndex(nodeA,10);
				int c = 1;
				for (String match_node: results){
					modelmatchs[c] = match_node;
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
	
	
	
	public static class IndexAnalyzer extends Analyzer {
		
		public TokenStream tokenStream(String fieldName, Reader reader) {
			StandardTokenizer stdTokenizer = new StandardTokenizer(Version.LUCENE_35, reader);
			
			// Then, we transform the text into lowercased form
			LowerCaseFilter lcFilter = new LowerCaseFilter(Version.LUCENE_35, stdTokenizer);
			
			// stop words removal using the default set of stop words in lucene.
			// if you want to use the indri stop words (the list of stopwords provided in your assignment)
			// you need to first read the stopwords from the file and save them as a Set<String>, then replace
			// StopAnalyzer.ENGLISH_STOP_WORDS_SET into the set of stopwords you read from the file.
			StopFilter stopFilter = new StopFilter(Version.LUCENE_35, lcFilter, StopAnalyzer.ENGLISH_STOP_WORDS_SET, true);
			
			// Read api document about this. This is related to the word position in the index.
			stopFilter.setEnablePositionIncrements(true);
			
			// stemming
			if (stemming) return new PorterStemFilter(stopFilter);
			else return stopFilter;
			
		}
		
	}
}
