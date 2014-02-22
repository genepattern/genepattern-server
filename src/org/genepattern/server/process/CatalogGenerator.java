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

/*
 * Created on Sep 2, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.genepattern.server.process;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringBufferInputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.local.IAdminClient;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.StringUtils;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * @author liefeld
 * 
 * TODO To change the template for this generated type comment go to Window - Preferences - Java - Code Style - Code
 * Templates
 */
public class CatalogGenerator {

    HashMap moduleDocMap = new HashMap();

    Properties reposProps;

    String userID = "";

    public CatalogGenerator(String userID) {
        this.userID = userID;
    }

    public String generateSuiteCatalog() throws IOException, WebServiceException, Exception {
        StringWriter strwriter = new StringWriter(); // for now just write to
        // the string
        BufferedWriter buff = new BufferedWriter(strwriter);
        IAdminClient adminClient = new LocalAdminClient(userID);

        // get the map of module doc files to pass in to the suite
        // so it can link to the module docs as well
        Collection tmTasks = adminClient.getTaskCatalog();

        for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext();) {
            TaskInfo taskInfo = (TaskInfo) itTasks.next();
            getModuleVersionsDocURLs(taskInfo);
        }
        SuiteInfo[] allSuites = adminClient.getAllSuites();

        buff.write("<?xml version=\"1.0\"?><!DOCTYPE suite_repository><suite_repository >");
        buff.write(getMOTDxml());

        // envDir = reposProps.getProperty(env+".gp_suite_repos_dir");

        for (int i = 0; i < allSuites.length; i++) {
            writeSuiteXML(allSuites[i], buff);
        }

