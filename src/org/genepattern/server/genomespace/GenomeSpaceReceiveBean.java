package org.genepattern.server.genomespace;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.dm.ExternalFile;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.webapp.ParameterInfoWrapper;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.server.webapp.uploads.UploadFilesBean;
import org.genepattern.server.webapp.uploads.UploadFilesBean.DirectoryInfoWrapper;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoCache;

public class GenomeSpaceReceiveBean {
    private static Logger log = Logger.getLogger(GenomeSpaceReceiveBean.class);
    
    private UploadFilesBean uploadBean = null;
    private GenomeSpaceBean genomeSpaceBean = null;
    private List<GpFilePath> receivedFiles = null;
    private List<SelectItem> validModules = null;
    private boolean redirected = false;
    private String oldRequestString = null;
    
    public String getRefreshPage() throws IOException {
        boolean skip = redirected;
        
        HttpServletRequest request = UIBeanHelper.getRequest();
        String queryString = request.getQueryString();
        if (queryString != null && queryString.length() > 0) {
            blankCurrentTaskInfo();
            cleanBean();
        }
        
        if (oldRequestString != null && !oldRequestString.equals(queryString)) {
            cleanBean();
        }
        
        List<GpFilePath> files = getReceivedFiles();
        if (files.size() < 1 && !skip) {
            UIBeanHelper.getResponse().sendRedirect("/gp/");
        }

        oldRequestString = queryString;
        return null;
    }
    
    private void cleanBean() {
        receivedFiles = null;
        redirected = false;
    }
    
    public List<SelectItem> getValidModules() {
        return validModules;
    }
    
    private List<GpFilePath> parseParameters() {
        List<GpFilePath> received = new ArrayList<GpFilePath>();
        HttpServletRequest request = UIBeanHelper.getRequest();
        String filesString = request.getParameter("files");
        
        if (!initGenomeSpaceBean()) {
            log.error("Unable to acquire reference to GenomeSpaceBean in GenomeSpaceReceiveBean.parseParameters()");
            return received;
        }
        
        if (filesString != null) {
            filesString = filesString.replaceAll("%2520", "%20");
            String[] fileParams = filesString.split(",");
            for (String param : fileParams) {
                try {
                    GpFilePath file = null;
                    if (param.contains("genomespace.org")) file = genomeSpaceBean.getFile(param);
                    else file = new ExternalFile(param);
                        
                    if (file != null) received.add(file);
                }
                catch (Throwable t) {
                    log.error("Error getting GenomeSpace file: " + param);
                }
            }
        }
        return received;
    }
    
    private void populateSelectItems() {
        validModules = new ArrayList<SelectItem>();
        Set<TaskInfo> minimalModuleSet = null;
        
        for (GpFilePath i : receivedFiles) {                                            // For every received file
            Set<TaskInfo> fileSuperSet = new HashSet<TaskInfo>();                       // Create a superset of all sets for all conversions of the file
            
            Set<String> conversions = null;
            if (i instanceof GenomeSpaceFile) {
                conversions = ((GenomeSpaceFile) i).getConversions();
                conversions.add(i.getExtension());
            }
            else {
                conversions = new HashSet<String>();
                conversions.add(i.getExtension());
            }
            for (String format : conversions) {                                         // For every convertible format
                
                Set<TaskInfo> infos = getKindToModules().get(format);                   // Get the set of TaskInfos for the format
                if (infos == null) { infos = new HashSet<TaskInfo>(); }                 // Protect against null sets
                fileSuperSet.addAll(infos);                                             // Union the set for this conversion with the super set
            }
            
            if (minimalModuleSet == null) { minimalModuleSet = fileSuperSet; }          // Get the intersection of the supersets of all files
            else { minimalModuleSet.retainAll(fileSuperSet); }
        }
        
        if (minimalModuleSet == null) { return; }                                       // Protect against nulls
        
        for (TaskInfo info : minimalModuleSet) {                                        // For all TaskInfos in the final set, make a select list
            SelectItem item = new SelectItem();
            item.setLabel(info.getName());
            item.setValue(info.getLsid());
            validModules.add(item);
        }
        Collections.sort(validModules, new SelectItemComparator());
    }
    
    private void blankCurrentTaskInfo() {
        GenomeSpaceBean genomeSpaceBean = (GenomeSpaceBean) UIBeanHelper.getManagedBean("#{genomeSpaceBean}");
        genomeSpaceBean.setSelectedModule("");
    }
    
    public List<GpFilePath> getReceivedFiles() {
        if (receivedFiles == null) {
            receivedFiles = parseParameters();
            populateSelectItems();
        }
        return receivedFiles;
    }
    
    private boolean initGenomeSpaceBean() {
        if (genomeSpaceBean == null) {
            genomeSpaceBean = (GenomeSpaceBean) UIBeanHelper.getManagedBean("#{genomeSpaceBean}");
        }
        return genomeSpaceBean != null ? true : false;
    }
    
