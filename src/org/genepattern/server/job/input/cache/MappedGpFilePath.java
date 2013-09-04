package org.genepattern.server.job.input.cache;

import java.io.File;

import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.serverfile.ServerFilePath;

/**
 * Represent a mapped local file as a ServerFilePath object, with one change,
 * it can be read by all users of the system.
 * 
 * @author pcarr
 *
 */
public class MappedGpFilePath extends ServerFilePath {
    public MappedGpFilePath(final File localPath) {
        super(localPath);
    }
    
    @Override
    public boolean canRead(final boolean isAdmin, final Context userContext) {
        //by definition this file is readable by all users
        return true;
    }
    
}
