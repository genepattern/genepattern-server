/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.genomespace;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;

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

    static public boolean isGenomeSpaceEnabled(final GpContext context) {
        return ServerConfigurationFactory.instance().getGPBooleanProperty(context, "genomeSpaceEnabled", true);
    }
    
    static public String getGenomeSpaceEnvironment(final GpContext context) {
        return ServerConfigurationFactory.instance().getGPProperty(context, "genomeSpaceEnvironment", "prod");
    }

    /**
     * This method initializes the singleton instance of a GsClient interface.
     * Make sure to not load any Java 6 dependent classes until the first time this method is called.
     * @return an instance of the GsClient interface
     */
    static public GenomeSpaceClient instance() {
        return GenomeSpaceClientSingleton.instance();
    }
}

class GenomeSpaceClientSingleton {
    public static Logger log = Logger.getLogger(GenomeSpaceClientSingleton.class);
    static GenomeSpaceClient gsClient = new GenomeSpaceClient();

    private GenomeSpaceClientSingleton() {}
    
    public static GenomeSpaceClient instance() {
        return gsClient;
    }
}