    private boolean initUploadBean() {
        if (uploadBean == null) {
            uploadBean = (UploadFilesBean) UIBeanHelper.getManagedBean("#{uploadFilesBean}");
        }
        return uploadBean != null ? true : false;
    }
    
    public List<SelectItem> getUploadDirectories() {
        List<SelectItem> selectItems = new ArrayList<SelectItem>();
        boolean addedUploadRoot = false;
        if (!initUploadBean()) {
            log.error("Unable to acquire reference to UploadFilesBean in GenomeSpaceReceiveBean.getUploadDirectories()");
            return selectItems;
        }
        for (DirectoryInfoWrapper dir : uploadBean.getDirectories()) {
            SelectItem item = new SelectItem();
            if (dir.getPath().equals("./")) {
                if (!addedUploadRoot) item.setLabel("Upload Directory");
                else continue;
                addedUploadRoot = true;
            }
            else {
                item.setLabel(dir.getPath());
            }
            item.setValue(dir.getPath());
            selectItems.add(item);
        }
        return selectItems;
    }
    
    public String getRootUploadDirectory() {
        return "Upload Directory";
    }
    
    public boolean isCorrectUser() {
        HttpServletRequest request = UIBeanHelper.getRequest();
        String paramUser = request.getParameter("genomespaceuser");
        String sessionUser = (String) UIBeanHelper.getSession().getAttribute(GenomeSpaceLoginManager.GS_USER_KEY);
        if (sessionUser == null) return false;      // If there is no genomespace user in the session there is no correct user
        if (paramUser == null) return true;         // If no user parameter was passed, assume user is correct (backwards compatibility)
        return paramUser.equals(sessionUser);       // If the two usernames match everything is as it should be
    }
    
    public Map<String, SortedSet<TaskInfo>> getKindToModules() {
        if (!initUploadBean()) {
            log.error("Unable to acquire reference to UploadFilesBean in GenomeSpaceReceiveBean.getKindToModules()");
            return null;
        }
        return uploadBean.getKindToTaskInfo();
    }

    public void loadTask() throws UnsupportedEncodingException, Exception {
        HttpServletRequest request = UIBeanHelper.getRequest();
        HttpServletResponse response = UIBeanHelper.getResponse();
        String lsid = request.getParameter("module");
        lsid = UIBeanHelper.decode(lsid);
        TaskInfo module = TaskInfoCache.instance().getTask(lsid);
        
        String redirectURL = "/gp/pages/index.jsf?lsid=" + lsid;
        
        for (GpFilePath file : receivedFiles) {
            ParameterInfoWrapper selectedParam = null;
            String url = URLEncoder.encode(file.getUrl().toString(), "UTF-8");
            
            List<ParameterInfoWrapper> relevantParams = null;
            
            if (file instanceof GenomeSpaceFile) {
                for (String format : ((GenomeSpaceFile) file).getConversions()) {
                    relevantParams = module._getKindToParameterInfoMap().get(format);
                    if (relevantParams != null) {
                        url = ((GenomeSpaceFile) file).getConversionUrls().get(format);
                        break;
                    }
                }
            }
            else {
                relevantParams = module._getKindToParameterInfoMap().get(file.getKind());
            }
            
            // Protect against null
            if (relevantParams == null) {
                relevantParams = new ArrayList<ParameterInfoWrapper>();
            }
            
            module._getKindToParameterInfoMap().get(file.getKind());
            if (relevantParams.size() > 0) {
                selectedParam = relevantParams.get(0);
            }
            redirectURL += "&" + selectedParam.getName() + "=" + url;
        }
        
        // Redirect to the correct task
        response.sendRedirect(redirectURL);
        redirected = true;
    }
    
    public String prepareSaveFile() throws IOException {
        HttpServletRequest request = UIBeanHelper.getRequest();
        String directoryPath = null;
        String fileUrl = null;
        
        for (Object i : request.getParameterMap().keySet()) {
            String parameter = (String) i;
            if (parameter.endsWith(":uploadDirectory")) {
                directoryPath = UIBeanHelper.decode(request.getParameter(parameter));
            }
            if (parameter.endsWith(":path")) {
                fileUrl = UIBeanHelper.decode(request.getParameter(parameter));
            }
        }
        
        if (directoryPath == null || fileUrl == null) {
            log.error("directoryPath was null in prepareSaveFile(): " + fileUrl);
            UIBeanHelper.setErrorMessage("Unable to get the selected directory to save file");
            return null;
        }
        
        GenomeSpaceBean genomeSpaceBean = (GenomeSpaceBean) UIBeanHelper.getManagedBean("#{genomeSpaceBean}");
        genomeSpaceBean.saveFileToUploads(fileUrl, directoryPath);

        return "home";
    }
    
    public class SelectItemComparator implements Comparator<SelectItem> {

        public int compare(SelectItem o1, SelectItem o2) {
            return o1.getLabel().toLowerCase().compareTo(o2.getLabel().toLowerCase());
        }
        
    }
}
