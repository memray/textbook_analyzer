import java.io.IOException;
import java.util.ArrayList;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.util.PDFTextStripper;

public class ParsePDF {
	
	private int nBookMark = 0;
	private PDFTextStripper stripper; 
	private String[] bad_titles;
	public ParsePDF(String[] bad_titles){
		this.bad_titles = bad_titles;
		try{
			stripper = new PDFTextStripper();
		}catch(Exception e){
			
		}
		
	}
	public static void main(String args[]) throws IOException{
		String pdffile = "data/algebra/book5/ElementaryAlgebra.pdf";
		String[] bad_titles = {"Exercises","Answers","Applications","Index"};
		ParsePDF parser = new ParsePDF(bad_titles);
		//System.out.println("Parsing: "+pdffile);
		
		BookNode book = parser.getTreeFromPDF(pdffile, "book5", "Elementary Algebra");
		//System.out.println(book.treeToString("  "));
		ModelReporter.writeFile(book.treeToString("  "),"data/output/pdf_parsed.txt");
	    
	    
	    //stripper.setStartBookmark(bookmarks.get(5));
	    //stripper.setEndBookmark(bookmarks.get(5));
	    //String text_content = stripper.getText(doc);
	    //System.out.println(bookmarks.get(5).getTitle()+"\n");
	    //System.out.println(text_content);
	   // System.out.println(bookmarks.get(4).getTitle()+"\n");
	    
	}
	
	public static void printTree(PDOutlineItem item, String offset, ArrayList<PDOutlineItem> bookmarks){
		if (item != null ){
			//System.out.println( offset+"Item:" + item.getTitle() );
			
		
			bookmarks.add(item);
			PDOutlineItem child = item.getFirstChild();
			printTree(child,offset+"   ",bookmarks );
			printTree(item.getNextSibling(),offset,bookmarks);
		}
		
	}
	
	public BookNode getTreeFromPDF(String pdffile, String book_id, String book_title) throws IOException{
		PDDocument doc = PDDocument.load(pdffile);
		
		//System.out.println(doc.getNumberOfPages()+" pages");
		
		
		ArrayList<PDOutlineItem> bookmarks = new ArrayList<PDOutlineItem>();
		BookNode book = new BookNode(book_id,book_title,null);
		PDDocumentOutline root = doc.getDocumentCatalog().getDocumentOutline();
		PDOutlineItem first_item = root.getFirstChild();
	   
		extractBranch(book, first_item, 1, bookmarks);
	    
		extractText(book, bookmarks, doc);
		
		filterNodesByTitle(book, bad_titles);
		doc.close();
		return book;
	}
	
	public void extractBranch(BookNode parent, PDOutlineItem item, int n, ArrayList<PDOutlineItem> bookmarks){
		if (item == null) return;
		
		String doc_id = parent.getDocId()+"_"+n;		
		String title = item.getTitle().trim();
		
		//System.out.println(doc_id+" :: "+title);
		
		BookNode book_node = new BookNode(doc_id,title,parent);
		bookmarks.add(item);
		
		PDOutlineItem child = item.getFirstChild();
		extractBranch(book_node, child, 1, bookmarks);
		PDOutlineItem sibling = item.getNextSibling();
		extractBranch(parent, sibling, n+1, bookmarks);
	}
	
	public void extractText(BookNode book,ArrayList<PDOutlineItem> bookmarks, PDDocument doc) throws IOException{
		BookNode[] nodes = book.toArray();
		for (int i=0;i<bookmarks.size();i++){
			PDOutlineItem bookmark_start = bookmarks.get(i);
			PDOutlineItem bookmark_end = null;
			if (i<bookmarks.size()-1) bookmark_end = bookmarks.get(i+1);
		    stripper.setStartBookmark(bookmark_start);
		    stripper.setEndBookmark(bookmark_end);
		    
		    String text_content = stripper.getText(doc);
		    // some processing of the text
		    text_content = text_content.replace("\n", " ");
		    text_content = text_content.replace("\r", " ");
		    text_content = text_content.replace("  ", " ");
		    
		    int start = text_content.indexOf(bookmark_start.getTitle());
		    int end = 9999999;
		    if (start == -1) start = 0;
		    else{
		    	text_content = text_content.substring(start);
		    }
		    if (bookmark_end != null){
		    	end = text_content.indexOf(bookmark_end.getTitle());
		    	if (end != -1) text_content = text_content.substring(0,end);
		    }
		    //System.out.println(" "+i+" : "+bookmark_start.getTitle()+" = "+nodes[i+1].getTitle()+" ("+start+","+end+")");
		    nodes[i+1].setText(text_content);
		}
		
	}
	
	public void filterNodesByTitle(BookNode book, String[] bad_titles){
		BookNode[] nodes = book.toArray();
		for(int i=1;i<nodes.length;i++){
			BookNode node = nodes[i];
			for(int j=0;j<bad_titles.length;j++){
				if (node.getTitle().trim().equalsIgnoreCase(bad_titles[j])){
					//System.out.println(" removing "+node.getTitle());
					node.getParentNode().children.remove(node);
				}
			}
		}
	}
}
