import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;

public class ParseSrcXML {
	static String[][] books = {
								{"Introduction to Information Retrieval", "src_iir.xml","iir"}, // book 1
								{"Modern Information Retrieval","src_mir.xml","mir"}, // book 2
								{"Finding Out About", "src_foa.xml","foa"},
							 	{"Information Retrieval","src_ir.xml","ir"},
							 	{"Information Storage and Retrieval Systems","src_isrs.xml","isrs"}
							 	
							  };
	static String book_dir = "data/information_retrieval/";
	
	public static void main(String[] args) throws Exception{
		int nbooks = books.length;
		String out = "";
		for (int i=0;i<nbooks;i++){
			String booktitle = books[i][0];
			String bookfile = book_dir+books[i][1];
			String bookid = books[i][2];
			BookNode book = parseXML(bookfile,booktitle,bookid);
			String ui_book = "book2";
			if (i==0) ui_book = "book1";
			String bookui = book.tree2HTML("", 0, "data/information_retrieval/ui/content", (i==0), ui_book);
			ModelReporter.writeFile(bookui, "data/information_retrieval/ui/"+ui_book+"_"+bookid+".html");
			out += book.treeToString("")+"\n";
			
		}
		ModelReporter.writeFile(out, book_dir+"check_out.txt");
	}
	
	public static BookNode parseXML(String filename, String title, String bookid) throws Exception{
		BookNode book = new BookNode(bookid,title,null);
		File fXmlFile = new File(filename);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);
		doc.getDocumentElement().normalize();
		
		BookNode current_parent = book;
		NodeList nList = doc.getElementsByTagName("DOC");
		
		int level = 0;
		for (int i = 0; i < nList.getLength(); i++) {
			Node nNode = nList.item(i);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				String ntitle = getTagValue("TITLE", eElement);
				String ndocno = getTagValue("DOCNO", eElement);
				String ntext = getTagValue("TEXT", eElement).replaceAll("\n", " ").replaceAll("\r", " ");
				
				
				String num = ntitle.split(" ")[0].toLowerCase();
				int dots = countDots(num);
				if (num.equals("chapter") || dots==0){
					current_parent = book;
					level = 0;
				}else{
					
					if (dots>level){
						level = dots;
					}else{
						int diff = level - dots + 1;
						for(int j=0;j<diff;j++){
							current_parent = current_parent.getParentNode();
						}
						level = dots;
					}
				}
				BookNode new_node = new BookNode(ndocno,ntitle,current_parent);
				new_node.setText(ntext);
				new_node.setRawContent(ntext);
				current_parent = new_node;
				
				
				
				//System.out.println("title : " + getTagValue("TITLE", eElement) + " (" + getTagValue("DOCNO", eElement) + ")");
			}
			
			//System.out.println(nNode.getTextContent());
		}
		
		return book;
	}
	private static String getTagValue(String sTag, Element eElement) {
		NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
	 
		Node nValue = (Node) nlList.item(0);
	 
		return nValue.getNodeValue();
	}
	
	public static int countDots(String num){
		int r = 0;
		for(int i=0;i<num.length();i++){
			if (num.charAt(i) == '.') r++;
		}
		return r;
	}

}
