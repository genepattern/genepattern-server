/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.plugin;

import static org.genepattern.util.GPConstants.COMMAND_LINE;
import static org.genepattern.util.GPConstants.MANIFEST_FILENAME;
import static org.genepattern.util.GPConstants.PATCH_ERROR_EXIT_VALUE;
import static org.genepattern.util.GPConstants.PATCH_SUCCESS_EXIT_VALUE;
import static org.genepattern.util.GPConstants.REQUIRED_PATCH_LSIDS;
import static org.genepattern.util.GPConstants.REQUIRED_PATCH_URLS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.executor.JobDispatchException;
import org.genepattern.server.genepattern.CommandLineParser;
import org.genepattern.server.genepattern.FileDownloader;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.server.webservice.server.Status;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

import com.google.common.base.Strings;

/**
 * Duplicate patch installation functions from GenePatternAnalysisTask class, circa GP 3.3.3.
 * 
 * @author pcarr
 */
public class PluginManagerLegacy {
    private static Logger log = Logger.getLogger(PluginManagerLegacy.class);
    
    private final HibernateSessionManager mgr;
    private GpConfig gpConfig;
    private final GpContext gpContext;
    private final PluginRegistry pluginRegistry;
    
    public PluginManagerLegacy(HibernateSessionManager mgr, GpConfig gpConfig, GpContext gpContext) {
        this(mgr, gpConfig, gpContext, initDefaultPluginRegistry(mgr, gpConfig, gpContext));
    }

    public PluginManagerLegacy(HibernateSessionManager mgr, GpConfig gpConfig, GpContext gpContext, PluginRegistry pluginRegistry) {
        this.mgr=mgr;
        this.gpConfig=gpConfig;
        this.gpContext=gpContext;
        this.pluginRegistry=pluginRegistry;
    }
    
