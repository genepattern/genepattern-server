/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.process;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * 
 * Class for parsing XML returned from module or patch repository
 * 
 */
public class ModuleRepository {
    static class SimpleAuthenticator extends Authenticator {
        private String username, password;

        public SimpleAuthenticator(String username, String password) {
            this.username = username;
            this.password = password;
        }

        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password.toCharArray());
        }
    }

    private static final String NODE_MANIFEST = "manifest";

    private static final String NODE_MODULEREPOSITORY = "module_repository";

    private static final String NODE_PATCHREPOSITORY = "patch_repository";

    private static final String NODE_MOTD_LATESTSERVERVERSION = "motd_latestServerVersion";

    private static final String NODE_MOTD_MESSAGE = "motd_message";

    private static final String NODE_MOTD_TIMESTAMP = "motd_timestamp";

    private static final String NODE_MOTD_URGENCY = "motd_urgency";

    private static final String NODE_MOTD_URL = "motd_url";

    private static final String NODE_SITE_MOTD = "site_motd";

    private static final String NODE_SITEMODULE = "site_module";

    private static final String NODE_SUPPORTFILE = "support_file";

    private String motd_latestServerVersion = "";

    private String motd_message = "";

    private long motd_timestamp = 0;

    private int motd_urgency = 0;

    private String motd_url = "";
    
    final URL repositoryUrl;

    public ModuleRepository(final URL repositoryUrl) {
        this.repositoryUrl=repositoryUrl;
    }

    public InstallTask[] parse(final String url) throws FileNotFoundException, Exception {
        String DBF = "javax.xml.parsers.DocumentBuilderFactory";
        String oldDocumentBuilderFactory = System.getProperty(DBF);
        final URL reposURL = new URL(url);

        InputStream is = null;
        Document doc = null;

        try {
            //
            // proxy support for people behind an authenticating web proxy.
            // use it only if we find a username/password in the System
            // properties
            //
            HttpURLConnection conn = (HttpURLConnection) reposURL.openConnection();
            String user = System.getProperty("http.proxyUser");
            String pass = System.getProperty("http.proxyPassword");
            if ((user != null) && (pass != null)) {
                Authenticator.setDefault(new SimpleAuthenticator(user, pass));
            }
            conn.setDoInput(true);

            is = conn.getInputStream();

            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new InputSource(new InputStreamReader(is)));
        }
        catch (FileNotFoundException e) {
            throw e;
        }
        catch (IOException ioe) {
            throw new IOException(ioe.getMessage() + " while connecting to " + url);
        } 
        finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException ioe) {
                // ignore
            }
            if (oldDocumentBuilderFactory != null)
                System.setProperty(DBF, oldDocumentBuilderFactory);
        }

        Element root = doc.getDocumentElement();
        // System.out.println("ModuleRepository: DOM=" + dumpDOM(root, 0));

        InstallTask[] module_list = parseDOM(root);
        return (module_list);

    }

    public String getMOTD_latestServerVersion() {
        return motd_latestServerVersion;
    }

    public String getMOTD_message() {
        return motd_message;
    }

    public Date getMOTD_timestamp() {
        return new Date(motd_timestamp);
    }

    public int getMOTD_urgency() {
        return motd_urgency;
    }

    public String getMOTD_url() {
        return motd_url;
    }

    public InstallTask[] parseDOM(Node node) {
        List module_list = new ArrayList();

        switch (node.getNodeType()) {
        case Node.ELEMENT_NODE:
            Element element = (Element) node;
            String manifest = "";
            String support_file;

            if (element.getTagName().equals(NODE_MODULEREPOSITORY) || element.getTagName().equals(NODE_PATCHREPOSITORY)) {
                NodeList moduleNodes = node.getChildNodes();
                for (int i = 0; i < moduleNodes.getLength(); i++) {
                    Node c_node = moduleNodes.item(i);
                    if (c_node.getNodeType() == Node.ELEMENT_NODE) {
                        Element c_elt = (Element) c_node;

                        if (c_elt.getTagName().equals(NODE_SITEMODULE)) {

                            String taskName = c_elt.hasAttribute("name") ? c_elt.getAttribute("name") : null;
                            int fileSize = c_elt.hasAttribute("zipfilesize") ? (int) (Integer.parseInt(c_elt
                                    .getAttribute("zipfilesize"))) : 0;

                            long timestamp = c_elt.hasAttribute("timestamp") ? Integer.decode(
                                    c_elt.getAttribute("timestamp")).longValue() : 0;
                            String url = c_elt.hasAttribute("url") ? c_elt.getAttribute("url") : null;
                            String siteName = c_elt.hasAttribute("sitename") ? c_elt.getAttribute("sitename") : null;
                            boolean external = c_elt.hasAttribute("isexternal") ? (boolean) (Boolean.getBoolean(c_elt
                                    .getAttribute("isexternal"))) : false;

                            boolean deprecated = c_elt.hasAttribute("deprecated") ? (boolean) (Boolean.getBoolean(c_elt
                                    .getAttribute("deprecated"))) : false;

                            Vector support_urls = new Vector();

                            NodeList children = c_node.getChildNodes();

                            for (int j = 0; j < children.getLength(); j++) {
                                Node c_childNode = children.item(j);
                                if (c_childNode.getNodeType() == Node.ELEMENT_NODE) {
                                    Element childElt = (Element) c_childNode;
                                    if (childElt.getTagName().equals(NODE_MANIFEST)) {
                                        Node valueNode = c_childNode.getFirstChild();
                                        manifest = valueNode.getNodeValue();
                                    } else if (childElt.getTagName().equals(NODE_SUPPORTFILE)) {
                                        support_file = childElt.hasAttribute("url") ? childElt.getAttribute("url")
                                                : null;
                                        support_urls.add(support_file);
                                    }
                                }
                            }

                            String[] supportUrlArray = (String[]) support_urls.toArray(new String[0]);
                            try {
                                InstallTask it = new InstallTask(null, manifest, supportUrlArray, url, fileSize,
                                        timestamp, siteName, deprecated);
                                it.setReposUrl(repositoryUrl);
                                module_list.add(it);
                            } catch (Throwable t) {
                                System.err.println(t.getMessage());
                                t.printStackTrace();
                            }

                        } else if (c_elt.getTagName().equals(NODE_SITE_MOTD)) {
                            // System.out.println("got " + c_elt.getTagName() +
                            // ": " + dumpDOM(c_node, 0));
                            NodeList motdNodes = c_node.getChildNodes();
                            for (int j = 0; j < motdNodes.getLength(); j++) {
                                c_node = motdNodes.item(j);
                                if (c_node.getNodeType() == Node.ELEMENT_NODE) {
                                    c_elt = (Element) c_node;
                                    if (c_elt.getTagName().equals(NODE_MOTD_MESSAGE)) {
                                        motd_message = c_node.getChildNodes().item(0).getNodeValue();
                                    } else if (c_elt.getTagName().equals(NODE_MOTD_URL)) {
                                        motd_url = c_node.getChildNodes().item(0).getNodeValue();
                                    } else if (c_elt.getTagName().equals(NODE_MOTD_URGENCY)) {
                                        try {
                                            motd_urgency = Integer.parseInt(c_node.getChildNodes().item(0)
                                                    .getNodeValue());
                                        } catch (NumberFormatException nfe) {
                                            // ignore
                                            System.out.println(nfe.getMessage()
                                                    + " in ModuleRepository.getModules() handling MOTD urgency");
                                        }
                                    } else if (c_elt.getTagName().equals(NODE_MOTD_TIMESTAMP)) {
                                        try {
                                            motd_timestamp = Long.parseLong(c_node.getChildNodes().item(0)
                                                    .getNodeValue());
                                        } catch (NumberFormatException nfe) {
                                            try {
                                                motd_timestamp = new SimpleDateFormat("dd-MMM-yyyy").parse(
                                                        c_node.getChildNodes().item(0).getNodeValue()).getTime();
                                            } catch (ParseException pe) {
                                                // ignore
                                                System.out.println(pe.getMessage()
                                                        + " in ModuleRepository.getModules() handling MOTD timestamp");
                                            }
                                        }
                                    } else if (c_elt.getTagName().equals(NODE_MOTD_LATESTSERVERVERSION)) {
                                        motd_latestServerVersion = c_node.getChildNodes().item(0).getNodeValue();
                                    } else {
                                        System.out.println("Got non-MOTD node where motd expected: "
                                                + c_elt.getTagName());
                                    }
                                }
                            }
                        } else {
                            System.out.println("Got non-sitemodule node where sitemodule expected: "
                                    + c_elt.getTagName());
                        }
                    } else {
                        // System.out.println("Got non-element node where
                        // element expected " + c_node.getNodeType());
                    }
                } // for loop
            } else {
                System.out.println("Got non-modulerepository node where modulerepository node expected: "
                        + element.getTagName());
            }

            break;

        case Node.ATTRIBUTE_NODE:
            System.out.println("Got attribute node where not expected: " + "name= " + node.getNodeName() + ", type="
                    + node.getNodeType() + ", value=" + node.getNodeValue());
            break;

        case Node.TEXT_NODE:
            System.out.println("Got text node where not expected: " + "name= " + node.getNodeName() + ", type="
                    + node.getNodeType() + ", value=" + node.getNodeValue());
            break;

        default:
            break;
        }

        InstallTask[] task_list = (InstallTask[]) module_list.toArray(new InstallTask[0]);

        return task_list;
    }

    public static String dumpDOM(Node node, int indent) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            // most likely a comment or text node
            System.out.println("name= " + node.getNodeName() + ", type=" + node.getNodeType() + ", value="
                    + node.getNodeValue());
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
                outputWriter.write("Attribute: " + ((Attr) attributes.item(i)).getName() + "="
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

    public static void main(String[] args) { 
        try {
            String url = "http://www.broadinstitute.org/cgi-bin/cancer/software/genepattern/gp_module_repository.cgi";
            ModuleRepository mr = new ModuleRepository(new URL(url));
            mr.parse(url);
        } 
        catch (Exception e) {
            e.printStackTrace();
        }

        // System.out.println(dumpDOM(root, 0));
    }
}