        buff.write("</suite_repository>");
        buff.flush();
        buff.close();
        return strwriter.getBuffer().toString();

    }

    public String generateModuleCatalog() throws IOException, WebServiceException, Exception {
        StringWriter strwriter = new StringWriter(); // for now just write to
        // the string
        BufferedWriter buff = new BufferedWriter(strwriter);

        buff.write("<?xml version=\"1.0\"?><!DOCTYPE module_repository><module_repository >");
        buff.write(getMOTDxml());

        Collection tmTasks = new LocalAdminClient(userID).getTaskCatalog();
        TaskInfo taskInfo = null;

        for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext();) {
            taskInfo = (TaskInfo) itTasks.next();
            writeModuleXML(taskInfo, buff);
        }

        buff.write("</module_repository>");
        buff.flush();
        buff.close();
        return strwriter.getBuffer().toString();
    }

    protected String getMOTDxml() throws IOException {

        // XXX add link to this server here

        return "<site_motd><motd_message>GenePattern Server Modules</motd_message>\n" + "<motd_url>"
                + getZipDownloadURLBase() + "</motd_url>\" \n" + "<motd_urgency>0</motd_urgency>\n"
                + "<motd_timestamp>" + System.currentTimeMillis() + "</motd_timestamp>\n"
                + "<motd_latestServerVersion>" 
                + ServerConfigurationFactory.instance().getGenePatternVersion()
                + "</motd_latestServerVersion></site_motd>\n";

    }

    public void getModuleVersionsDocURLs(TaskInfo taskInfo) throws Exception {
        Map tia = taskInfo.getTaskInfoAttributes();
        String lsid = (String) tia.get(GPConstants.LSID);
        String taskDir = DirectoryManager.getTaskLibDir(null, (String) tia.get(GPConstants.LSID), this.userID);
        File dir = new File(taskDir);

        File[] supportFiles = dir.listFiles(new SupportFileFilter());
        for (int i = 0; i < supportFiles.length; i++) {
            String url = getZipDownloadURLBase() + "getTaskDoc.jsp?name=" + lsid + "&amp;file="
                    + supportFiles[i].getName();

            if (supportFiles[i].getName().endsWith(".pdf")) {
                moduleDocMap.put(lsid, url);
            }
        }

    }

    // simply copy the suiteManifest.xml onto the stream unchanged
    public void writeSuiteXML(SuiteInfo suite, BufferedWriter buff) throws Exception {

        String urlBase = getZipDownloadURLBase();

        StringWriter sbw = new StringWriter();

        SuiteInfoManifestXMLGenerator.generateXMLFile(suite, new BufferedWriter(sbw));

        SuiteManifestParser suiteManifest = new SuiteManifestParser(new StringBufferInputStream(sbw.getBuffer()
                .toString()));

        String suiteManifestString = suiteManifest.getSuiteManifestWithDocURLs(urlBase, moduleDocMap);

        // strip of the <?xml version=...?>
        int idx = suiteManifestString.indexOf("?>");

        buff.write(suiteManifestString.substring(idx + 2));

    }

    /**
     * for a particular module (directory) write the catalog XML to the writer e.g.
     * 
     * <site_module name="ClassNeighbors" zipfilesize="642025" timestamp="1103748609"
     * url="ftp://ftp.broadinstitute.org/pub/genepattern/modules/ClassNeighbors/broadinstitute.org:cancer.software.genepattern.module.analysis/1/ClassNeighbors.zip"
     * sitename="ftp://ftp.broadinstitute.org/pub/genepattern/modules" isexternal="false"> <support_file
     * url="ftp://ftp.broadinstitute.org/pub/genepattern/modules/ClassNeighbors/broadinstitute.org:cancer.software.genepattern.module.analysis/1/ClassNeighbors.pdf">ClassNeighbors.pdf</support_file >
     * <manifest>#Fri Nov 19 10:24:28 EST 2004 &#xA;p12_type=java.lang.Integer &#xA;p8_prefix_when_specified=
     * &#xA;p5_optional=
     * 
     * <support_file
     * url="ftp://ftp.broadinstitute.org/pub/genepattern/modules/ClassNeighbors/broadinstitute.org:cancer.software.genepattern.module.analysis/1/trove.jar">trove.jar</support_file >
     * </site_module >
     * 
     * @param dir
     * @param buff
     * @throws IOException
     */
    public void writeModuleXML(TaskInfo taskInfo, BufferedWriter buff) throws Exception {
        // load the manifest to get the name
        Map tia = taskInfo.getTaskInfoAttributes();
        ZipTask zt = new ZipTask();
        String lsid = (String) tia.get(GPConstants.LSID);
        String taskDir = DirectoryManager.getTaskLibDir(null, (String) tia.get(GPConstants.LSID), userID);

        File dir = new File(taskDir);
        File manifestFile = new File(dir.getAbsolutePath(), "manifest");
        if (!manifestFile.exists())
            return;

        // File zipFile = zt.packageTask(taskInfo, userID);

        buff.write("<site_module name=\"");
        buff.write(taskInfo.getName());
        buff.write("\" zipfilesize=\"");
        buff.write("-1"); // XXX
        buff.write("\" timestamp=\"");
        buff.write("" + (dir.lastModified() / 1000)); // XXX
        buff.write("\" url=\"");
        buff.write(getZipDownloadURLBase() + "makeZip.jsp?name=" + lsid);

        buff.write("\" sitename=\"");
        buff.write(getZipDownloadURLBase());
        buff.write("\" isexternal=\"");
        buff.write("false"); // XXX

        buff.write("\" deprecated=\"");
        buff.write("false"); // XXX

        buff.write("\" >");
        buff.newLine();

        buff.write("<manifest>");
        buff.newLine();
        this.readFile(dir.getAbsolutePath(), "manifest", buff, true, true);
        buff.newLine();
        buff.write("</manifest>");
        buff.newLine();
        File[] supportFiles = dir.listFiles(new SupportFileFilter());
        for (int i = 0; i < supportFiles.length; i++) {
            buff.write("<support_file url=\"");
            buff.write(getZipDownloadURLBase() + "getTaskDoc.jsp?name=" + lsid + "&amp;file="
                    + supportFiles[i].getName());

            buff.write("/");
            buff.write(supportFiles[i].getName());
            buff.write("\"> ");

            buff.write(supportFiles[i].getName());
            buff.write("</support_file>");
            buff.newLine();

            // store the path to the module doc files for suites to use
            // when generating their catalogs
            // if (supportFiles[i].getName().endsWith(".pdf")){
            // moduleDocMap.put(manifestProps.getProperty("LSID"), urlBase + "/"
            // + supportFiles[i].getName());
            // }
        }

        buff.write("</site_module >");
    }

    public void getModuleDocURL(String env, File dir, ArrayList subdirs) throws IOException {
        // load the manifest to get the name
        Properties manifestProps = new Properties();
        FileInputStream fio = new FileInputStream(new File(dir, "manifest"));
        manifestProps.load(fio);
        fio.close();

        String urlBase = reposProps.getProperty(env + ".gp_module_repos_url");
        for (int i = 0; i < subdirs.size(); i++) {
            urlBase = urlBase + "/" + ((File) subdirs.get(i)).getName();
        }

        File[] docFiles = dir.listFiles(new DocFileFilter());
        for (int i = 0; i < docFiles.length; i++) {
            // store the path to the module doc files for suites to use
            // when generating their catalogs
            moduleDocMap.put(manifestProps.getProperty("LSID"), urlBase + "/" + docFiles[i].getName());

        }
    }

    String fqHostName = null;

    public String getZipDownloadURLBase() throws IOException {
        if (fqHostName == null) {
            String portStr = System.getProperty("GENEPATTERN_PORT", "");
            portStr = portStr.trim();
            if (portStr.length()>0) {
                portStr = ":"+portStr;
            }
            fqHostName = System.getProperty("fullyQualifiedHostName");
            if (fqHostName == null) {
                fqHostName = InetAddress.getLocalHost().getCanonicalHostName();
            }
            if (fqHostName.equals("localhost")) {
                fqHostName = "127.0.0.1";
            }
            fqHostName = fqHostName + portStr + "/gp/";
        }
        if (!fqHostName.startsWith("http://")) {
            fqHostName = "http://" + fqHostName;
        }
        return fqHostName;
    }

    public static StringBuffer readFile(String dirName, String fileName, boolean includeNewlines) throws IOException {
        StringWriter writer = new StringWriter();
        readFile(dirName, fileName, new BufferedWriter(writer), false, includeNewlines);
        return writer.getBuffer();
    }

    public static void readFile(String dirName, String fileName, BufferedWriter writer, boolean htmlEncode,
            boolean includeNewlines) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(new File(dirName, fileName)));
        String str;
        while ((str = in.readLine()) != null) {
            if (htmlEncode)
                str = StringUtils.htmlEncode(str);
            writer.write(str);
            if (includeNewlines)
                writer.newLine();
        }

        writer.flush();
        in.close();
    }

    public static void copyFile(File origFile, File newFile) throws IOException {
        FileInputStream inStream = new java.io.FileInputStream(origFile);
        FileOutputStream outStream = new java.io.FileOutputStream(newFile);
        boolean success = true;
        byte[] buf = new byte[1024];
        int len = 0;
        try {
            while ((len = inStream.read(buf)) != -1) {
                outStream.write(buf, 0, len);
                outStream.flush();
            }
            inStream.close();

        } finally {
            outStream.close();
        }
    }

}

