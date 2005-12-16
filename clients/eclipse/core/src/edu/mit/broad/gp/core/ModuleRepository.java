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


package edu.mit.broad.gp.core;

import java.io.*;
import java.util.*;
import org.xml.sax.*;
import org.w3c.dom.*;

import edu.mit.broad.gp.core.ServiceManager;
import org.genepattern.webservice.AnalysisService;

import javax.xml.parsers.*;

import java.net.URL;

public class ModuleRepository {
    static private String REPOSITORY_URL = "http://www.broad.mit.edu/cgi-bin/cancer/software/genepattern/gp_module_repository.cgi";

    private ModuleRepository() {
    }

    /*
     * static { System .setProperty( "ModuleRepositoryURL",
     * "http://www.broad.mit.edu/cgi-bin/cancer/software/genepattern/gp_module_repository.cgi"); }
     */

    public static InstallTask[] getModules(ServiceManager serviceManager)
            throws SAXException, IOException, ParserConfigurationException,
            FactoryConfigurationError {
        String xmlContent;
        URL reposURL = new URL(REPOSITORY_URL);
        try {
            InputStream is = reposURL.openStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer content_buf = new StringBuffer(40000);
            while ((line = in.readLine()) != null) {
                content_buf.append(line);
            }
            xmlContent = content_buf.toString();
        } catch (Exception e) {
            xmlContent = "";
            e.printStackTrace();
        }
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().parse(
                        new InputSource(new StringReader(xmlContent)));
        Element root = doc.getDocumentElement();

        InstallTask[] module_list = parseDOM(root, serviceManager);
        return (module_list);

    }

    public static String dumpDOM(Node node, int indent) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            // most likely a comment or text node
            System.out.println("name= " + node.getNodeName() + ", type="
                    + node.getNodeType() + ", value=" + node.getNodeValue());
            return "";
        }
        Element element = (Element) node;
        String indentString = "\t\t\t\t\t\t\t\t\t".substring(0, indent);
        StringWriter outputWriter = new StringWriter();
        NamedNodeMap attributes = element.getAttributes();
        outputWriter.write(indentString);
        outputWriter.write("<" + element.getTagName());
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                outputWriter.write("Attribute: "
                        + ((Attr) attributes.item(i)).getName() + "="
                        + ((Attr) attributes.item(i)).getValue() + "\n");
            }
        }
        outputWriter.write("/>\n");
        NodeList children = element.getChildNodes();
        for (int child = 0; child < children.getLength(); child++) {
            outputWriter.write(dumpDOM(children.item(child), indent + 1));
        }
        return outputWriter.toString();
    }

    public static InstallTask[] parseDOM(Node node,
            ServiceManager serviceManager) {
        Vector module_list = new Vector();
        int c_module = 0;

        switch (node.getNodeType()) {
        case Node.ELEMENT_NODE:
            Element element = (Element) node;
            String manifest = "";
            String support_file;

            if (element.getTagName().equals(NODE_MODULEREPOSITORY)) {
                NodeList moduleNodes = node.getChildNodes();
                for (int i = 0; i < moduleNodes.getLength(); i++) {
                    Node c_node = moduleNodes.item(i);
                    if (c_node.getNodeType() == Node.ELEMENT_NODE) {
                        Element c_elt = (Element) c_node;

                        if (c_elt.getTagName().equals(NODE_SITEMODULE)) {

                            String taskName = c_elt.hasAttribute("name") ? c_elt
                                    .getAttribute("name")
                                    : null;
                            int fileSize = c_elt.hasAttribute("zipfilesize") ? (int) (Integer
                                    .parseInt(c_elt.getAttribute("zipfilesize")))
                                    : 0;

                            long timestamp = c_elt.hasAttribute("timestamp") ? Integer
                                    .decode(c_elt.getAttribute("timestamp"))
                                    .longValue()
                                    : 0;
                            String url = c_elt.hasAttribute("url") ? c_elt
                                    .getAttribute("url") : null;
                            String siteName = c_elt.hasAttribute("sitename") ? c_elt
                                    .getAttribute("sitename")
                                    : null;
                            boolean external = c_elt.hasAttribute("isexternal") ? (boolean) (Boolean
                                    .getBoolean(c_elt
                                            .getAttribute("isexternal")))
                                    : false;

                            Vector support_urls = new Vector();

                            NodeList children = c_node.getChildNodes();

                            for (int j = 0; j < children.getLength(); j++) {
                                Node c_childNode = children.item(j);
                                Element childElt = (Element) c_childNode;
                                if (childElt.getTagName().equals(NODE_MANIFEST)) {
                                    Node valueNode = c_childNode
                                            .getFirstChild();
                                    manifest = valueNode.getNodeValue();
                                } else if (childElt.getTagName().equals(
                                        NODE_SUPPORTFILE)) {
                                    support_file = childElt.hasAttribute("url") ? childElt
                                            .getAttribute("url")
                                            : null;
                                    support_urls.add(support_file);
                                }
                            }

                            String[] supportUrlArray = (String[]) support_urls
                                    .toArray(new String[0]);
                            InstallTask it = new InstallTask(null, manifest,
                                    supportUrlArray, url, fileSize, timestamp,
                                    siteName);
                            AnalysisService s = serviceManager.getService(it
                                    .getName());
                            if (s != null) {
                                it.setExistingTaskInfo(s.getTaskInfo());
                            }
                            module_list.add(it);
                        } else {
                            System.out
                                    .println("Got non-sitemodule node where sitemodule expected: "
                                            + c_elt.getTagName());
                        }
                    } else {
                        System.out
                                .println("Got non-element node where element expected "
                                        + c_node.getNodeType());
                    }
                } // for loop
            } else {
                System.out
                        .println("Got non-modulerepository node where modulerepository node expected: "
                                + element.getTagName());
            }

            break;

        case Node.ATTRIBUTE_NODE:
            System.out.println("Got attribute node where not expected: "
                    + "name= " + node.getNodeName() + ", type="
                    + node.getNodeType() + ", value=" + node.getNodeValue());
            break;

        case Node.TEXT_NODE:
            System.out.println("Got text node where not expected: " + "name= "
                    + node.getNodeName() + ", type=" + node.getNodeType()
                    + ", value=" + node.getNodeValue());
            break;

        default:
            break;
        }

        InstallTask[] task_list = (InstallTask[]) module_list
                .toArray(new InstallTask[0]);

        return task_list;
    }

    public static String NODE_MANIFEST = "manifest";

    public static String NODE_SUPPORTFILE = "support_file";

    public static String NODE_SITEMODULE = "site_module";

    public static String NODE_MODULEREPOSITORY = "module_repository";

}

