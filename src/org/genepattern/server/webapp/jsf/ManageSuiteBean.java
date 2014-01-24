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

/**
 *
 */
package org.genepattern.server.webapp.jsf;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.Suite;
import org.genepattern.server.domain.SuiteDAO;
import org.genepattern.server.process.MissingTaskException;
import org.genepattern.server.process.ZipSuite;
import org.genepattern.server.process.ZipSuiteWithDependents;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.local.IAdminClient;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.LSIDUtil;
import org.genepattern.webservice.SuiteInfo;

public class ManageSuiteBean {

    private static Logger log = Logger.getLogger(ManageSuiteBean.class);

    private List<Suite> suites;

    private Map<String, Boolean> editPermissions;

    private Suite currentSuite = null;

    private List<ModuleCategory> categories;

    private boolean includeDependents = false;

    public ManageSuiteBean() {
    }

    /**
     * Delete the selected suites.
     * 
     * @return
     */
    public String delete() {
	String[] selectedSuites = UIBeanHelper.getRequest().getParameterValues("selectedSuites");
	deleteSuites(selectedSuites);
	return "delete suite";
    }

    public void delete(ActionEvent event) {
	String[] lsids = UIBeanHelper.getRequest().getParameterValues("selectedVersions");
	deleteSuites(lsids);
    }

    public void deleteSupportFile(ActionEvent event) {
	if (currentSuite != null) {
	    String key = UIBeanHelper.getRequest().getParameter("supportFileKey");
	    String[] supportFiles = getSupportFiles();
	    if (supportFiles != null) {
		for (String f : supportFiles) {
		    if (f.equals(key)) {
			try {
			    String suiteDirPath = DirectoryManager.getSuiteLibDir(currentSuite.getName(), currentSuite
				    .getLsid(), currentSuite.getUserId());
			    new File(suiteDirPath, f).delete();

			} catch (Exception e) {
			    log.error(e);
			}
			break;

		    }
		}
	    }
	}
    }

    public String edit() {
	loadCurrentSuite();
	return "edit suite";
    }

    /**
     * @param event
     */
    public void exportExDependents(ActionEvent event) {
	ZipSuite zs = new ZipSuite();
	export(zs);
    }

    public String exportInDependents() {
	ZipSuite zs = new ZipSuiteWithDependents();
	return export(zs);
    }

    public List<List<ModuleCategory>> getCategoryColumnsForSuite() {
	if (currentSuite != null) {
	    if (categories == null) {
		categories = (new ModuleHelper()).getTasksByTypeForSuite(currentSuite);
	    }
	}
	return CreateSuiteBean.layoutSuiteCategories(categories);
    }

    public Suite getCurrentSuite() {
	return currentSuite;
    }

    public Map<String, Boolean> getEditPermissions() {
	return editPermissions;
    }

    public boolean getIncludeDependents() {
	return includeDependents;
    }

    public List<Suite> getSuites() {
	if (suites == null) {
	    resetSuites();
	}
	return suites;
    }

    public String[] getSupportFiles() {
	if (currentSuite != null) {
	    try {
		String suiteDirPath = DirectoryManager.getSuiteLibDir(currentSuite.getName(), currentSuite.getLsid(),
			currentSuite.getUserId());
		File suiteDir = new File(suiteDirPath);
		return suiteDir.list();
	    } catch (Exception e) {
		log.error(e);
		HibernateUtil.rollbackTransaction();
	    }
	}
	return null;
    }

    public boolean isCurrentSuiteSet() {
	return (currentSuite != null);
    }

    public void setCurrentSuite(Suite currentSuite) {
	this.currentSuite = currentSuite;
    }

    public void setIncludeDependents(boolean includeDependents) {
	this.includeDependents = includeDependents;
    }

    public String view() {
	String lsid = UIBeanHelper.getRequest().getParameter("lsid");
	currentSuite = (new SuiteDAO()).findById(lsid);
	return (currentSuite != null) ? "view suite" : "failure";
    }

