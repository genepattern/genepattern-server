package org.genepattern.server.webservice.server.dao;

import java.io.*;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.log4j.Logger;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.genepattern.LSIDManager;
import org.genepattern.server.process.SuiteRepository;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.Util;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.WebServiceException;
import org.hibernate.Query;
import org.hibernate.Transaction;

public class TaskIntegratorDataService extends BaseService {

    private static Logger logger = Logger.getLogger(TaskIntegratorDataService.class);

    private static TaskIntegratorDataService theInstance = null;

    private TaskIntegratorDAO dao = new TaskIntegratorDAO();

    public static synchronized TaskIntegratorDataService getInstance() {
        if (theInstance == null) {
            theInstance = new TaskIntegratorDataService();
        }
        return theInstance;

    }

    public String cloneSuite(String lsid, String cloneName) throws WebServiceException {
        throw new WebServiceException("Clone suite Not implemented yet");
    }

    public void deleteSuite(String lsid) throws WebServiceException {

        Transaction transaction = null;
        try {
            if (!getSession().getTransaction().isActive()) {
                transaction = getSession().beginTransaction();
            }

            dao.deleteSuite(lsid);

            if (transaction != null) {
                transaction.commit();
            }

            return;
        }
        catch (WebServiceException e) {
            getSession().getTransaction().rollback();
            logger.error(e);
            throw e;
        }
        catch (Exception e) {
            getSession().getTransaction().rollback();
            logger.error(e);
            throw new WebServiceException(e);

        }
        finally {
            cleanupSession();
        }

    }

    public String installSuite(SuiteInfo suiteInfo) throws WebServiceException {

        try {
            if (suiteInfo.getLSID() != null)
                if (suiteInfo.getLSID().trim().length() == 0)
                    suiteInfo.setLSID(null);

            dao.createSuite(suiteInfo);

            String suiteDir = DirectoryManager.getSuiteLibDir(suiteInfo.getName(), suiteInfo.getLSID(), suiteInfo
                    .getOwner());

            String[] docs = suiteInfo.getDocumentationFiles();
            for (int i = 0; i < docs.length; i++) {
                System.out.println("Doc=" + docs[i]);
                File f2 = new File(docs[i]);
                // if it is a url, download it and put it in the suiteDir now
                if (!f2.exists()) {
                    String file = GenePatternAnalysisTask.downloadTask(docs[i]);
                    f2 = new File(suiteDir, filenameFromURL(docs[i]));
                    boolean success = GenePatternAnalysisTask.rename(new File(file), f2, true);
                    System.out.println("Doc rename =" + success);

                }
                else {
                    // move file to suitedir

                    File f3 = new File(suiteDir, f2.getName());
                    boolean success = GenePatternAnalysisTask.rename(f2, f3, true);
                    System.out.println("Doc rename =" + success);

                }

            }

            String[] modLsids = suiteInfo.getModuleLSIDs();
            for (int i = 0; i < modLsids.length; i++) {
                dao.installSuiteModule(suiteInfo.getLSID(), modLsids[i]);
            }

            return suiteInfo.getLSID();
        }
        catch (Exception e) {
            logger.error(e);
            throw new WebServiceException(e);
        }

    }

    public void installSuite(String lsid) throws WebServiceException {
        try {
            SuiteRepository sr = new SuiteRepository();
            HashMap suites = sr.getSuites(System.getProperty("SuiteRepositoryURL"));

            HashMap hm = (HashMap) suites.get(lsid);
            // get the info from the HashMap and install it into the DB
            SuiteInfo suite = new SuiteInfo(hm);

            installSuite(suite);
        }
        catch (Exception e) {
            throw new WebServiceException(e);
        }
    }

