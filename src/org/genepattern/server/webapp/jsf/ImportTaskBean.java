package org.genepattern.server.webapp.jsf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.faces.component.UIInput;
import javax.faces.model.SelectItem;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.util.AuthorizationManager;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.WebServiceErrorMessageException;
import org.genepattern.webservice.WebServiceException;

public class ImportTaskBean {
    private String url;

    private UploadedFile zipFile;

    private static Logger log = Logger.getLogger(ImportTaskBean.class);

    private SelectItem[] filePrivacyItems;

    private SelectItem[] urlPrivacyItems;

    private String selectedFilePrivacy;

    private String selectedUrlPrivacy;

    private static final String ALL_USERS = "all users";

    public ImportTaskBean() {
        filePrivacyItems = new SelectItem[2];
        filePrivacyItems[0] = new SelectItem(UIBeanHelper.getUserId());
        filePrivacyItems[1] = new SelectItem(ALL_USERS);
        selectedFilePrivacy = ALL_USERS;

        urlPrivacyItems = new SelectItem[2];
        urlPrivacyItems[0] = new SelectItem(UIBeanHelper.getUserId());
        urlPrivacyItems[1] = new SelectItem(ALL_USERS);
        selectedUrlPrivacy = ALL_USERS;
    }

    public void importUrl() {

        try {
            String file = GenePatternAnalysisTask.downloadTask(url);
            doImport(
                    file,
                    selectedUrlPrivacy.equals(ALL_USERS) ? GPConstants.ACCESS_PUBLIC
                            : GPConstants.ACCESS_PRIVATE);
        } catch (IOException e) {
            UIBeanHelper.setInfoMessage("An error occurred while downloading "
                    + url + ".");
            log.error(e);
        }

    }

    public void importFile() {

        try {
            File tmp = File.createTempFile("upload", "zip");
            tmp.delete();
            tmp.mkdir();
            doImport(
                    saveFile(zipFile, tmp).getCanonicalPath(),
                    selectedFilePrivacy.equals(ALL_USERS) ? GPConstants.ACCESS_PUBLIC
                            : GPConstants.ACCESS_PRIVATE);
        } catch (IOException e) {
            UIBeanHelper
                    .setInfoMessage("An error occurred while importing the file "
                            + zipFile.getName() + ".");
            log.error(e);
        }

    }

    private void doImport(String file, int privacy) {
        AuthorizationManager authManager = new AuthorizationManager();
        String username = UIBeanHelper.getUserId();
        boolean taskInstallAllowed = authManager.checkPermission("createTask",
                username);
        boolean pipelineInstallAllowed = authManager.checkPermission(
                "createPipeline", username);
        boolean suiteInstallAllowed = authManager.checkPermission(
                "createSuite", username);

        LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(
                username);
        boolean isSuiteZip = false;
        try {
            isSuiteZip = taskIntegratorClient.isSuiteZip(file);
            boolean isPipelineZip = taskIntegratorClient.isPipelineZip(file);
            if (isSuiteZip && !suiteInstallAllowed) {
                UIBeanHelper
                        .setInfoMessage("You do not have permission to install suites on this server.");
                return;
            } else if (isPipelineZip && !pipelineInstallAllowed) {
                UIBeanHelper
                        .setInfoMessage("You do not have permission to install pipelines on this server.");
                return;
            } else if (!taskInstallAllowed) {
                UIBeanHelper
                        .setInfoMessage("You do not have permission to install tasks on this server.");
                return;
            }
        } catch (WebServiceException e) {
            UIBeanHelper.setInfoMessage("An error occurred during import.");
            log.error(e);
        }
        boolean doRecursive = taskInstallAllowed; // TODO ask user?
        try {

            String lsid = taskIntegratorClient.importZipFromURL(file, privacy,
                    doRecursive);
            String name = null;
            if (isSuiteZip) {
                name = new LocalAdminClient(username).getSuite(lsid).getName();
            } else {
                name = new LocalAdminClient(username).getTask(lsid).getName();
            }
            UIBeanHelper.setInfoMessage("Successfully installed " + name + ".");
        } catch (WebServiceException e) {
            if (e instanceof WebServiceErrorMessageException) {
                WebServiceErrorMessageException wseme = (WebServiceErrorMessageException) e;
                UIBeanHelper.setInfoMessage(wseme.getMessage());
            }
            UIBeanHelper.setInfoMessage("An error occurred during import.");
            log.error(e);
        }
    }

    public static File saveFile(UploadedFile uploadedFile,
            File destinationDirectory) throws IOException {
        FileOutputStream out = null;
        InputStream in = null;
        try {
            String fileName = uploadedFile.getName();
            if (fileName != null) {
                fileName = FilenameUtils.getName(fileName);

            }
            File file = new File(destinationDirectory, fileName);
            out = new FileOutputStream(file);
            in = uploadedFile.getInputStream();
            int bytesRead = -1;
            byte[] b = new byte[10000];
            while ((bytesRead = in.read(b, 0, b.length)) != -1) {
                out.write(b, 0, bytesRead);
            }
            return file;

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public UploadedFile getZipFile() {
        return zipFile;
    }

    public void setZipFile(UploadedFile zipFile) {
        this.zipFile = zipFile;
    }

    public SelectItem[] getUrlPrivacyItems() {
        return urlPrivacyItems;
    }

    public void setUrlPrivacyItems(SelectItem[] urlPrivacyItems) {
        this.urlPrivacyItems = urlPrivacyItems;
    }

    public SelectItem[] getFilePrivacyItems() {
        return filePrivacyItems;
    }

    public void setFilePrivacyItems(SelectItem[] filePrivacyItems) {
        this.filePrivacyItems = filePrivacyItems;
    }

    public String getSelectedFilePrivacy() {
        return selectedFilePrivacy;
    }

    public void setSelectedFilePrivacy(String selectedFilePrivacy) {
        this.selectedFilePrivacy = selectedFilePrivacy;
    }

    public String getSelectedUrlPrivacy() {
        return selectedUrlPrivacy;
    }

    public void setSelectedUrlPrivacy(String selectedUrlPrivacy) {
        this.selectedUrlPrivacy = selectedUrlPrivacy;
    }

}
