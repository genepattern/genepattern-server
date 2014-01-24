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

import static org.genepattern.util.GPConstants.ACCESS_PUBLIC;

import java.io.File;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.genepattern.TaskInstallationException;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.Status;
import org.genepattern.server.webservice.server.dao.TaskIntegratorDAO;
import org.genepattern.server.webservice.server.local.IAdminClient;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.WebServiceException;

public class InstallSuite {
    private static Logger log = Logger.getLogger(InstallSuite.class);

    private String username;

    public InstallSuite(String username) {
        this.username = username;
    }

    /**
     * Install the suite with the given LSID from the repository.
     *
     * @param lsid
     * @throws WebServiceException
     */
    public void install(final String lsid) throws WebServiceException, TaskInstallationException {
        try {
            SuiteRepository sr = new SuiteRepository();
            HashMap suites = sr.getSuites(System.getProperty("SuiteRepositoryURL"));

            HashMap hm = (HashMap) suites.get(lsid);
            // get the info from the HashMap and install it into the DB
            SuiteInfo suiteInfo = new SuiteInfo(hm);
            File suiteDir = DirectoryManager.getSuiteLibDir(suiteInfo.getName(), suiteInfo.getLSID(), suiteInfo.getOwner());
            boolean success=suiteDir.mkdirs();
            if (success) {
                log.debug("created suiteDir="+suiteDir);
            }
            install(suiteInfo, suiteDir);
        } 
        catch (TaskInstallationException e) {
            log.error("Error installing suiteLsid="+lsid, e);
            throw e;
        } 
        catch (Throwable t) {
            log.error("Error installing suiteLsid="+lsid, t);
            throw new WebServiceException(t);
        }
    }

    /**
     * Create a new suite from the SuiteInfo object.
     *
     * @param suiteInfo
     * @return
     * @throws WebServiceException
     */
    private String install(final SuiteInfo suiteInfo, final File suiteDir) throws WebServiceException, TaskInstallationException {
        try {
            if (suiteInfo.getLSID() != null) {
                if (suiteInfo.getLSID().trim().length() == 0)
                    suiteInfo.setLSID(null);
            }

            (new TaskIntegratorDAO()).saveOrUpdate(suiteInfo);

            //String suiteDir = DirectoryManager.getSuiteLibDir(suiteInfo.getName(), suiteInfo.getLSID(), suiteInfo
            //        .getOwner());
            String[] docs = suiteInfo.getDocumentationFiles();
            for (int i = 0; i < docs.length; i++) {
                log.debug("Doc=" + docs[i]);
                File f2 = new File(docs[i]);
                // if it is a url, download it and put it in the suiteDir now
                if (!f2.exists()) {
                    String file = GenePatternAnalysisTask.downloadTask(docs[i]);
                    f2 = new File(suiteDir, filenameFromURL(docs[i]));
                    boolean success = GenePatternAnalysisTask.rename(new File(file), f2, true);
                    log.debug("Doc rename =" + success);

                } 
                else {
                    // move file to suitedir
                    File f3 = new File(suiteDir, f2.getName());
                    boolean success = GenePatternAnalysisTask.rename(f2, f3, true);
                    log.debug("Doc rename =" + success);

                }
            }
            installTask(suiteInfo.getModuleLsids());
            return suiteInfo.getLSID();
        } catch (TaskInstallationException e) {
            log.error(e);
            throw e;
        } catch (Exception e) {
            log.error(e);
            throw new WebServiceException(e);
        }

    }

    public void installTask(String[] lsids) throws WebServiceException, TaskInstallationException {
        final IAdminClient adminClient = new LocalAdminClient(username);
        for (String lsid : lsids) {
            if (adminClient.getTask(lsid) == null) {
                this.installTask(lsid);
            }
        }
    }

    /**
     * Installs the task with the given LSID from the module repository
     *
     * @param lsid
     *            The task LSID
     * @throws WebServiceException
     *             If an error occurs
     */

    public void installTask(String lsid) throws WebServiceException, TaskInstallationException {

        InstallTasksCollectionUtils utils = new InstallTasksCollectionUtils(username, false);
        try {
            InstallTask[] tasks = utils.getAvailableModules();
            for (int i = 0; i < tasks.length; i++) {
                if (tasks[i].getLsid().equalsIgnoreCase(lsid)) {
                    tasks[i].install(username, ACCESS_PUBLIC, new Status() {

                        public void beginProgress(String string) {
                        }

                        public void continueProgress(int percent) {
                        }

                        public void endProgress() {
                        }

                        public void statusMessage(String message) {
                        }

                    });
                }
            }
        } catch (TaskInstallationException e) {
            log.error(e);
            throw e;
        } catch (Exception e) {
            log.error(e);
            throw new WebServiceException(e);
        }
    }

    protected static String filenameFromURL(String url) {
        int idx = url.lastIndexOf("/");
        if (idx >= 0)
            return url.substring(idx + 1);
        else
            return url;
    }
}
