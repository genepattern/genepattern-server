package org.genepattern.server.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ModuleRepository {
    protected String motd_message = "";
    protected String motd_url = "";
    protected int motd_urgency = 0;
    protected long motd_timestamp = 0;
    protected String motd_latestServerVersion = "";

	ModuleRepository(){
		return;
	}

	public static void main(String [] args)  {
		String url = "http://www.broad.mit.edu/cgi-bin/cancer/software/genepattern/gp_module_repository.cgi";

		ModuleRepository mr = new ModuleRepository();

		try {
			mr.GetModules(url);
		} catch (Exception e) {
			e.printStackTrace();
		}

		//		System.out.println(dumpDOM(root, 0));
	}


	public InstallTask [] GetModules(String url) throws IOException, SAXException, ParserConfigurationException,IllegalArgumentException, IllegalAccessException, NoSuchMethodException, SecurityException {
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

		InstallTask [] module_list = parseDOM(root);
		return (module_list);

	}


	public static String dumpDOM(Node node, int indent) {
		if (node.getNodeType() != Node.ELEMENT_NODE) {
			// most likely a comment or text node
			System.out.println("name= "+node.getNodeName() + ", type=" + node.getNodeType() + ", value=" + node.getNodeValue());
			return "";
		}
		Element element = (Element)node;
		String indentString = "\t\t\t\t\t\t\t\t\t".substring(0, indent);
			StringWriter outputWriter = new StringWriter();
		NamedNodeMap attributes = element.getAttributes();
		outputWriter.write(indentString);
		outputWriter.write("<" + element.getTagName());
		if (attributes != null) {
			for (int i = 0; i < attributes.getLength(); i++) {
				outputWriter.write("Attribute: " + ((Attr)attributes.item(i)).getName() + "=" + ((Attr)attributes.item(i)).getValue() + "\n");
			}
		}
		outputWriter.write("/>\n");
		NodeList children = element.getChildNodes();
		for (int child = 0; child < children.getLength(); child++) {
			outputWriter.write(dumpDOM(children.item(child), indent+1));
		}
		return outputWriter.toString();
}

	public InstallTask[] parseDOM(Node node) {
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
					                            InstallTask it = new InstallTask(null, manifest,
					                                    supportUrlArray, url, fileSize, timestamp,
					                                    siteName);
					                            module_list.add(it);
								} catch (Throwable t) {
									System.err.println(t.getMessage());
									t.printStackTrace();
								}

							} else if (c_elt.getTagName().equals(NODE_SITE_MOTD)) {
//System.out.println("got " + c_elt.getTagName() + ": " + dumpDOM(c_node, 0));
								NodeList motdNodes = c_node.getChildNodes();
								for (int j = 0; j < motdNodes.getLength(); j++) {
									c_node = motdNodes.item(j);
									if (c_node.getNodeType() == Node.ELEMENT_NODE) {
										c_elt = (Element)c_node;
										if (c_elt.getTagName().equals(NODE_MOTD_MESSAGE)) {
											motd_message = c_node.getChildNodes().item(0).getNodeValue();
										} else if (c_elt.getTagName().equals(NODE_MOTD_URL)) {
											motd_url = c_node.getChildNodes().item(0).getNodeValue();
										} else if (c_elt.getTagName().equals(NODE_MOTD_URGENCY)) {
											try {
												motd_urgency = Integer.parseInt(c_node.getChildNodes().item(0).getNodeValue());
											} catch (NumberFormatException nfe) {
												// ignore
												System.out.println(nfe.getMessage() + " in ModuleRepository.getModules() handling MOTD urgency");
											}
										} else if (c_elt.getTagName().equals(NODE_MOTD_TIMESTAMP)) {
											try {
												motd_timestamp = Long.parseLong(c_node.getChildNodes().item(0).getNodeValue());
											} catch (NumberFormatException nfe) {
												try {
													motd_timestamp = new SimpleDateFormat("dd-MMM-yyyy").parse(c_node.getChildNodes().item(0).getNodeValue()).getTime();
												} catch (ParseException pe) {
													// ignore
													System.out.println(pe.getMessage() + " in ModuleRepository.getModules() handling MOTD timestamp");
												}
											}
										} else if (c_elt.getTagName().equals(NODE_MOTD_LATESTSERVERVERSION)) {
											motd_latestServerVersion = c_node.getChildNodes().item(0).getNodeValue();
										} else {
											System.out.println("Got non-MOTD node where motd expected: " + c_elt.getTagName());
										}
									}
								}
							} else {
								System.out.println("Got non-sitemodule node where sitemodule expected: " + c_elt.getTagName());
							}
						} else {
							//System.out.println("Got non-element node where element expected " + c_node.getNodeType());
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

		InstallTask [] task_list = (InstallTask [])module_list.toArray(new InstallTask[0]);

		return task_list;
	}

    public String getMOTD_message() {
	return motd_message;
    }

    public String getMOTD_url() {
	return motd_url;
    }

    public int getMOTD_urgency() {
	return motd_urgency;
    }

    public Date getMOTD_timestamp() {
	return new Date(motd_timestamp);
    }

    public String getMOTD_latestServerVersion() {
	return motd_latestServerVersion;
    }

	public static String NODE_MANIFEST="manifest";
	public static String NODE_SUPPORTFILE="support_file";
	public static String NODE_SITEMODULE="site_module";
	public static String NODE_MODULEREPOSITORY ="module_repository";
	public static String NODE_SITE_MOTD = "site_motd";

	public static String NODE_MOTD_MESSAGE = "motd_message";
	public static String NODE_MOTD_URL = "motd_url";
	public static String NODE_MOTD_URGENCY = "motd_urgency";
	public static String NODE_MOTD_TIMESTAMP = "motd_timestamp";
	public static String NODE_MOTD_LATESTSERVERVERSION = "motd_latestServerVersion";
}
