/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.plugin;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.GpServerProperties;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.plugin.dao.PatchInfoDao;

/**
 * Store the registry of installed plugins into the GenePattern Database.
 * 
 * @author pcarr
 *
 */
public class PluginRegistryGpDb implements PluginRegistry {
    private static final Logger log = Logger.getLogger(PluginRegistryGpDb.class);
    
    private final HibernateSessionManager mgr;

    /** @deprecated */
    public PluginRegistryGpDb() {
        this(org.genepattern.server.database.HibernateUtil.instance());
    }

    public PluginRegistryGpDb(final HibernateSessionManager mgr) {
        this.mgr=mgr;
    }

    @Override
    public List<PatchInfo> getInstalledPatches(GpConfig gpConfig, GpContext gpContext) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("getting installed patches...");
        }
        return new PatchInfoDao(mgr).getInstalledPatches();
    }

    @Override
    public boolean isInstalled(GpConfig gpConfig, GpContext gpContext, PatchInfo patchInfo) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("isInstalled, patchLsid="+patchInfo);
        }
        PatchInfo inDb=new PatchInfoDao(mgr).selectPatchInfoByLsid(patchInfo.getLsid());
        if (inDb!=null) {
            return true;
        }
        return false;
    }

    @Override
    public void recordPatch(GpConfig gpConfig, GpContext gpContext, PatchInfo patchInfo) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("recordPatch, patchLsid="+patchInfo);
        }
        new PatchInfoDao(mgr).recordPatch(patchInfo);
        if (patchInfo.hasCustomProps()) {
            if (log.isDebugEnabled()) {
                log.debug("updating custom.properties for patch, patchLsid="+patchInfo);
            }
            final File customPropsFile=new File(gpConfig.getResourcesDir(), "custom.properties");
            final boolean skipExisting=true;
            GpServerProperties.updateCustomProperties(customPropsFile, patchInfo.getCustomProps(), "adding custom properties from patch, lsid="+patchInfo.getLsid(), skipExisting);
        }
    } 
}
