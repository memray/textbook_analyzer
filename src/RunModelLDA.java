import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.Locale;

import cc.mallet.topics.TopicModelDiagnostics;

public class RunModelLDA {
	private static DecimalFormat formatter;
	static String lda_model = "LDA"; // HLDA

	private static String domain; // Algebra	Information Retrieval
	private static String domainID; // algebra	ir
	private static String domain_folder; // algebra	information_retrieval
	// Parameters for topic modeling
	static int iterations = 2000; // iteration of each run
	// @@@@
	
	static boolean using_vocabulary = false;  // set to true to tokenize n-word defined in the file vocabulary_file
	static String vocabulary_file = "data/"+domain_folder+"/vocabulary/domain_vocabulary.txt"; // file containing a list of n-word tokens to consider
	static boolean stemming = false; // set to true to perform stemming
	static boolean symmetric_alpha = false; // set to true to tell the topic modeler to set alpha symmetric  
	
	static boolean repeat_titles = true; // set to true to repeat titles in nodes as part of the text 
	static boolean agg_parent_titles = true; // concatenates the titles of the parent in the node (but repeat only the node title)
	
	// @@@@
	static double min_topic_prob = 0.05; // for topic DF: minimum probability in a document-topic relation to consider the topic appear in the document
	
	// GROUND TRUTH
	static String truthdir = "data/"+domain_folder+"/expert_mapping/truth"; // ground truth file containing mapping between books 1 and 2
	static String truthdirP = "data/"+domain_folder+"/expert_mapping/truthP"; // ground truth file containing mapping between books 1 and 2
	static String truthdirL = "data/"+domain_folder+"/expert_mapping/truthL"; // ground truth file containing mapping between books 1 and 2
	
	// LDA SETUP
	static double initial_alpha = 0.01;
	static double initial_beta = 0.01;

	// FOR SET UP CONDITIONS
	static boolean aggregatecontent = false; // aggregate the content and input all parts to LDA
	
	static double threshold; // @@@@
	static boolean cut_tp; // @@@@
	static boolean using_tfidf; // @@@@

	static Collection col;
	
	static{
		formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
		formatter.applyPattern("#.####");
	}
	
	// main method
	public static void setUpParameters(){
		// TEXT PREPROCESSING
		using_vocabulary = false;  // set to true to tokenize n-word defined in the file vocabulary_file
		vocabulary_file = "data/"+domain_folder+"/vocabulary/domain_vocabulary.txt"; // file containing a list of n-word tokens to consider
		stemming = false; // set to true to perform stemming
		repeat_titles = true; // set to true to repeat titles in nodes as part of the text 
		agg_parent_titles = true; // concatenates the titles of the parent in the node (but repeat only the node title)
		
		symmetric_alpha = false; // set to true to tell the topic modeler to set alpha symmetric  
		
		// GROUND TRUTH
		truthdir = "data/"+domain_folder+"/expert_mapping/truth"; // ground truth file containing mapping between books 1 and 2
		truthdirP = "data/"+domain_folder+"/expert_mapping/truthP"; // ground truth file containing mapping between books 1 and 2
		truthdirL = "data/"+domain_folder+"/expert_mapping/truthL"; // ground truth file containing mapping between books 1 and 2

		// @@@@ TOPIC POST-PROCESSING
		min_topic_prob = 0.05; // minimum probability in a document-topic relation to consider the topic appear in the document
		threshold = 0.02; // @@@@
		cut_tp = false; // @@@@
		using_tfidf = false; // @@@@
		
		// LDA SETUP
		initial_alpha = 0.01;
		initial_beta = 0.01;
		
		
		aggregatecontent = false; // aggregate the content and input all parts to LDA
	}
	
