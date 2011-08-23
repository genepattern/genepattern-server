package org.genepattern.server.gs.impl;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.server.gs.GenomeSpaceBean;
import org.genepattern.server.gs.GenomeSpaceDirectory;
import org.genepattern.server.gs.GenomeSpaceFileInfo;
import org.genepattern.server.gs.GsClientException;
import org.genepattern.server.gs.GsClientUrl;
import org.genepattern.server.gs.GsClientUtil;
import org.genepattern.server.gs.GsLoginResponse;
import org.genepattern.server.gs.WebToolDescriptorWrapper;
import org.genepattern.webservice.TaskInfo;
import org.genomespace.atm.model.FileParameter;
import org.genomespace.atm.model.WebToolDescriptor;
import org.genomespace.client.ConfigurationUrls;
import org.genomespace.client.DataManagerClient;
import org.genomespace.client.FileParameterWrapper;
import org.genomespace.client.GsSession;
import org.genomespace.client.User;
import org.genomespace.client.exceptions.AuthorizationException;
import org.genomespace.client.exceptions.InternalServerException;
import org.genomespace.datamanager.core.GSDataFormat;
import org.genomespace.datamanager.core.GSDirectoryListing;
import org.genomespace.datamanager.core.GSFileMetadata;

/**
 * replaces GenomeSpaceHelper ...
 * @author pcarr
 *
 */
public class GsClientUtilImpl implements GsClientUtil {
    private static Logger log = Logger.getLogger(GsClientUtilImpl.class);

    public boolean isLoggedIn(Object gsSessionObj) {
        GsSession gsSession = null;
        if (gsSessionObj instanceof GsSession) {
            gsSession = (GsSession) gsSessionObj;
        }
        return ((gsSession != null) && (gsSession.isLoggedIn()));
     }
    
    /**
     * 
     * @param env
     * @param username
     * @param password
     * @return a Map of attributes to be added to the httpSession
     * @throws GsClientException
     */
    public GsLoginResponse submitLogin(String env, String username, String password) throws GsClientException {
        if (env == null) {
            log.error("Environment for GenomeSpace not set");
            env = "test";
        }
        
        if (username == null) {
            throw new GsClientException("Username must be set");
        }

        try {
           ConfigurationUrls.init(env);
           GsSession gsSession = new GsSession();
           User gsUser = gsSession.login(username, password);

           GsLoginResponse response = new GsLoginResponse();
           response.attrs = new HashMap<String,Object>();
           response.attrs.put(GenomeSpaceBean.GS_USER_KEY, gsUser);
           response.attrs.put(GenomeSpaceBean.GS_SESSION_KEY, gsSession);
           response.gsAuthenticationToken = gsSession.getAuthenticationToken();
           response.unknownUser = false;
           response.gsUsername = gsUser.getUsername();
           return response;
        }  
        catch (AuthorizationException e) {
            throw new GsClientException("Authentication error, please check your username and password.");
        } 
        catch (Exception t) {
            throw new GsClientException("An error occurred logging in to GenomeSpace.  Please contact the GenePattern administrator. Error was: "+t.getLocalizedMessage(), t);
        }
    }

    public void registerUser(String env, String username, String password, String regEmail) throws GsClientException {
        if (env == null) {
            log.error("Environment for GenomeSpace not set");
            env = "test";
        }

        try {
            ConfigurationUrls.init(env);
            GsSession gsSession = new GsSession();
            gsSession.registerUser(username, password, regEmail);
        }
        catch (Throwable t) {
            throw new GsClientException("Error registering GenomeSpace account for username="+username+": "+t.getLocalizedMessage());
        }
    }
    
    public void logout(Object gsSessionObj) {
        GsSession gsSession = null;
        if (gsSessionObj instanceof GsSession) {
            gsSession = (GsSession) gsSessionObj;
        }
        if (gsSession == null) {
            log.error("Invalid arg, gsSessionObj is null or not an instanceof GsSession, ignoring logout request");
        }
        gsSession.logout();
    }

