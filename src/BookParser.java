import java.awt.print.Book;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/*
 * This class implements a simple parser with methods for parsing 
 * HTML content and getting the BookNode tree structure from it.
 * The implemented methods expect that each chapter of a book will be in  
 * a separate html file and that sections and subsections are labeled with 
 * specific CSS classes (set in sectionSeparator and subsectionSeparator) 
 * @author Julio Guerra
 */
public class BookParser {
	
	private String[] filteredTitles;
	private String[] filteredClasses;
	private String sectionSeparator;
	private String subsectionSeparator;

	public BookParser(String sectionSeparator, String subsectionSeparator, String[] filteredTitles, String[] filteredClasses) {
		this.filteredTitles = filteredTitles;
		this.filteredClasses = filteredClasses;
		this.sectionSeparator = sectionSeparator;
		this.subsectionSeparator = subsectionSeparator;
	}
	
	public String[] getFilteredTitles() {
		return filteredTitles;
	}
	public void setFilteredTitles(String[] filteredTitles) {
		this.filteredTitles = filteredTitles;
	}
	public String[] getFilteredClasses() {
		return filteredClasses;
	}
	public void setFilteredClasses(String[] filteredClasses) {
		this.filteredClasses = filteredClasses;
	}
	public String getSectionSeparator() {
		return sectionSeparator;
	}
	public void setSectionSeparator(String sectionSeparator) {
		this.sectionSeparator = sectionSeparator;
	}
	public String getSubsectionSeparator() {
		return subsectionSeparator;
	}
	public void setSubsectionSeparator(String subsectionSeparator) {
		this.subsectionSeparator = subsectionSeparator;
	} 
	
	public static boolean contains(String[] a, String b){
		if (a == null || a.length == 0) return false;
		for (int i=0;i<a.length;i++){
			if (b.toLowerCase().indexOf(a[i].toLowerCase())!=-1) return true;
		}
		return false;
	}
	
	public boolean isGoodTitle(String b){
		return !contains(filteredTitles,b);
	}
	
	public boolean isGoodCSSClass(String b){
		return !contains(filteredClasses,b);
	}

	
	/**
	 * Returns a BookNode object containing the tree of content of a complete HTML book.
	 * Content is reading from all files in a specific directory book_dir, each file containing a 
	 * chapter. Each file is processed using the method getNodeFromHTMLFile
	 * 
	 * @param book_dir the directory from which the files with the content are read. Each file should have a Chapter
	 * @param book_title
	 * @param book_id
	 * @param comment
//	 * @param parser_filter a BookParserFilter object is passed to the method getNodeFromHTMLFile
	 * @return returns a BookNode object containing the tree of content for an entire book.
	 * @throws Exception
	 */
	public BookNode getBookFromHTMLFiles(String book_dir, String book_title, String book_id, String comment) throws Exception{

		BookNode book = null;
		
		// open directory and read file names, then for each file creates BookNode object 
		File dir = new File(book_dir);
		
		if (dir != null){
			String[] filenames = dir.list();
			if (filenames == null || filenames.length == 0) return null;
			else{
				// Creates the BookNode object
				book = new BookNode(book_id,book_title,null);
				book.setComments(comment);
				
				for (int i=0; i<filenames.length; i++) {
			        // Get filename of file or directory
			        String filename = filenames[i];
			        // Just considering files not starting with "."
			        if (!filename.startsWith(".")){
			        	// Get the file name without extension
			        	String filename_ne = filename.substring(0,filename.indexOf("."));
			        	
			        	String node_id = book_id + "_" + filename_ne;
			        	//System.out.print("Processing file: " + book_dir+"/"+filename);
			        	book.add(getNodeFromHTMLFile(book_dir+"/"+filename,node_id));
			        	//System.out.println("  ... ok!");
			        	//System.out.println(filename + "  " + filename_ne);
			        }
			    }
				//System.out.println(book.treeToString(""));
			}
		}
		

		return book;
	}
	
	
	// Generates a BookNode object (tree with each heading of the content file as a node)
	public  BookNode getNodeFromHTMLFile(String htmlfile, String node_id) throws Exception{
		
		File input = new File(htmlfile);
		Document htmldoc = Jsoup.parse(input, "UTF-8", "http://example.com/");
		
		String node_title = htmldoc.title();
		
		// the node to return is the top node in the tree created below
		BookNode pnode = new BookNode(node_id, node_title, null);
		
		Element e_body = htmldoc.body();
		
/*		String[] classes_to_filter = getFilteredClasses();
		for(int i=0;i<classes_to_filter.length;i++){
			Elements filtered = e_body.select("."+classes_to_filter[i]);
			
			for (int j = 0; j < filtered.size(); j++) {
				Element to_filter = filtered.get(j);
				to_filter.remove();
			}
			
		}*/
		
		// gets all html elements matching the string section_separator (see Element.select method in Jsoup)
		Elements sections = e_body.select(getSectionSeparator());
		
		for (int i = 0; i < sections.size(); i++) {
			//System.out.println(i);
		    Element section = sections.get(i);
		    String title = section.attr("title");
		    
		    String sec_node_id = node_id + "_" + (i+1);
		    //System.out.println(sec_node_id);
		    if (isGoodTitle(title)){
		    
			    //System.out.println(title);
			    
			    // create the section node
			    BookNode sec_node = new BookNode(sec_node_id, title, pnode);
			    
			    Elements subsections = section.select(getSubsectionSeparator());

			    
			    if (subsections.size()>1){
			    	for (int j = 1; j < subsections.size(); j++) {
				    	Element subsection = subsections.get(j);
				    	String subsectiontitle = subsection.attr("title");
				    	//subsection.ownText()
				    	String subsectiontext = subsection.text()+" ";
				    	String subsectionhtml = subsection.html();
				    	String subsectioncssclass = subsection.attr("class");
				    	//System.out.println(subsectioncssclass);
				    	if (isGoodCSSClass(subsectioncssclass) && isGoodTitle(subsectiontitle)){
				    		if (subsectiontitle != null && !subsectiontitle.isEmpty()){
				    			String sub_sec_node_id = sec_node_id + "_" + j;
				    			BookNode sub_sec_node = new BookNode(sub_sec_node_id, subsectiontitle, sec_node);
				    			sub_sec_node.setText(subsectiontext); // get only text
				    			sub_sec_node.setRawContent(subsectionhtml); // get the html content
				    			//System.out.println("   "+subsectiontitle+"\t"+subsectiontext);
				    		}
				    	}else{
				    		//if (subsectiontitle != null && !subsectiontitle.isEmpty()) System.out.println("   FILTERED: "+subsectiontitle);
				    	}
				    }
			    }else{
			    	Elements allchildren = section.children();
			    	String text = "";
			    	for (int j = 1; j < allchildren.size(); j++) {
			    		text += allchildren.get(j).text();
			    	}
			    	sec_node.setText(text);
			    	//System.out.println(text);
			    }
			    
			    //System.out.println("-----------------------------------");
			    //System.out.println("-----------------------------------");	
		    }
		}
		return pnode;
	}