	public static void testExperiment(String domainname) throws Exception{
		// DOMAIN
		domain = domainname;//"Information Retrieval"; // Algebra	Information Retrieval
		domainID = domainname.toLowerCase(); // algebra	information_retrieval
		domain_folder = domainID;//"information_retrieval"; // algebra	information_retrieval
		
		// CONDITION
		boolean[][] _usingbooks = {
					{true,false,false,false,false},
					{true,false,true,true,true},
					{false,false,false,false,true},
					{false,false,true,false,false},
					{false,false,true,true,false},
					{false,false,true,true,true}
		};
		String[] _cond_books = {"SB","MB","SB","SB","MB","MB"};
		//String output_folder = "data/output/"+domainID+"/01_TA_SB";
		String[] _output_folder = {
				"data/output/"+domainID+"/TEST_10000",
				"data/output/"+domainID+"/TEST_10111",
				"data/output/"+domainID+"/TEST_00001",
				"data/output/"+domainID+"/TEST_00100",
				"data/output/"+domainID+"/TEST_00110",
				"data/output/"+domainID+"/TEST_00111"				
		};

		int repeat_runs = 5; // number of runs (will run repeat_runs times for each topic)
		int topics = 150;
		
		setUpParameters();
		
		col = createCollection(domain,domainID,domain_folder,using_vocabulary,repeat_titles,stemming,vocabulary_file,aggregatecontent);
		for (int i=0;i<_usingbooks.length;i++){
			runModelIterations(repeat_runs,topics, "TA", _cond_books[i], _usingbooks[i], _output_folder[i]);
		}
		
	
	}
	public static void simpleTestExperiment(String domainname) throws Exception{
		// DOMAIN
		domain = domainname;//"Information Retrieval"; // Algebra	Information Retrieval
		domainID = domainname.toLowerCase(); // algebra	information_retrieval
		domain_folder = domainID;//"information_retrieval"; // algebra	information_retrieval
		
		// CONDITION
		boolean[] usingbooks = {true,false,false,false,false};
		//String output_folder = "data/output/"+domainID+"/01_TA_SB";
		String output_folder = "data/output/"+domainID+"/TEST_10000";

		int repeat_runs = 3; // number of runs (will run repeat_runs times for each topic)
		int topics = 150;
		
		setUpParameters();

		col = createCollection(domain,domainID,domain_folder,using_vocabulary,repeat_titles,stemming,vocabulary_file,aggregatecontent);
		runModelIterations(repeat_runs,topics, "TA", "SB", usingbooks, output_folder);
		
	}
	
	public static void experiment(String domainname, String aggregation, String books, int topics, int repeat_runs){
		domain = domainname;
		domainID = domainname.toLowerCase();
		domain_folder = domainID;
		
		java.util.Date date= new java.util.Date();
		String timestamp = new Timestamp(date.getTime()).toString();

		System.out.println("----------------------------------------");
		System.out.println("---------------- "+books+" -----------------");
		System.out.println("----------------------------------------");

		timestamp = timestamp.replace(' ', '.');
		timestamp = timestamp.replace(':', '.');
		// CONDITION
		boolean[] usingbooks = new boolean[books.length()];
		for(int i=0;i<books.length();i++) {
			usingbooks[i] = (books.charAt(i)=='1');
		}
		int c = 0;
		for(int i=0;i<usingbooks.length;i++) {
			if (usingbooks[i]) c++;
			//System.out.print(" "+usingbooks[i]);
		}
		String strbooks = "SB";
		if (c>1) strbooks = "MB";
		System.out.println();
		String output_folder = "data/output/"+domainID+"/EX_"+topics+"_"+repeat_runs+"_"+books+"_"+timestamp;
		System.out.println("  creating directory: " + output_folder);
		boolean dir1_created = ModelReporter.createDir(output_folder);
		boolean dir2_created = ModelReporter.createDir(output_folder+"/runs");
		if (dir1_created && dir2_created){
			System.out.println("  directory created");
			
			setUpParameters();
			try{
				col = createCollection(domain,domainID,domain_folder,using_vocabulary,repeat_titles,stemming,vocabulary_file,aggregatecontent);
				runModelIterations(repeat_runs,topics, aggregation, strbooks, usingbooks, output_folder);
			}catch(Exception e){
				e.printStackTrace();
			}			
		}else{
			System.out.println("  fail in creating directory");
		}
		

	}
	
