package org.genepattern.server.job.input.choice;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.executor.CommandProperties.Value;
import org.genepattern.webservice.ParameterInfo;

import scala.actors.threadpool.Arrays;

/**
 * Helper class for by-passing a remote choiceDir with a local directory path.
 * For a module input file parameter that has a dynamically generated drop-down menu from an ftp site, e.g.
 *    ftp://gpftp.broadinstitute.org/
 * If the ftp files are accessible via a local file path, e.g.
 *   /xchip/gpdev/gpftp/pub/
 * you can configure your server to avoid making the FTP calls to list and download the files.
 * 
 * To do so, make an edit to the config.yaml file for your server. Define a 'local.choice.dirs' 
 * property in the 'default.properties' section of the config file. 
 * Do not customize this on a per-user or per-module basis! This defines a shared resource and 
 * we must use the same setting for all users.
 * 
 * When the GP server initializes the menu for a module with a dynamic drop-down, it will first check 
 * for a matching entry in the 'local.choice.dirs' map. If there is one, it will use the local path
 * instead of making FTP calls. When a user selects a value from the drop-down the server will use 
 * the local path on the command line, and will not do an FTP transfer.
 * 
 * You can use the same source FTP directory among a number of different modules with dynamic file drop-downs. 
 * You do this by defining a mapping between a root FTP directory and a root path on your server's file system. 
 * The GP server will find matching directories. 
 * 
 * Example config.yaml entry,
<pre>
default.properties:
    # ...
    #
    # create a map of "<externalDir>": "<localDir>"
    #
    local.choice.dirs: {
        "ftp://gpftp.broadinstitute.org/": "/xchip/gpdev/gpftp/pub/"
    }
</pre>
 *
 * For an example module, 'demoDropdown' with a 'genome' File field with a dynamic drop-down.
<pre>
    p1_choiceDir=ftp://gpftp.broadinstitute.org/rna_seq/whole_genomes
</pre>

  Because there is a matching entry in 'local.choice.dirs', the matching local directory would be, '/xchip/gpdev/gpftp/pub/rna_seq/whole_genomes'.
  The server generates a list of 'virtual' URL values into the drop-down. Each value is an FTP url, e.g.
      ftp://gpftp.broadinstitute.org/rna_seq/whole_genomes/Arabidopsis_thaliana_Ensembl_TAIR10.fa
  When job is run, the GP server will find the matching local path to the file, thus avoiding the FTP download.
  The command line value would be,
      /xchip/gpdev/gpftp/pub/rna_seq/whole_genomes/Arabidopsis_thaliana_Ensembl_TAIR10.fa

  Caveats: Duplicate matching keys in the 'local.choice.dirs' map are not allowed. E.g.
  <pre>
    local.choice.dirs: {
        "ftp://ftp.broadinstitute.org/pub/genepattern/": "/web/ftp/pub/genepattern/",
        "ftp://ftp.broadinstitute.org/pub/genepattern/rna_seq": "/web/ftp/pub/genepattern/rna_seq",
    }
  </pre>
  This is not allowed, because key[0] is a prefix of key[1]. Don't do this.

  It's possible to map two different external urls to the same local path. E.g.
  <pre>
    local.choice.dirs: {
        "ftp://ftp.broadinstitute.org/pub/genepattern/": "/xchip/gpdev/gpftp/pub/",
        "ftp://gpftp.broadinstitute.org/": "/xchip/gpdev/gpftp/pub/"
    }
  </pre>
 * 
 * @author pcarr
 *
 */
public class LocalChoiceInfoObj {
    private final static Logger log = Logger.getLogger(LocalChoiceInfoObj.class);

    public static final String PROP_LOCAL_CHOICE_DIRS="local.choiceDirs";

    private final ParameterInfo param;
    private final String choiceDir;
    private final File localChoiceDir;
    private String externalUrlRoot;
    private String localPathRoot;
    private final List<Choice> localChoices;

    /**
     * Initialize a LocalChoiceObj for the given param and choiceDir. May be a no-op
     * if the param doesn't have a dynamic file drop-down, or if there is no local path
     * entered in the config file.
     * 
     * @param param
     * @param choiceDir
     */
    public LocalChoiceInfoObj(final ParameterInfo param, final String choiceDir) {
        this.param=param;
        this.choiceDir=choiceDir;
        
        if (choiceDir==null || choiceDir.length()==0) {
            localChoiceDir=null;
            localChoices=Collections.emptyList();
        }
        else {
            this.localChoiceDir=initLocalChoiceDir();
            this.localChoices=initLocalChoices();
        }
    }
    
