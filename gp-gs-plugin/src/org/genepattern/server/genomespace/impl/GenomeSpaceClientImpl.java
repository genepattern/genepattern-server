package org.genepattern.server.genomespace.impl;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.genomespace.GenomeSpaceBean;
import org.genepattern.server.genomespace.GenomeSpaceClient;
import org.genepattern.server.genomespace.GenomeSpaceDatabaseManager;
import org.genepattern.server.genomespace.GenomeSpaceException;
import org.genepattern.server.genomespace.GenomeSpaceFile;
import org.genepattern.server.genomespace.GenomeSpaceFileManager;
import org.genepattern.server.genomespace.GenomeSpaceLogin;
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
 * Implementation of the GenomeSpaceClient interface.
 * This class, and related classes directly reference classes in the GenomeSpace CDK.
 */
public class GenomeSpaceClientImpl implements GenomeSpaceClient {
	private static Logger log = Logger.getLogger(GenomeSpaceClientImpl.class);
	
    public InputStream getInputStream(String gpUserId, URL url) throws GenomeSpaceException {
        InputStream inputStream = null;
        String token = GenomeSpaceDatabaseManager.getGSToken(gpUserId);
        
        if (token == null) {
            throw new GenomeSpaceException("Unable to get the GenomeSpace session token needed to access GenomeSpace files");
        }
        else {
            GsSession session;
            try {
                session = new GsSession(token);
            }
            catch (InternalServerException e) {
                throw new GenomeSpaceException("Unable to initialize GenomeSpace session", e);
            }
            DataManagerClient dmc = session.getDataManagerClient();
            inputStream = dmc.getInputStream(url);
        }
        return inputStream;
    }

	public GenomeSpaceLogin submitLogin(String env, String username, String password) throws GenomeSpaceException {
		if (env == null) {
            log.error("Environment for GenomeSpace not set");
            env = "test";
        }
        
        if (username == null) {
            throw new GenomeSpaceException("Username must be set");
        }

        try {
           ConfigurationUrls.init(env);
           GsSession gsSession = new GsSession();
           User gsUser = gsSession.login(username, password);

           GenomeSpaceLogin response = new GenomeSpaceLogin();
           response.setAttributes(new HashMap<String,Object>());
           response.getAttributes().put(GenomeSpaceBean.GS_USER_KEY, gsUser);
           response.getAttributes().put(GenomeSpaceBean.GS_SESSION_KEY, gsSession);
           response.setAuthenticationToken(gsSession.getAuthenticationToken());
           response.setUnknownUser(false);
           response.setUsername(gsUser.getUsername());
           response.setEmail(gsUser.getEmail());
           return response;
        }  
        catch (AuthorizationException e) {
            throw new GenomeSpaceException("Authentication error, please check your username and password.");
        } 
        catch (Exception e) {
            throw new GenomeSpaceException("An error occurred logging in to GenomeSpace.  Please contact the GenePattern administrator. Error was: "+ e.getLocalizedMessage(), e);
        }
	}

	public boolean isLoggedIn(Object gsSessionObject) {
		GsSession gsSession = null;
        if (gsSessionObject instanceof GsSession) {
            gsSession = (GsSession) gsSessionObject;
        }
        return ((gsSession != null) && (gsSession.isLoggedIn()));
	}

    public void logout(Object gsSessionObject) {
        GsSession gsSession = null;
        if (gsSessionObject instanceof GsSession) {
            gsSession = (GsSession) gsSessionObject;
        }
        if (gsSession == null) {
            log.error("Invalid arg, gsSessionObj is null or not an instanceof GsSession, ignoring logout request");
        }
        gsSession.logout();
    }

