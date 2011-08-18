package org.genepattern.server.webapp.genomespace;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.faces.event.ActionEvent;

import org.genepattern.server.gs.GsClientException;
import org.genepattern.webservice.ParameterInfo;
import org.richfaces.component.UITree;
import org.richfaces.model.TreeNode;

public class GenomeSpaceBean implements GenomeSpaceBeanHelper {
    GenomeSpaceBeanHelper gsHelper = null;
    
    public GenomeSpaceBean() {
        gsHelper = new GenomeSpaceBeanHelperImpl();
    }

    public boolean isGenomeSpaceEnabled() {
        return gsHelper.isGenomeSpaceEnabled();
    }

    public void setGenomeSpaceEnabled(boolean genomeSpaceEnabled) {
        gsHelper.setGenomeSpaceEnabled(genomeSpaceEnabled);
    }

    public String getRegPassword() {
        return gsHelper.getRegPassword();
    }

    public void setRegPassword(String regPassword) {
        gsHelper.setRegPassword(regPassword);
    }

    public String getRegEmail() {
        return gsHelper.getRegEmail();
    }

    public void setRegEmail(String regEmail) {
        gsHelper.setRegEmail(regEmail);
    }

    public String getPassword() {
        return gsHelper.getPassword();
    }

    public String getUsername() {
        return gsHelper.getUsername();
    }

    public void setMessageToUser(String messageToUser) {
        gsHelper.setMessageToUser(messageToUser);
    }

    public boolean isInvalidPassword() {
        return gsHelper.isInvalidPassword();
    }

    public boolean isLoginError() {
        return gsHelper.isLoginError();
    }

    public boolean isUnknownUser() {
        return gsHelper.isUnknownUser();
    }

    public void setPassword(String password) {
        gsHelper.setPassword(password);
    }

    public void setUsername(String username) {
        gsHelper.setUsername(username);
    }

    public boolean isLoggedIn() {
        return gsHelper.isLoggedIn();
    }

    public String submitLogin() {
        return gsHelper.submitLogin();
    }

    public String submitRegistration() {
        return gsHelper.submitRegistration();
    }

    public String submitLogout() {
        return gsHelper.submitLogout();
    }

    public void deleteFileFromGenomeSpace(ActionEvent ae) throws GsClientException {
        gsHelper.deleteFileFromGenomeSpace(ae);
    }

    public GenomeSpaceFileInfo getFile(String dirname, String file) {
        return gsHelper.getFile(dirname, file);
    }

    public String getFileURL(String dirname, String filename) {
        return gsHelper.getFileURL(dirname, filename);
    }

    public void saveFileLocally(ActionEvent ae) {
        gsHelper.saveFileLocally(ae);
    }

    public String sendInputFileToGenomeSpace() {
        return gsHelper.sendInputFileToGenomeSpace();
    }

    public String sendToGenomeSpace() {
        return gsHelper.sendToGenomeSpace();
    }

    public Map<String, List<String>> getGsClientTypes() {
        return gsHelper.getGsClientTypes();
    }

    public Map<String, List<GSClientUrl>> getClientUrls() {
        return gsHelper.getClientUrls();
    }

    public void addToClientUrls(GenomeSpaceFileInfo file) {
        gsHelper.addToClientUrls(file);
    }

    public void sendGSFileToGSClient() throws IOException {
        gsHelper.sendGSFileToGSClient();
    }

    public void sendInputFileToGSClient() throws IOException, GsClientException {
        gsHelper.sendInputFileToGSClient();
    }

    public List<GSClientUrl> getGSClientURLs(GenomeSpaceFileInfo file) {
        return gsHelper.getGSClientURLs(file);
    }

    public boolean openTreeNode(UITree tree) {
        return gsHelper.openTreeNode(tree);
    }

    public TreeNode<GenomeSpaceFileInfo> getFileTree() {
        return gsHelper.getFileTree();
    }

    public TreeNode<GenomeSpaceFileInfo> getGenomeSpaceFilesTree(GenomeSpaceDirectory gsDir) {
        return gsHelper.getGenomeSpaceFilesTree(gsDir);
    }

    public List<GenomeSpaceDirectory> getGsDirectories() {
        return gsHelper.getGsDirectories();
    }

    public List<GenomeSpaceFileInfo> getGsFiles() {
        return gsHelper.getGsFiles();
    }

    public List<GenomeSpaceDirectory> getAvailableDirectories() {
        return gsHelper.getAvailableDirectories();
    }

    public List<GenomeSpaceDirectory> getGenomeSpaceDirectories() {
        return gsHelper.getGenomeSpaceDirectories();
    }

    public void setGenomeSpaceDirectories(List<GenomeSpaceDirectory> dirs) {
        gsHelper.setGenomeSpaceDirectories(dirs);
    }

    public List<ParameterInfo> getSendToParameters(String type) {
        return gsHelper.getSendToParameters(type);
    }

    public void initCurrentLsid() {
        gsHelper.initCurrentLsid();
    }

    public void setSelectedModule(String selectedModule) {
        gsHelper.setSelectedModule(selectedModule);
    }

    public List<WebToolDescriptorWrapper> getToolWrappers() {
        return gsHelper.getToolWrappers();
    }
}
