package org.genepattern.server.gs;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.faces.event.ActionEvent;

import org.genepattern.webservice.ParameterInfo;
import org.richfaces.component.UITree;
import org.richfaces.model.TreeNode;

public interface GenomeSpaceBeanHelper {
    
    public static String GS_SESSION_KEY = "GS_SESSION";
    public static String GS_USER_KEY = "GS_USER";
    public static String GS_DIRECTORIES_KEY = "GS_DIRECTORIES";
    public static String GSFILEMETADATAS = "GSFILEMETADATAS";

    public abstract boolean isGenomeSpaceEnabled();

    public abstract void setGenomeSpaceEnabled(boolean genomeSpaceEnabled);

    public abstract String getRegPassword();

    public abstract void setRegPassword(String regPassword);

    public abstract String getRegEmail();

    public abstract void setRegEmail(String regEmail);

    public abstract String getPassword();

    public abstract String getUsername();

    public abstract void setMessageToUser(String messageToUser);

    public abstract boolean isInvalidPassword();

    public abstract boolean isLoginError();

    public abstract boolean isUnknownUser();

    public abstract void setPassword(String password);

    public abstract void setUsername(String username);

    public abstract boolean isLoggedIn();

    /**
     * Submit the user / password. For now this uses an action listener since we are redirecting to a page outside of
     * the JSF framework. This should be changed to an action to use jsf navigation in the future.
     * @param event -- ignored        
     */
    public abstract String submitLogin();

    /**
     * register a user into GenomeSpace
     * @return
     */
    public abstract String submitRegistration();

    public abstract String submitLogout();

    /**
     * Delete a file from the user's home dir on GenomeSpace
     * @param ae
     */
    public abstract void deleteFileFromGenomeSpace(ActionEvent ae) throws GsClientException;

    /**
     * lots of room for optimization and caching here
     * @param dirname
     * @param file
     * @return
     */
    public abstract GenomeSpaceFileInfo getFile(String dirname, String file);

    /**
     * gets a one time use link to the file on S3
     * @param ae
     */
    public abstract String getFileURL(String dirname, String filename);

    /**
     * redirects to a time limited, one time use link to the file on S3
     * @param ae
     */
    public abstract void saveFileLocally(ActionEvent ae);

    /**
     * Save a local GenePattern result file back to the GenomeSpace data repository
     * @return
     */
    public abstract String sendInputFileToGenomeSpace();

    /**
     * Save a local GenePattern result file back to the GenomeSpace data repository
     * @return
     */
    public abstract String sendToGenomeSpace();

    public abstract Map<String, List<String>> getGsClientTypes();

    public abstract Map<String, List<GsClientUrl>> getClientUrls();

    public abstract void addToClientUrls(GenomeSpaceFileInfo file);

    public abstract void sendGSFileToGSClient() throws IOException;

    public abstract void sendInputFileToGSClient() throws IOException, GsClientException;

    public abstract List<GsClientUrl> getGSClientURLs(GenomeSpaceFileInfo file);

    public abstract boolean openTreeNode(UITree tree);

    public abstract TreeNode<GenomeSpaceFileInfo> getFileTree();

    public abstract TreeNode<GenomeSpaceFileInfo> getGenomeSpaceFilesTree(GenomeSpaceDirectory gsDir);

    public abstract List<GenomeSpaceDirectory> getGsDirectories();
    
    public List<WebToolDescriptorWrapper> getToolWrappers();

    public abstract List<GenomeSpaceFileInfo> getGsFiles();

    /**
     * get the list of directories in GenomeSpace this user can look at
     * @return
     */
    public abstract List<GenomeSpaceDirectory> getAvailableDirectories();

    public abstract List<GenomeSpaceDirectory> getGenomeSpaceDirectories();

    public abstract void setGenomeSpaceDirectories(List<GenomeSpaceDirectory> dirs);

    public abstract List<ParameterInfo> getSendToParameters(String type);

    public abstract void initCurrentLsid();

    public abstract void setSelectedModule(String selectedModule);
    
    

}