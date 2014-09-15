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
import org.genepattern.server.genomespace.GenomeSpaceClient;
import org.genepattern.server.genomespace.GenomeSpaceDatabaseManager;
import org.genepattern.server.genomespace.GenomeSpaceException;
import org.genepattern.server.genomespace.GenomeSpaceFile;
import org.genepattern.server.genomespace.GenomeSpaceFileManager;
import org.genepattern.server.genomespace.GenomeSpaceLogin;
import org.genepattern.server.genomespace.GenomeSpaceLoginManager;
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
	
	/**
	 * Returns an InputStream used to download a GenomeSpace file to the local GenePattern install
	 */
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
    
    /**
     * Submits login to GenomeSpace.  Intended for use when the user is manually logging in.
     */
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
           response.getAttributes().put(GenomeSpaceLoginManager.GS_USER_KEY, gsUser);
           response.getAttributes().put(GenomeSpaceLoginManager.GS_SESSION_KEY, gsSession);
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
            log.error("An error occurred logging in to GenomeSpace.  Please contact the GenePattern administrator. Error was: "+ e.getMessage());
            throw new GenomeSpaceException("An error occurred logging in to GenomeSpace.  Please contact the GenePattern administrator. Error was: "+ e.getLocalizedMessage(), e);
        }
	}
	
	/**
     * Submits login to GenomeSpace.  Intended for use when reconstructing a session from the token.
     */
	public GenomeSpaceLogin submitLogin(String env, String token) throws GenomeSpaceException {
	    if (env == null) {
            log.error("Environment for GenomeSpace not set");
            env = "test";
        }
        
        if (token == null) {
            throw new GenomeSpaceException("Token must be set");
        }
        
        try {
            ConfigurationUrls.init(env);
            GsSession gsSession = new GsSession(token);
            
            // Make a simple call to GenomeSpace to test the validity of the token
            gsSession.getAnalysisToolManagerClient().getWebTools();

            GenomeSpaceLogin response = new GenomeSpaceLogin();
            response.setAttributes(new HashMap<String,Object>());
            response.getAttributes().put(GenomeSpaceLoginManager.GS_SESSION_KEY, gsSession);
            response.setAuthenticationToken(gsSession.getAuthenticationToken());
            response.setUnknownUser(false);
            response.setUsername(gsSession.getCachedUsernameForSSO()); // Known to have stale data sometimes
            return response;
         }  
         catch (AuthorizationException e) {
             throw new GenomeSpaceException("Authentication error, please check your username and password.");
         } 
         catch (Exception e) {
             log.error("An error occurred logging in to GenomeSpace.  Please contact the GenePattern administrator. Error was: "+ e.getMessage());
             throw new GenomeSpaceException("An error occurred logging in to GenomeSpace.  Please contact the GenePattern administrator. Error was: "+ e.getLocalizedMessage(), e);
         }
    }

	/**
	 * Determines if a GenomeSpace session is currently logged in
	 */
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
    
    /**
     * Registers a new user with GenomeSpace using the provided information
     * @deprecated - We no longer directly handle GenomeSpace registration
     */
    @Deprecated
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
    
    /**
     * Constructs a tree of GenomeSpaceFiles representing the user's files on GenomeSpace
     */
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
        return buildDirectory(gsSession, root, root.getDirectory());
    }
    
    /**
     * Constructs a recursive representation as a directory as a GenomeSpaceFile
     * @return
     */
    public GenomeSpaceFile buildDirectory(Object gsSessionObject, Object metadataObject) {
        GsSession gsSession = null;
        if (gsSessionObject instanceof GsSession) {
            gsSession = (GsSession) gsSessionObject;
        }
        else {
            log.error("Object other than GsSession passed into buildDirectory: " + gsSessionObject);
            return null;
        }
        
        GSFileMetadata metadata = null;
        if (metadataObject instanceof GSFileMetadata) {
            metadata = (GSFileMetadata) metadataObject;
        }
        else {
            log.error("Object other than GSFileMetadata passed into buildDirectory: " + metadataObject);
            return null;
        }
        
        DataManagerClient dmClient = gsSession.getDataManagerClient();
        GSDirectoryListing dir = dmClient.list(metadata);
        
        return buildDirectory(gsSession, dir, metadata);
    }
    
    /**
     * Constructs a representation of a directory as a GenomeSpaceFile
     * @param dir
     * @param metadata
     * @return
     */
    private GenomeSpaceFile buildDirectory(Object gsSessionObject, GSDirectoryListing dir, GSFileMetadata metadata) {
        GsSession gsSession = null;
        if (gsSessionObject instanceof GsSession) {
            gsSession = (GsSession) gsSessionObject;
        }
        else {
            log.error("Object other than GsSession passed into buildDirectory: " + gsSessionObject);
            return null;
        }
        DataManagerClient dmClient = gsSession.getDataManagerClient();
        
        GenomeSpaceFile directoryFile = (GenomeSpaceFile) GenomeSpaceFileManager.createFile(gsSession, metadata.getUrl(), metadata);
        directoryFile.setKind(GenomeSpaceFile.DIRECTORY_KIND);
        directoryFile.setChildFiles(new HashSet<GenomeSpaceFile>());
        
        for (GSFileMetadata i : dir.findDirectories()) {
            GenomeSpaceFile aFile = (GenomeSpaceFile) GenomeSpaceFileManager.createFile(gsSession, i.getUrl(), i);
            aFile.setKind(GenomeSpaceFile.DIRECTORY_KIND);
            directoryFile.getChildFilesNoLoad().add(aFile);
        }
        
        for (GSFileMetadata i : dir.findFiles()) {
            GenomeSpaceFile aFile = (GenomeSpaceFile) GenomeSpaceFileManager.createFile(gsSession, i.getUrl(), i);
            directoryFile.getChildFilesNoLoad().add(aFile);
        }
    
        return directoryFile;
    }
    
    /**
     * Returns the last modified date of a file, given the file's metadata object
     */
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

    /**
     * Returns the size of a file given the file's metadata object
     */
    public Long getSizeFromMetadata(Object metadataObject) {
        GSFileMetadata metadata = null;
        if (metadataObject instanceof GSFileMetadata) {
            metadata = (GSFileMetadata) metadataObject;
        }
        else {
            log.error("Object other than GSFileMetadata passed into getModifiedFromMetadata: " + metadataObject);
            return null;
        }
        
        Long size = metadata.getSize();
        if (size == null) return 0L;
        else return size;
    }
    
    /**
     * Deletes a file from GenomeSpace given that file's GenomeSpaceFile object
     */
    public boolean deleteFile(Object gsSessionObject, GenomeSpaceFile file) throws GenomeSpaceException {
        GsSession gsSession = null;
        if (gsSessionObject instanceof GsSession) {
            gsSession = (GsSession) gsSessionObject;
        }
        else {
            log.error("Object other than GsSession passed into deleteFile: " + gsSessionObject);
            return false;
        }

        GSFileMetadata metadata = (GSFileMetadata) file.getMetadata();
        try {
            gsSession.getDataManagerClient().delete(metadata);
        }
        catch (Throwable t) {
            return false;
        }
        return true;
    }
    
    /**
     * Creates a GenomeSpace directory with the given name in the given parent directory
     * @throws org.genepattern.server.genomespace.GenomeSpaceException
     */
    public void createDirectory(Object gsSessionObject, String dirName, GenomeSpaceFile parentDir) throws GenomeSpaceException {
        GsSession gsSession = null;
        if (gsSessionObject instanceof GsSession) {
            gsSession = (GsSession) gsSessionObject;
        }
        else {
            log.error("Object other than GsSession passed into createDirectory: " + gsSessionObject);
            return;
        }
        
        DataManagerClient dmClient = gsSession.getDataManagerClient();
        GSFileMetadata metadata = (GSFileMetadata) parentDir.getMetadata();
        dmClient.createDirectory(metadata, dirName);
    }
    
    /**
     * Lists all available conversion formats for a file given the file's metadata object
     */
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
            formats.add(j.getFileExtension());
        }
        return formats;
    }
    
    /**
     * Saves a file local to the GenePattern server to GenomeSpace in the given GenomeSpace directory
     */
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
    
    /**
     * Takes a GenomeSpace session and returns a map of file kinds to GenomeSpace-enabled tools.
     * 
     * Note: Currently uses format.getName() as the file extension accepted by the tools.  When 
     * the CDK changes this likely won't be correct and something like format.getExtension() 
     * should be used instead.
     */
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
    
    /**
     * Given a list of parameters for a file and that file's metadata, this will wrap the parameters in a way that 
     * AnalysisToolManagerClient.getWebToolLaunchUrl() understands.
     * @param params
     * @param metadata
     * @return
     */
    private List<FileParameterWrapper> prepareFileParameterWrappers(List<FileParameter> params, GSFileMetadata metadata) {
        List<FileParameterWrapper> wrappers = new ArrayList<FileParameterWrapper>();
        for (FileParameter i : params) {
            wrappers.add(new FileParameterWrapper(i, metadata));
        }
        return wrappers;
    }
    
    /**
     * Get the URL for sending a specified GenomeSpace file to a specified GenomeSpace tool.
     * 
     * Note: Will automatically convert the file to one the tool accepts, is available.  If multiple formats are available it 
     * will arbitrarily pick the first one.  This oddity is part of the GenomeSpace implementation which can cause errors in 
     * the UI if we are not aware of it.  Hopefully they will fix this in future versions of the CDK.
     */
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
    
    /**
     * Gets the URL to the GenomeSpace file provided the file and the given format for converting the file
     */
    public URL getConvertedURL(Object gsSessionObject, GenomeSpaceFile file, String formatType) throws GenomeSpaceException {
        GsSession gsSession = null;
        if (gsSessionObject instanceof GsSession) {
            gsSession = (GsSession) gsSessionObject;
        }
        else {
            log.error("Object other than GsSession passed into getConvertedURL(): " + gsSessionObject);
            throw new GenomeSpaceException("Object other than GsSession passed into getConvertedURL(): " + gsSessionObject);
        }
        
        // Declare necessary objects
        DataManagerClient dmClient = gsSession.getDataManagerClient();
        GSFileMetadata metadata = (GSFileMetadata) file.getMetadata();
        
        // Handle the null formatType condition
        if (formatType == null) {
            formatType = file.getExtension();
        }
        
        // If converting to the file's base type, simply return the URL
        if (formatType.equals(file.getExtension())) {
            try {
                return file.getUrl();
            }
            catch (Exception e) {
                log.error("Unable to get base URL in getConvertedURL(): " + file);
                throw new GenomeSpaceException("Unable to get base URL in getConvertedURL(): " + file);
            }
        }
        
        // Find the correct GSDataFormat object
        GSDataFormat format = null;
        for (GSDataFormat i : metadata.getAvailableDataFormats()) {
            if (i.getFileExtension().equals(formatType)) {
                format = i;
            }
        }
        
        // Check if the format wasn't found
        if (format == null) {
            log.error("Unable to find an appropriate GSDataFormat object for: " + formatType);
            throw new GenomeSpaceException("Unable to convert the file " + file.getName() + " to: " + formatType);
        }
        
        // Get the URL
        return dmClient.getFileUrl(metadata, format);
    }

    public Object obtainMetadata(Object gsSessionObject, URL gsUrl) throws GenomeSpaceException {
        GsSession gsSession = null;
        if (gsSessionObject instanceof GsSession) {
            gsSession = (GsSession) gsSessionObject;
        }
        else {
            log.error("Object other than GsSession passed into obtainMetadata(): " + gsSessionObject);
            throw new GenomeSpaceException("Object other than GsSession passed into obtainMetadata(): " + gsSessionObject);
        }
        
        // Declare necessary objects
        DataManagerClient dmClient = gsSession.getDataManagerClient();
        return dmClient.getMetadata(gsUrl.toString());
    }

    public String getToken(Object session) throws GenomeSpaceException {
        GsSession gsSession = null;
        if (session instanceof GsSession) {
            gsSession = (GsSession) session;
        }
        else {
            log.error("Object other than GsSession passed into obtainMetadata(): " + session);
            throw new GenomeSpaceException("Object other than GsSession passed into obtainMetadata(): " + session);
        }
        
        return gsSession.getAuthenticationToken();
    }
}
