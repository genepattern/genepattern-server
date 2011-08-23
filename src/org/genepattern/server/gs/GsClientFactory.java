package org.genepattern.server.gs;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.webservice.TaskInfo;

/**
 * GenomeSpace integration, wrapper class. This is part of the core of GenePattern,
 * however, to allow for a GP server to run on a Java 5 VM, the actual GS implementation
 * will be loaded by reflection only when GS is enabled.
 * 
 * This class is a factory for instances which implement GenomeSpace integration interfaces.
 * 
 * @author pcarr
 */
public class GsClientFactory {
    public static Logger log = Logger.getLogger(GsClientFactory.class);

    static public boolean isGenomeSpaceEnabled(Context context) {
        return ServerConfiguration.instance().getGPBooleanProperty(context, "genomeSpaceEnabled", false);
    }

    /**
     * This method initializes the singleton instance of a GsClient interface.
     * Make sure to not load any Java 6 dependent classes until the first time this method is called.
     * @return an instance of the GsClient interface
     */
    static public GsClient getGsClient() {
        return GsClientSingleton.gsClient;
    }

    /**
     * Get the singleton instance of the GsClientUtil interface, for GenomeSpace integration.
     * @return
     */
    static public GsClientUtil getGsClientUtil() {
        return GsClientUtilSingleton.gsClientUtil;
    }
}

class GsClientSingleton {
    public static Logger log = Logger.getLogger(GsClientSingleton.class);
    static GsClient gsClient = initGsClient();
    final static public String gsClientClassname = "org.genepattern.server.gs.impl.GsClientImpl";

    private GsClientSingleton() {
    }
    
    private static GsClient initGsClient() {
        log.info("intializing GsClient ...");
        //initialize by reflection the gs client instance
        if (gsClientClassname == null) {
            log.error("Error initializing GsClient: gsClientClassname is null");
        }
        else {
            try {
                Class<?> gsClientClass = Class.forName(gsClientClassname);
                if (GsClient.class.isAssignableFrom( gsClientClass )) {
                    GsClient gsClient = (GsClient) gsClientClass.newInstance();
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
    private static GsClient createDummyImpl() {
        return new GsClient() {
            public boolean isGenomeSpaceFile(URL url) {
                return false;
            }
            public InputStream getInputStream(String gpUserId, URL url) throws GsClientException {
                return null;
            }
        };
    }
}

class GsClientUtilSingleton {
    public static Logger log = Logger.getLogger(GsClientUtilSingleton.class);
    static GsClientUtil gsClientUtil = initGsClientUtil();
    private static GsClientUtil initGsClientUtil() {
        log.info("initializing GsClientUtil ... ");
        try {
            Class<?> gsClientUtilClass = Class.forName("org.genepattern.server.gs.impl.GsClientUtilImpl");
            if (GsClientUtil.class.isAssignableFrom(gsClientUtilClass)) {
                GsClientUtil gsClientUtil = (GsClientUtil) gsClientUtilClass.newInstance();
                log.info(" ... done!");
                return gsClientUtil;
            }
        }
        catch (Throwable t) {
            log.error("Error initializing GsClientUtil: "+t.getLocalizedMessage(), t);
        }
        return new GsClientUtil() {

            public boolean isLoggedIn(Object gsSessionObj) {
                return false;
            }

            public GsLoginResponse submitLogin(String env, String username, String password) throws GsClientException {
                throw new GsClientException("GP - GenomeSpace configuration error");
            }

            public void registerUser(String env, String username, String password, String regEmail) throws GsClientException {
                throw new GsClientException("GP - GenomeSpace configuration error");
            }

            public void logout(Object gsSessionObj) {
            }

            public void deleteFile(Object gsSessionObj, GenomeSpaceFileInfo file) throws GsClientException {
                throw new GsClientException("GP - GenomeSpace configuration error");
            }

            public GenomeSpaceFileInfo saveFileToGenomeSpace(Object gsSessionObj, Map<String, List<String>> gsClientTypes, File in) throws GsClientException {
                throw new GsClientException("GP - GenomeSpace configuration error");
            }

            public List<WebToolDescriptorWrapper> getToolWrappers(Object gsSessionObj, Map<String, List<String>> gsClientTypes) {
                return Collections.emptyList();
            }

            public void initGsClientTypes(Object gsSessionObj, Map<String, List<String>> gsClientTypes) {
            }

            public List<GsClientUrl> getGSClientURLs(Object gsSessionObj, GenomeSpaceFileInfo file) {
                return Collections.emptyList();
            }

            public List<GenomeSpaceDirectory> initUserDirs(Object gsSessionObj, Map<String, Set<TaskInfo>> kindToModules, Map<String, List<String>> gsClientTypes, Map<String, List<GsClientUrl>> clientUrls) {
                return Collections.emptyList();
            }
        };
    }
}