    public static final List<PatchInfo> getInstalledPatches(final String installedPatches) throws MalformedURLException {
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
    
    public static PluginRegistry initDefaultPluginRegistry(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext gpContext) {
        return new PluginRegistryGpDb(mgr);
    }
    
    /**
     * Get the list of required patches for the given task.
     * @param taskInfo
     * @return
     */
    protected static List<PatchInfo> getRequiredPatches(final TaskInfo taskInfo) throws JobDispatchException {
        final TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
        String requiredPatchLSID = tia.get(REQUIRED_PATCH_LSIDS);
        String requiredPatchURL = tia.get(REQUIRED_PATCH_URLS);
        return getRequiredPatches(requiredPatchLSID, requiredPatchURL);
    }
    
    protected static List<PatchInfo> getRequiredPatches(final String requiredPatchLSIDs, final String requiredPatchURLs) 
    throws JobDispatchException {
        // no patches required?
        if (requiredPatchLSIDs == null || requiredPatchLSIDs.length() == 0) {
            return Collections.emptyList();
        }
        
        final String[] requiredPatchLSIDArray = requiredPatchLSIDs.split(",");
        final String[] requiredPatchURLArray = (requiredPatchURLs != null && requiredPatchURLs.length() > 0 ? requiredPatchURLs.split(",")
                : new String[requiredPatchLSIDArray.length]);
        if (requiredPatchURLArray != null && requiredPatchURLArray.length != requiredPatchLSIDArray.length) {
            throw new JobDispatchException("manifest has " + requiredPatchLSIDArray.length + " patch LSIDs but " + requiredPatchURLArray.length + " URLs");
        }
        final List<PatchInfo> patchInfos=new ArrayList<PatchInfo>();
        for(int i=0; i<requiredPatchLSIDArray.length; ++i) {
            try {
                final String patchUrl=updateUrlIfNecessary(requiredPatchURLArray[i]);
                final PatchInfo patchInfo = new PatchInfo(requiredPatchLSIDArray[i], patchUrl);
                patchInfos.add(patchInfo);
            }
            catch (MalformedURLException e) {
                throw new JobDispatchException("Error initializing patchInfo from args, "+
                        "lsid="+requiredPatchLSIDArray[i]+
                        "url="+requiredPatchURLArray[i], e);
            }
        }
        return patchInfos;
    }
    
    /**
     * If necessary, change the given requiredPatchUrl from the manifest to the newer Broad 
     * module repository url, 
     *     from 'http://www.broadinstitute.org/...' 
     *     to   'http://software.broadinstitute.org/...'.
     * This is required because the patch downloader does not follow the redirect.
     * 
     * @param requiredPatchUrl the original value from the manifest file
     * @return
     */
    protected static String updateUrlIfNecessary(final String requiredPatchUrl) {
        if (requiredPatchUrl != null && requiredPatchUrl.startsWith("http://www.broadinstitute.org/")) {
            final String updatedUrl=requiredPatchUrl.replaceFirst("http://www.broadinstitute.org/", "http://software.broadinstitute.org/");
            log.warn("The 'www.broadinstitute.org' server is no longer available, updating url to: "+updatedUrl);
            return updatedUrl;
        }
        else {
            return requiredPatchUrl;
        } 
    }
    
    protected List<PatchInfo> getPatchesToInstall(final TaskInfo taskInfo) throws Exception, MalformedURLException, JobDispatchException {
        final List<PatchInfo> requiredPatches=getRequiredPatches(taskInfo);
        // no patches required?
        if (requiredPatches==null || requiredPatches.size()==0) {
            return Collections.emptyList();
        }
        
        // remove installed patches from list of required patches
        for (Iterator<PatchInfo> iterator = requiredPatches.iterator(); iterator.hasNext();) {
            final PatchInfo requiredPatch = iterator.next();
            if (pluginRegistry.isInstalled(gpConfig, gpContext, requiredPatch)) {
                iterator.remove();
            }
        }
        return requiredPatches;
    }

    /**
     * Get the list of installed patches.
     * @return
     * @throws Exception
     */
    protected List<PatchInfo> getInstalledPatches() throws Exception {
        return pluginRegistry.getInstalledPatches(gpConfig, gpContext);
    }

    /**
     * Check that each patch listed in the TaskInfoAttributes for this task is installed.
     * if not, download and install it.
     * For any problems, throw an exception
     * 
     * @param taskInfo
     * @param status
     * 
     * @throws Exception
     * @throws MalformedURLException
     * @throws JobDispatchException
     */
    public boolean validatePatches(final TaskInfo taskInfo, final Status status) throws Exception, MalformedURLException, JobDispatchException {
        final List<PatchInfo> patchesToInstall=getPatchesToInstall(taskInfo);
        // no patches to install?
        if (patchesToInstall==null) {
            log.error("Unexpected null value returned from getPatchesToInstall, for task="
                    +taskInfo.getName()+", "+taskInfo.getLsid());
            return true;
        } 
        for(final PatchInfo patchToInstall : patchesToInstall) {
            // download and install this patch
            installIfAbsent(patchToInstall, status);
        }
        // end of loop for each patch LSID for the task
        return true;
    }

    /**
     * Install the patch if and only if it is not already installed.
     * @param patchInfo loaded from manifest file
     * @param status indicator, can be null
     * @throws Exception
     */
    protected void installIfAbsent(final PatchInfo patchInfo, final Status status) throws Exception {
        final boolean isInTransaction=mgr.isInTransaction();
        if (mgr.isInTransaction()) {
            log.debug("isInTransaction="+isInTransaction+", closeCurrentSession");
            mgr.closeCurrentSession();
        }
        final boolean isInstalled=pluginRegistry.isInstalled(gpConfig, gpContext, patchInfo);
        if (isInstalled) {
            status.statusMessage("Patch is already installed, patchLsid="+patchInfo.getLsid());
            return;
        }
        checkNullPatchUrl(patchInfo);
        installPatch(patchInfo, status);
    }
    
    protected void checkNullPatchUrl(final PatchInfo patchInfo) throws JobDispatchException {
        final boolean wasNullURL=Strings.isNullOrEmpty(patchInfo.getUrl());
        if (wasNullURL) {
            final String error="Unable to install patch, lsid="+patchInfo.getLsid()+", patch url is null";
            log.error(error);
            throw new JobDispatchException(error);
        }
        
        // note: original implementation, no longer supported because the service is not available on the new host
        // GPConstants.DEFAULT_PATCH_URL, DefaultPatchURL=
        //     (old host) http://www.broadinstitute.org/cgi-bin/cancer/software/genepattern/gp_patch.cgi
        //     (new host) http://portals.broadinstitute.org/cgi-bin/cancer/software/genepattern/gp_patch.cgi  
        
        // see also, DefaultPatchRepositoryURL=
        //     (new host) http://portals.broadinstitute.org/cgi-bin/cancer/software/genepattern/gp_patch_repository.cgi

    }

    /**
     * Get the location on the server file system for installing the patch.
     * @param patchLSID
     * @return
     * @throws JobDispatchException, if there is no configured root plugin directory (e.g. '<GENEPATTERN_HOME>/patches')
     */
    protected File getPatchDirectory(final LSID patchLSID) 
    throws JobDispatchException
    {
        return getPatchDirectory(gpConfig, gpContext, patchLSID);
    }

    protected static File getPatchDirectory(final GpConfig gpConfig, final GpContext gpContext, final LSID patchLSID) 
    throws JobDispatchException
    {
        final String patchDirName = patchLSID.getAuthority() + "." + patchLSID.getNamespace() + "." + patchLSID.getIdentifier() + "." + patchLSID.getVersion();
        final File rootPluginDir=gpConfig.getRootPluginDir(gpContext);
        if (rootPluginDir==null) {
            throw new JobDispatchException("Configuration error: Unable to get patch directory");
        }
        return new File(rootPluginDir, patchDirName);
    }

    /**
     * Install a specific patch, downloading a zip file with a manifest containing a command line, 
     * running that command line after substitutions, and recording the result 
     * in the genepattern.properties patch registry
     */
    protected void installPatch(final PatchInfo patchInfo, final Status status) throws MalformedURLException, JobDispatchException {
        log.debug("installPatch, lsid="+patchInfo.getLsid()+", url="+patchInfo.getUrl());
        if (status != null) {
            status.statusMessage("Downloading required patch from " + patchInfo.getUrl() + "...");
        }
        String zipFilename = null;
        try {
            zipFilename = FileDownloader.downloadTask(patchInfo.getPatchUrl(), status);
        }
        catch (IOException e) {
            String errorMessage="Error downloading patch, lsid="+patchInfo.getLsid()+", url="+patchInfo.getUrl()+": "+e.getLocalizedMessage();
            log.error(errorMessage, e);
            throw new JobDispatchException(errorMessage, e);
        }
        
        final File patchDirectory = getPatchDirectory(patchInfo.getPatchLsid());
        if (status != null) {
            status.statusMessage("Installing patch from " + patchDirectory.getPath() + ".");
        }
        try {
            explodePatch(zipFilename, patchDirectory, status);
        }
        catch (IOException e) {
            String errorMessage="Error unzipping patch from "+zipFilename+" into "+patchDirectory;
            log.error(errorMessage, e);
            throw new JobDispatchException(errorMessage, e);
        }
        new File(zipFilename).delete();

        // entire zip file has been exploded, now load the manifest, get the command line, and execute it
        Properties props = null;
        try {
            props = loadManifest(patchDirectory);
        }
        catch (IOException e) {
            String errorMessage="Error loading manifest from "+patchDirectory;
            log.error(errorMessage, e);
            throw new JobDispatchException(errorMessage, e);
        }
        
        validatePatchLsid(patchInfo.getLsid(), props);
        
        String nomDePatch = props.getProperty("name");
        if (status != null) {
            status.statusMessage("Running " + nomDePatch + " Installer.");
        }
        String exitValue = null; 
        
        String cmdLine = props.getProperty(COMMAND_LINE);
        if (cmdLine == null || cmdLine.length() == 0) {
            throw new JobDispatchException("No command line defined in " + MANIFEST_FILENAME);
        }
        List<String> cmdLineArgs=initCmdLineArray(cmdLine);
        String[] cmdLineArray = new String[0];
        cmdLineArray = cmdLineArgs.toArray(cmdLineArray);
        try {
            exitValue = "" + executePatch(cmdLineArray, patchDirectory, status);
        }
        catch (IOException e) {
            String errorMessage="IOException while running patch: "+e.getLocalizedMessage();
            log.error(errorMessage, e);
            throw new JobDispatchException(errorMessage, e);
        }
        catch (InterruptedException e2) {
            Thread.currentThread().interrupt();
            throw new JobDispatchException("Patch install interrupted", e2);
        }
        catch (Throwable t) {
            throw new JobDispatchException("Unexpected error while installing patch, lsid="+patchInfo.getLsid()+": "+t.getLocalizedMessage(), t);
        }
        if (status != null) {
            status.statusMessage("Patch installed, exit code " + exitValue);
        }
        String goodExitValue = props.getProperty(PATCH_SUCCESS_EXIT_VALUE, "0");
        String failureExitValue = props.getProperty(PATCH_ERROR_EXIT_VALUE, "");
        if (exitValue.equals(goodExitValue) || !exitValue.equals(failureExitValue)) {
            try {
                final File patchManifest=new File(patchDirectory, "manifest");
                final PatchInfo installedPatchInfo=MigratePlugins.initPatchInfoFromManifest(patchManifest);
                installedPatchInfo.setUrl(patchInfo.getUrl());
                installedPatchInfo.setPatchDir(patchDirectory.getAbsolutePath());
                pluginRegistry.recordPatch(gpConfig, gpContext, installedPatchInfo);
                // always reload config
                ServerConfigurationFactory.reloadConfiguration();
                this.gpConfig=ServerConfigurationFactory.instance();
            }
            catch (Exception e) {
                String errorMessage="Exception while recording patch: "+e.getLocalizedMessage();
                log.error(errorMessage, e);
                throw new JobDispatchException(errorMessage, e);
            }
            if (status != null) {
                status.statusMessage("Patch LSID recorded");
            }

            // keep the manifest file around for future reference
            if (!new File(patchDirectory, MANIFEST_FILENAME).exists()) {
                try {
                    explodePatch(zipFilename, patchDirectory, null, MANIFEST_FILENAME);
                }
                catch (IOException e) {
                    String errorMessage="IOException while unzipping patch from "+zipFilename+" into "+patchDirectory+": "+e.getLocalizedMessage();
                    log.error(errorMessage, e);
                    throw new JobDispatchException(errorMessage, e);
                }
                if (props.getProperty(REQUIRED_PATCH_URLS, null) == null) {
                    try {
                        File f = new File(patchDirectory, MANIFEST_FILENAME);
                        Properties mprops = new Properties();
                        mprops.load(new FileInputStream(f));
                        mprops.setProperty(REQUIRED_PATCH_URLS, patchInfo.getUrl());
                        mprops.store(new FileOutputStream(f), "added required patch");
                    } 
                    catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
        } 
        else {
            if (status != null) {
                status.statusMessage("Deleting patch directory after installation failure");
            }
            // delete patch directory
            File[] old = patchDirectory.listFiles();
            for (int i = 0; old != null && i < old.length; i++) {
                old[i].delete();
            }
            patchDirectory.delete();
            throw new JobDispatchException("Could not install required patch: " + props.get("name") + "  " + props.get("LSID"));
        }
    }
    
    /**
     * Check for patch LSID mismatch
     * @param requiredPatchLSID, the patch LSID declared in the module manifest file
     * @param patchProperties, the actual properties loaded from the manifest file for the patch
     * @return
     * @throws JobDispatchException iff the requiredPatchLSID does not match the LSID in the patch properties
     */
    protected static boolean validatePatchLsid(final String requiredPatchLSID, final Properties patchProperties) 
    throws JobDispatchException
    {
         final String actualPatchLsid=patchProperties.getProperty("LSID");
        if (!Objects.equals(actualPatchLsid, requiredPatchLSID)) {
            throw new JobDispatchException("patch LSID mismatch, requiredPatchLSID="+requiredPatchLSID+" does not match the LSID in the patch manifest, patchLSID="+actualPatchLsid);
        } 
        return true;
    }
    
    protected static List<String> initCmdLineArray(final String cmdLine) {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        final GpContext gpContext=GpContext.getServerContext();
        return initCmdLineArray(gpConfig, gpContext, cmdLine);
    }

    protected static List<String> initCmdLineArray(final GpConfig gpConfig, final GpContext gpContext, final String cmdLine) {
        final Map<String,ParameterInfoRecord> paramInfoMap=Collections.emptyMap();
        final List<String> cmdLineArgs = CommandLineParser.createCmdLine(gpConfig, gpContext, cmdLine, paramInfoMap);        
        return cmdLineArgs;
    }

    // unzip the patch files into their own directory
    private static void explodePatch(String zipFilename, File patchDirectory, Status status) throws IOException {
        explodePatch(zipFilename, patchDirectory, status, null);
    }

    // unzip the patch files into their own directory
    private static void explodePatch(final String zipFilename, final File patchDirectory, final Status status, final String zipEntryName)
    throws IOException 
    {
        ZipFile zipFile = new ZipFile(zipFilename);
        InputStream is = null;
        patchDirectory.mkdirs();
        if (zipEntryName == null) {
            // clean out existing directory
            File[] old = patchDirectory.listFiles();
            for (int i = 0; old != null && i < old.length; i++) {
                old[i].delete();
            }
        }
        for (final Enumeration<? extends ZipEntry> eEntries = zipFile.entries(); eEntries.hasMoreElements();) {
            final ZipEntry zipEntry = eEntries.nextElement();
            if (zipEntryName != null && !zipEntryName.equals(zipEntry.getName())) {
                continue;
            }
            final File outFile = new File(patchDirectory, zipEntry.getName());
            if (zipEntry.isDirectory()) {
                if (status != null) {
                    status.statusMessage("Creating subdirectory " + outFile.getAbsolutePath());
                }
                outFile.mkdirs();
                continue;
            }
            is = zipFile.getInputStream(zipEntry);
            OutputStream os = new FileOutputStream(outFile);
            long fileLength = zipEntry.getSize();
            long numRead = 0;
            byte[] buf = new byte[100000];
            int i;
            while ((i = is.read(buf, 0, buf.length)) > 0) {
                os.write(buf, 0, i);
                numRead += i;
            }
            os.close();
            os = null;
            if (numRead != fileLength) {
                throw new IOException("only read " + numRead + " of " + fileLength + " bytes in " + zipFile.getName() + "'s " + zipEntry.getName());
            }
            is.close();
        } // end of loop for each file in zip file
        zipFile.close();
    }

    // load the patch manifest file into a Properties object
    protected static Properties loadManifest(File patchDirectory) throws IOException {
        File manifestFile = new File(patchDirectory, MANIFEST_FILENAME);
        if (!manifestFile.exists()) {
            throw new IOException(MANIFEST_FILENAME + " missing from patch " + patchDirectory.getName());
        }
        Properties props = new Properties();
        FileInputStream manifest = new FileInputStream(manifestFile);
        props.load(manifest);
        manifest.close();
        return props;
    }

    /**
     * Run the patch command line in the patch directory, returning the exit code from the executable.
     */
    private static int executePatch(String[] commandLineArray, File patchDirectory, Status status) throws IOException, InterruptedException {
        if (log.isDebugEnabled()) {
            log.debug("patch dir="+patchDirectory);
            log.debug("patch commandLine ...");
            int i=0;
            for(final String arg : commandLineArray) {
                log.debug("\targ["+i+"]: "+arg);
            }
        }
        
        // spawn the command
        Process process = Runtime.getRuntime().exec(commandLineArray, null, patchDirectory);

        // BUG: there is race condition during a tiny time window between the exec and the close
        // (the lines above and below this comment) during which it is possible for an application
        // to imagine that there might be useful input coming from stdin.
        // This seemed to be the case for Perl 5.0.1 on Wilkins, and might be a problem in
        // other applications as well.
        process.getOutputStream().close(); 
        // there is no stdin to feed to the program. So if it asks, let it see EOF!

        // create threads to read from the command's stdout and stderr streams
        Thread outputReader = (status != null) ? antStreamCopier(process.getInputStream(), status)
                : streamCopier(process.getInputStream(), System.out);
        Thread errorReader = (status != null) ? antStreamCopier(process.getErrorStream(), status) : streamCopier(
                process.getInputStream(), System.err);

        // drain the output and error streams
        outputReader.start();
        errorReader.start();

        // wait for all output
        outputReader.join();
        errorReader.join();

        // the process will be dead by now
        process.waitFor();
        int exitValue = process.exitValue();
        return exitValue;
    }
    
    // copy an InputStream to a PrintStream until EOF
    public static Thread streamCopier(final InputStream is, final PrintStream ps) throws IOException {
    // create thread to read from the a process' output or error stream
    return new Thread(new Runnable() {
        public void run() {
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while ((line = in.readLine()) != null) {
            ps.println(line);
            ps.flush();
            }
        } catch (IOException ioe) {
            System.err.println(ioe + " while reading from process stream");
        }
        }
    });
    }

    // copy an InputStream to a PrintStream until EOF
    public static Thread streamCopier(final InputStream is, final Status status) throws IOException {
    // create thread to read from the a process' output or error stream
    return new Thread(new Runnable() {
        public void run() {
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while ((line = in.readLine()) != null) {
            if (status != null && line != null) {
                status.statusMessage(line);
            }
            }
        } catch (IOException ioe) {
            System.err.println(ioe + " while reading from process stream");
        }
        }
    });
    }

    // copy an InputStream to a PrintStream until EOF
    public static Thread antStreamCopier(final InputStream is, final Status status) throws IOException {
    // create thread to read from the a process' output or error stream
    return new Thread(new Runnable() {
        public void run() {
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while ((line = in.readLine()) != null) {
            int idx = 0;
            if ((idx = line.indexOf("[echo]")) >= 0) {
                line = line.substring(idx + 6);
            }
            if (status != null && line != null) {
                status.statusMessage(line);
            }
            }
        } catch (IOException ioe) {
            System.err.println(ioe + " while reading from process stream");
        }
        }
    });
    }

}
