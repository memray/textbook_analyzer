import java.io.File;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ParseIIR {
	public static String toc_html = "data/information_retrieval/iir_toc_manning_proc.html";
	public static String output_folder = "data/information_retrieval/iir/";
	
	public static void main(String[] args) throws Exception{
		File input = new File(toc_html);
		Document htmldoc = Jsoup.parse(input, "UTF-8", "http://example.com/");
		Element toc = htmldoc.select("#toc").first();
		//String out = "<toc>\n"; 
		//out += getChildren(toc, "iir", "  ");
		//out += "</toc>\n";
		String out = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		out += "<docs>\n";
		out += getDocs(toc, "iir", "");
		out += "</docs>\n";
		
		ModelReporter.writeFile(out, "data/information_retrieval/iir/src_iir.xml");
	/*	
		Elements children = toc.children();
		// getting the chapters
		for (Element child : children){
			Element link = child.children().select("a").first();
			String title = link.text();
			String url = link.attr("href");
			System.out.println(title+" \t"+url);
		}
	*/
	}
	public static String getChildren(Element e, String parent_id, String offset){
		String out = ""; 
		if (e != null){
			Elements children = e.children();
			if (children != null){
				int i=1;
				for (Element child : children){
					
					Element link = child.children().select("a").first();
					String id = parent_id+"_"+i;
					String title = link.text();
					
					String url = link.attr("href");
					
					
					
					String filestr = url.substring(url.lastIndexOf('/')+1);
					String text = "";
					if (true){
						try{
							Document doc = Jsoup.connect(url).get();
							Element body = doc.body();
							Element h1 = doc.select("h1").first();
							Element hr = doc.select("hr").last();
							//text = h1.text()+"\n";
							boolean start = false;
							for (Element el : body.children()){
								if (el == hr) start = false;
								if (el == h1) start = true;
								if (start) text += " "+el.text();
							}
							
							//text = doc.text();
							//ModelReporter.writeFile(text, "data/information_retrieval/iir/files_txt2/"+filestr);
						}catch(Exception exc){
							
						}
					}
					out += offset+"<section>\n";
					out += offset+"  <id>"+id+"</id>\n";
					out += offset+"  <title>"+title+"</title>\n";
					out += offset+"  <url>"+url+"</url>\n";
					out += offset+"  <file>"+filestr+"</file>\n";
					out += offset+"  <text>"+text+"</text>\n";
					
					out += offset+"  <subsections>\n";
					out += getChildren(child.select("ul").first(),id,offset+"     ");
					out += offset+"  </subsections>\n";
					
					out += offset+"</section>\n";
					i++;
				}
			}
		}
		return out;
	}

	public static String getDocs(Element e, String parent_id, String parent_num){
		String out = ""; 
		if (e != null){
			Elements children = e.children();
			if (children != null){
				int i=1;
				for (Element child : children){
					
					Element link = child.children().select("a").first();
					String id = parent_id+"_"+i;
					String num = ""+i;
					String chapter = "";
					if (parent_num.length() > 0){
						num = parent_num + "."+i;
					}else{
						chapter = "Chapter ";
					}
					String title = link.text();
					
					String url = link.attr("href");
					
					String filestr = url.substring(url.lastIndexOf('/')+1);
					String text = "";
					if (true){
						try{
							//Document doc = Jsoup.connect(url).get();
							String inputfile = output_folder+"files_html_ORIG/"+filestr;
							Document doc = Jsoup.parse(new File(inputfile), "UTF-8", "http://example.com/");
							Element body = doc.body();
							Element h1 = doc.select("h1, h2, h3").first();
							Element hr = doc.select("hr").last();
							String contenthtml = "";
							//text = h1.text()+"\n";
							boolean start = false;
							for (Element el : body.children()){
								if (el == hr) start = false;
								if (el == h1) start = true;
								if (start){
									text += " "+el.text().replace('&', ' ').replaceAll("\\<.*?>","");
									contenthtml += el.html();
								}
								
							}
							
							
							ModelReporter.writeFile(contenthtml, "data/information_retrieval/iir/files_html/"+id+".html");
						}catch(Exception exc){
							
						}
					}
					out += "<DOC>\n";
					out += "  <DOCNO>"+id+"</DOCNO>\n";
					out += "  <TITLE>"+chapter + num + " " + title+"</TITLE>\n";
					out += "  <TEXT>\n  "+text+"\n  </TEXT>\n";
					out += "</DOC>\n\n";
					
					out += getDocs(child.select("ul").first(),id,num);
					i++;
				}
			}
		}
		return out;
	}
	
	
}
