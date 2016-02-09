import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.Locale;

import cc.mallet.topics.TopicModelDiagnostics;

public class RunModel {
	private static DecimalFormat formatter;
	private static String domain_folder = "information_retrieval";//"information_retrieval"; // algebra	information_retrieval
	private static String domain = "Information Retrieval";//"Information Retrieval"; // Algebra	Information Retrieval
	private static String domainID = "ir"; // algebra	ir
	static String condition = "condition_test"; //  12_LDA_MB_TA  16_HLDA_MB_TA  
											   // 15_HLDA_SB_TA (15 to go)
	// Parameters for topic modeling
	static int iterations = 2000; // iteration of each run
	static int repeat_runs = 1; // number of runs (will run repeat_runs times for each topic)
	static boolean using_vocabulary = false;  // set to true to tokenize n-word defined in the file vocabulary_file
	static boolean stemming = false; // set to true to perform stemming
	static boolean symmetric_alpha = false; // set to true to tell the topic modeler to set alpha symmetric  
	static boolean repeat_titles = true; // set to true to repeat titles in nodes as part of the text 
	static double min_topic_prob = 0.05; // minimum probability in a document-topic relation to consider the topic appear in the document
//	static String truthfile = "data/"+domain_folder+"/truth_1_2_merged.txt"; // ground truth file containing mapping between books 1 and 2
	static String truthdir = "data/"+domain_folder+"/truth"; // ground truth file containing mapping between books 1 and 2
	static String truthdirP = "data/"+domain_folder+"/truthP"; // ground truth file containing mapping between books 1 and 2
	static String truthdirL = "data/"+domain_folder+"/truthL"; // ground truth file containing mapping between books 1 and 2
	static String vocabulary_file = "data/"+domain_folder+"/algebra_nterm_vocabulary.txt"; // file containing a list of n-word tokens to consider
	static boolean aggregatecontent = false; // aggregate the content and input all parts to LDA
	static boolean addotherbooks = true; // add more books to the generation of the model
	static int external_book = 2; // this book will be not including in generation of the model and will be indexed by the build model
	static double threshold = 0.02;
	static boolean cut_tp = false;
	static boolean using_tfidf = false;
	static String lda_model = "LDA"; // HLDA
	static int[] topics = {100}; // array with the number of topics for each run
	static double alpha = 0.01;
	static double beta = 0.01;
	// for multiple book condition:
	// for single book condition: alpha=5, gamma=3.0, eta= 0.001
	static double[] alpha_list = {5.0}; // 2 // ** 5  // smoothing on topic distributions
	static double[] gamma_list = {3.5}; // 3.5 // ** 0.1, ***0.05, 0.02 // "imaginary" customers at the next, as yet unused table
	static double[] eta_list = {0.001}; // 0.02 0.01 // ** 0.01, 0.005 // smoothing on word distributions
	static int[] levels = {4}; // **5, 4, 6 //array with the number of topics for each run
	static double[] level_weights = {1.0,1.0,1.0,1.0}; // for hLDA each level is weighted. All probs for topics in a level will be weighted with these
	

	//static int hlda_nlevels = 7;