    public void deleteFile(Object gsSessionObj, GenomeSpaceFileInfo file) {
        GsSession sess = null;
        if (gsSessionObj instanceof GsSession) {
            sess = (GsSession) gsSessionObj;
        }
        GSFileMetadata metadata = (GSFileMetadata) file.getMetadata();
        sess.getDataManagerClient().delete(metadata);
    }

    public String getFileURL(Object gsSessionObj, GSFileMetadata gsFile) {
        GsSession sess = null;
        if (gsSessionObj instanceof GsSession) {
            sess = (GsSession) gsSessionObj;
        }
        if (gsSessionObj == null) return null;
        
        URL s3Url = sess.getDataManagerClient().getFileUrl(gsFile, null);
        return s3Url.toString();
    }
    
    private static String getFileURL(GsSession sess, GSFileMetadata gsFile) {
        if (gsFile == null) return null; 
        URL s3Url = sess.getDataManagerClient().getFileUrl(gsFile, null);
        return s3Url.toString();
    }

   public GenomeSpaceFileInfo saveFileToGenomeSpace(Object gsSessionObj, Map<String, List<String>> gsClientTypes, File in) throws GsClientException {
        if (in == null) {
            log.error("Invalid null file arg");
            return null;
        }
        GsSession sess = null;
        if (gsSessionObj instanceof GsSession) {
            sess = (GsSession) gsSessionObj;
        }
        else {
            throw new GsClientException("Error saving file to GenomeSpace,  '"+in.getName()+"': gsSessionObj is not a valid GsSession");
        }
        GSFileMetadata metadata = null;
        try {
            DataManagerClient dmClient = sess.getDataManagerClient();
            GSDirectoryListing rootDir = dmClient.listDefaultDirectory();
            metadata = dmClient.uploadFile(in, rootDir.getDirectory());
        } 
        catch (Throwable t) {
            log.error(t);
            throw new GsClientException("Error saving file to GenomeSpace,  '"+in.getName()+"': "+t.getLocalizedMessage());
        }
        
        GenomeSpaceDirectory parent = null;
        String filename = metadata.getName();
        String url = getFileURL(sess, metadata);
        Set<String> availableDataFormats = getAvailableDataFormatNames(metadata.getAvailableDataFormats());
        Date lastModified = metadata.getLastModified();
        GenomeSpaceFileInfo gsFile = new GenomeSpaceFileInfo(parent, filename, url, 
                availableDataFormats, lastModified, metadata, gsClientTypes);
        return gsFile;
    }
    
    public List<WebToolDescriptorWrapper> getToolWrappers(Object gsSessionObj, Map<String, List<String>> gsClientTypes) {
        GsSession gsSession = null;
        if (gsSessionObj instanceof GsSession) {
            gsSession = (GsSession) gsSessionObj;
        }
        else {
            log.error("gsSessionObj is not instanceof GsSession");
            return new ArrayList<WebToolDescriptorWrapper>();
        }
        List<WebToolDescriptorWrapper> wrappers = new ArrayList<WebToolDescriptorWrapper>();
        List<WebToolDescriptor> tools = getGSClients(gsSession);        
        if (gsClientTypes.size() == 0) {
            initGSClientTypesMap(gsClientTypes, tools); 
        }
        for (WebToolDescriptor i : tools) {
            wrappers.add(new WebToolDescriptorWrapper(i.getName(), gsClientTypes));
        }
        return wrappers;
    }
    
    public void initGsClientTypes(Object gsSessionObj, Map<String, List<String>> gsClientTypes) {
        getToolWrappers(gsSessionObj, gsClientTypes);
    }
    
    private static List<WebToolDescriptor> getGSClients(GsSession gsSession) {
        List<WebToolDescriptor> tools;
        try {
            tools = gsSession.getAnalysisToolManagerClient().getWebTools();
        }
        catch (InternalServerException e) {
            log.error("Error getting getAnalysisToolManagerClient().getWebTools().  Session: " + gsSession + " Message: " + e.getMessage());
            return new ArrayList<WebToolDescriptor>();
        }
        WebToolDescriptor gp = null;  // Remove GenePattern from the list
        for (WebToolDescriptor i : tools) { 
            if (i.getName().equals("GenePattern")) {
                gp = i;
            }
        }
        tools.remove(gp);
        return tools;
    }
    
