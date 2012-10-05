package org.genepattern.server.eula;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Comparator;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;

/**
 * Data representation of a single End-user license agreement 'form' for a module.
 * 
 * This object is needed in a number of different contexts.
 * 
 * 1) At runtime, to check to see if there is a local record of agreement for a particular user.
 *     For this we need some kind of unique ID for the license object.
 * 
 * 2) In the job submit form, to present to the end user the content of the license agreement.
 *     For this we need the full text of the agreement, or a link to where the full text lives.
 * 
 * @author pcarr
 *
 */
public class EulaInfo {
    private static Logger log = Logger.getLogger(EulaInfo.class);
    public static class EulaInitException extends Exception {
        public EulaInitException(String message) {
            super(message);
        }
        public EulaInitException(String message, Throwable t) {
            super(message,t);
        }
    }
    
    public static Comparator<EulaInfo> defaultComparator(final TaskInfo currentTaskInfo) {
        return new Comparator<EulaInfo>() {

            // @Override
            public int compare(EulaInfo arg0, EulaInfo arg1) {
                //null check
                if (arg0 == null) {
                    if (arg1 == null) {
                        return 0;
                    }
                    //null is always the last item
                    return 1;
                }
                if (arg1 == null) {
                    return -1;
                }
                
                //currentTask is always first
                if (arg0.getModuleLsid().equals( currentTaskInfo.getLsid() )) {
                    return -1;
                }
                if (arg1.getModuleLsid().equals( currentTaskInfo.getLsid() )) {
                    return 1;
                } 
                
                //name check
                int i = arg0.getModuleName().compareTo(arg1.getModuleName());
                if (i != 0) {
                    return i;
                }
                //LSID check
                if (arg0.getLsid() != null && arg1.getLsid() != null) {
                    i = arg0.getLsid().compareTo(arg1.getLsid());
                }
                else {
                    //ERROR
                    log.error("LSID not set in EulaInfo, using faulty string comparison method instead");
                    i = arg0.getModuleLsid().compareTo(arg1.getModuleLsid());
                }
                return i;
            }
        };
    }

    private String ID;
    private String content;

    private LSID theLsid;
    //the String representation of the lsid of the module which requires the EULA
    private String moduleLsid;
    //the version of the module which requires the EULA, derived from moduleLsid
    private String moduleLsidVersion;
    //the name of the module which requires the EULA
    private String moduleName;
    //the value of the license= property in the manifest for the module
    private String license;
    
    public void setLicense(final String license) {
        this.license=license;
    }
    public void setModuleLsid(final String lsid) {
        this.moduleLsid=lsid; 
        //automatically init lsidVersion
        try {
            theLsid = new LSID(lsid);
            this.moduleLsidVersion=theLsid.getVersion();
        }
        catch (MalformedURLException e) {
            log.error("Error computing lsidVersion from lsid string, lsid="+lsid, e);
        } 
    }
    public String getModuleLsid() {
        return this.moduleLsid;
    }
    public String getModuleLsidVersion() {
        return this.moduleLsidVersion;
    }
    public void setModuleName(final String name) {
        this.moduleName=name;
    }
    public String getModuleName() {
        return moduleName;
    }
    //it's easier to hang on to the LSID instance for sorting by LSID
    public LSID getLsid() {
        return theLsid;
    }
    
    public String getLink() {
        //http://gpbroad.broadinstitute.org:8080/gp/getTaskDoc.jsp?name=urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:9&file=ComparativeMarkerSelection.pdf
        String rel="/gp/getTaskDoc?name="+moduleLsid+"&file="+license;
        return rel;
    }
    
    public String getContent() throws EulaInitException {
        if (!inited_content) {
            inited_content=true;
            this.content = initContent();
        }
        return this.content;
        //final String NL = "\n";
        //final String content=
        //        "To view the license agreement, click this link: "+NL+
        //        "    <a href=\""+getLink()+"\"></a>";
        //return content; 
    }
    
    private boolean inited_content = false;
    private String initContent() throws EulaInitException {
        if (license==null) {
            throw new EulaInitException("license==null");
        }
        
        //need path to tasklib
        File tasklibDir = null;
        //TODO: implement safer method, e.g. File tasklibDir = DirectoryManager.getTaskLibDirFromCache(moduleLsid);
        //    getLibDir automatically creates a directory on the file system; it's possible to cause problems if there is bogus input
        try {
            String path = DirectoryManager.getLibDir(moduleLsid);
            if (path != null) {
                tasklibDir = new File(path);
            }
        }
        catch (Throwable t) {
            log.error("Error getting libdir for moduleLsid="+moduleLsid+": "+t.getLocalizedMessage(), t);
        }
        if (tasklibDir == null) {
            throw new EulaInitException("tasklibDir==null");
        }
        File licenseFile = new File(tasklibDir, license);
        if (!licenseFile.exists()) {
            throw new EulaInitException("licenseFile doesn't exist: "+licenseFile.getPath());
        }
        if (!licenseFile.canRead()) {
            throw new EulaInitException("can't read license file: "+licenseFile.getPath());
        }
        
        //TODO: for future development: check to see if the licenseFile is a meta file, e.g. licenseInfo.yaml
        //      which should be parsed into a license object
        
        //otherwise, default case, read the file into a string
        String content=fileToString(licenseFile);
        return content;
    }
    
    /**
     * Read the contents of the file into a String.
     * @param file
     * @return
     */
    private String fileToString(File file) {
        try {
            final String str = FileUtils.readFileToString(file);
            return str;
        }
        catch (IOException e) {
            log.error(e);
        }
        return "";
    }
    
}