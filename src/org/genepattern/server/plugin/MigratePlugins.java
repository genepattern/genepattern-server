/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.plugin;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.GpServerProperties;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.domain.PropsTable;

/**
 * Helper class for migrating the list of installed plugins (aka patches) into the GP database.
 * 
 * This scans the file system and adds a new record to the database for each installed plugin
 * in the rootPluginDir. The scan goes one level deep. 
 * 
 * The intention is for this to be done as a one-time thing after updating GP from 3.9.1 to 3.9.2.
 * It can also be forced to be invoked by removing the 'sync.installedPatchLSIDs.complete' entry from the PROPS table.
 * 
 * @author pcarr
 *
 */
public class MigratePlugins {
    private static Logger log = Logger.getLogger(MigratePlugins.class);
    
    /**
     * This 'sync.installedPatchLSIDs.complete' key in the PROPS table database indicates that the plugins have already been migrated. 
     */
    public static final String PROP_DB_CHECK="sync.installedPatchLSIDs.complete";

    private final HibernateSessionManager mgr;
    private final GpConfig gpConfig;
    private final GpContext gpContext;
    private PluginRegistry pluginRegistry;
    private List<PatchInfo> patchInfos=new ArrayList<PatchInfo>();

    public MigratePlugins(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext gpContext) {
        this.mgr=mgr;
        this.gpConfig=gpConfig;
        this.gpContext=gpContext;
        this.pluginRegistry=PluginManagerLegacy.initDefaultPluginRegistry(mgr, gpConfig, gpContext);
    }

    public MigratePlugins(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext gpContext, final PluginRegistry pluginRegistry) {
        this.mgr=mgr;
        this.gpConfig=gpConfig;
        this.gpContext=gpContext;
        this.pluginRegistry=pluginRegistry;
    }
    
    /**
     * Call this on server startup to migrate from the GP <= 3.9.1 'genepattern.properties' based
     * method of saving the list of installed patches to the GP >= 3.9.2 database based
     * method.
     *  
     * @throws Exception
     */
    public void migratePlugins() throws Exception {
        if (checkDb()) {
            return;
        }
        log.info("scanning file system for installed plugins ...");
        scanRootPluginDir();
        log.info("adding installed plugins to database ...");
        for(final PatchInfo patchInfo : patchInfos) {
            log.info("adding plugin: "+patchInfo.getLsid());
            pluginRegistry.recordPatch(gpConfig, gpContext, patchInfo);
        }
        log.info("\trecording status to db");
        updateDb();
        log.info("reloading configuration");
        ServerConfigurationFactory.reloadConfiguration();
    }
    
    /**
     * Check the db, has the {@link #PROP_DB_CHECK} flag already been set.
     */
    protected boolean checkDb() {
        String val="";
        try {
            val=PropsTable.selectValue(mgr, PROP_DB_CHECK);
        }
        catch (DbException e) {
            val="";
        }
        Boolean isComplete=Boolean.valueOf(val);
        if (isComplete) {
            return true;
        }
        return false;
    }

    /**
     * Update the db to indicate that the plugins have already been migrated.
     * @return
     */
    protected boolean updateDb() 
    throws DbException
    {
        boolean success=PropsTable.saveProp(mgr, PROP_DB_CHECK, "true");
        return success;
    }

    /**
     * Get the read-only list of installed patches loaded from the file system, ordered by the lastModified date
     * of the manifest file. This gets populated after calling 'scanRootPluginDir'.
     * 
     * @return
     */
    protected List<PatchInfo> getPatchInfos() {
        return Collections.unmodifiableList(patchInfos);
    }

    /**
     * Scan the file system for all installed plugins (aka patches). 
     * This call resets the set of pathInfos.
     * 
     * @throws Exception
     */
    protected void scanRootPluginDir() throws Exception {
        this.patchInfos.clear();
        scanPluginDir(gpConfig.getRootPluginDir(gpContext));
    }