	public static void main(String[] args) throws Exception {
//		BookParser parser = new BookParser(null, null, null, null);
//		try {
//			parser.getBookFromHTMLFiles("/home/memray/Project/textbook_analyzer/data/algebra_documents/book1/","book1","book1", "");
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
        Collection books = new Collection("Algebra", "algebra");

        String INPUT_PATH = "/home/memray/Project/textbook_analyzer/data/algebra_documents/";

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
        BookNode book1 = parser_1.getBookFromHTMLFiles(INPUT_PATH+"/book1","Elementary Algebra","book1","Wade Ellis, Denny Burzynski");
        if (book1 != null) books.add(book1);
        System.out.println("Ok");

        System.out.print("Parsing book2 - Elementary Algebra (John Redden) ...");
        BookNode book2 = parser_2.getBookFromHTMLFiles(INPUT_PATH+"/book2","Elementary Algebra, v1","book2","John Redden");
        if (book2 != null) books.add(book2);
        System.out.println("Ok");

        System.out.print("Parsing book3 - Understanding Algebra (James W. Brennan) ...");
        BookNode book3 = parser_3.getBookFromHTMLFiles(INPUT_PATH+"/book3","Understanding Algebra","book3","James W. Brennan");
        if (book3 != null) books.add(book3);
        System.out.println("Ok");

        System.out.print("Parsing book4 - Fundamentals of Mathematics (Denny Burzynski and Wade Ellis) ...");
        BookNode book4 = parser_1.getBookFromHTMLFiles(INPUT_PATH+"/book4","Fundamentals of Mathematics" , "book4" ,"Denny Burzynski and Wade Ellis");
        if (book4 != null) books.add(book4);
        System.out.println("Ok");

        System.out.print("Parsing book5 - Elementary Algebra (PDF parsed) ...");
        BookNode book5 = parser_7.getTreeFromPDF(INPUT_PATH+"/book5/ElementaryAlgebra.pdf", "book5", "Elementary Algebra");
        if (book5 != null) books.add(book5);
        System.out.println("Ok");

        /**
         * 3. Output to files
         */

        books.aggregateContent();

/*        String OUTPUT_PATH = "/home/memray/Project/textbook_analyzer/data/algebra_textbook/";

        // iterate books
        for (BookNode book : books.getChildren() ){
//            System.out.println(book.getText());
            for (BookNode chapter : book.getChildren()){
                FileUtils.writeStringToFile(new File(OUTPUT_PATH+chapter.getDocId()+".txt"), chapter.getText(), "utf8");
                System.out.println(chapter.getDocId());
                for(BookNode section : chapter.getChildren()){
                    FileUtils.writeStringToFile(new File(OUTPUT_PATH+section.getDocId()+".txt"), section.getText(), "utf8");
                    System.out.println(section.getDocId());
                    for(BookNode subsection : section.getChildren()){
                        FileUtils.writeStringToFile(new File(OUTPUT_PATH+subsection.getDocId()+".txt"), subsection.getText(), "utf8");
                        System.out.println(subsection.getDocId());
                    }
                }
            }
        }*/

        String CORPUS_PATH = "/home/memray/Project/textbook_analyzer/data/corpus_algebra_two.txt";
        HashSet<String> set = new HashSet<>();
        set.add("book1");
        set.add("book2");
//        set.add("book3");
//        set.add("book4");
//        set.add("book5");

        StringBuilder stringBuilder = new StringBuilder();

        for (BookNode book : books.getChildren() ) {
//            System.out.println(book.getText());
            if (!set.contains(book.getDocId()))
                continue;
            for (BookNode chapter : book.getChildren()) {
                stringBuilder.append(chapter.getDocId() + '\t' + chapter.getText() + '\n');
                System.out.println(chapter.getDocId());
                for (BookNode section : chapter.getChildren()) {
                    stringBuilder.append(section.getDocId() + '\t' + section.getText() + '\n');
                    System.out.println(section.getDocId());
                    for (BookNode subsection : section.getChildren()) {
                        stringBuilder.append(subsection.getDocId() + '\t' + subsection.getText() + '\n');
                        System.out.println(subsection.getDocId());
                    }
                }
            }
        }

        FileUtils.writeStringToFile(new File(CORPUS_PATH), stringBuilder.toString(), "utf8");
    }
}
