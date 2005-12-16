/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/


import java.io.*;
import java.net.URL;
import java.text.*;
import java.util.*;
import org.xml.sax.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.text.DateFormat;

public class TaskCatalogChecker {

	TaskCatalogChecker (){
		return;
	}

	public static void main(String [] args)  {
		String url = "http://www.broad.mit.edu/cgi-bin/cancer/software/genepattern/gp_module_repository.cgi";

		if (args.length > 0) {
			url = args[0];
		}

		Date today = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("MMddyyyy_HHmm");

		String outFile = "catalogLsidList.txt";// + sdf.format(today);
		TaskCatalogChecker mr = new TaskCatalogChecker ();

		try {
			String[] ids = mr.GetModuleIds(url);

			BufferedWriter out = new BufferedWriter(new FileWriter(new File(outFile))); 
			for (int i=0; i < ids.length; i++){
				//System.out.println(ids[i]);
				out.write(ids[i]);
				if (i != (ids.length -1)) out.write("\n");
			}
			out.flush();
			out.close();
			mr.createBuildXml(ids);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	public String[] GetModuleIds(String url) throws IOException, SAXException, ParserConfigurationException,IllegalArgumentException, IllegalAccessException, NoSuchMethodException, SecurityException {
		String DBF = "javax.xml.parsers.DocumentBuilderFactory";
		String oldDocumentBuilderFactory = System.getProperty(DBF);
		URL reposURL = new URL(url);
		InputStream is = null;
		Document doc = null;
		try {
			is = reposURL.openStream();
			System.setProperty(DBF, "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new InputStreamReader(is)));
		} catch (IOException ioe) {
			throw new IOException(ioe.getMessage() + " while connecting to " +url);
	        } finally {
			try {
				if (is != null) is.close();
			} catch (IOException ioe) {
				// ignore
			}
			if (oldDocumentBuilderFactory != null)
				System.setProperty(DBF, oldDocumentBuilderFactory);
		}

		Element root = doc.getDocumentElement();
		// System.out.println("ModuleRepository: DOM=" + dumpDOM(root, 0));

		String [] module_list = parseDOM(root);
		return (module_list);

	}


	public String[] parseDOM(Node node) throws IOException {
		Vector module_list = new Vector();
		int c_module = 0;

		switch (node.getNodeType()) {
			case Node.ELEMENT_NODE:
				Element element = (Element)node;
				String manifest = "";
				String support_file;

				if (element.getTagName().equals(NODE_MODULEREPOSITORY)){
					NodeList moduleNodes = node.getChildNodes();
					for (int i = 0; i < moduleNodes.getLength(); i++) {
						Node c_node = moduleNodes.item(i);
						if (c_node.getNodeType() == Node.ELEMENT_NODE) {
							Element c_elt = (Element)c_node;

							if (c_elt.getTagName().equals(NODE_SITEMODULE)){

								String taskName = c_elt.hasAttribute("name") ? c_elt.getAttribute("name") : null;
								int fileSize = c_elt.hasAttribute("zipfilesize") ? (int)(Integer.parseInt(c_elt.getAttribute("zipfilesize"))) : 0;
								long timestamp = c_elt.hasAttribute("timestamp") ? Integer.decode(c_elt.getAttribute("timestamp")).longValue() : 0;
								String url = c_elt.hasAttribute("url") ? c_elt.getAttribute("url") : null;
								String siteName = c_elt.hasAttribute("sitename") ? c_elt.getAttribute("sitename") : null;
								boolean external = c_elt.hasAttribute("isexternal") ? (boolean)(Boolean.getBoolean(c_elt.getAttribute("isexternal"))) : false;
                                                int slashidx = url.lastIndexOf('/');
								String zipname = url.substring(slashidx+1);
								
								Vector support_urls = new Vector();

								NodeList children = c_node.getChildNodes();

								for (int j = 0; j < children.getLength(); j++) {
									Node c_childNode = children.item(j);
									if (c_childNode.getNodeType() == Node.ELEMENT_NODE) {
										Element childElt = (Element)c_childNode;
										if (childElt.getTagName().equals(NODE_MANIFEST)) {
												Node valueNode = c_childNode.getFirstChild();
												manifest = valueNode.getNodeValue();
										} else if (childElt.getTagName().equals(NODE_SUPPORTFILE)) {
												support_file = childElt.hasAttribute("url") ? childElt.getAttribute("url") : null;
												support_urls.add(support_file);
										}
									}
								}
								
								
								String [] supportUrlArray = (String [])support_urls.toArray(new String[0]);
								try {
					             
									String lsid = getLsid(manifest);
   									String ver = lsid.substring(lsid.lastIndexOf(":")+1);
									String lsidTag=lsid.replace('.','_');									
									lsidTag = lsidTag.replace(':','=');
									int zipidx = zipname.indexOf(".zip");
									zipname = zipname.substring(0,zipidx) +"."+ ver + ".zip";

					                        module_list.add(zipname +"=" + lsidTag);
								} catch (Throwable t) {
									System.err.println(t.getMessage());
									t.printStackTrace();
								}

							} 
						} 
					} // for loop
				} else {
					System.out.println("Got non-modulerepository node where modulerepository node expected: "+ element.getTagName());
				}

			break;

			case Node.ATTRIBUTE_NODE:
				System.out.println("Got attribute node where not expected: "+ "name= "+node.getNodeName() + ", type=" + node.getNodeType() + ", value=" + node.getNodeValue());
				break;

			case Node.TEXT_NODE:
				System.out.println("Got text node where not expected: " + "name= "+node.getNodeName() + ", type=" + node.getNodeType() + ", value=" + node.getNodeValue());
				break;

			default:
				break;
			}

		String [] task_list = (String [])module_list.toArray(new String[0]);

		return task_list;
	}

   

	public String getLsid(String manifest) throws IOException {
		
		Properties props = new Properties();
		props.load(new StringBufferInputStream(manifest));
		String LSID = props.getProperty("LSID");
		return LSID;
	}


	/**
	* Create a build.xml file that can be called in the modules/release_archive directory that
	* will check out all of the zip files using their LSID tag in CVS
	*/
	public void createBuildXml(String[] task_list) throws IOException{
		BufferedWriter out = new BufferedWriter(new FileWriter(new File("catalogcontents.xml")));
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n");
		out.write("<project basedir=\".\" name=\"getzip\" default=\"getzip\">\n\n");
		out.write("<target name=\"getzip\">\n\n");
		out.write("<mkdir dir=\"catalogregen\"/>\n\n");		

		for (int i=0; i < task_list.length; i++){
			addZip(out, task_list[i]);
		}

		out.write("</target>\n");
		out.write("</project>\n");
		out.close();
	}

	public void addZip(BufferedWriter out, String zipandtag) throws IOException {
		int idx1 = zipandtag.indexOf(".");
		int idx2 = zipandtag.indexOf(".", idx1+1);
		int idx3 = zipandtag.indexOf("=", idx2+1);

		String name = zipandtag.substring(0, idx1);
		String ver = zipandtag.substring(idx1+1, idx2);
		String tag = zipandtag.substring(idx3+1);

		String zip= name + ".zip";
		out.write("<cvs cvsRoot=\":ext:darwin:/xchip/software/mprcvs/\" failonerror=\"true\">\n");
		out.write("\t<commandline>\n");
		out.write("\t\t<argument line=\"update -r "+tag+" " + zip );
		out.write("\"/>\n");
		out.write("\t</commandline>\n");
		out.write("</cvs>\n");
		out.write("<copy file=\""+zip+"\" tofile=\"catalogregen/"+name + "." + ver +".zip\" />\n\n");

		out.flush();

	}


	public static String NODE_MANIFEST="manifest";
	public static String NODE_SUPPORTFILE="support_file";
	public static String NODE_SITEMODULE="site_module";
	public static String NODE_MODULEREPOSITORY ="module_repository";
	
	
}