	public static void main(String[] args){
		iterations = 4000;
		try{
			
			
			//experiment("Algebra", "TA", "10000", 150, 30);
			//experiment("Algebra", "TA", "10110", 150, 30);
			//experiment("Algebra", "TA", "00100", 150, 30);
			//experiment("Algebra", "TA", "00110", 150, 30);
			//experiment("Algebra", "TA", "00001", 150, 30);
			//experiment("Algebra", "TA", "00111", 150, 30);
			
			// ALGEBRA
			//experiment("Algebra", "TA", "10000", 150, 10);
			//experiment("Algebra", "TA", "11111", 150, 1);
			
			/*
			experiment("Algebra", "TA", "10000", 150, 30);
			experiment("Algebra", "TA", "10111", 150, 1);
			experiment("Algebra", "TA", "10110", 150, 1);
			experiment("Algebra", "TA", "00100", 150, 1);
			experiment("Algebra", "TA", "00110", 150, 1);
			
			experiment("Algebra", "TA", "00111", 150, 1);
			experiment("Algebra", "TA", "00001", 150, 1);
			experiment("Algebra", "TA", "11111", 150, 30);
			experiment("Algebra", "TA", "11000", 150, 1);
			experiment("Algebra", "TA", "01000", 150, 1);
			experiment("Algebra", "TA", "01111", 150, 1);
			*/
			
			// IR try many topics options
			//experiment("Information_Retrieval", "TA", "10000", 50, 30);
			//experiment("Information_Retrieval", "TA", "10000", 75, 30);
			//experiment("Information_Retrieval", "TA", "10000", 100, 30);
			//experiment("Information_Retrieval", "TA", "10000", 125, 30);
			//experiment("Information_Retrieval", "TA", "10000", 150, 30);
			//experiment("Information_Retrieval", "TA", "10000", 175, 30);
			//experiment("Information_Retrieval", "TA", "10000", 200, 30);
			//experiment("Information_Retrieval", "TA", "10000", 225, 30);
			//experiment("Information_Retrieval", "TA", "10000", 250, 30);
			
			/*
			experiment("Information_Retrieval", "TA", "10000", 10, 30);
			experiment("Information_Retrieval", "TA", "10000", 25, 30);
			experiment("Information_Retrieval", "TA", "10000", 275, 30);
			experiment("Information_Retrieval", "TA", "10000", 300, 30);
			*/
			// TODO @@@@ HERE!!!			
			//experiment("Information_Retrieval", "TA", "10000", 350, 30);
			//experiment("Information_Retrieval", "TA", "10000", 400, 30);
			//experiment("Information_Retrieval", "TA", "10000", 450, 30);
			//experiment("Information_Retrieval", "TA", "10000", 500, 30);
			
			//experiment("Information_Retrieval", "TA", "10000", 150, 30);
			//experiment("Information_Retrieval", "TA", "10111", 150, 60);
			//experiment("Information_Retrieval", "TA", "00010", 150, 60);
			//experiment("Information_Retrieval", "TA", "00110", 150, 60);
			//experiment("Information_Retrieval", "TA", "00111", 150, 60);
			experiment("Information_Retrieval", "TA", "00001", 150, 60);
			
			//experiment("Information_Retrieval", "TA", "11111", 150, 1);
			//experiment("Information_Retrieval", "TA", "11000", 150, 1);
			//experiment("Information_Retrieval", "TA", "01000", 150, 1);
			//experiment("Information_Retrieval", "TA", "01111", 150, 1);
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	// TODO
	public static void buildLDAModel(String content, int ntopics, boolean[] usingbooks, String output_folder, String prefix) throws Exception{
		col.runLDA(content, ntopics, iterations, initial_alpha*ntopics, initial_beta, symmetric_alpha, min_topic_prob);
		System.out.println("  Model build");
		ModelReporter.writeVocabularyList(col, output_folder+"/runs/"+prefix+"_alphabet.txt");
		//for(int i=0;i<usingbooks.length;i++){
		for(int i=0;i<2;i++){
			if (!usingbooks[i]) {
				col.indexBookLDA(i+1);
				System.out.println("  Book "+(i+1)+" indexed");
			}
			
		}
		
		// set the topic weight array
		// @@@ here should call the diagnosis for junk topics
		double[] tw = new double[ntopics];
		for(int h=0;h<ntopics;h++) tw[h] = 1.0;
		col.setTopicWeights(tw);
		
		if (cut_tp){
			col.cutLowProbabilities(threshold);
		}
							
		if (!aggregatecontent){
			// this recursive method propagates the text sizes from children
	        // containing text to parents. It is needed to have these sizes in
	        // order to propagate the probabilities (method propagateTopicAssignment called below)
			int collection_size = col.propagateTreeTextSizes(); // @@@@ content has been aggregated
			// propagates probabilities from leaf nodes to parents
			col.propagateTopicAssignment2(); // @@@@ content has been aggregated
		}	
	}
	
	// MAIN
	// @@@ make this method parameterized to topics, runs, condition
	// @@@ and take the collection processing apart
	// @@@ then make it output a string/file in a format like:
	// @@@ run,topics,TA/RI,SB/MB,B1/B0,NDCG1,NDCG3,NDCG10,Parents_NDCG1,Parents_NDCG3,Parents_NDCG10,Leaves_NDCG1,Leaves_NDCG3,Leaves_NDCG10
	public static void runModelIterations(int runs, int ntopics, String cond_agg, String cond_books, boolean[] usingbooks, String output_folder) throws Exception{
		String output = "";
		String eval_string = "run,topics,TA/RI,SB/MB,usebook1,ndcg1,ndcg3,ndcg10,ndcg1_P,ndcg3_P,ndcg10_P,ndcg1_L,ndcg3_L,ndcg10_L\n";
		String prefix = "";
		String content_4_lda = col.generateLDAInputString(usingbooks); // external_book will be excluded
		ModelReporter.writeFile(content_4_lda,output_folder+"/LDA_input.txt");
		double[][] book_sim_matrix;
		double[][] book_div_matrix;
		//@@@@double[][] avg_book_sim_matrix = new double[col.children.size()][col.children.size()];
		//@@@@double[][] avg_book_div_matrix = new double[col.children.size()][col.children.size()];
		
		System.out.println("Run topic modeling");
		double[] averages = new double[9];
		for(int i=0;i<runs;i++){
			System.out.println("LDA RUN: "+(i+1)+"/"+runs+" ("+iterations+" iterations)");
			prefix = ""+(i+1);
			if (i<9) prefix = "0"+prefix;
			buildLDAModel(content_4_lda, ntopics, usingbooks, output_folder, prefix);
			
	        System.out.print("  Start evaluation... ");
	        //double[] testgt1 = Evaluator.evaluateNDCG(col, truthdir, "COSINE", 1, 2, using_tfidf, false);
	        double[] testgt2 = Evaluator.evaluateNDCG(col, truthdir, "KL-DIVERGENCE", 1, 2, using_tfidf, false);
	        //System.out.println(testgt.toString());

	        //double[] testgt1P = Evaluator.evaluateNDCG(col, truthdirP, "COSINE", 1, 2, using_tfidf, false);
	        double[] testgt2P = Evaluator.evaluateNDCG(col, truthdirP, "KL-DIVERGENCE", 1, 2, using_tfidf, false);

			//double[] testgt1L = Evaluator.evaluateNDCG(col, truthdirL, "COSINE", 1, 2, using_tfidf, false);
	        double[] testgt2L = Evaluator.evaluateNDCG(col, truthdirL, "KL-DIVERGENCE", 1, 2, using_tfidf, false);
	        
	        averages[0] += testgt2[0]; averages[1] += testgt2[1]; averages[2] += testgt2[2];
	        averages[3] += testgt2P[0]; averages[4] += testgt2P[1]; averages[5] += testgt2P[2];
	        averages[6] += testgt2L[0]; averages[7] += testgt2L[1]; averages[8] += testgt2L[2];
			
	        System.out.println("Ok!");
			//System.out.println("      Cosine p(m): \n"
			//			+ "        NDCG(1): "+formatter.format(testgt1[0])+"\n "
			//			+ "        NDCG(3): "+formatter.format(testgt1[1])+"\n "
			//			+ "        NDCG(10): "+formatter.format(testgt1[2])+"\n ");
			System.out.println("      KL-div p(m): \n"
						+ "        NDCG(1): "+formatter.format(testgt2[0])+"\n "
						+ "        NDCG(3): "+formatter.format(testgt2[1])+"\n "
						+ "        NDCG(10): "+formatter.format(testgt2[2])+"\n ");
			
			eval_string +=  prefix+","+ntopics+","+cond_agg+","+cond_books+","+((usingbooks[0]) ? "1" : "0")+","+
					formatter.format(testgt2[0]) + "," + formatter.format(testgt2[1]) + "," + formatter.format(testgt2[2]) + "," +
					formatter.format(testgt2P[0]) + "," + formatter.format(testgt2P[1]) + "," + formatter.format(testgt2P[2]) + "," +
					formatter.format(testgt2L[0]) + "," + formatter.format(testgt2L[1]) + "," + formatter.format(testgt2L[2]) + "\n";
			
			//eval_cosine += ","+formatter.format(eval1);
			//eval_divergence += ","+formatter.format(eval2);
			
			ModelReporter.writeDocsTopicSummary(col, output_folder+"/runs/"+prefix+"_summary.txt");
		
			// DIAGNOSIS
			//TopicModelDiagnostics diagnostics = new TopicModelDiagnostics(col.model, 15); // ParallelTopicModel, num_words
			//ModelReporter.writeFile(diagnostics.toXML(), "data/output/"+condition+"/diagnostics_"+suffix+".txt");
			
			// SAVE TOPIC WORD distributions
			//System.out.print("Getting the topic-word distribution matrix ");
			//double[][] t_dist = col.getTopicWordDistribution();
			//System.out.println(" ... OK!");
			
			//System.out.print("Saving Topic-Word distribution file");							
			//ModelReporter.writeTopicWordDistribution(t_dist, "data/output/"+condition+"/TWDistrib_"+suffix+".txt");
			//System.out.println(" ... OK!");
			
			//col.model.printTopicWordWeights(new File("data/output/"+condition+"/TWWeights_"+suffix+".txt"));
			ModelReporter.writeListOFTopics(col, output_folder+"/runs/"+prefix+"_topics.txt",50);
			
			//@@@@book_div_matrix = col.getBookSimilarities("kldiv");
			//@@@@ModelReporter.writeSimilarityMatrix(book_div_matrix, output_folder+"/runs/"+prefix+"_book_KLDivergence.txt");
			
			//@@@@book_sim_matrix = col.getBookSimilarities("cosine");
			//@@@@ModelReporter.writeSimilarityMatrix(book_sim_matrix, output_folder+"/runs/"+prefix+"_book_cosine_sim.txt");
			
			//@@@@accumulateMatrix(avg_book_sim_matrix,book_sim_matrix);
			//@@@@accumulateMatrix(avg_book_div_matrix,book_div_matrix);
			
			//if (i==(runs-1)) ModelReporter.writeVocabularyList(col, output_folder+"/alphabet.txt");
		}
		
		//@@@@divideMatrix(avg_book_sim_matrix, runs);
		//@@@@divideMatrix(avg_book_div_matrix, runs);
		divideVector(averages,runs);
		
		//@@@@ModelReporter.writeSimilarityMatrix(avg_book_sim_matrix, output_folder+"/avg_book_cosine_sim.txt");
		//@@@@ModelReporter.writeSimilarityMatrix(avg_book_div_matrix, output_folder+"/avg_book_divergence.txt");
		
		System.gc();

		java.util.Date date= new java.util.Date();
		String timestamp = new Timestamp(date.getTime()).toString();
		String suffix = lda_model+"_"+domainID+"_"+ntopics+"T_"+timestamp;
		output = "Model: "+lda_model+"\n";
		output = "Topics: "+ntopics+"\n";
		output += "Domain: "+domain+"\n";

		output += "Total Nodes: "+(col.size()-1)+"\n";
		for(int i=0;i<col.children.size();i++){
			if (usingbooks[i]) output += "  (*M*) ";else output += "  ";
			output += ""+(i+1)+" - "+col.children.get(i).getTitle()+": "+col.children.get(i).size()+"\n";
		}
		output += "Condition: "+cond_agg+"/"+cond_books+"\n";
		output += "Truth dir: "+truthdir+"\n";
		output += "Aggregating content: "+aggregatecontent+"\n";
		output += "Runs: "+runs+"\n";
		output += "Iterations in each run: "+iterations+"\n";
		output += "\n"+eval_string;
		output += "\nAverages:\nndcg1,ndcg3,ndcg10,ndcg1_P,ndcg3_P,ndcg10_P,ndcg1_L,ndcg3_L,ndcg10_L\n";
		output += ""+formatter.format(averages[0]);
		for(int k=1;k<9;k++) output += ","+formatter.format(averages[k]);
		ModelReporter.writeFile(output, output_folder+"/EVAL_"+suffix+".txt");
			
	}
	
	public static Collection createCollection(String domain, String domainID, String domain_folder,
									  boolean using_vocabulary, boolean repeat_titles, boolean stemming,
									  String vocabulary_file, boolean aggregatecontent) throws Exception{
		System.out.println(domain+" ("+domainID+")");
		Collection col = new Collection(domain,domainID);
		if (domainID.equalsIgnoreCase("algebra")){
			String[] filtered_titles_1 		= {"Sample Set","Practice Set","Solutions to Exercises","Exercises", "Exercises for Review", "Exercise Supplement", "Proficiency Exam", "Overview", "Objectives", "Summary"};
			String[] filtered_classes_1		= {"section homework","section solutions"};

			String[] filtered_titles_2 		= {"Sample Set","Practice Set","Solutions to Exercises","Exercises", "Exercises for Review", "Exercise Supplement", "Proficiency Exam", "Overview", "Objectives", "Summary"};
			String[] filtered_classes_2		= {"key_takeaways","learning_objectives","video"};
			
			String[] filtered_titles_7 		= {"Exercises","Answers","Applications","Index"};
			
			BookParser parser_1 = new BookParser(".section.module", ".section", filtered_titles_1, filtered_classes_1);
			BookParser parser_2 = new BookParser(".section.module", ".section", filtered_titles_2, filtered_classes_2);
			BookParser parser_3 = new BookParser(".Section1", ".section", filtered_titles_2, filtered_classes_2);
			ParsePDF parser_7 = new ParsePDF(filtered_titles_7);
			
			//BookParserFilter parser_filter_5 = new BookParserFilter(".section", "UNKNOWN-12512", filtered_titles_1, filtered_classes_1);
			
			// 2. GET THE CONTENT FROM HTML FILES OF THE BOOKS AND ADD BOOKNODE OBJECTS TO COLLECTION
			System.out.print("Parsing book1 - Elementary Algebra (Wade Ellis, Denny Burzynski) ...");
			BookNode book1 = parser_1.getBookFromHTMLFiles("data/"+domain_folder+"/books/book1","Elementary Algebra","book1","Wade Ellis, Denny Burzynski");
			if (book1 != null) col.add(book1);
			System.out.println("Ok");
			
			System.out.print("Parsing book2 - Elementary Algebra (John Redden) ...");
			BookNode book2 = parser_2.getBookFromHTMLFiles("data/"+domain_folder+"/books/book2","Elementary Algebra, v1","book2","John Redden");
			if (book2 != null) col.add(book2);
			System.out.println("Ok");
			
			System.out.print("Parsing book3 - Understanding Algebra (James W. Brennan) ...");
			BookNode book3 = parser_3.getBookFromHTMLFiles("data/"+domain_folder+"/books/book3","Understanding Algebra","book3","James W. Brennan");
			if (book3 != null) col.add(book3);
			System.out.println("Ok");
			
			System.out.print("Parsing book4 - Fundamentals of Mathematics (Denny Burzynski and Wade Ellis) ...");
			BookNode book4 = parser_1.getBookFromHTMLFiles("data/"+domain_folder+"/books/book4","Fundamentals of Mathematics" , "book4" ,"Denny Burzynski and Wade Ellis");
			if (book4 != null) col.add(book4);
			System.out.println("Ok");
				
			System.out.print("Parsing book5 - Elementary Algebra (PDF parsed) ...");
			BookNode book5 = parser_7.getTreeFromPDF("data/"+domain_folder+"/books/book5/ElementaryAlgebra.pdf", "book5", "Elementary Algebra");
			if (book5 != null) col.add(book5);
			System.out.println("Ok");
			
			// 3. SET THE VOCABULARY AND PREPROCESS THE TEXT
			Vocabulary vocabulary = null;
			if (using_vocabulary){
				vocabulary = new Vocabulary(vocabulary_file,"-");
			}
			PreprocessText.preprocessTree(col,repeat_titles, stemming, vocabulary);// repeat title=true, stemming-true,  vocabulary
			
			// recursively aggregates the content from children to parents
			if (aggregatecontent) col.aggregateContent();

		}else if(domainID.equalsIgnoreCase("information_retrieval")){
			String[][] books = {
					{"Introduction to Information Retrieval", "book1_src_iir.xml","iir"}, // book 1
					{"Modern Information Retrieval","book2_src_mir.xml","mir"}, // book 2
					{"Finding Out About", "book3_src_foa.xml","foa"},
				 	{"Information Retrieval","book4_src_ir.xml","ir"},
				 	{"Information Storage and Retrieval Systems","book5_src_isrs.xml","isrs"}
				  };
			int nbooks = books.length;
			
			for (int i=0;i<nbooks;i++){
				String booktitle = books[i][0];
				String bookfile = "data/"+domain_folder+"/books/"+books[i][1];
				String bookid = books[i][2];
				
				System.out.print("Parsing book"+(i+1)+" - "+booktitle);
				BookNode book = ParseSrcXML.parseXML(bookfile,booktitle,bookid);
				if (book != null) col.add(book);
				System.out.println(" ...Ok");
			}
			Vocabulary vocabulary = null;
			if (using_vocabulary){
				vocabulary = new Vocabulary(vocabulary_file,"-");
			}
			PreprocessText.preprocessTree(col,repeat_titles, stemming, vocabulary);
			if (aggregatecontent) col.aggregateContent();
		}
		return col;
	}
	
	public static void accumulateMatrix(double[][] matrixA,double[][] matrixB){
		for (int i=0;i<matrixA.length;i++){
			for (int j=0;j<matrixA[i].length;j++){
				matrixA[i][j] += matrixB[i][j];
			}
		}
	}
	public static void divideMatrix(double[][] matrixA, double val){
		if (val == 0) val = 0.0001;
		for (int i=0;i<matrixA.length;i++){
			for (int j=0;j<matrixA[i].length;j++){
				matrixA[i][j] = matrixA[i][j] / val;
			}
		}
	}
	public static void divideVector(double[] vector, double val){
		if (val == 0) val = 0.0001;
		for (int i=0;i<vector.length;i++){
			vector[i] = vector[i] / val;
		}
	}
	
	
}