    public String viewTaskProperty() {
	String[] selectedSuites = UIBeanHelper.getRequest().getParameterValues("selectedSuites");
	deleteSuites(selectedSuites);
	return "delete suite";
    }

    /**
     * @param suiteLsids
     */
    private void deleteSuites(String[] suiteLsids) {
        String user = UIBeanHelper.getUserId();
        boolean admin = AuthorizationHelper.adminSuites();
        if (suiteLsids != null) {
            for (String lsid : suiteLsids) {
                Suite s = (Suite) HibernateUtil.getSession().get(org.genepattern.server.domain.Suite.class, lsid);
                if (s.getUserId() == null || s.getUserId().equals(user) || admin) {
                    (new SuiteDAO()).delete(s);
                    // Delete supporting files
                    String suiteDirPath = null;
                    try {
                        suiteDirPath = DirectoryManager.getSuiteLibDir(s.getName(), s.getLsid(), s.getUserId());
                    } catch (Exception e) {
                        log.error("Error", e);
                        return;
                    }
                    Delete del = new Delete();
                    del.setDir(new File(suiteDirPath));
                    del.setIncludeEmptyDirs(true);
                    del.setProject(new Project());
                    del.execute();

                }
            }
            resetSuites();
        }
    }

    private String export(ZipSuite zs) {
	String lsid = UIBeanHelper.getRequest().getParameter("lsid");
	if (lsid == null || lsid.equals("null") || lsid.length() == 0)
	    return "";

	String userID = UIBeanHelper.getUserId();
	if (userID == null)
	    return ""; // come back after login

	FacesContext facesContext = UIBeanHelper.getFacesContext();
	try {
	    File zipFile = zs.packageSuite(lsid, userID);

	    HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();

	    IAdminClient adminClient = new LocalAdminClient(userID);
	    SuiteInfo si = adminClient.getSuite(lsid);
	    String contentType = "application/x-zip-compressed" + "; name=\"" + si.getName() + ".zip" + "\";";
	    response.addHeader("Content-Disposition", "attachment; filename=\"" + si.getName() + ".zip" + "\";");
	    response.setContentLength((int) zipFile.length());
	    response.setContentType(contentType);
	    OutputStream out = response.getOutputStream();

	    // Copy the contents of the file to the output stream
	    byte[] buf = new byte[1024];
	    int count = 0;
	    FileInputStream in = new FileInputStream(zipFile);
	    while ((count = in.read(buf)) >= 0) {
		out.write(buf, 0, count);
	    }
	    in.close();
	    out.flush();
	    out.close();

	    zipFile.delete();
	    facesContext.responseComplete();

	} catch (MissingTaskException e) {
	    TaskCatalogBean taskCatalogBean = (TaskCatalogBean) UIBeanHelper.getManagedBean("#{taskCatalogBean}");
	    taskCatalogBean.refilter(e.getMissingLsids());
	    return "task catalog";
	} catch (Exception e) {
	    log.error(e);
	    throw new RuntimeException(e);
	}
	return "success";
    }

    private void loadCurrentSuite() {
	if (currentSuite == null) {
	    String lsid = UIBeanHelper.getRequest().getParameter("lsid");
	    currentSuite = (new SuiteDAO()).findById(lsid);
	}
    }

    /**
     * Query for suites the current user is authorized to see.
     * 
     */
    private void resetSuites() {
	if (AuthorizationHelper.adminSuites()) {
	    suites = (new SuiteDAO()).findAll();
	} else {
	    suites = (new SuiteDAO()).findByOwnerOrPublic(UIBeanHelper.getUserId());
	}

	editPermissions = new HashMap<String, Boolean>();
	for (Suite s : suites) {
	    boolean canEdit = s.getUserId().equals(UIBeanHelper.getUserId())
		    && LSIDUtil.getInstance().isAuthorityMine(s.getLsid());
	    editPermissions.put(s.getLsid(), canEdit);

	}
    }

}
