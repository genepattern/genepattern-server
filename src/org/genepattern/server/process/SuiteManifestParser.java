/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

/*
 * Created on Sep 1, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.genepattern.server.process;

import java.io.*;
import java.util.*;
import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author liefeld
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class SuiteManifestParser {



/**
 * This class is just like WebAppConfig, but it uses the JDOM (Beta 4) API
 * instead of the DOM and JAXP APIs
 **/

    /** The main method creates and demonstrates a WebAppConfig2 object */
    public static void main(String[] args)
        throws IOException, JDOMException, Exception    {
        // Create a new WebAppConfig object that represents the web.xml
        // file specified by the first command-line argument
	ZipFile zipFile = new ZipFile(args[0]);
		
	ZipEntry suiteManifestEntry = zipFile.getEntry("suiteManifest.xml");
	

    	SuiteManifestParser config = new SuiteManifestParser(zipFile.getInputStream(suiteManifestEntry));

  	HashMap map = config.getSuiteMap();

	System.out.println("Suite=\n\n" + map);
    }

    /**
     * This field holds the parsed JDOM tree.  Note that this is a JDOM
     * Document, not a DOM Document.
     **/
    protected org.jdom.Document document;  

    /**
     * Read the specified File and parse it to create a JDOM tree
     **/
    public SuiteManifestParser(File configfile) throws IOException, JDOMException {
        // JDOM can build JDOM trees from a variety of input sources.  One
        // of those input sources is a SAX parser.  
        SAXBuilder builder = new SAXBuilder();
        // Parse the specified file and convert it to a JDOM document
        document = builder.build(configfile);
    }
    
    public SuiteManifestParser(InputStream suiteConfig) throws Exception {
        // JDOM can build JDOM trees from a variety of input sources.  One
        // of those input sources is a SAX parser.  
        SAXBuilder builder = new SAXBuilder();
        // Parse the specified file and convert it to a JDOM document
        document = builder.build(suiteConfig);
    }

    /**
     * This method looks for specific Element nodes in the JDOM tree in order
     * to figure out the classname associated with the specified servlet name
     **/
    public HashMap getSuiteMap() throws JDOMException {
    	HashMap<String, Object> manifest = new HashMap<String, Object> ();
    	// Get the root element of the document.
        Element root = document.getRootElement();

        // instead of org.w3c.dom.NodeList.
        Text name = (Text)root.getChild("name").getContent().get(0);
        Text lsid = (Text)root.getChild("lsid").getContent().get(0);
        Text author = (Text)root.getChild("author").getContent().get(0);
        Text owner = (Text)root.getChild("owner").getContent().get(0);
        Text description = (Text)root.getChild("description").getContent().get(0);
        
        manifest.put("name", name.getText());
        manifest.put("lsid", lsid.getText());
        manifest.put("author", author.getText());
        manifest.put("owner", owner.getText());
        manifest.put("description", description.getText());
        
        System.out.println("name=" + name.getText());
        System.out.println("lsid=" + lsid.getText());
        System.out.println("author=" + author.getText());
        System.out.println("owner=" + owner.getText());
        System.out.println("description=" + description.getText());

        Properties modules = new Properties();
        manifest.put("modules", modules);
        for(Iterator i = root.getChildren("module").iterator(); i.hasNext(); ) {
            Element module = (Element) i.next();
            // Get the text of the <servlet-name> tag within the <servlet> tag
            Text tname = (Text)module.getChild("name").getContent().get(0);
            Text tlsid = (Text)module.getChild("lsid").getContent().get(0); 
            modules.setProperty("name", tname.getText());
            modules.setProperty("lsid", tlsid.getText());
            
            System.out.println("\tModule Name=" + tname.getText());
            System.out.println("\tModule LSID=" + tlsid.getText());
        }
        for(Iterator i = root.getChildren("documentationFile").iterator(); i.hasNext(); ) {
            Text docFile = (Text)((Element) i.next()).getContent().get(0);   
            System.out.println("\n\tDocFile=" + docFile.getText());
        }
        return manifest;
    }
    
    public String getSuiteManifestWithDocURLs(String suitebaseUrl, HashMap moduleDocURLs) throws JDOMException, IOException {
    	// Get the root element of the document.
        Element root = document.getRootElement();
        root.addContent(new Comment("URLs to docs added by Module Repository on: " + new Date()));
        for(Iterator i = root.getChildren("documentationFile").iterator(); i.hasNext(); ) {
            Text docFile = (Text)((Element) i.next()).getContent().get(0);
           
           // System.out.println("\n\tDocFile=" + docFile.getText());
            docFile.setText(suitebaseUrl + docFile.getText());
        }
       
        for(Iterator i = root.getChildren("module").iterator(); i.hasNext(); ) {
            Element module = (Element) i.next();
            // Get the text of the <servlet-name> tag within the <servlet> tag
            Text tlsid = (Text)module.getChild("lsid").getContent().get(0);
            
            String docUrl = (String)moduleDocURLs.get(tlsid.getText());
            if (docUrl != null){
            	if (docUrl.trim().length() != 0){	
            		module.addContent(new Element("docFile").addContent(docUrl));
            	}
            }
        }
        XMLOutputter outputter = new XMLOutputter();
        StringWriter writer = new StringWriter();
        outputter.output(document, writer);
        writer.close();
            
       
        return writer.getBuffer().toString();
    }
}
