package org.genepattern.server.plugin;

import java.util.List;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;

/**
 * Interface for getting the list of installed plugins (aka patches) and adding new entries to the list.
 * 
 * @author pcarr
 *
 */
public interface PluginRegistry {

    /**
     * Get the list of installed patches.
     * @return
     */
    List<PatchInfo> getInstalledPatches(final GpConfig gpConfig, final GpContext gpContext) throws Exception;

    /**
     * 
     * @param gpConfig
     * @param gpContext
     * @param patchLsid
     * @return true if the given patch is installed.
     */
    boolean isInstalled(final GpConfig gpConfig, final GpContext gpContext, final PatchInfo patchInfo) throws Exception;
    
    /**
     * Add the given patchLsid to the list of installed patches. Make sure this value persists 
     * through a server restart and/or a server update.
     * 
     * @param gpConfig
     * @param gpContext
     * @param patchLsid
     */
    void recordPatch(final GpConfig gpConfig, final GpContext gpContext, final PatchInfo patchInfo) throws Exception;
}