    static private void initGSClientTypesMap(Map<String, List<String>> gsClientTypes, List<WebToolDescriptor> tools) {
        for (WebToolDescriptor i : tools) {
            List<String> types = prepareTypesFilter(i.getFileParameters());
            gsClientTypes.put(i.getName(), types);
        }
    }
    
    static private List<String> prepareTypesFilter(List<FileParameter> params) {
        Set<GSDataFormat> superset = new HashSet<GSDataFormat>();
        List<String> types = new ArrayList<String>();
        for (FileParameter i : params) {
            superset.addAll(i.getFormats());
        }
        for (GSDataFormat i : superset) {
            types.add(i.getName());
        }
        return types;
    }

    public Set<String> getAvailableDataFormatNames(Set<GSDataFormat> dataFormats) {
        Set<String> formats = new HashSet<String>();
        for (GSDataFormat j : dataFormats) {
            formats.add(j.getName());
        }
        return formats;
    }

    public List<GsClientUrl> getGSClientURLs(Object gsSessionObj, GenomeSpaceFileInfo file)  {
        GSFileMetadata metadata = (GSFileMetadata) file.getMetadata();
        if (metadata == null) {
            log.error("Error getting metadata for " + file.getFilename() + " URL: " + file.getUrl());
        }
        GsSession gsSession = (GsSession) gsSessionObj;
        List<WebToolDescriptor> tools = getGSClients(gsSession);
        
        List<GsClientUrl> urls = new ArrayList<GsClientUrl>();
        for (WebToolDescriptor i : tools) {
            List<FileParameterWrapper> wrappers = prepareFileParameterWrappers(i.getFileParameters(), metadata);
            URL url = null;
            try {
                url = gsSession.getAnalysisToolManagerClient().getWebToolLaunchUrl(i, wrappers);
            }
            catch (InternalServerException e) {
                log.error("Error getting gs url. Session: " + gsSession + " WebToolDescriptor: " + i + " FileParameterWrappers: " + wrappers + " Message: " + e.getMessage());
            }
            urls.add(new GsClientUrl(i.getName(), url));
        }
        return urls;
    }


    private static List<FileParameterWrapper> prepareFileParameterWrappers(List<FileParameter> params, GSFileMetadata metadata) {
        List<FileParameterWrapper> wrappers = new ArrayList<FileParameterWrapper>();
        for (FileParameter i : params) {
            wrappers.add(new FileParameterWrapper(i, metadata));
        }
        return wrappers;
    }
    
    public List<GenomeSpaceDirectory> initUserDirs(Object gsSessionObj, Map<String, Set<TaskInfo>> kindToModules, Map<String, List<String>> gsClientTypes, Map<String, List<GsClientUrl>> clientUrls) {
        List<GenomeSpaceDirectory> userDirs = new ArrayList<GenomeSpaceDirectory>();
        GsSession gsSession = (GsSession) gsSessionObj;
        if ((gsSession == null) || (! gsSession.isLoggedIn())) return userDirs;            
        DataManagerClient dmClient = gsSession.getDataManagerClient();
        GSDirectoryListing rootDir = dmClient.listDefaultDirectory();
        GenomeSpaceDirectory userDir = new GenomeSpaceDirectoryImpl(gsSessionObj, "GenomeSpace Files", 0, rootDir, dmClient, kindToModules, gsClientTypes, clientUrls); 
        userDirs.add(userDir);
        return userDirs;
    }

    //TODO: simplify the interface, by removing kindToModules, gsClientTypes, and clientUrls args from initUserDirs
    //TODO: create new GsFileFactory class which initializes the tree, rather than in the GenomeSpaceDirectory constructor
   
}
