package org.genepattern.server.gs;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.genepattern.webservice.TaskInfo;

/**
 * Interface to GenomeSpace from JSF pages, so that core gs libraries need not be included in the core of GenePattern.
 * This interface was manually extracted from GenomeSpaceBean.
 * 
 * @author pcarr
 */
public interface GsClientUtil {
    boolean isLoggedIn(Object gsSessionObj);
    
    GsLoginResponse submitLogin(String env, String username, String password) throws GsClientException;
    
    void registerUser(String env, String username, String password, String regEmail) throws GsClientException;
    
    void logout(Object gsSessionObj);

    void deleteFile(Object gsSessionObj, GenomeSpaceFileInfo file) throws GsClientException;    

    /**
     * @param gsSessionObj, must be instanceof GsSession
     * @param in
     */
    public GenomeSpaceFileInfo saveFileToGenomeSpace(Object gsSessionObj, Map<String, List<String>> gsClientTypes, File in) throws GsClientException;
    
    public List<WebToolDescriptorWrapper> getToolWrappers(Object gsSessionObj, Map<String, List<String>> gsClientTypes);
    
    void initGsClientTypes(Object gsSessionObj, Map<String, List<String>> gsClientTypes);

    List<GsClientUrl> getGSClientURLs(Object gsSessionObj, GenomeSpaceFileInfo file);
    
    List<GenomeSpaceDirectory> initUserDirs(Object gsSessionObj, Map<String, Set<TaskInfo>> kindToModules, Map<String, List<String>> gsClientTypes, Map<String, List<GsClientUrl>> clientUrls);
}