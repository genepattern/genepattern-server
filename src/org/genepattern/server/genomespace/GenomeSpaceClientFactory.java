package org.genepattern.server.genomespace;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFilePath;

/**
 * GenomeSpace integration, wrapper class. This is part of the core of GenePattern,
 * however, to allow for a GP server to run on a Java 5 VM, the actual GS implementation
 * will be loaded by reflection only when GS is enabled.
 * 
 * This class is a factory for instances which implement GenomeSpace integration interfaces.
 * 
 * @author pcarr
 */
public class GenomeSpaceClientFactory {
    public static Logger log = Logger.getLogger(GenomeSpaceClientFactory.class);

    static public boolean isGenomeSpaceEnabled(Context context) {
        return ServerConfiguration.instance().getGPBooleanProperty(context, "genomeSpaceEnabled", true);
    }
    
    static public String getGenomeSpaceEnvironment(Context context) {
        return ServerConfiguration.instance().getGPProperty(context, "genomeSpaceEnvironment", "prod");
    }

    /**
     * This method initializes the singleton instance of a GsClient interface.
     * Make sure to not load any Java 6 dependent classes until the first time this method is called.
     * @return an instance of the GsClient interface
     */
    static public GenomeSpaceClient getGenomeSpaceClient() {
        return GenomeSpaceClientSingleton.instance();
    }
}

class GenomeSpaceClientSingleton {
    public static Logger log = Logger.getLogger(GenomeSpaceClientSingleton.class);
    static GenomeSpaceClient gsClient = initGenomeSpaceClient();
    final static public String gsClientClassname = "org.genepattern.server.genomespace.impl.GenomeSpaceClientImpl";

    private GenomeSpaceClientSingleton() {}
    
    public static GenomeSpaceClient instance() {
        return gsClient;
    }
    
    private static GenomeSpaceClient initGenomeSpaceClient() {
        log.info("intializing GsClient ...");
        //initialize by reflection the gs client instance
        if (gsClientClassname == null) {
            log.error("Error initializing GsClient: gsClientClassname is null");
        }
        else {
            try {
                Class<?> gsClientClass = Class.forName(gsClientClassname);
                if (GenomeSpaceClient.class.isAssignableFrom( gsClientClass )) {
                    GenomeSpaceClient gsClient = (GenomeSpaceClient) gsClientClass.newInstance();
                    log.info(" ... done!");
                    return gsClient;
                }
            }
            catch (Throwable t) {
                log.error("Error initializing GsClient: "+t.getLocalizedMessage(), t);
            }
        }
        return createDummyImpl();
    }

    /**
     * If there are errors initializing the GS client, create a dummy implementation
     * which does nothing.
     * 
     * @return
     */
    private static GenomeSpaceClient createDummyImpl() {
        return new GenomeSpaceClient() {
            public InputStream getInputStream(String gpUserId, URL url) throws GenomeSpaceException { return null; }
            public GenomeSpaceLogin submitLogin(String env, String username, String password) throws GenomeSpaceException { return null; }
            public boolean isLoggedIn(Object gsSession) { return false; }
            public void logout(Object gsSession) {}
            public void registerUser(String env, String username, String password, String regEmail) throws GenomeSpaceException {}
            public GenomeSpaceFile buildFileTree(Object gsSession) { return null; }
            public Date getModifiedFromMetadata(Object metadata) { return null; }
            public Long getSizeFromMetadata(Object metadata) { return null; }
            public boolean deleteFile(Object gsSessionObject, GenomeSpaceFile file) throws GenomeSpaceException { return false; }
            public Set<String> getAvailableFormats(Object metadata) { return null; }
            public void saveFileToGenomeSpace(Object gsSessionObj, GpFilePath savedFile, GenomeSpaceFile directory) throws GenomeSpaceException {}
            public Map<String, Set<String>> getKindToTools(Object gsSession) { return null; }
            public URL getSendToToolUrl(Object gsSession, GenomeSpaceFile file, String toolName) throws GenomeSpaceException { return null; }
            public GenomeSpaceLogin submitLogin(String env, String token) throws GenomeSpaceException { return null; }
            public void createDirectory(Object gsSessionObject, String dirName, GenomeSpaceFile parentDir) throws GenomeSpaceException {}
            public URL getConvertedURL(Object gsSessionObject, GenomeSpaceFile file, String format) throws GenomeSpaceException { return null; }
            public Object obtainMetadata(Object gsSessionObject, URL gsUrl) throws GenomeSpaceException { return null; }
            public GenomeSpaceFile buildDirectory(Object gsSessionObject, Object metadataObject) throws GenomeSpaceException { return null; }
        };
    }
}


