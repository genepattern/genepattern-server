/**
 * 
 */
package org.genepattern.server.webapp.jsf;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.io.FilenameUtils;
import org.apache.myfaces.custom.fileupload.HtmlInputFileUpload;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.TaskIntegrator;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.dao.TaskIntegratorDAO;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

import static org.genepattern.server.webapp.jsf.UIBeanHelper.getUserId;

/**
 * @author jrobinso
 * 
 */
public class CreateSuiteBean implements java.io.Serializable {

    private static final long serialVersionUID = 352540582209631173l;
    private String name;
    private String description;
    private String author;
    private Integer accessId = 1; // Public
    private UploadedFile supportFile1;
    private UploadedFile supportFile2;
    private UploadedFile supportFile3;
    private List<ModuleCategory> categories = null;

    public Integer getAccessId() {
        return accessId;
    }

    public void setAccessId(Integer accessId) {
        this.accessId = accessId;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List getCategoryColumns() {

        List<List> cols = new ArrayList<List>();

        List<ModuleCategory> allCategories = getTasksByType();
        int totalCount = 0;
        for (ModuleCategory cat : allCategories) {
            totalCount += cat.getModuleCount();
        }
        int midpoint = totalCount / 2;

        cols.add(new ArrayList());
        cols.add(new ArrayList());
        int cumulativeCount = 0;
        for (ModuleCategory cat : allCategories) {
            if (cumulativeCount < midpoint) {
                cols.get(0).add(cat);
            }
            else {
                cols.get(1).add(cat);
            }
            cumulativeCount += cat.getModuleCount();
        }
        return cols;
    }

    private List<ModuleCategory> getTasksByType() {
        if (categories == null) {
            System.out.println("Creating categories");
            categories = new ArrayList<ModuleCategory>();
            TaskInfo[] alltasks = (new AdminDAO()).getAllTasksForUser(getUserId());
            Map<String, List<TaskInfo>> taskMap = new HashMap<String, List<TaskInfo>>();

            for (int i = 0; i < alltasks.length; i++) {
                TaskInfo ti = alltasks[i];
                String taskType = ti.getTaskInfoAttributes().get("taskType");
                List<TaskInfo> tasks = taskMap.get(taskType);
                if (tasks == null) {
                    tasks = new ArrayList<TaskInfo>();
                    taskMap.put(taskType, tasks);
                }
                tasks.add(ti);
            }

            List<String> categoryNames = new ArrayList(taskMap.keySet());
            Collections.sort(categoryNames);
            for (String categoryName : categoryNames) {
                TaskInfo[] modules = new TaskInfo[taskMap.get(categoryName).size()];
                modules = taskMap.get(categoryName).toArray(modules);
                categories.add(new ModuleCategory(categoryName, modules));
            }
        }
        return categories;
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

        try {
            // Save database record
            SuiteInfo suiteInfo = new SuiteInfo();
            suiteInfo.setName(name);
            suiteInfo.setDescription(description);
            suiteInfo.setAccessId(accessId);
            suiteInfo.setAuthor(author);
            suiteInfo.setOwner(getUserId());
            List<String> selectedLSIDs = new ArrayList<String>();
            for (ModuleCategory cat : categories) {
                for (Module mod : cat.getModules()) {
                    if (mod.isSelected()) {
                        selectedLSIDs.add(mod.getSelectedVersion());
                        suiteInfo.setModuleLsids(selectedLSIDs);
                    }
                }
            }
            (new TaskIntegratorDAO()).createSuite(suiteInfo);

            // Save uploaded files, if any
            String suiteDir = DirectoryManager.getSuiteLibDir(suiteInfo.getName(), suiteInfo.getLSID(), suiteInfo
                    .getOwner());
            if (supportFile1 != null) {
                saveUploadedFile(supportFile1, suiteDir);
            }
            if (supportFile2 != null) {
                saveUploadedFile(supportFile2, suiteDir);
            }
            if (supportFile3 != null) {
                saveUploadedFile(supportFile3, suiteDir);
            }
        }
        catch (Exception e) {
            HibernateUtil.rollbackTransaction();  // This shouldn't be neccessary, but just in case
            throw new RuntimeException(e);        // @todo -- replace with appropriate GP exception
        }

        return null;
    }

    private void saveUploadedFile(UploadedFile uploadedFile, String suiteDir) throws FileNotFoundException, IOException {
        String fileName = uploadedFile.getName();
        if (fileName != null) {
            fileName = FilenameUtils.getName(fileName);

        }
        FileOutputStream out = new FileOutputStream(new File(suiteDir, fileName));
        InputStream in = uploadedFile.getInputStream();
        int c;
        while ((c = in.read()) != -1) {
            out.write(c);
        }
        in.close();
        out.close();
    }

}
