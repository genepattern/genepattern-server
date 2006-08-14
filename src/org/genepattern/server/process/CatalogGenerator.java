/*
 * Created on Sep 2, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.genepattern.server.process;

import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.WebServiceException;
import org.genepattern.util.StringUtils;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.local.*;
import org.genepattern.util.GPConstants;
import org.genepattern.util.IGPConstants;
import org.genepattern.util.LSID;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.FileOutputStream;

import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.HashMap;
/**
 * @author liefeld
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class CatalogGenerator {

	HashMap moduleDocMap = new HashMap();
	Properties reposProps;
	String userID = "";
	
	public CatalogGenerator(String userID){
		this.userID = userID;
	}
	
	public String generateSuiteCatalog(String env) throws IOException {
		StringWriter strwriter = new StringWriter(); // for now just write to the string
		BufferedWriter buff = new BufferedWriter(strwriter);
		
		
		// get the map of module doc files to pass in to the suite
		// so it can link to the module docs as well
		String envDir = reposProps.getProperty(env+".gp_module_repos_dir");
		File catDir = new File(envDir);
		File[] moduleDirs = catDir.listFiles(new DirFilter()); 
		for (int i=0; i < moduleDirs.length; i++){
			getModuleVersionsDocURLs(env, moduleDirs[i]);
		} 
		
		
		buff.write("<?xml version=\"1.0\"?><!DOCTYPE suite_repository><suite_repository >");
		buff.write(getMOTDxml());
		
		envDir = reposProps.getProperty(env+".gp_suite_repos_dir");
			
		catDir = new File(envDir);
		File[] suiteDirs = catDir.listFiles(new DirFilter()); 
		
		for (int i=0; i < suiteDirs.length; i++){
			generateSuiteVersionsXML(env, suiteDirs[i], buff);
		} 
		
		buff.write("</suite_repository>");
		buff.flush();
		buff.close();
		return strwriter.getBuffer().toString();
		
	}
	
	public String generateModuleCatalog() throws IOException, WebServiceException, Exception {
		StringWriter strwriter = new StringWriter(); // for now just write to the string
		BufferedWriter buff = new BufferedWriter(strwriter);
		
		
		buff.write("<?xml version=\"1.0\"?><!DOCTYPE module_repository><module_repository >");
		buff.write(getMOTDxml());
		
			
		//File catDir = new File(envDir);
		//File[] moduleDirs = catDir.listFiles(new DirFilter()); 
		
		//for (int i=0; i < moduleDirs.length; i++){
		//	generateModuleVersionsXML(env, moduleDirs[i], buff);
		//} 
//==============================		
		Collection tmTasks = new LocalAdminClient(userID).getTaskCatalog();
		TaskInfo taskInfo = null;
	
		for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext(); ) {
			
			taskInfo = (TaskInfo)itTasks.next();
				
			writeModuleXML(taskInfo, buff);
			
			
		}



//====================================
		buff.write("</module_repository>");
		buff.flush();
		buff.close();
		return strwriter.getBuffer().toString();
	}
	
	protected String getMOTDxml(){
			

	// XXX add link to this server here

		return "<site_motd><motd_message>GenePattern MODULE REPOSITORY</motd_message>\n" +
			"<motd_url>http://www.broad.mit.edu/cancer/software/genepattern/doc/relnotes/current</motd_url>\" \n"+
			"<motd_urgency>0</motd_urgency>\n" +
			"<motd_timestamp>today</motd_timestamp>\n" +
			"<motd_latestServerVersion>2.0.2</motd_latestServerVersion></site_motd>\n";
		
	}
	
	/**
	 * expect dir structure of the form
	 * 	   modules/NMF/broad.mit.edu:cancer.software.genepattern.module.analysis/0 
	 * where we enter this method with 'NMF' as the dir and must drop down the
	 * two levels to get to the real stuff
	 */
	public void generateSuiteVersionsXML(String env, File topSuiteDir, BufferedWriter buff)throws IOException{
		File[] lsidDirs = topSuiteDir.listFiles(new DirFilter()); 
		ArrayList subdirs = new ArrayList();
		subdirs.add(topSuiteDir);
		for (int i=0; i < lsidDirs.length; i++){
			subdirs.add(lsidDirs[i]);
			File[] versionDirs = lsidDirs[i].listFiles(new DirFilter()); 
			for (int j=0; j < versionDirs.length; j++){
				subdirs.add(versionDirs[j]);
				File manifest = new File(versionDirs[j], "suiteManifest.xml");
				if (manifest.exists()){
					writeSuiteXML(env, versionDirs[j], buff, subdirs);
				} else {
					File[] subVersionDirs = versionDirs[j].listFiles(new DirFilter()); 
					for (int k=0; k< subVersionDirs.length; k++){
						File submanifest = new File(subVersionDirs[k], "suiteManifest.xml");
						if (submanifest.exists()){
							subdirs.add(subVersionDirs[k]);
							writeSuiteXML(env, subVersionDirs[k], buff, subdirs);
							subdirs.remove(subVersionDirs[k]);
							}
					}
				}
				subdirs.remove(versionDirs[j]);
			}
			subdirs.remove(lsidDirs[i]);
		}
	}
	
	
	
	
	public void getModuleVersionsDocURLs(String env, File topModDir)throws IOException{
		File[] lsidDirs = topModDir.listFiles(new DirFilter()); 
		ArrayList subdirs = new ArrayList();
		subdirs.add(topModDir);
		for (int i=0; i < lsidDirs.length; i++){
			subdirs.add(lsidDirs[i]);
			File[] versionDirs = lsidDirs[i].listFiles(new DirFilter()); 
			for (int j=0; j < versionDirs.length; j++){
				subdirs.add(versionDirs[j]);
				File manifest = new File(versionDirs[j], "manifest");
				if (manifest.exists()){
					getModuleDocURL(env, versionDirs[j], subdirs);
				} else {
					File[] subVersionDirs = versionDirs[j].listFiles(new DirFilter()); 
					for (int k=0; k< subVersionDirs.length; k++){
						File submanifest = new File(subVersionDirs[k], "manifest");
						if (submanifest.exists()){
							subdirs.add(subVersionDirs[k]);
							getModuleDocURL(env, subVersionDirs[k], subdirs);
							subdirs.remove(subVersionDirs[k]);
							}
					}
				}
				subdirs.remove(versionDirs[j]);
			}
			subdirs.remove(lsidDirs[i]);
		}
	}
	
	
	// simply copy the suiteManifest.xml onto the stream unchanged
	public void writeSuiteXML(String env, File dir, BufferedWriter buff, ArrayList subdirs) throws IOException{
		try {
			String urlBase = reposProps.getProperty(env+".gp_suite_repos_url");
			for (int i=0; i < subdirs.size(); i++){
				urlBase = urlBase + "/" + ((File)subdirs.get(i)).getName(); 
			}
			urlBase += "/"; // end with a trailing slash
			
			SuiteManifestParser suiteManifest = new SuiteManifestParser(new File(dir, "suiteManifest.xml"));
			String suiteManifestString = suiteManifest.getSuiteManifestWithDocURLs(urlBase, moduleDocMap);
			
			// strip of the <?xml version=...?>
			int idx = suiteManifestString.indexOf("?>");
			
			buff.write(suiteManifestString.substring(idx+2));
		} catch (org.jdom.JDOMException jde){
			throw new IOException(jde.getMessage());
		}
		
	}
	/**
	 * for a particular module (directory) write the catalog XML to the writer
	 * e.g.
	 * 
	 * <site_module name="ClassNeighbors" 
	 * 		zipfilesize="642025" 
	 * 		timestamp="1103748609" 
	 * 		url="ftp://ftp.broad.mit.edu/pub/genepattern/modules/ClassNeighbors/broad.mit.edu:cancer.software.genepattern.module.analysis/1/ClassNeighbors.zip" 
	 * 		sitename="ftp://ftp.broad.mit.edu/pub/genepattern/modules" 
	 * 		isexternal="false">
	 * <support_file url="ftp://ftp.broad.mit.edu/pub/genepattern/modules/ClassNeighbors/broad.mit.edu:cancer.software.genepattern.module.analysis/1/ClassNeighbors.pdf">ClassNeighbors.pdf</support_file >
	 * <manifest>#Fri Nov 19 10:24:28 EST 2004
	 * &#xA;p12_type=java.lang.Integer
	 * &#xA;p8_prefix_when_specified=
	 * &#xA;p5_optional=
	 * 
	 * <support_file url="ftp://ftp.broad.mit.edu/pub/genepattern/modules/ClassNeighbors/broad.mit.edu:cancer.software.genepattern.module.analysis/1/trove.jar">trove.jar</support_file >
	 *  </site_module >
	 * @param dir
	 * @param buff
	 * @throws IOException
	 */
	public void writeModuleXML(TaskInfo taskInfo, BufferedWriter buff) throws Exception{
		// load the manifest to get the name 
		Map tia = taskInfo.getTaskInfoAttributes();
		ZipTask zt = new ZipTask();
		String lsid = (String)tia.get(GPConstants.LSID);
		String taskDir = DirectoryManager.getTaskLibDir((String) tia.get(IGPConstants.LSID));

		File dir = new File(taskDir);
		File manifestFile = new File(dir.getAbsolutePath(), "manifest");
		if (!manifestFile.exists()) return;
		
        //File zipFile = zt.packageTask(taskInfo, userID);
	
		buff.write("<site_module name=\"");
		buff.write(taskInfo.getName());
		buff.write("\" zipfilesize=\"");
		buff.write("-1"); //XXX
		buff.write("\" timestamp=\"");
		buff.write(""+(dir.lastModified()/1000)); //XXX
		buff.write("\" url=\"");
		buff.write(getZipDownloadURLBase()+"makeZip.jsp?name="+ lsid); 
		
		buff.write("\" sitename=\"");
		buff.write(getZipDownloadURLBase()); 
		buff.write("\" isexternal=\"");
		buff.write("false"); //XXX
		
		buff.write("\" deprecated=\"");
		buff.write("false"); //XXX
		
		
		buff.write("\" >");
		buff.newLine();
		

		buff.write("<manifest>");
		buff.newLine();
		this.readFile(dir.getAbsolutePath(), "manifest", buff, true, true);
		buff.newLine();
		buff.write("</manifest>");
		buff.newLine();
		File[] supportFiles = dir.listFiles(new SupportFileFilter()); 
		for (int i=0; i < supportFiles.length; i++){
			buff.write("<support_file url=\"");
			buff.write(getZipDownloadURLBase()+"getTaskDoc.jsp?name=" + lsid + "&amp;file=" + supportFiles[i].getName());
			
			buff.write("/");
			buff.write(supportFiles[i].getName());
			buff.write("\"> ");
		
			buff.write(supportFiles[i].getName());
			buff.write("</support_file>");
			buff.newLine();
			
			// store the path to the module doc files for suites to use
			// when generating their catalogs
		//	if (supportFiles[i].getName().endsWith(".pdf")){
		//		moduleDocMap.put(manifestProps.getProperty("LSID"), urlBase + "/" + supportFiles[i].getName());
		//	}
		}
		
		buff.write("</site_module >");
	}

	public void getModuleDocURL(String env, File dir, ArrayList subdirs) throws IOException{
		// load the manifest to get the name 
		Properties manifestProps = new Properties();
		FileInputStream fio = new FileInputStream(new File(dir, "manifest"));	
		manifestProps.load(fio);
		fio.close();
		
		String urlBase = reposProps.getProperty(env+".gp_module_repos_url");
		for (int i=0; i < subdirs.size(); i++){
			urlBase = urlBase + "/" + ((File)subdirs.get(i)).getName(); 
		}
		
		File[] supportFiles = dir.listFiles(new SupportFileFilter()); 
		for (int i=0; i < supportFiles.length; i++){	
			// store the path to the module doc files for suites to use
			// when generating their catalogs
			if (supportFiles[i].getName().endsWith(".pdf")){
				moduleDocMap.put(manifestProps.getProperty("LSID"), urlBase + "/" + supportFiles[i].getName());
			}
		}
	}

	String fqHostName = null;
	public String getZipDownloadURLBase() throws IOException{
		
		if (fqHostName == null){
			String port = System.getProperty("GENEPATTERN_PORT");
			fqHostName = System.getProperty("fullyQualifiedHostName");
			if (fqHostName == null) fqHostName = InetAddress.getLocalHost().getCanonicalHostName();
			if (fqHostName.equals("localhost")) fqHostName = "127.0.0.1";
			fqHostName = fqHostName + ":" + port + "/gp/";;
		}
		if (!fqHostName.startsWith("http://")) fqHostName = "http://" + fqHostName;
		return fqHostName;
	}
	

	public static StringBuffer readFile(String dirName, String fileName, boolean includeNewlines) throws IOException{
		StringWriter writer = new StringWriter();
		readFile(dirName, fileName, new BufferedWriter(writer), false, includeNewlines);
		return writer.getBuffer();
	}
	
	public static void readFile(String dirName, String fileName, BufferedWriter writer, boolean htmlEncode, boolean includeNewlines) throws IOException{
		BufferedReader in = new BufferedReader(new FileReader(new File(dirName, fileName)));
		String str;
        while ((str = in.readLine()) != null) {
        	if (htmlEncode) str = StringUtils.htmlEncode(str);
            writer.write(str);
            if (includeNewlines) writer.newLine();
        }

        writer.flush();
        in.close();
 	}

	public static void copyFile(File origFile, File newFile) throws IOException {
		FileInputStream inStream=new java.io.FileInputStream(origFile);
		FileOutputStream outStream=new java.io.FileOutputStream(newFile);
		boolean success = true;
		byte[] buf = new byte[1024];
		int len = 0;
		try {
			while ((len = inStream.read(buf)) != -1){
				outStream.write(buf,0,len);
				outStream.flush();
			}
			inStream.close();
		
		} finally {
			outStream.close();
		}
	}
	
	


}


class DirFilter implements FileFilter {
	public DirFilter(){}
	public boolean accept(File pathname){
		return pathname.isDirectory();
}
}

class SupportFileFilter implements FileFilter {
	public SupportFileFilter(){}
	public boolean accept(File pathname){
		String name = pathname.getName();
		if (pathname.isDirectory()) return false;
		else if ("manifest".equalsIgnoreCase(name)) return false;
		else if (!name.endsWith(".zip")) return true;
		else {
			// its a zip file, ignore it if it contains a manifest
			try {
				ZipFile zfile = new ZipFile(pathname);
				ZipEntry manifest = zfile.getEntry("manifest");
				return manifest == null;
			} catch (Exception e){
				return true;
			}
		}
	}
}

class TaskZipFilter implements FileFilter {
	public TaskZipFilter(){}
	public boolean accept(File pathname){
		String name = pathname.getName();
	
		// its a zip file, ignore it if it contains a manifest
		try {
			ZipFile zfile = new ZipFile(pathname);
			ZipEntry manifest = zfile.getEntry("manifest");
			return manifest != null;
		} catch (Exception e){
			return false;
		}
	}
}
	