	static{
		formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
		formatter.applyPattern("#.####");
	}
	// MAIN
	public static void main(String[] args){
		
		String eval_cosine = "";
		String eval_divergence = "";
		String eval_ndcg_cosine_1 = "";
		String eval_ndcg_cosine_3 = "";
		String eval_ndcg_cosine_10 = "";
		String eval_ndcg_divergence_1 = "";
		String eval_ndcg_divergence_3 = "";
		String eval_ndcg_divergence_10 = "";
		
		// for storing efficace with Parents nodes
		String eval_ndcg_cosine_1P = "";
		String eval_ndcg_cosine_3P = "";
		String eval_ndcg_cosine_10P = "";
		String eval_ndcg_divergence_1P = "";
		String eval_ndcg_divergence_3P = "";
		String eval_ndcg_divergence_10P = "";

		// for storing efficace with Leaf nodes
		String eval_ndcg_cosine_1L = "";
		String eval_ndcg_cosine_3L = "";
		String eval_ndcg_cosine_10L = "";
		String eval_ndcg_divergence_1L = "";
		String eval_ndcg_divergence_3L = "";
		String eval_ndcg_divergence_10L = "";

		String eval_string = "";

		
		try{
			// 1. CREATE THE COLLECTION OBJECT AND FILL IT WITH THE BOOKS
			Collection col = new Collection(domain,domainID);
			fillCollection(col,domainID,domain_folder,addotherbooks,using_vocabulary,repeat_titles,stemming,vocabulary_file,aggregatecontent);
						
			// 2. GENERATE A STRING INPUT FOR TOPIC MODELING 
//			String content_4_lda = col.generateLDAInputString(external_book); // external_book will be excluded
			String content_4_lda = col.generateLDAInputString(new boolean[]{false, true}); // external_book will be excluded
			ModelReporter.writeFile(content_4_lda,"data/output/"+condition+"/LDA_input.txt");
						
			// 3. RUN THE TOPIC MODELING
			int run_count = 0;
			int total_run = 0;
			System.out.println("Run topic modeling");
			if (lda_model.equalsIgnoreCase("LDA")){
				run_count = 0;
				total_run = topics.length*repeat_runs;
				eval_string += "\n";
				for (int t=0;t<topics.length;t++){
					//eval_cosine = "COSINE,PRECISION,T="+topics[t];
					//eval_divergence = "DIVERGENCE,PRECISION,T="+topics[t];
					eval_ndcg_cosine_1 = "COSINE,NDCG(1),T="+topics[t];
					eval_ndcg_cosine_3 = "COSINE,NDCG(3),T="+topics[t];
					eval_ndcg_cosine_10 = "COSINE,NDCG(10),T="+topics[t];
					eval_ndcg_divergence_1 = "DIVERGENCE,NDCG(1),T="+topics[t];
					eval_ndcg_divergence_3 = "DIVERGENCE,NDCG(3),T="+topics[t];
					eval_ndcg_divergence_10 = "DIVERGENCE,NDCG(10),T="+topics[t];
					eval_ndcg_cosine_1P = "PARENTS,COSINE,NDCG(1),T="+topics[t];
					eval_ndcg_cosine_3P = "PARENTS,COSINE,NDCG(3),T="+topics[t];
					eval_ndcg_cosine_10P = "PARENTS,COSINE,NDCG(10),T="+topics[t];
					eval_ndcg_divergence_1P = "PARENTS,DIVERGENCE,NDCG(1),T="+topics[t];
					eval_ndcg_divergence_3P = "PARENTS,DIVERGENCE,NDCG(3),T="+topics[t];
					eval_ndcg_divergence_10P = "PARENTS,DIVERGENCE,NDCG(10),T="+topics[t];
					eval_ndcg_cosine_1L = "LEAVES,COSINE,NDCG(1),T="+topics[t];
					eval_ndcg_cosine_3L = "LEAVES,COSINE,NDCG(3),T="+topics[t];
					eval_ndcg_cosine_10L = "LEAVES,COSINE,NDCG(10),T="+topics[t];
					eval_ndcg_divergence_1L = "LEAVES,DIVERGENCE,NDCG(1),T="+topics[t];
					eval_ndcg_divergence_3L = "LEAVES,DIVERGENCE,NDCG(3),T="+topics[t];
					eval_ndcg_divergence_10L = "LEAVES,DIVERGENCE,NDCG(10),T="+topics[t];

					for(int i=0;i<repeat_runs;i++){
						// RUN the modeling
						run_count++;
						System.out.println("LDA RUN: "+run_count+"/"+total_run);
						col.runLDA(content_4_lda, topics[t], iterations, alpha*topics[t], beta, symmetric_alpha, min_topic_prob);
						System.out.println("  Model build");
						col.indexBookLDA(external_book);
						System.out.println("  Book "+external_book+" indexed");							
						
						// set the topic weight array
						// @@@ here should call the diagnosis for junk topics
						double[] tw = new double[topics[t]];
						for(int h=0;h<topics[t];h++) tw[h] = 1.0;
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
				        
				        
				        String suffix = "[a="+alpha+"]"+"[b="+beta+"]"+"[T="+topics[t]+"]"+"["+i+"]";
				        
				        System.out.print("  Start evaluation... ");
				        double[] testgt1 = Evaluator.evaluateNDCG(col, truthdir, "COSINE", 1, 2, using_tfidf, false);
				        double[] testgt2 = Evaluator.evaluateNDCG(col, truthdir, "KL-DIVERGENCE", 1, 2, using_tfidf, false);
				        //System.out.println(testgt.toString());

				        double[] testgt1P = Evaluator.evaluateNDCG(col, truthdirP, "COSINE", 1, 2, using_tfidf, false);
				        double[] testgt2P = Evaluator.evaluateNDCG(col, truthdirP, "KL-DIVERGENCE", 1, 2, using_tfidf, false);

						double[] testgt1L = Evaluator.evaluateNDCG(col, truthdirL, "COSINE", 1, 2, using_tfidf, false);
				        double[] testgt2L = Evaluator.evaluateNDCG(col, truthdirL, "KL-DIVERGENCE", 1, 2, using_tfidf, false);
				        	
				        // DELETE!!

				        
				        //double eval1 = Evaluator.evaluate(col,truthfile,"data/output/"+condition+"/computed_similars/COSINE_"+suffix+".txt","COSINE",1,2,using_tfidf, false);
						//double eval2 = Evaluator.evaluate(col,truthfile,"data/output/"+condition+"/computed_similars/KLDIVERGENCE_"+suffix+".txt","KL-DIVERGENCE",1,2,using_tfidf, false);
						System.out.println("Ok!");
						System.out.println("      Cosine p(m): \n"
									+ "        NDCG(1): "+formatter.format(testgt1[0])+"\n "
									+ "        NDCG(3): "+formatter.format(testgt1[1])+"\n "
									+ "        NDCG(10): "+formatter.format(testgt1[2])+"\n ");
						System.out.println("      KL-div p(m): \n"
									+ "        NDCG(1): "+formatter.format(testgt2[0])+"\n "
									+ "        NDCG(3): "+formatter.format(testgt2[1])+"\n "
									+ "        NDCG(10): "+formatter.format(testgt2[2])+"\n ");
						
						//eval_cosine += ","+formatter.format(eval1);
						//eval_divergence += ","+formatter.format(eval2);
						eval_ndcg_cosine_1 += ","+formatter.format(testgt1[0]);
						eval_ndcg_cosine_3 += ","+formatter.format(testgt1[1]);
						eval_ndcg_cosine_10 += ","+formatter.format(testgt1[2]);
						eval_ndcg_divergence_1 += ","+formatter.format(testgt2[0]);
						eval_ndcg_divergence_3 += ","+formatter.format(testgt2[1]);
						eval_ndcg_divergence_10 += ","+formatter.format(testgt2[2]);

						eval_ndcg_cosine_1P += ","+formatter.format(testgt1P[0]);
						eval_ndcg_cosine_3P += ","+formatter.format(testgt1P[1]);
						eval_ndcg_cosine_10P += ","+formatter.format(testgt1P[2]);
						eval_ndcg_divergence_1P += ","+formatter.format(testgt2P[0]);
						eval_ndcg_divergence_3P += ","+formatter.format(testgt2P[1]);
						eval_ndcg_divergence_10P += ","+formatter.format(testgt2P[2]);

						eval_ndcg_cosine_1L += ","+formatter.format(testgt1L[0]);
						eval_ndcg_cosine_3L += ","+formatter.format(testgt1L[1]);
						eval_ndcg_cosine_10L += ","+formatter.format(testgt1L[2]);
						eval_ndcg_divergence_1L += ","+formatter.format(testgt2L[0]);
						eval_ndcg_divergence_3L += ","+formatter.format(testgt2L[1]);
						eval_ndcg_divergence_10L += ","+formatter.format(testgt2L[2]);
						
						ModelReporter.writeDocsTopicSummary(col, "data/output/"+condition+"/runs/summary_"+suffix+".txt");
					
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
						ModelReporter.writeListOFTopics(col, "data/output/"+condition+"/runs/topics_"+suffix+".txt",15);
						ModelReporter.writeVocabularyList(col, "data/output/"+condition+"/alphabet.txt");
					}
					System.gc();
					eval_string +=  "\n"  + "\n" + eval_ndcg_cosine_1 + "\n" + eval_ndcg_cosine_3 + "\n" + eval_ndcg_cosine_10 + "\n" + 
							 "\n" + eval_ndcg_divergence_1 + "\n" + eval_ndcg_divergence_3 + "\n" + eval_ndcg_divergence_10 + "\n\n";
					eval_string +=  "\nPARENT NODES"  + "\n" + eval_ndcg_cosine_1P + "\n" + eval_ndcg_cosine_3P + "\n" + eval_ndcg_cosine_10P + "\n" + 
							 "\n" + eval_ndcg_divergence_1P + "\n" + eval_ndcg_divergence_3P + "\n" + eval_ndcg_divergence_10P + "\n\n";
					eval_string +=  "\nLEAF NODES"  + "\n" + eval_ndcg_cosine_1L + "\n" + eval_ndcg_cosine_3L + "\n" + eval_ndcg_cosine_10L + "\n" + 
							 "\n" + eval_ndcg_divergence_1L + "\n" + eval_ndcg_divergence_3L + "\n" + eval_ndcg_divergence_10L + "\n\n";
				}
			}else{ // hLDA
				run_count = 0;
				total_run = alpha_list.length*gamma_list.length*eta_list.length*levels.length*repeat_runs;
				
				for(int a=0;a<alpha_list.length;a++){
					for(int b=0;b<gamma_list.length;b++){
						for(int c=0;c<eta_list.length;c++){
							//eval_cosine += "alpha="+alpha_list[a]+",gamma="+formatter.format(gamma_list[b])+",eta="+formatter.format(eta_list[c]);
							//eval_divergence += "alpha="+alpha_list[a]+",gamma="+formatter.format(gamma_list[b])+",eta="+formatter.format(eta_list[c]);
							for (int t=0;t<levels.length;t++){
								eval_string +=  "\n  alpha="+alpha_list[a]+",gamma="+formatter.format(gamma_list[b])+",eta="+formatter.format(eta_list[c])+",Levels="+levels[t];
								eval_string +=  "\n    Topics";
								//eval_cosine 	= "    COSINE"; 
								//eval_divergence	= "    DIVERGENCE";
								eval_ndcg_cosine_1 = "    COSINE,NDCG(1)";
								eval_ndcg_cosine_3 = "    COSINE,NDCG(3)";
								eval_ndcg_cosine_10 = "    COSINE,NDCG(10)";
								eval_ndcg_divergence_1 = "    DIVERGENCE,NDCG(1)";
								eval_ndcg_divergence_3 = "    DIVERGENCE,NDCG(3)";
								eval_ndcg_divergence_10 = "    DIVERGENCE,NDCG(10)";
								eval_ndcg_cosine_1P = "    PARENTS,COSINE,NDCG(1)";
								eval_ndcg_cosine_3P = "    PARENTS,COSINE,NDCG(3)";
								eval_ndcg_cosine_10P = "    PARENTS,COSINE,NDCG(10)";
								eval_ndcg_divergence_1P = "    PARENTS,DIVERGENCE,NDCG(1)";
								eval_ndcg_divergence_3P = "    PARENTS,DIVERGENCE,NDCG(3)";
								eval_ndcg_divergence_10P = "    PARENTS,DIVERGENCE,NDCG(10)";
								eval_ndcg_cosine_1L = "    LEAVES,COSINE,NDCG(1)";
								eval_ndcg_cosine_3L = "    LEAVES,COSINE,NDCG(3)";
								eval_ndcg_cosine_10L = "    LEAVES,COSINE,NDCG(10)";
								eval_ndcg_divergence_1L = "    LEAVES,DIVERGENCE,NDCG(1)";
								eval_ndcg_divergence_3L = "    LEAVES,DIVERGENCE,NDCG(3)";
								eval_ndcg_divergence_10L = "    LEAVES,DIVERGENCE,NDCG(10)";
								for(int i=0;i<repeat_runs;i++){
									// RUN the modeling
									run_count++;
									System.out.println("hLDA RUN: "+run_count+"/"+total_run);

									col.runHLDA(content_4_lda, levels[t], iterations, alpha_list[a], gamma_list[b], eta_list[c]);
									System.out.print("  Model build");
									int inftopcis = col.hlda_util.countTopics();
									System.out.println(" - N Topics: " + inftopcis);
									eval_string += ","+inftopcis;
									
									String run_report = col.hlda_util.printTopicNodes(10);
									
									//eval_divergence += ",Topics="+ inftopcis;
									col.indexBookHLDA(external_book);
									System.out.println("  Book "+external_book+" indexed");
									
									// this set the array
									col.setTopicWeights(col.hlda_util.getTopicWeights(level_weights));
									
									if (cut_tp){
										col.cutLowProbabilities(threshold);
									}
									
									
									if (!aggregatecontent){
										// this recursive method propagates the text sizes from children
								        // containing text to parents. It is needed to have these sizes in
								        // order to propagate the probabilities (method propagateTopicAssignment called below)
										int collection_size = col.propagateTreeTextSizes(); 
										// propagates probabilities from leaf nodes to parents
										col.propagateTopicAssignment2(); 
									}
							        
									String suffix = "[a="+alpha_list[a]+"]"+"[g="+gamma_list[b]+"]"+"[e="+eta_list[c]+"]"+"[L="+levels[t]+"]"+"["+i+"]";

									System.out.print("  Start evaluation... ");
									// NDCG!!
									double[] testgt1 = Evaluator.evaluateNDCG(col, truthdir, "COSINE", 1, 2, using_tfidf, true);
							        double[] testgt2 = Evaluator.evaluateNDCG(col, truthdir, "KL-DIVERGENCE", 1, 2, using_tfidf, true);

									double[] testgt1P = Evaluator.evaluateNDCG(col, truthdirP, "COSINE", 1, 2, using_tfidf, true);
							        double[] testgt2P = Evaluator.evaluateNDCG(col, truthdirP, "KL-DIVERGENCE", 1, 2, using_tfidf, true);

									double[] testgt1L = Evaluator.evaluateNDCG(col, truthdirL, "COSINE", 1, 2, using_tfidf, true);
							        double[] testgt2L = Evaluator.evaluateNDCG(col, truthdirL, "KL-DIVERGENCE", 1, 2, using_tfidf, true);
							        	
							        // DELETE!!
							        //double eval1 = Evaluator.evaluate(col,truthfile,"data/output/"+condition+"/computed_similars/COSINE_"+suffix+".txt","COSINE",1,2,using_tfidf, true);
									//double eval2 = Evaluator.evaluate(col,truthfile,"data/output/"+condition+"/computed_similars/KLDIVERGENCE_"+suffix+".txt","KL-DIVERGENCE",1,2,using_tfidf, true);
									System.out.println("Ok!");
									System.out.println("    alpha = "+formatter.format(alpha_list[a]));
									System.out.println("    gamma = "+formatter.format(gamma_list[b]));
									System.out.println("    eta   = "+formatter.format(eta_list[c]));
									System.out.println("      Cosine : \n"
											+ "        NDCG(1): "+formatter.format(testgt1[0])+"\n"
											+ "        NDCG(3): "+formatter.format(testgt1[1])+"\n"
											+ "        NDCG(10): "+formatter.format(testgt1[2])+"\n");
									System.out.println("      KL-div p(m): \n"
											+ "        NDCG(1): "+formatter.format(testgt2[0])+"\n"
											+ "        NDCG(3): "+formatter.format(testgt2[1])+"\n"
											+ "        NDCG(10): "+formatter.format(testgt2[2])+" ");
								
									System.out.println();
									//eval_cosine += ","+formatter.format(eval1);
									//eval_divergence += ","+formatter.format(eval2);
									eval_ndcg_cosine_1 += ","+formatter.format(testgt1[0]);
									eval_ndcg_cosine_3 += ","+formatter.format(testgt1[1]);
									eval_ndcg_cosine_10 += ","+formatter.format(testgt1[2]);
									eval_ndcg_divergence_1 += ","+formatter.format(testgt2[0]);
									eval_ndcg_divergence_3 += ","+formatter.format(testgt2[1]);
									eval_ndcg_divergence_10 += ","+formatter.format(testgt2[2]);

									eval_ndcg_cosine_1P += ","+formatter.format(testgt1P[0]);
									eval_ndcg_cosine_3P += ","+formatter.format(testgt1P[1]);
									eval_ndcg_cosine_10P += ","+formatter.format(testgt1P[2]);
									eval_ndcg_divergence_1P += ","+formatter.format(testgt2P[0]);
									eval_ndcg_divergence_3P += ","+formatter.format(testgt2P[1]);
									eval_ndcg_divergence_10P += ","+formatter.format(testgt2P[2]);

									eval_ndcg_cosine_1L += ","+formatter.format(testgt1L[0]);
									eval_ndcg_cosine_3L += ","+formatter.format(testgt1L[1]);
									eval_ndcg_cosine_10L += ","+formatter.format(testgt1L[2]);
									eval_ndcg_divergence_1L += ","+formatter.format(testgt2L[0]);
									eval_ndcg_divergence_3L += ","+formatter.format(testgt2L[1]);
									eval_ndcg_divergence_10L += ","+formatter.format(testgt2L[2]);
									

									run_report = suffix + "\n" +  "ACCURACY:\n" + 
											"  COSINE NDCG(1):  " + formatter.format(testgt1[0]) +  "\n" + 
											"  COSINE NDCG(3):  " + formatter.format(testgt1[1]) +  "\n" + 
											"  COSINE NDCG(10): " + formatter.format(testgt1[2]) +  "\n" + 
											"  KL-DIVERGENCE NDCG(1):  " + formatter.format(testgt2[0]) +  "\n" + 
											"  KL-DIVERGENCE NDCG(3):  " + formatter.format(testgt2[1]) +  "\n" + 
											"  KL-DIVERGENCE NDCG(10): " + formatter.format(testgt2[2]) +  "\n" + 
											"  PARENTS COSINE NDCG(1):  " + formatter.format(testgt1P[0]) +  "\n" + 
											"  PARENTS COSINE NDCG(3):  " + formatter.format(testgt1P[1]) +  "\n" + 
											"  PARENTS COSINE NDCG(10): " + formatter.format(testgt1P[2]) +  "\n" + 
											"  PARENTS KL-DIVERGENCE NDCG(1):  " + formatter.format(testgt2P[0]) +  "\n" + 
											"  PARENTS KL-DIVERGENCE NDCG(3):  " + formatter.format(testgt2P[1]) +  "\n" + 
											"  PARENTS KL-DIVERGENCE NDCG(10): " + formatter.format(testgt2P[2]) +  "\n" + 
											"  LEAVES COSINE NDCG(1):  " + formatter.format(testgt1L[0]) +  "\n" + 
											"  LEAVES COSINE NDCG(3):  " + formatter.format(testgt1L[1]) +  "\n" + 
											"  LEAVES COSINE NDCG(10): " + formatter.format(testgt1L[2]) +  "\n" + 
											"  LEAVES KL-DIVERGENCE NDCG(1):  " + formatter.format(testgt2L[0]) +  "\n" + 
											"  LEAVES KL-DIVERGENCE NDCG(3):  " + formatter.format(testgt2L[1]) +  "\n" + 
											"  LEAVES KL-DIVERGENCE NDCG(10): " + formatter.format(testgt2L[2]) +  "\n" + 
											run_report;
									
									ModelReporter.writeFile(run_report, "data/output/"+condition+"/runs/HLDA_TOPICS_"+suffix+".txt");
									
									
									
									/* These methods should be re-done with no need of re-computing the sample
									 * instead, a method for getting the word distribution of the hlda topics
									String traindata = col.hlda_util.trainDataToString(col.training_instances);
									ModelReporter.writeFile(traindata, "data/output/"+condition+"/hlda_traindata.txt");
										
									String testdata = col.hlda_util.testDataToString(col.test_instances);
									ModelReporter.writeFile(testdata, "data/output/"+condition+"/hlda_testdata.txt");
									*/
								}
								eval_string +=  "\n"  + "\n" + eval_ndcg_cosine_1 + "\n" + eval_ndcg_cosine_3 + "\n" + eval_ndcg_cosine_10 + "\n" + 
										 "\n" + eval_ndcg_divergence_1 + "\n" + eval_ndcg_divergence_3 + "\n" + eval_ndcg_divergence_10 + "\n\n";
								eval_string +=  "\nPARENT NODES"  + "\n" + eval_ndcg_cosine_1P + "\n" + eval_ndcg_cosine_3P + "\n" + eval_ndcg_cosine_10P + "\n" + 
										 "\n" + eval_ndcg_divergence_1P + "\n" + eval_ndcg_divergence_3P + "\n" + eval_ndcg_divergence_10P + "\n\n";
								eval_string +=  "\nLEAF NODES"  + "\n" + eval_ndcg_cosine_1L + "\n" + eval_ndcg_cosine_3L + "\n" + eval_ndcg_cosine_10L + "\n" + 
										 "\n" + eval_ndcg_divergence_1L + "\n" + eval_ndcg_divergence_3L + "\n" + eval_ndcg_divergence_10L + "\n\n";
								
							}
							
							System.gc();
							//eval_cosine += "\n";
							//eval_divergence += "\n";
						}
					}

				}
				
			}
			java.util.Date date= new java.util.Date();
			String timestamp = new Timestamp(date.getTime()).toString();
			String suffix = lda_model+"_"+RunModel.domainID+"_"+timestamp;
			String output = "Model: "+lda_model+"\n";
			output += "Domain: "+RunModel.domain+"\n";
			output += "Books used: "+col.children.size()+"\n";
			output += "Total Nodes: "+(col.size()-1)+"\n";
			for(int i=0;i<col.children.size();i++){
				output += "  "+col.children.get(i).getTitle()+": "+col.children.get(i).size()+"\n";
			}
			output += "Truth dir: "+truthdir+"\n";
			output += "Aggregating content: "+RunModel.aggregatecontent+"\n";
			output += "Iterations in each run: "+RunModel.iterations+"\n";
			if (lda_model.equalsIgnoreCase("HLDA")){
				output += "Level weights: ";
				for(int i=0;i<level_weights.length;i++){
					output += level_weights[i] + "\t";
				}
				output += "\n";
			}
			output += eval_string;
					
			ModelReporter.writeFile(output, "data/output/"+condition+"/EVAL_"+suffix+".txt");
	        // Write the output
			
		}catch(Exception e){
			System.out.println(e.getMessage());
		}
	}
	
