package org.genepattern.server.job.input.cache;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.executor.CommandProperties.Value;

/**
 * Helper class which allows you to map a local directory as if it 
 * were downloaded from a remote location.
 * 
 * @author pcarr
 *
 */
public class MapLocalEntry {
    private static final Logger log = Logger.getLogger(MapLocalEntry.class);
    
    public static final String PROP_LOCAL_CHOICE_DIRS="local.choiceDirs";

    /**
     * Get the Map<?,?> of url->localFile from the config.yaml file.
     * 
     * @return an empty Map if there is no valid entry in the config file. 
     *     For a default gp install this will return an empty map.
     */
    private static Map<?,?> getLocalChoiceDirsMap() {
        final Context serverContext=Context.getServerContext();
        final Value value=ServerConfiguration.instance().getValue(serverContext, PROP_LOCAL_CHOICE_DIRS);
        if (value==null || !value.isMap()) {
            return Collections.emptyMap();
        }
        final Map<?,?> map=value.getMap();
        if (map==null) {
            return Collections.emptyMap();
        }
        return map;
    }

    /**
     * This returns a File object if there is a matching entry in the 'local.choiceDirs' map of the config.yaml file.
     * 
     * @param selectedUrlValue
     * @return
     */
    public static File initLocalFileSelection(final String selectedUrlValue) {
        final Map<?,?> map=getLocalChoiceDirsMap();
        if (map.isEmpty()) {
            log.debug("No map configured");
            return null;
        }
        for(final Entry<?,?> entry : map.entrySet()) {
            final Object key=entry.getKey();
            final Object val=entry.getValue();
            if (key instanceof String && val instanceof String) {
                final File match=MapLocalEntry.initLocalValue(selectedUrlValue, (String) key, (String) val);
                if (match != null) {
                    log.debug("found mapping from "+selectedUrlValue+" to "+match);
                    return match;
                }
            }
            else {
                log.error("error in config file, expecting a map of String->String");
            }
        }
        log.debug("No match for file: "+selectedUrlValue);
        return null;
    }

    /**
     * Check the config for a matching local dir and initialize the localChoiceDir field.
     * @return
     */
    public static MapLocalEntry initLocalChoiceDir(final String choiceDir) {
        final Map<?,?> map=getLocalChoiceDirsMap();
        if (map.isEmpty()) {
            return null;
        }
        //find matching prefix
        for(final Entry<?,?> entry : map.entrySet()) {
            final Object key=entry.getKey();
            final Object val=entry.getValue();
            if (key instanceof String && val instanceof String) {
                final File match=MapLocalEntry.initLocalChoiceDir(choiceDir, (String) key, (String) val);
                if (match != null) {
                    return new MapLocalEntry( (String) key, (String) val);
                }
            }
        }
        //no match
        return null;
    }

    /**
     * For each entry in the 'local.choiceDirs' map, check to see if the parameter's 'choiceDir' field is a good match.
     * This also checks for the existence and ability to read the directory on the local path. 
     * This simple implementation is case-sensitive and only works with unix file paths for the localPathRoot.
     *  
     * @param choiceDir,       e.g. ftp://ftp.broadinstitute.org/pub/genepattern/rna_seq/whole_genomes
     * @param externalUrlRoot, e.g. ftp://ftp.broadinstitute.org/pub/genepattern/
     * @param localPathRoot,   e.g. /web/ftp/pub/genepattern/
     * 
     * @return a valid local File or null if there is no match.
     */
    public static File initLocalChoiceDir(final String choiceDir, final String fromUrlRoot, final String toLocalPathRoot) {
        if (log.isDebugEnabled()) {
            log.debug("checking choiceDir="+choiceDir+", local.choiceDirs[ '"+fromUrlRoot+"' ] = '"+toLocalPathRoot+"'");
        }
        
        final File localChoiceDir=initLocalValue(choiceDir, fromUrlRoot, toLocalPathRoot);
        if (localChoiceDir==null) {
            log.debug("no match");
            return localChoiceDir;
        }

        if (!localChoiceDir.canRead()) {
            log.error("for choiceDir="+choiceDir+" mapped to "+fromUrlRoot+", cannot read localDir: "+localChoiceDir);
            return null;
        }
        if (!localChoiceDir.isDirectory()) {
            log.error("for choiceDir="+choiceDir+" mapped to "+fromUrlRoot+", localDir is not a directory: "+localChoiceDir);
            return null;
        }
        return localChoiceDir;
    }