    public void registerUser(String env, String username, String password, String regEmail) throws GenomeSpaceException {
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
            throw new GenomeSpaceException("Error registering GenomeSpace account for username: " + username + ": " + t.getLocalizedMessage());
        }
    }

    public GenomeSpaceFile buildFileTree(Object gsSessionObject) {
        GsSession gsSession = null;
        if (gsSessionObject instanceof GsSession) {
            gsSession = (GsSession) gsSessionObject;
        }
        else {
            log.error("Object other than GsSession passed into buildFileTree: " + gsSessionObject);
            return null;
        }
        
        DataManagerClient dmClient = gsSession.getDataManagerClient();
        GSDirectoryListing root = dmClient.listDefaultDirectory();
        return buildDirectory(dmClient, root, root.getDirectory());
    }
    
    private GenomeSpaceFile buildDirectory(DataManagerClient dmClient, GSDirectoryListing dir, GSFileMetadata metadata) {
        GenomeSpaceFile directoryFile = (GenomeSpaceFile) GenomeSpaceFileManager.createFile(metadata.getUrl(), metadata);
        directoryFile.setKind(GenomeSpaceFile.DIRECTORY_KIND);
        directoryFile.setChildFiles(new HashSet<GenomeSpaceFile>());
        
        for (GSFileMetadata i : dir.findDirectories()) {
            GenomeSpaceFile aFile = (GenomeSpaceFile) buildDirectory(dmClient, dmClient.list(i), i);
            directoryFile.getChildFiles().add(aFile);
        }
        
        for (GSFileMetadata i : dir.findFiles()) {
            GenomeSpaceFile aFile = (GenomeSpaceFile) GenomeSpaceFileManager.createFile(i.getUrl(), i);
            directoryFile.getChildFiles().add(aFile);
        }
        
        return directoryFile;
    }

    public Date getModifiedFromMetadata(Object metadataObject) {
        GSFileMetadata metadata = null;
        if (metadataObject instanceof GSFileMetadata) {
            metadata = (GSFileMetadata) metadataObject;
        }
        else {
            log.error("Object other than GSFileMetadata passed into getModifiedFromMetadata: " + metadataObject);
            return null;
        }
        
        return metadata.getLastModified();
    }

    public Long getSizeFromMetadata(Object metadataObject) {
        GSFileMetadata metadata = null;
        if (metadataObject instanceof GSFileMetadata) {
            metadata = (GSFileMetadata) metadataObject;
        }
        else {
            log.error("Object other than GSFileMetadata passed into getModifiedFromMetadata: " + metadataObject);
            return null;
        }
        
        return metadata.getSize();
    }

    public void deleteFile(Object gsSessionObject, GenomeSpaceFile file) throws GenomeSpaceException {
        GsSession gsSession = null;
        if (gsSessionObject instanceof GsSession) {
            gsSession = (GsSession) gsSessionObject;
        }
        else {
            log.error("Object other than GsSession passed into deleteFile: " + gsSessionObject);
            return;
        }

        GSFileMetadata metadata = (GSFileMetadata) file.getMetadata();
        gsSession.getDataManagerClient().delete(metadata);
    }

    public Set<String> getAvailableFormats(Object metadataObject) {
        GSFileMetadata metadata = null;
        if (metadataObject instanceof GSFileMetadata) {
            metadata = (GSFileMetadata) metadataObject;
        }
        else {
            log.error("Object other than GSFileMetadata passed into getAvailableFormats(): " + metadataObject);
            return null;
        }
        
        Set<GSDataFormat> dataFormats = metadata.getAvailableDataFormats();
        Set<String> formats = new HashSet<String>();
        for (GSDataFormat j : dataFormats) {
            formats.add(j.getName());
        }
        return formats;
    }

    public void saveFileToGenomeSpace(Object gsSessionObject, GpFilePath savedFile, GenomeSpaceFile directory) throws GenomeSpaceException {
        GsSession gsSession = null;
        if (gsSessionObject instanceof GsSession) {
            gsSession = (GsSession) gsSessionObject;
        }
        else {
            log.error("Object other than GsSession passed into saveFileToGenomeSpace(): " + gsSessionObject);
            return;
        }
        
        DataManagerClient dmClient = gsSession.getDataManagerClient();
        GSFileMetadata directoryMetadata = (GSFileMetadata) directory.getMetadata();
        dmClient.uploadFile(savedFile.getServerFile(), directoryMetadata);
    }
    
    /**
     * Get list of all GenomeSpace Tools
     * @param gsSession
     * @return
     */
    private List<WebToolDescriptor> getTools(GsSession gsSession) {
        try {
            return gsSession.getAnalysisToolManagerClient().getWebTools();
        }
        catch (InternalServerException e) {
            log.error("Error getting getAnalysisToolManagerClient().getWebTools().  Session: " + gsSession + " Message: " + e.getMessage());
            return null;
        }
    }

    public Map<String, Set<String>> getKindToTools(Object gsSessionObject) {
        GsSession gsSession = null;
        if (gsSessionObject instanceof GsSession) {
            gsSession = (GsSession) gsSessionObject;
        }
        else {
            log.error("Object other than GsSession passed into getKindToTools(): " + gsSessionObject);
            return null;
        }
        
        List<WebToolDescriptor> tools = getTools(gsSession);
        Map<String, Set<String>> kindToTools = new HashMap<String, Set<String>>();
        
        for (WebToolDescriptor tool : tools) { 
            // Remove GenePattern from the list
            if (tool.getName().equals("GenePattern")) {
                continue;
            }
            
            for (FileParameter parameter : tool.getFileParameters()) {
                for (GSDataFormat format : parameter.getFormats()) {
                    if (kindToTools.get(format.getName()) == null) {
                        kindToTools.put(format.getName(), new HashSet<String>());
                    }
                    kindToTools.get(format.getName()).add(tool.getName());
                }
            }
        }
        
        return kindToTools;
    }
    
    private List<FileParameterWrapper> prepareFileParameterWrappers(List<FileParameter> params, GSFileMetadata metadata) {
        List<FileParameterWrapper> wrappers = new ArrayList<FileParameterWrapper>();
        for (FileParameter i : params) {
            wrappers.add(new FileParameterWrapper(i, metadata));
        }
        return wrappers;
    }

    public URL getSendToToolUrl(Object gsSessionObject, GenomeSpaceFile file, String toolName) throws GenomeSpaceException {
        GsSession gsSession = null;
        if (gsSessionObject instanceof GsSession) {
            gsSession = (GsSession) gsSessionObject;
        }
        else {
            log.error("Object other than GsSession passed into getSendToToolUrl(): " + gsSessionObject);
            throw new GenomeSpaceException("Object other than GsSession passed into getSendToToolUrl(): " + gsSessionObject);
        }
        
        List<WebToolDescriptor> tools = getTools(gsSession);
        GSFileMetadata metadata = (GSFileMetadata) file.getMetadata();
        
        for (WebToolDescriptor tool : tools) { 
            // Remove GenePattern from the list
            if (tool.getName().equals(toolName)) {
                List<FileParameterWrapper> wrappers = prepareFileParameterWrappers(tool.getFileParameters(), metadata);
                try {
                    return gsSession.getAnalysisToolManagerClient().getWebToolLaunchUrl(tool, wrappers);
                }
                catch (InternalServerException e) {
                    log.error("Error getting the tool URL for the tool: " + tool.getName() + " file: " + file.getName());
                    throw new GenomeSpaceException(e.getMessage());
                }
            }
        }
        
        log.error("Unable to find a GenomeSpace tool matching the name: " + toolName);
        throw new GenomeSpaceException("Unable to find a GenomeSpace tool matching the name: " + toolName);
    }
}
