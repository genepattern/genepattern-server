/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

/**
 *
 */
package org.genepattern.server.webapp.jsf;

import static org.genepattern.server.webapp.jsf.UIBeanHelper.getUserId;
import static org.genepattern.util.GPConstants.SUITE_NAMESPACE;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.SuiteDAO;
import org.genepattern.server.genepattern.LSIDManager;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * @author jrobinso
 *
 */
public class CreateSuiteBean implements java.io.Serializable {

    private static Logger log = Logger.getLogger(CreateSuiteBean.class);

    private String name;

    private String description;

    private String author;

    private int accessId;

    private UploadedFile supportFile1;

    private UploadedFile supportFile2;

    private UploadedFile supportFile3;

    private List<ModuleCategory> categories;

    private boolean success = false;

    private SuiteInfo currentSuite = null;

    public CreateSuiteBean() throws WebServiceException {
        if (!AuthorizationHelper.createSuite()) {
            throw new SecurityException();
        }
        if (currentSuite == null) {
            accessId = AuthorizationHelper.createPublicSuite() ? GPConstants.ACCESS_PUBLIC : GPConstants.ACCESS_PRIVATE;
            String lsid = UIBeanHelper.getRequest().getParameter("lsid");
            if (lsid != null) {
                try {
                    currentSuite = (new LocalAdminClient(UIBeanHelper.getUserId())).getSuite(lsid);
                } catch (WebServiceException e) {
                    log.error(e);
                    throw e;
                }
            } else {
                // set default author
                author = UIBeanHelper.getUserId();
            }
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getAccessId() {
        return (currentSuite == null) ? accessId : currentSuite.getAccessId();
    }

    public void setAccessId(int accessID) {
        this.accessId = accessID;
    }

    public String getAuthor() {
        return (currentSuite == null) ? author : currentSuite.getAuthor();
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return (currentSuite == null) ? description : currentSuite.getDescription();
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return (currentSuite == null) ? name : currentSuite.getName();
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SelectItem> getAllowedPrivacies() {
        ArrayList<SelectItem> all = new ArrayList<SelectItem>();

        if (AuthorizationHelper.createPublicSuite()) {
            all.add(new SelectItem(GPConstants.ACCESS_PUBLIC, "all users"));
        }
        if (AuthorizationHelper.createPrivateSuite()) {
            all.add(new SelectItem(GPConstants.ACCESS_PRIVATE, UIBeanHelper.getUserId()));
        }

        return all;
    }

    public static List<List<ModuleCategory>> layoutSuiteCategories(List<ModuleCategory> categories) {
        List<List<ModuleCategory>> cols = new ArrayList<List<ModuleCategory>>();
        // Find the midpoint in the category list.
        int totalCount = 0;
        for (ModuleCategory cat : categories) {
            totalCount += cat.getModuleCount();
        }
        int midpoint = totalCount / 2;

        cols.add(new ArrayList<ModuleCategory>());
        cols.add(new ArrayList<ModuleCategory>());
        int cumulativeCount = 0;
        for (ModuleCategory cat : categories) {
            if (cumulativeCount <= midpoint) {
                cols.get(0).add(cat);
            } else {
                cols.get(1).add(cat);
            }
            cumulativeCount += cat.getModuleCount();
        }
        return cols;
    }

    public List<List<ModuleCategory>> getCategoryColumns() {
        if (categories == null) {
            if (currentSuite != null) {
                categories = (new ModuleHelper(true)).getSelectedTasksByType(currentSuite.getModuleLsids());
            } else {
                categories = (new ModuleHelper(true)).getTasksByType();
            }
        }
        return CreateSuiteBean.layoutSuiteCategories(categories);
    }

    public UploadedFile getSupportFile1() {
        return supportFile1;
    }

    public void setSupportFile1(UploadedFile supportFile1) {
        this.supportFile1 = supportFile1;
    }

    public UploadedFile getSupportFile2() {
        return supportFile2;
    }

    public void setSupportFile2(UploadedFile supportFile2) {
        this.supportFile2 = supportFile2;
    }

    public UploadedFile getSupportFile3() {
        return supportFile3;
    }

    public void setSupportFile3(UploadedFile supportFile3) {
        this.supportFile3 = supportFile3;
    }

    public String save() {
        if (!AuthorizationHelper.createSuite()) {
            throw new SecurityException();
        }
        try {

            SuiteInfo theSuite;
            if (currentSuite == null) {
                theSuite = new SuiteInfo();
                theSuite.setLsid(LSIDManager.getInstance().createNewID(SUITE_NAMESPACE).toString());
            } else {
                theSuite = currentSuite;
            }

            // Save or update database record

            theSuite.setOwner(getUserId());
            theSuite.setName(name);
            theSuite.setDescription(description);
            accessId = AuthorizationHelper.checkSuiteAccessId(accessId);
            theSuite.setAccessId(accessId);
            theSuite.setAuthor(author);

            List<String> selectedLSIDs = new ArrayList<String>();
            for (ModuleCategory cat : categories) {
                for (Module mod : cat.getModules()) {
                    if (mod.isSelected()) {
                        String lsid = mod.getLSID().toStringNoVersion();
                        if (mod.getSelectedVersion() != null && !mod.getSelectedVersion().equals("")) {
                            lsid += ":" + mod.getSelectedVersion();
                        }
                        selectedLSIDs.add(lsid);
                    }
                }
            }
            theSuite.setModuleLsids(selectedLSIDs);
            LocalTaskIntegratorClient ti = new LocalTaskIntegratorClient(UIBeanHelper.getUserId());
            ti.saveOrUpdateSuite(theSuite);

            // Save uploaded files, if any
            final File suiteDir = DirectoryManager.getSuiteLibDir(theSuite.getName(), theSuite.getLsid(), theSuite.getOwner());
            if (supportFile1 != null) {
                saveUploadedFile(supportFile1, suiteDir);
            }
            if (supportFile2 != null) {
                saveUploadedFile(supportFile2, suiteDir);
            }
            if (supportFile3 != null) {
                saveUploadedFile(supportFile3, suiteDir);
            }

            ManageSuiteBean manageSuiteBean = (ManageSuiteBean) UIBeanHelper.getManagedBean("#{manageSuiteBean}");
            manageSuiteBean.setCurrentSuite((new SuiteDAO()).findById(theSuite.getLsid()));
            currentSuite = theSuite;
            return "view suite";
        } catch (Exception e) {
            // This shouldn't be neccessary, but just in case
            HibernateUtil.rollbackTransaction(); 
            throw new RuntimeException(e);
        }

    }

    private void saveUploadedFile(UploadedFile uploadedFile, File suiteDir) throws IOException {
        String fileName = uploadedFile.getName();
        if (fileName != null) {
            fileName = FilenameUtils.getName(fileName);
        }
        BufferedOutputStream out = null;
        InputStream in = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(new File(suiteDir, fileName)));
            in = new BufferedInputStream(uploadedFile.getInputStream());
            int c;
            byte[] b = new byte[10240];
            while ((c = in.read(b)) != -1) {
                out.write(b, 0, c);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException x) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException x) {
                }
            }
        }
    }

    public String clear() {
        return null;
    }

    public List<ModuleCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<ModuleCategory> categories) {
        this.categories = categories;
    }

}