    /**
     * For a given url value for an input parameter, check to see if there is a locally mapped value.
     * 
     * @param fromUrlValue, the url value selected from the  file drop-down
     * @param fromUrlRoot, the key in the 'local.choiceDirs' map in the config.yaml file.
     * @param toLocalPathRoot, the value from the 'local.choiceDirs' map
     * 
     * @return a local File, or null if there is no matching entry in the map, or if there is no matching file in the file system.
     */
    public static File initLocalValue(final String fromUrlValue, final String fromUrlRoot, final String toLocalPathRoot) {
        if (fromUrlValue==null) {
            throw new IllegalArgumentException("fromUrlValue==null");
        }
        if (fromUrlRoot==null) {
            throw new IllegalArgumentException("fromUrlRoot==null");
        }
        if (toLocalPathRoot==null) {
            throw new IllegalArgumentException("toLocalPathRoot==null");
        }
        if (fromUrlValue.startsWith(fromUrlRoot)) {
            String localFileValue=fromUrlValue.replaceFirst(Pattern.quote(fromUrlRoot), toLocalPathRoot);
            final File localFile=new File(localFileValue);
            if (!localFile.exists()) {
                log.error("localFile does not exist: "+localFile+", Ignoring local.choiceDirs[ '"+fromUrlRoot+"' ] = '"+toLocalPathRoot+"' for value="+fromUrlValue);
                return null;
            }
            return localFile;
        }
        return null;
    }
    
    
    final String fromUrlRoot;
    final String toLocalPathRoot;

    /**
     * Initialize a new instance.
     * 
     * @param fromUrl - the remote url
     * @param toLocalPath - the local file path, must use unix path separator characters
     */
    public MapLocalEntry(final String fromUrl, final String toLocalPath) {
        if (fromUrl==null) {
            throw new IllegalArgumentException("fromUrl==null");
        }
        if (toLocalPath==null) {
            throw new IllegalArgumentException("toLocalPath==null");
        }
        if (fromUrl.endsWith("/")) {
            this.fromUrlRoot=fromUrl;
        }
        else {
            this.fromUrlRoot=fromUrl+"/";
        }
        if (toLocalPath.endsWith("/")) {
            this.toLocalPathRoot=toLocalPath;
        }
        else {
            this.toLocalPathRoot=toLocalPath+"/";
        }
    }

    /**
     * Convert a selected external url value into a local file path.
     * @param fromUrlValue 
     * @return null if there is no match
     */
    public File initLocalValue(final String fromUrlValue) {
        return MapLocalEntry.initLocalValue(fromUrlValue, fromUrlRoot, toLocalPathRoot);
    }
    
    /**
     * We want to use the remote URL as a the value in the drop-down menu. This helper method converts
     * the local file path back to the remote URL value. E.g.
     *     localPathActual /Volumes/xchip_gpdev/gpftp/pub/test/file_1.txt
     *     externalUrlRoot ftp://gpftp.broadinstitute.org/
     *     localPathRoot   /Volumes/xchip_gpdev/gpftp/pub/
     *     
     *     actualValue: ftp://gpftp.broadinstitute.org/test/file_1.txt
     *      
     *     Drop localPathRoot from the beginning of localPathActual to get 'test/file_1.txt'
     *     Then append externalUrlRoot to that result to get ftp://gpftp.broadinstitute.org/test/file_1.txt.
     */
    public String initUrlValue(final File localFile) {
        final String localPath=FilenameUtils.separatorsToUnix(localFile.getPath());
        if (localPath.startsWith(this.toLocalPathRoot)) {
            String tail=localPath.substring(this.toLocalPathRoot.length());
            String tailEncoded=UrlUtil.encodeFilePath(new File(tail));
            String rval = this.fromUrlRoot + tailEncoded;
            return rval;
        }
        log.error("Didn't create url value for localFile: "+localFile);
        return null;
    }

}