    /**
     * Scan the file system starting at the given top level directory.
     * This call does not reset the set of pathInfos.
     * 
     * @param rootPluginDir
     * @throws Exception
     */
    protected void scanPluginDir(final File rootPluginDir) throws Exception {
        for(final File file : rootPluginDir.listFiles()) {
            File manifest=checkForManifest(file);
            if (manifest != null) {
                visitPluginManifest(manifest);
            }
        }
        // sort the patchInfos by modification date
        Collections.sort(patchInfos, new Comparator<PatchInfo>() {
        @Override
        public int compare(PatchInfo o1, PatchInfo o2) {
            Long t1 = o1.getStatusDate() == null ? null : o1.getStatusDate().getTime();
            Long t2 = o2.getStatusDate() == null ? null : o2.getStatusDate().getTime();
            if (t1==null) {
                if (t2==null) {
                    return 0;
                }
                return -1;
            }
            return t1.compareTo(t2);
        }
        });

    }
    
    /**
     * Visit a potential plugin directory in the file system. If it is a plugin
     * add it to the set of patchInfos and scan for potential custom properties.
     * 
     * @param file, a potential parent directory
     * @return the manifest File, or null if this is not a plugin directory
     */
    protected File checkForManifest(final File file) {
        if (!file.isDirectory()) {
            return null;
        }
        File[] manifests=file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return (pathname.isFile() && pathname.getName().equals("manifest"));
            }
        });
        
        if (manifests != null && manifests.length==1) {
            return manifests[0];
        }
        return null;
    }

    protected void visitPluginManifest(File manifest) throws FileNotFoundException, IOException, MalformedURLException {
        PatchInfo patchInfo=initPatchInfoFromManifest(manifest);
        if (patchInfo != null) {
            patchInfos.add(patchInfo);
        }
    }
    
    /**
     * Initialize a PatchInfo instance for the given patch manifest file.
     * 
     * @param manifest
     * @param customPropFiles, for logging purposes, append any custom prop files to this arg
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static PatchInfo initPatchInfoFromManifest(final File manifest) throws FileNotFoundException, IOException {
        // get the LSID from the manifest file
        Properties props=new Properties();
        FileReader fr=null;
        try {
            fr = new FileReader(manifest);
            props.load(fr);
        }
        finally {
            if (fr != null) {
                fr.close();
            }
        }
        props.load(new FileReader(manifest));
        String patchLsid=props.getProperty("LSID");

        PatchInfo patchInfo=new PatchInfo(patchLsid);
        patchInfo.setPatchDir(manifest.getParentFile().getAbsolutePath());
        patchInfo.setStatusDate(new Date(manifest.lastModified()));
        
        // list custom properties
        File[] customPropFilesArray=listCustomProperties(manifest.getParentFile());
        for(final File customPropFile : customPropFilesArray) {
            Properties customProps=GpServerProperties.loadProps(customPropFile);
            patchInfo.addCustomProps(customPropFile, customProps);
            patchInfo.addCustomProps(customProps);
        }
        return patchInfo;
    }

    protected static File[] listCustomProperties(File dir) {
        return dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.getName().endsWith(".custom.properties")) {
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * For testing/debugging, get the custom properties from each installed patch.
     * @return
     */
    protected Properties collectPluginCustomProps() {
        Properties pluginCustomProps=new Properties();
        for(final PatchInfo patchInfo : patchInfos) {
            if (patchInfo.getCustomProps()!=null) {
                pluginCustomProps.putAll(patchInfo.getCustomProps());
            }
        }
        return pluginCustomProps;
    }
    
    /**
     * For testing/debugging, get the list of custom properties files from each installed patch.
     * @return
     */
    protected List<File> collectPluginCustomPropFiles() {
        List<File> all=new ArrayList<File>();
        for(final PatchInfo patchInfo : patchInfos) {
            all.addAll( patchInfo.getCustomPropFiles() );
        }
        return all;        
    }

}
