/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.server.process;

import java.io.*;
import java.util.*;


import org.genepattern.server.webservice.server.local.IAdminClient;
import org.genepattern.server.webservice.server.local.LocalAdminClient;

import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.SuiteInfo;


public class SuiteInfoManifestXMLGenerator{
	static String userId = "";

	
	public static void generateXMLFile(SuiteInfo suite, BufferedWriter bout){
		try {
			//BufferedWriter bout = new BufferedWriter(new FileWriter(suiteFile));

			bout.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			bout.newLine();
			bout.write("<GenePatternSuite xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
			bout.newLine();
			bout.write("\t xsi:noNamespaceSchemaLocation=\"suite.xsd\" quality=\"String\">");
			bout.newLine();

			bout.write("<!-- Generated on ");
			bout.write("" + (new Date()));
			bout.write("-->");
			bout.newLine();
			bout.newLine();

			writeSuiteData(bout, suite);
			bout.newLine();

			writeSuiteDocs(bout, suite);
			bout.newLine();

			writeSuitePrerequisites(bout, suite);
			bout.newLine();

			writeSuiteModules(bout, suite);
			bout.newLine();


			bout.write("</GenePatternSuite>");
			bout.flush();
			//bout.close();
		} catch (IOException ioe){
			ioe.printStackTrace();
		}
	}


	protected static void writeSuiteData(BufferedWriter bout, SuiteInfo suite) throws IOException{
		bout.write("<lsid>");
		bout.write(suite.getLSID());
		bout.write("</lsid>");
		bout.newLine();

		bout.write("<name>");
		bout.write(suite.getName());
		bout.write("</name>");
		bout.newLine();

		String author = suite.getAuthor();
		if (author== null) {
			author= "anonymous";
		} else if (author.trim().length() == 0) {
			author= "anonymous";
		}

		bout.write("<author>");
		bout.write(author);
		bout.write("</author>");
		bout.newLine();

		bout.write("<owner>");
		String owner = suite.getOwner();
		if (owner == null) {
			owner = author;
		} else if (owner.trim().length() == 0) {
			owner = author;
		}
		bout.write(owner);
		bout.write("</owner>");
		bout.newLine();

		String description = suite.getDescription();
		if (description== null) {
			description= "no description";
		} else if (description.trim().length() == 0) {
			description= "no description";
		}

		bout.write("<description>");
		bout.write(description);
		bout.write("</description>");
		bout.newLine();

		//bout.write("<versionComment>");
		//bout.write(sv.getVersionComment());
		//bout.write("</versionComment>");
		//bout.newLine();
		bout.flush();
	}

	protected static void writeSuiteDocs(BufferedWriter bout, SuiteInfo sv) throws IOException{
		String[] docFileNames = sv.getDocumentationFiles();
		for (int i=0; i < docFileNames.length; i++){
			bout.write("<documentationFile>");
			bout.write(docFileNames[i]);
			bout.write("</documentationFile>");
			bout.newLine();
		}
	}

	protected static void writeSuitePrerequisites(BufferedWriter bout, SuiteInfo sv) throws IOException{
		//ArrayList prereqs = sv.getPrerequisites();
		//for (Iterator iter = prereqs.iterator(); iter.hasNext(); ){
	//		bout.write("<prerequisite_lsid>");
	//		bout.write((String)iter.next());
	//		bout.write("</prerequisite_lsid>");
	//		bout.newLine();
	//	}

	}


	protected static void writeSuiteModules(BufferedWriter bout, SuiteInfo sv) throws IOException{
		String[] modules = sv.getModuleLSIDs();
		IAdminClient adminClient = new LocalAdminClient(userId);


		for (int i=0; i < modules.length; i++ ){
			bout.newLine();
			bout.write("<module>");
			bout.newLine();
			String lsid = modules[i];
			TaskInfo ti = null;
			try {
				ti = adminClient.getTask(lsid);
			} catch (Exception e){
				e.printStackTrace();
			}		

			bout.write("<lsid>");
			bout.write(lsid);
			bout.write("</lsid>");
			bout.newLine();
			if (ti != null) {
				bout.write("<name>");
				bout.write(ti.getName());
				bout.write("</name>");
				bout.newLine();
			} 
			bout.write("</module>");
			bout.newLine();
		}


	}



}