	public static void fillCollection(Collection col, 
									  String domainID, 
									  String domain_folder,
									  boolean addotherbooks, 
									  boolean using_vocabulary,
									  boolean repeat_titles,
									  boolean stemming,
									  String vocabulary_file,
									  boolean aggregatecontent) throws Exception{
		
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
			BookNode book1 = parser_1.getBookFromHTMLFiles("data/"+domain_folder+"/book1","Elementary Algebra","book1","Wade Ellis, Denny Burzynski");
			if (book1 != null) col.add(book1);
			System.out.println("Ok");
			
			System.out.print("Parsing book2 - Elementary Algebra (John Redden) ...");
			BookNode book2 = parser_2.getBookFromHTMLFiles("data/"+domain_folder+"/book2","Elementary Algebra, v1","book2","John Redden");
			if (book2 != null) col.add(book2);
			System.out.println("Ok");
			
			if (addotherbooks){
				System.out.print("Parsing book3 - Understanding Algebra (James W. Brennan) ...");
				BookNode book3 = parser_3.getBookFromHTMLFiles("data/"+domain_folder+"/book3","Understanding Algebra","book3","James W. Brennan");
				if (book3 != null && addotherbooks) col.add(book3);
				System.out.println("Ok");
				
				System.out.print("Parsing book4 - Fundamentals of Mathematics (Denny Burzynski and Wade Ellis) ...");
				BookNode book4 = parser_1.getBookFromHTMLFiles("data/"+domain_folder+"/book4","Fundamentals of Mathematics" , "book4" ,"Denny Burzynski and Wade Ellis");
				if (book4 != null && addotherbooks) col.add(book4);
				System.out.println("Ok");
				
				//System.out.print("Parsing book5 - Elementary Algebra (PDF parsed) ...");
				//BookNode book5 = parser_7.getTreeFromPDF("data/"+domain_folder+"/book5/ElementaryAlgebra.pdf", "book5", "Elementary Algebra");
				//if (book5 != null && addotherbooks) col.add(book5);
				//System.out.println("Ok");
			}
			// 3. SET THE VOCABULARY AND PREPROCESS THE TEXT
			Vocabulary vocabulary = null;
			if (using_vocabulary){
				vocabulary = new Vocabulary(vocabulary_file,"-");
			}
			PreprocessText.preprocessTree(col,repeat_titles, stemming, vocabulary);// repeat title=true, stemming-true,  vocabulary
			
			// recursively aggregates the content from children to parents
			if (aggregatecontent) col.aggregateContent();

		}else if(domainID.equalsIgnoreCase("ir")){
			String[][] books = {
					{"Introduction to Information Retrieval", "src_iir.xml","iir"}, // book 1
					{"Modern Information Retrieval","src_mir.xml","mir"}, // book 2
					{"Finding Out About", "src_foa.xml","foa"},
				 	{"Information Retrieval","src_ir.xml","ir"},
				 	{"Information Storage and Retrieval Systems","src_isrs.xml","isrs"}
				  };
			int nbooks = books.length;
			if (!addotherbooks) nbooks = 2; // just process first 2 books when addotherbooks==false
			
			for (int i=0;i<nbooks;i++){
				String booktitle = books[i][0];
				String bookfile = "data/"+domain_folder+"/"+books[i][1];
				String bookid = books[i][2];
				
				System.out.print("Parsing book"+(i+1)+" - "+booktitle);
				BookNode book = ParseSrcXML.parseXML(bookfile,booktitle,bookid);
				if (book != null) col.add(book);
				System.out.println("Ok");
			}
			Vocabulary vocabulary = null;
			if (using_vocabulary){
				vocabulary = new Vocabulary(vocabulary_file,"-");
			}
			PreprocessText.preprocessTree(col,repeat_titles, stemming, vocabulary);
			if (aggregatecontent) col.aggregateContent();
		}

	}
	
}