class DirFilter implements FileFilter {
    public DirFilter() {
    }

    public boolean accept(File pathname) {
        return pathname.isDirectory();
    }
}

class SupportFileFilter implements FileFilter {
    public SupportFileFilter() {
    }

    public boolean accept(File pathname) {
        String name = pathname.getName();
        if (pathname.isDirectory())
            return false;
        else if ("manifest".equalsIgnoreCase(name))
            return false;
        else if (!name.endsWith(".zip"))
            return true;
        else {
            // its a zip file, ignore it if it contains a manifest
            try {
                ZipFile zfile = new ZipFile(pathname);
                ZipEntry manifest = zfile.getEntry("manifest");
                return manifest == null;
            } catch (Exception e) {
                return true;
            }
        }
    }
}

class DocFileFilter implements FileFilter {
    public DocFileFilter() {
    }

    public boolean accept(File pathname) {
        String name = pathname.getName();
        if (pathname.isDirectory())
            return false;
        else if ("manifest".equalsIgnoreCase(name))
            return false;
        else if (name.endsWith(".pdf"))
            return true;
        else if (name.endsWith(".doc"))
            return true;
        else {
            return false;
        }
    }
}

class TaskZipFilter implements FileFilter {
    public TaskZipFilter() {
    }

    public boolean accept(File pathname) {
        String name = pathname.getName();

        // its a zip file, ignore it if it contains a manifest
        try {
            ZipFile zfile = new ZipFile(pathname);
            ZipEntry manifest = zfile.getEntry("manifest");
            return manifest != null;
        } catch (Exception e) {
            return false;
        }
    }
}
