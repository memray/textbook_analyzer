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

import java.util.*;
import java.util.logging.Level;
import java.util.regex.*;
import java.io.*;

public class HLDATest {
	public static HierarchicalLDA model_hLDA;
	public static InstanceList training_instances = null;
	public static InstanceList testing_instances = null;
	public static int iterations = 1000;
	public static int levels = 6;
	public static double alpha = 1;
	public static double gamma = 0.1;
	public static double eta = 0.01;
	
	public static void main(String args[]) throws IOException{
		training_instances = prepareInstances("data/hLDA_test/docs_instances_short.txt",null);
		testing_instances = prepareInstances("data/hLDA_test/docs_testing.txt",training_instances);
		
		model_hLDA = new HierarchicalLDA();
		
		// Set hyperparameters

		model_hLDA.setAlpha(alpha);
		model_hLDA.setGamma(gamma);
		model_hLDA.setEta(eta);
		
		// Display preferences

		model_hLDA.setTopicDisplay(500, 10);
		model_hLDA.setProgressDisplay(true);

		// Initialize random number generator

		Randoms random = new Randoms();

		// Initialize and start the sampler

		model_hLDA.initialize(training_instances, testing_instances, levels, random);
		model_hLDA.estimate(iterations);
		
		//training_instances.get(0).
	
		// Inferencer
		HierarchicalLDAInferencer inferencer = new HierarchicalLDAInferencer(model_hLDA);
		HLDAUtil hlda_util = new HLDAUtil(inferencer);
		// Output results
		inferencer.printNode(inferencer.rootNode, 0);

		//model_hLDA.printState(new PrintWriter("data/hLDA_test/output_state.txt"));
		
		//if (testing != null) {
		//	double empiricalLikelihood = model_hLDA.empiricalLikelihood(1000, testing);
		//	System.out.println("Empirical likelihood: " + empiricalLikelihood);
		//}
		try{
			// print tree structure 
			//FileWriter fstream = new FileWriter("raw-hlda-3level-topicTree_eta07.csv");
			FileWriter fstream = new FileWriter("data/hLDA_test/output.txt");
			BufferedWriter out = new BufferedWriter(fstream);
			inferencer.printNodeTofile(inferencer.rootNode, 0, out);
					
			//Close the output stream
			out.close();
			
			System.out.println("total nodes:" + inferencer.counter);
			System.out.println();
			   
			// print train instance 
			//BufferedWriter trainOut = new BufferedWriter(new FileWriter("raw-hlda-3level-topic_distribution_eta07.csv"));
			BufferedWriter trainOut = new BufferedWriter(new FileWriter("data/hLDA_test/output2.txt")); 
			inferencer.printTrainData(training_instances, trainOut);
			trainOut.close();
			   
			System.out.println("total train instance:" + training_instances.size());
			System.out.println();
			
			String traindata = hlda_util.trainDataToString(training_instances);
			
			ModelReporter.writeFile(traindata, "data/hLDA_test/output_traindata.txt");
			
			String testdata = hlda_util.testDataToString(testing_instances);
			
			ModelReporter.writeFile(testdata, "data/hLDA_test/output_testdata.txt");
			// print test instance   
			//BufferedWriter testOut = new BufferedWriter(new FileWriter("hlda-test-v5-4level-5000.csv"));
			//inferencer.printTestData(testInstances, 300, testOut);
			//testOut.close();
			   
			System.out.println("total test instance:" + testing_instances.size());
			   	  
			   
			}catch(Exception e){
				e.printStackTrace();
			}
		
	}
	
	private static InstanceList prepareInstances(String content_file, InstanceList training_instances) throws IOException{
		InstanceList instances = null;
		if (training_instances==null){
	        ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

	        // Pipes: lowercase, tokenize, remove stopwords, map to features
	        pipeList.add( new CharSequenceLowercase() );
	        pipeList.add( new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")) );
	        pipeList.add( new TokenSequenceRemoveStopwords(new File("stoplists/en.txt"), "UTF-8", false, false, false) );
	        // an extra stop word list is considered for the domain
	        pipeList.add( new TokenSequenceRemoveStopwords(new File("stoplists/en_extra.txt"), "UTF-8", false, false, false) );
	        pipeList.add( new TokenSequence2FeatureSequence() );

	        instances = new InstanceList (new SerialPipes(pipeList));
			
		}else{
			instances = new InstanceList (training_instances.getPipe());
		}
        
        Reader contentReader = new FileReader(content_file);
        instances.addThruPipe(new CsvIterator (contentReader, Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"),
                                               3, 2, 1)); // data, label, name fields
        return instances;
	}
	
}
