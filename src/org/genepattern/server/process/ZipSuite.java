package org.genepattern.server.process;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.*;
import java.util.*;


import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.util.KeySortedProperties;

public class ZipSuite extends CommandLineAction {
	public static final String suiteManifestFileName = "suiteManifest.xml";

	public ZipSuite() {
	}

	public void zipFiles(ZipOutputStream zos, File dir) throws Exception {
		File[] fileList = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return !name.endsWith(".old") && !name.endsWith(".bak");
			}
		});

		for (int i = 0; i < fileList.length; i++) {
			zipFile(zos, fileList[i]);
		}

	}

	public void zipFile(ZipOutputStream zos, File f) throws Exception {
		zipFile(zos, f, null);
	}

	public void zipFile(ZipOutputStream zos, File f, String comment)
			throws Exception {
		try {
			if (f.isDirectory()) return;

			ZipEntry zipEntry = null;
			FileInputStream is = null;
			String value = null;
			String attachmentName = null;

			byte[] buf = new byte[100000];
			zipEntry = new ZipEntry(f.getName());
			zipEntry.setTime(f.lastModified());
			zipEntry.setSize(f.length());
			if (comment != null)
				zipEntry.setComment(comment);
			zos.putNextEntry(zipEntry);
			long fileLength = f.length();
			long numRead = 0;
			is = new FileInputStream(f);
			int n;
			while ((n = is.read(buf, 0, buf.length)) > 0) {
				zos.write(buf, 0, n);
				numRead += n;
			}
			is.close();
			if (numRead != fileLength) {
				throw new Exception("only read " + numRead + " of "
						+ fileLength + " bytes in " + f.getPath());
			}
			zos.closeEntry();
			//System.out.println("zipSuiteFile: zipped " + f.getAbsolutePath() +
			// ", length=" + numRead);
		} catch (Exception t) {
			t.printStackTrace();
			throw t;
		}
	}

	public ZipEntry zipSuiteManifest(ZipOutputStream zos,	SuiteInfo suite) throws Exception {
		// insert manifest
		//	System.out.println("creating manifest");		

		ZipEntry zipEntry = new ZipEntry(GPConstants.SUITE_MANIFEST_FILENAME);
		zos.putNextEntry(zipEntry);

		OutputStreamWriter osw = new OutputStreamWriter(zos);
		BufferedWriter bout = new BufferedWriter(osw);
		
		SuiteInfoXMLGenerator.generateXMLFile(suite, bout);
		
		zos.closeEntry();
		return zipEntry;
	}


	


	public File packageSuite(String name, String userID) throws Exception {

		if (name == null || name.length() == 0) {
			throw new Exception(
					"Must specify task name as name argument to this page");
		}

		SuiteInfo suite = null;
		
		LocalAdminClient adminClient = new LocalAdminClient(userID);
		SuiteInfoXMLGenerator.userId = userID;

		suite = adminClient.getSuite(name);
		return packageSuite(suite, userID);
	}

	public File packageSuite(SuiteInfo suiteInfo, String userID) throws Exception {
		String name = suiteInfo.getName();

		// use an LSID-unique name so that different versions of same named task
		// don't collide within zip file
		String suffix = "_"
				+ Integer.toString(Math.abs(suiteInfo.getLSID()
						.hashCode()), 36); // [a-z,0-9]

		// create zip file
		File zipFile = File.createTempFile(name + suffix, ".zip");
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));

		// add the manifest
		zipSuiteManifest(zos, suiteInfo);

		// insert attachments
		// find $OMNIGENE_ANALYSIS_ENGINE/taskLib/<taskName> to locate DLLs,
		// other support files

		File dir = new File(DirectoryManager.getSuiteLibDir(name, suiteInfo.getLSID(), userID));
		zipFiles(zos, dir);
		zos.finish();
		zos.close();

		return zipFile;
	}

	
	
	
}


class SuiteInfoXMLGenerator{
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
		LocalAdminClient adminClient = new LocalAdminClient(userId);


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