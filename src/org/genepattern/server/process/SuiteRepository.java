/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.process;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.genepattern.util.GPConstants;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;

public class SuiteRepository {
    protected org.jdom.Document document;

    protected static String DBF = "javax.xml.parsers.DocumentBuilderFactory";

    HashMap<String, Map> suites = new HashMap<String, Map>();

    protected String motd_message = "";

    protected String motd_url = "";

    protected int motd_urgency = 0;

    protected long motd_timestamp = 0;

    protected String motd_latestServerVersion = "";

    public SuiteRepository() {

    }

    public static void main(String[] args) {
        String url = "http://lead:7070/ModuleRepository/suite?env=dev";

        SuiteRepository sr = new SuiteRepository();

        try {
            sr.getSuites(url);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // System.out.println(dumpDOM(root, 0));
    }

    // returns a map keyed by LSID
    public HashMap<String, Map> getSuites(String url) throws IOException,
            IllegalArgumentException, IllegalAccessException,
            NoSuchMethodException, SecurityException {
        String oldDocumentBuilderFactory = System.getProperty(DBF);
        URL reposURL = new URL(url);
        InputStream is = null;
        Document doc = null;

        try {
            //
            // proxy support for people behind an authenticating web proxy.
            // use it only if we find a username/password in the System
            // properties
            //
            HttpURLConnection conn = (HttpURLConnection) reposURL
                    .openConnection();
            String user = System.getProperty("http.proxyUser");
            String pass = System.getProperty("http.proxyPassword");
            if ((user != null) && (pass != null)) {
                Authenticator.setDefault(new SimpleAuthenticator(user, pass));
            }
            conn.setDoInput(true);
            is = conn.getInputStream();

            SAXBuilder builder = new SAXBuilder();
            // Parse the specified file and convert it to a JDOM document
            document = builder.build(is);
            Element root = document.getRootElement();
            getMessageOfTheDay(root);
            for (Iterator i = root.getChildren("GenePatternSuite").iterator(); i
                    .hasNext();) {
                Element suite = (Element) i.next();
                Map suiteMap = getSuiteMap(suite);
                suites.put((String) suiteMap.get("lsid"), suiteMap);
            }

        } catch (IOException ioe) {
            throw new IOException(ioe.getMessage() + " while connecting to "
                    + url);
        } catch (JDOMException ioe) {
            throw new IOException(ioe.getMessage() + " while parsing from "
                    + url);
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException ioe) {
                // ignore
            }
            if (oldDocumentBuilderFactory != null)
                System.setProperty(DBF, oldDocumentBuilderFactory);
        }

        // InstallTask[] module_list = parseDOM(root);

        return suites;
    }

    public static HashMap getSuiteMap(ZipFile zipFile) throws Exception {

        ZipEntry suiteManifestEntry = zipFile
                .getEntry(GPConstants.SUITE_MANIFEST_FILENAME);
        InputStream is = zipFile.getInputStream(suiteManifestEntry);

        SAXBuilder builder = new SAXBuilder();
        // Parse the specified file and convert it to a JDOM document
        org.jdom.Document doc = builder.build(is);
        Element root = doc.getRootElement();
        return getSuiteMap(root);

    }

    public void getMessageOfTheDay(Element root) throws JDOMException {
        Element site_motd = root.getChild(NODE_SITE_MOTD);
        motd_message = ((Text) site_motd.getChild(NODE_MOTD_MESSAGE)
                .getContent().get(0)).getText();
        motd_url = ((Text) site_motd.getChild(NODE_MOTD_URL).getContent()
                .get(0)).getText();
        motd_urgency = Integer.parseInt(((Text) site_motd.getChild(
                NODE_MOTD_URGENCY).getContent().get(0)).getText());
        motd_latestServerVersion = ((Text) site_motd.getChild(
                NODE_MOTD_LATESTSERVERVERSION).getContent().get(0)).getText();

        String timestamp = ((Text) site_motd.getChild(NODE_MOTD_TIMESTAMP)
                .getContent().get(0)).getText();
        try {
            motd_timestamp = Long.parseLong(timestamp);
        } catch (NumberFormatException nfe) {
            try {
                motd_timestamp = (new SimpleDateFormat("dd-MMM-yyyy")
                        .parse(timestamp)).getTime();
            } catch (ParseException pe) {
                // ignore
                System.out
                        .println(pe.getMessage()
                                + " in ModuleRepository.getModules() handling MOTD timestamp");
            }
        }

    }

    public static HashMap getSuiteMap(Element root) throws JDOMException {
        HashMap manifest = new HashMap();
        // Get the root element of the document.
        // Element root = document.getRootElement();

        // instead of org.w3c.dom.NodeList.
        Text name = (Text) root.getChild("name").getContent().get(0);
        Text lsid = (Text) root.getChild("lsid").getContent().get(0);
        Text author = (Text) root.getChild("author").getContent().get(0);
        Text owner = (Text) root.getChild("owner").getContent().get(0);
        Text description = (Text) root.getChild("description").getContent()
                .get(0);

        manifest.put("name", name.getText());
        manifest.put("lsid", lsid.getText());
        manifest.put("author", author.getText());
        manifest.put("owner", owner.getText());
        manifest.put("description", description.getText());

        ArrayList modules = new ArrayList();
        manifest.put("modules", modules);
        for (Iterator i = root.getChildren("module").iterator(); i.hasNext();) {
            HashMap moduleMap = new HashMap();
            Element module = (Element) i.next();

            if (module.getChild("name") != null) {
                Text tname = (Text) module.getChild("name").getContent().get(0);
                moduleMap.put("name", tname.getText());
            }
            if (module.getChild("lsid") != null) {
                Text tlsid = (Text) module.getChild("lsid").getContent().get(0);
                moduleMap.put("lsid", tlsid.getText());
            }

            Element edoc = module.getChild("docFile");
            if (edoc != null) {
                Text tdoc = (Text) edoc.getContent().get(0);

                moduleMap.put("docFile", tdoc.getText());
            }
            modules.add(moduleMap);
        }

        ArrayList docFiles = new ArrayList();
        manifest.put("docFiles", docFiles);

        for (Iterator i = root.getChildren("documentationFile").iterator(); i
                .hasNext();) {
            Text docFile = (Text) ((Element) i.next()).getContent().get(0);
            docFiles.add(docFile.getText());
        }

        return manifest;
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

    public static String NODE_MANIFEST = "suiteManifest.xml";

    public static String NODE_SUPPORTFILE = "support_file";

    public static String NODE_SITEMODULE = "site_module";

    public static String NODE_MODULEREPOSITORY = "module_repository";

    public static String NODE_SITE_MOTD = "site_motd";

    public static String NODE_MOTD_MESSAGE = "motd_message";

    public static String NODE_MOTD_URL = "motd_url";

    public static String NODE_MOTD_URGENCY = "motd_urgency";

    public static String NODE_MOTD_TIMESTAMP = "motd_timestamp";

    public static String NODE_MOTD_LATESTSERVERVERSION = "motd_latestServerVersion";

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
}