    public String installSuite(ZipFile zipFile) throws WebServiceException {
        try {
            System.out.println("Installing suite from zip");

            HashMap hm = SuiteRepository.getSuiteMap(zipFile);
            SuiteInfo suite = new SuiteInfo(hm);

            // now we need to extract the doc files and repoint the suiteInfo
            // docfiles to the file url of the extracted version
            String[] filenames = suite.getDocumentationFiles();
            for (int j = 0; j < filenames.length; j++) {
                int i = 0;
                String name = filenames[j];
                ZipEntry zipEntry = (ZipEntry) zipFile.getEntry(name);
                System.out.println("name= " + name + " ze= " + zipEntry);
                if (zipEntry != null) {
                    InputStream is = zipFile.getInputStream(zipEntry);
                    File outFile = new File(System.getProperty("java.io.tmpdir"), zipEntry.getName());
                    FileOutputStream os = new FileOutputStream(outFile);
                    long fileLength = zipEntry.getSize();
                    long numRead = 0;
                    byte[] buf = new byte[100000];
                    while ((i = is.read(buf, 0, buf.length)) > 0) {
                        os.write(buf, 0, i);
                        numRead += i;
                    }
                    os.close();
                    os = null;
                    outFile.setLastModified(zipEntry.getTime());
                    is.close();
                    filenames[j] = outFile.toURL().toString();
                }
            }

            return installSuite(suite);
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new WebServiceException(e);
        }
    }

    public String modifySuite(int access_id, String lsid, String name, String description, String author, String owner,
            String[] moduleLsids, javax.activation.DataHandler[] dataHandlers, String[] fileNames)
            throws WebServiceException {

        String newLsid = modifySuite(access_id, lsid, name, description, author, owner, new ArrayList(Arrays
                .asList(moduleLsids)), new ArrayList());
        LocalAdminClient adminClient = new LocalAdminClient("GenePattern");

        SuiteInfo si = adminClient.getSuite(newLsid);
        ArrayList docFiles = new ArrayList(Arrays.asList(si.getDocFiles()));
        if (dataHandlers != null) {
            for (int i = 0; i < dataHandlers.length; i++) {
                File axisFile = Util.getAxisFile(dataHandlers[i]);
                try {
                    File dir = new File(DirectoryManager.getSuiteLibDir(null, newLsid, "GenePattern"));
                    File newFile = new File(dir, fileNames[i]);
                    axisFile.renameTo(newFile);
                    docFiles.add(newFile.getAbsolutePath());
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

            }
            if (lsid != null) {
                int start = dataHandlers != null && dataHandlers.length > 0 ? dataHandlers.length - 1 : 0;

                try {
                    File oldLibDir = new File(DirectoryManager.getSuiteLibDir(null, lsid, "GenePattern"));
                    for (int i = start; i < fileNames.length; i++) {
                        String text = fileNames[i];
                        if (oldLibDir != null && oldLibDir.exists()) { // file
                            // from
                            // previous version
                            // of task
                            File src = new File(oldLibDir, text);
                            Util.copyFile(src, new File(DirectoryManager.getSuiteLibDir(null, newLsid, "GenePattern"),
                                    text));
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

            }

            si.setDocFiles((String[]) docFiles.toArray(new String[0]));
        }
        return newLsid;

    }



    public String modifySuite(int access_id, String lsid, String name, String description, String author, String owner,
            ArrayList moduleLsids, ArrayList files) throws WebServiceException {

        String newlsid = lsid;
        ArrayList docs = new ArrayList();

        if ((lsid != null) && (lsid.length() > 0)) {
            try {
                LSIDManager lsidManager = LSIDManager.getInstance();
                newlsid = lsidManager.getNextIDVersion(lsid).toString();

                LocalAdminClient adminClient = new LocalAdminClient("GenePattern");

                SuiteInfo oldsi = adminClient.getSuite(lsid);
                String oldDir = DirectoryManager.getSuiteLibDir(null, lsid, "GenePattern");
                String[] oldDocs = oldsi.getDocumentationFiles();

                for (int i = 0; i < oldDocs.length; i++) {
                    File f = new File(oldDir, oldDocs[i]);
                    docs.add(f.getAbsolutePath());
                }

            }
            catch (Exception e) {
                e.printStackTrace();
                throw new WebServiceException(e);
            }
        }
        else {
            newlsid = null;
        }

        for (int i = 0; i < files.size(); i++) {
            File f = (File) files.get(i);
            docs.add(f.getAbsolutePath());
        }

        SuiteInfo si = new SuiteInfo(newlsid, name, description, author, owner, moduleLsids, access_id, docs);
        return installSuite(si);
    }


}
