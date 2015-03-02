package org.genepattern.server.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;


/**
 * Legacy GP <= 3.9.1 implementation of the plugin registry, which saves state to the genepattern.properties file.
 * @author pcarr
 *
 */
public class PluginRegistrySystemProps implements PluginRegistry {
    private static final Logger log = Logger.getLogger(PluginRegistrySystemProps.class);

    @Override
    public List<PatchInfo> getInstalledPatches(GpConfig gpConfig, GpContext gpContext) throws MalformedURLException {
        final String installedPatches = System.getProperty(GPConstants.INSTALLED_PATCH_LSIDS);
        String[] installedPatchLSIDs = new String[0];
        if (installedPatches != null) {
            installedPatchLSIDs = installedPatches.split(",");
        }
        List<PatchInfo> patchInfos=new ArrayList<PatchInfo>();
        for(final String patchLsid : installedPatchLSIDs) {
            patchInfos.add(new PatchInfo(patchLsid, null));
        }
        return patchInfos;
    }

    @Override
    public boolean isInstalled(final GpConfig gpConfig, final GpContext gpContext, final PatchInfo patchInfo)  throws MalformedURLException {
        final List<PatchInfo> installedPatches=getInstalledPatches(gpConfig, gpContext);
        return isInstalled(patchInfo, installedPatches);
    }
    
    protected boolean isInstalled(final PatchInfo requiredPatch, List<PatchInfo> installedPatchInfos) throws MalformedURLException {
        for(final PatchInfo installedPatchInfo : installedPatchInfos) {
            final LSID installedPatchLsid = installedPatchInfo.getPatchLsid();
            if (installedPatchLsid.isEquivalent(requiredPatch.getPatchLsid())) {
                // there are installed patches, and there is an LSID match to this one
                log.info(requiredPatch.getPatchLsid().toString() + " is already installed");
                return true;
            }
        }
        return false;
    }

    /**
     * record the patch LSID in the genepattern.properties file
     */
    @Override    
    public synchronized void recordPatch(final GpConfig gpConfig, final GpContext gpContext, final PatchInfo patchInfo) throws Exception {
        // add this LSID to the installed patches repository
        String installedPatches = System.getProperty(GPConstants.INSTALLED_PATCH_LSIDS);
        if (installedPatches == null || installedPatches.length() == 0) {
            installedPatches = "";
        } 
        else {
            installedPatches = installedPatches + ",";
        }
        installedPatches = installedPatches + patchInfo.getPatchLsid();
        System.setProperty(GPConstants.INSTALLED_PATCH_LSIDS, installedPatches);
        Properties props = new Properties();
        props.load(new FileInputStream(new File(System.getProperty("resources"), "genepattern.properties")));

        // make sure any changes are properly set in the System props
        props.setProperty(GPConstants.INSTALLED_PATCH_LSIDS, installedPatches);
        props.store(new FileOutputStream(new File(System.getProperty("resources"), "genepattern.properties")), "added installed patch LSID");

        for (Iterator iter = props.keySet().iterator(); iter.hasNext();) {
            String k = (String) iter.next();
            String v = (String) props.get(k);
            System.setProperty(k, v);
        }
    }

}
