import java.io.File;

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
	 * @param parser_filter a BookParserFilter object is passed to the method getNodeFromHTMLFile
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
		
		String[] classes_to_filter = getFilteredClasses();
		for(int i=0;i<classes_to_filter.length;i++){
			Elements filtered = e_body.select("."+classes_to_filter[i]);
			
			for (int j = 0; j < filtered.size(); j++) {
				Element to_filter = filtered.get(j);
				to_filter.remove();
			}
			
		}
		
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
	
}