    /**
     * Get the local directory path, or null if there is none.
     * @return
     */
    public File getLocalChoiceDir() {
        return localChoiceDir;
    }

    /**
     * Is there a local path for the drop-down menu?
     * 
     * @return true If the parameter has a dynamic drop-down AND there is a matching
     * local directory path. Otherwise, return false.
     */
    public boolean hasLocalChoiceDir() {
        return localChoiceDir!=null;
    }
    
    public List<Choice> getLocalChoices() {
        return localChoices;
    }

    /**
     * Check the config for a matching local dir and initialize the localChoiceDir field.
     * @return
     */
    private File initLocalChoiceDir() {
        final Context serverContext=Context.getServerContext();
        final Value value=ServerConfiguration.instance().getValue(serverContext, PROP_LOCAL_CHOICE_DIRS);
        final Map<?,?> map=value.getMap();
        if (map != null) {
            //find matching prefix
            for(final Entry<?,?> entry : map.entrySet()) {
                final Object key=entry.getKey();
                final Object val=entry.getValue();
                if (key instanceof String && val instanceof String) {
                    this.externalUrlRoot = (String) key;
                    this.localPathRoot = (String) val;
                    if (choiceDir.startsWith(externalUrlRoot)) {
                        final File match=initLocalChoiceDir((String) key, (String) val);
                        if (match != null) {
                            this.externalUrlRoot = (String) key;
                            this.localPathRoot = (String) val;
                            return match;
                        }
                        else {
                            log.debug("No matching local path found for choiceDir="+choiceDir+", localChoiceDirs[ '"+externalUrlRoot+"' ] = '"+localPathRoot+"'");
                            this.externalUrlRoot = null;
                            this.localPathRoot = null;
                        }
                    }
                }
            }
        }
        //no match
        return null;
    }
    
    /**
     * For each entry in the 'local.choiceDirs' map, check to see if the parameter's 'choiceDir' field is a good match.
     * This also checks for the existence and ability to read the file on the local path. 
     * It will return null
     *  
     * @param choiceDir,       e.g. ftp://ftp.broadinstitute.org/pub/genepattern/rna_seq/whole_genomes
     * @param externalUrlRoot, e.g. ftp://ftp.broadinstitute.org/pub/genepattern/
     * @param localPathRoot,   e.g. /web/ftp/pub/genepattern/
     * 
     * @return a valid local File or null if there is no match.
     */
    private File initLocalChoiceDir(final String externalUrlRoot, final String localPathRoot) {
        if (externalUrlRoot==null) {
            throw new IllegalArgumentException("externalUrlRoot==null");
        }
        if (localPathRoot==null) {
            throw new IllegalArgumentException("localPathRoot==null");
        }
        //simple implementation is case-sensitive and only works with unix file paths for the localPathRoot
        final String localDirPath=choiceDir.replaceFirst(Pattern.quote(externalUrlRoot), localPathRoot);
        final File localDir=new File(localDirPath);
        if (!localDir.exists()) {
            //TODO: handle exception
            log.error("localDir does not exist: "+localDir);
            return null;
        }
        if (!localDir.canRead()) {
            //TODO: handle exception
            log.error("cannot read localDir: "+localDir);
            return null;
        }
        return localDir;
    }

    private List<Choice> initLocalChoices() {
        if (localChoiceDir == null) {
            return Collections.emptyList();
        }
        if (!localChoiceDir.exists()) {
            throw new IllegalArgumentException("localChoiceDir does not exist: "+localChoiceDir);
        }
        if (!localChoiceDir.canRead()) {
            throw new IllegalArgumentException("can't read localChoiceDir: "+localChoiceDir);
        }
        final LocalDirFilter filter=new LocalDirFilter(param);
        final File[] localFiles=localChoiceDir.listFiles(filter);
        if (localFiles.length == 0) {
            return Collections.emptyList();
        }
        List<Choice> choices = new ArrayList<Choice>();
        Arrays.sort(localFiles);
        for(final File localFile : localFiles) {
            final String actualValue=initUrlValue(localFile);
            final String displayValue=localFile.getName();
            final Choice choice=new Choice(displayValue, actualValue, localFile.isDirectory());
            choices.add(choice);                
        }
        return choices;
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
    private String initUrlValue(final File localFile) {
        String localPath=FilenameUtils.separatorsToUnix(localFile.getPath());
        if (localPath.startsWith(localPathRoot)) {
            String tail=localPath.substring(localPathRoot.length());
            String tailEncoded=UrlUtil.encodeFilePath(new File(tail));
            String rval = externalUrlRoot + tailEncoded;
            return rval;
        }
        log.error("Didn't create url value for localFile: "+localFile);
        return null;
    }
}