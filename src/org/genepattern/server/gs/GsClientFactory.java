package org.genepattern.server.gs;

import java.io.InputStream;
import java.net.URL;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;

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
     * Factory method for the GenomeSpaceBeanHelper interface
     * @return a new instanceof a GenomeSpaceBeanHelper
     */
    static public GenomeSpaceBeanHelper getNewGenomeSpaceBeanHelper() throws GsClientException {
        GenomeSpaceBeanHelper gsHelper = null;
        Class classDefinition;
        try {
            classDefinition = Class.forName("org.genepattern.server.gs.GenomeSpaceBeanHelperImpl");
            gsHelper = (GenomeSpaceBeanHelper) classDefinition.newInstance();
            return gsHelper;
        }
        catch (ClassNotFoundException e) {
            log.error("ClassNotFoundException creating GenomeSpaceBeanHelper through reflection", e);
        }
        catch (InstantiationException e) {
            log.error("InstantiationException creating GenomeSpaceBeanHelper through reflection", e);
        }
        catch (IllegalAccessException e) {
            log.error("IllegalAccessException creating GenomeSpaceBeanHelper through reflection", e);
        }
        catch (Throwable t) {
            log.error("Error initializing GenomeSpaceBeanHelper: "+t.getLocalizedMessage(), t);
        }
        
        throw new GsClientException("Error initializing GenomeSpaceBeanHelper, there are server errors which prevent you from using GenomeSpace");
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
