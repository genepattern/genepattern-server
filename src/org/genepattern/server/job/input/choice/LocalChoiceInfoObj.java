package org.genepattern.server.job.input.choice;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.job.input.cache.MapLocalEntry;

/**
 * Helper class for by-passing a remote choiceDir with a local directory path.
 * For a module input file parameter that has a dynamically generated drop-down menu from an ftp site, e.g.
 *    ftp://gpftp.broadinstitute.org/
 * If the ftp files are accessible via a local file path, e.g.
 *   /xchip/gpdev/gpftp/pub/
 * you can configure your server to avoid making the FTP calls to list and download the files.
 * 
 * To do so, make an edit to the config.yaml file for your server. Define a 'local.choiceDirs' 
 * property in the 'default.properties' section of the config file. 
 * Do not customize this on a per-user or per-module basis! This defines a shared resource and 
 * we must use the same setting for all users.
 * 
 * When the GP server initializes the menu for a module with a dynamic drop-down, it will first check 
 * for a matching entry in the 'local.choiceDirs' map. If there is one, it will use the local path
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
    local.choiceDirs: {
        "ftp://gpftp.broadinstitute.org/": "/xchip/gpdev/gpftp/pub/"
    }
</pre>
 *
 * For an example module, 'demoDropdown' with a 'genome' File field with a dynamic drop-down.
<pre>
    p1_choiceDir=ftp://gpftp.broadinstitute.org/rna_seq/whole_genomes
</pre>

  Because there is a matching entry in 'local.choiceDirs', the matching local directory would be, '/xchip/gpdev/gpftp/pub/rna_seq/whole_genomes'.
  The server generates a list of 'virtual' URL values into the drop-down. Each value is an FTP url, e.g.
      ftp://gpftp.broadinstitute.org/rna_seq/whole_genomes/Arabidopsis_thaliana_Ensembl_TAIR10.fa
  When job is run, the GP server will find the matching local path to the file, thus avoiding the FTP download.
  The command line value would be,
      /xchip/gpdev/gpftp/pub/rna_seq/whole_genomes/Arabidopsis_thaliana_Ensembl_TAIR10.fa

  Caveats: Duplicate matching keys in the 'local.choiceDirs' map are not allowed. E.g.
  <pre>
    local.choiceDirs: {
        "ftp://ftp.broadinstitute.org/pub/genepattern/": "/web/ftp/pub/genepattern/",
        "ftp://ftp.broadinstitute.org/pub/genepattern/rna_seq": "/web/ftp/pub/genepattern/rna_seq",
    }
  </pre>
  This is not allowed, because key[0] is a prefix of key[1]. Don't do this.

  It's possible to map two different external urls to the same local path. E.g.
  <pre>
    local.choiceDirs: {
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

    /** the initial choiceDir */
    private final String choiceDir;
    /** optional choiceDirFilter */
    //private final String choiceDirFilter;
    private final DirFilter dirFilter;
    /** help class which maintains the mapping between external urls and local file paths */
    private final MapLocalEntry mapLocalEntry;
    /** the local directory (if it exists) which maps to the choiceDir url for the param. */
    private final File localChoiceDir;
    /** the list of drop-down items */
    private final List<Choice> localChoices;

    /**
     * Initialize a LocalChoiceObj for the given choiceDir and choiceDirFilter. 
     * May be a no-op if the param doesn't have a dynamic file drop-down, 
     * or if there is no local path in the config file.
     * 
     * @param choiceDir
     * @param choiceDirFilter
     */
    public LocalChoiceInfoObj(final String choiceDir, final DirFilter dirFilter) {
        this.choiceDir=choiceDir;
        //this.choiceDirFilter=choiceDirFilter;
        this.dirFilter=dirFilter;
        if (choiceDir==null || choiceDir.length()==0) {
            mapLocalEntry=null;
        }
        else {
            this.mapLocalEntry=MapLocalEntry.initLocalChoiceDir(choiceDir);
        }
        if (mapLocalEntry==null) {
            this.localChoiceDir=null;
            this.localChoices=Collections.emptyList();
        }
        else {
            this.localChoiceDir=mapLocalEntry.initLocalValue(choiceDir);
            this.localChoices=initLocalChoices();
        }
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
        final File[] localFiles=localChoiceDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return dirFilter.acceptName(pathname.getName());
            }
        });
        if (localFiles.length == 0) {
            return Collections.emptyList();
        }
        List<Choice> choices = new ArrayList<Choice>();
        Arrays.sort(localFiles);
        for(final File localFile : localFiles) {
            final String actualValue=mapLocalEntry.initUrlValue(localFile);
            final String displayValue=localFile.getName();
            final Choice choice=new Choice(displayValue, actualValue, localFile.isDirectory());
            choices.add(choice);                
        }
        log.debug("initialized drop-down from local directory, "+choiceDir+" is mapped to "+localChoiceDir);
        return choices;
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

}