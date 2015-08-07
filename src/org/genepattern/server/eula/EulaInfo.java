/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.eula;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Comparator;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.webapp.HtmlEncoder;

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
public class EulaInfo implements Comparable<EulaInfo> {
    final static private Logger log = Logger.getLogger(EulaInfo.class);

    private static LibdirStrategy libdirImpl = null;

    public static void setLibdirStrategy(LibdirStrategy m) {
        libdirImpl=m;
    }

    private static LibdirStrategy getLibdirStrategy() {
        if (libdirImpl==null) {
            libdirImpl = new LibdirLegacy();
        }
        return libdirImpl;
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
                
                //currentTask is always first, 
                //    but allow the currentTask to have more than one license ...
                boolean arg0_isCurrent=arg0.getModuleLsid().equals( currentTaskInfo.getLsid() );
                boolean arg1_isCurrent=arg1.getModuleLsid().equals( currentTaskInfo.getLsid() );
                if (arg0_isCurrent && !arg1_isCurrent) {
                    return -1;
                }
                else if (arg1_isCurrent && !arg0_isCurrent) {
                    return 1;
                }
                //    ... which is why we continue, if arg0 == arg1 == currentTask
                
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
                if (i != 0) {
                    return i;
                }
                //finally, license check
                //    to allow for multiple license files on the same module
                if (arg0.license != null) {
                    i = arg0.license.compareTo(arg1.license);
                }
                else {
                    if (arg1.license == null) {
                        return 0;
                    }
                    return 1;
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
    //the path to the license file, must be in ascii-format
    private File licenseFile;
    
    public void setLicense(final String license) {
        this.license=license;
    }
    public String getLicense() {
        return this.license;
    }
    
    public void setLicenseFile(final File licenseFile) {
        this.license=licenseFile.getName();
        this.licenseFile=licenseFile;
    }
    public void setModuleLsid(final String lsid) throws InitException {
        //Note: LSID constructor does not check for null or empty arg
        if (lsid==null) {
            throw new InitException("lsid==null");
        }
        if (lsid.length()==0) {
            throw new InitException("lsid not set");
        }
        try {
            theLsid = new LSID(lsid);
            this.moduleLsid=lsid; 
            //automatically initialize lsidVersion
            this.moduleLsidVersion=theLsid.getVersion();
        }
        catch (MalformedURLException e) {
            log.error("Error computing lsidVersion from lsid string, lsid="+lsid, e);
            throw new InitException("Invalid moduleLsid="+lsid,e);
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

    /**
     * @deprecated, must pass in a valid contextPath ('/gp').
     * @return
     */
    public String getLink() {
        return getLink(null);
    }
    
    /**
     * Pass in the contextPath (e.g. "/gp") so that we can generate a relative link to the file.
     * @param contextPath
     * @return
     */
    public String getLink(String contextPath) {
        if (contextPath==null) {
            contextPath = ServerConfigurationFactory.instance().getGpPath();
            log.warn("contextPath==null, using '"+contextPath+"'");
        }
        //http://gpbroad.broadinstitute.org:8080/gp/getTaskDoc.jsp?name=urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:9&file=ComparativeMarkerSelection.pdf
        String htmlEncodedLicense = HtmlEncoder.htmlEncode(license);
        //String rel="/gp/getTaskDoc.jsp?name="+moduleLsid+"&file="+htmlEncodedLicense;
        String rel=contextPath+"/getTaskDoc.jsp?name="+moduleLsid+"&file="+htmlEncodedLicense;
        return rel;
    }
    
    public String getContent() throws InitException {
        if (!inited_content) {
            inited_content=true;
            this.content = initContent();
        }
        return this.content;
    }
    
    private boolean inited_content = false;
    private String initContent() throws InitException {
        if (licenseFile == null) {
            licenseFile=initLicenseFile();
        }
        return initContentFromLicenseFile(licenseFile);
    }
    private File initLicenseFile() throws InitException {
        if (license==null) {
            throw new InitException("license==null");
        }
        
        //need path to tasklib
        File tasklibDir=getLibdirStrategy().getLibdir(moduleLsid);
        File licenseFile = new File(tasklibDir, license);
        if (!licenseFile.exists()) {
            throw new InitException("licenseFile doesn't exist: "+licenseFile.getPath());
        }
        if (!licenseFile.canRead()) {
            throw new InitException("can't read license file: "+licenseFile.getPath());
        }
        return licenseFile;
    }

    private String initContentFromLicenseFile(File licenseFile) throws InitException {
        if (!licenseFile.exists()) {
            throw new InitException("licenseFile doesn't exist: "+licenseFile.getPath());
        }
        if (!licenseFile.canRead()) {
            throw new InitException("can't read license file: "+licenseFile.getPath());
        }
        //TODO: for future development: check to see if the licenseFile is a meta file, e.g. .eulaInfo.yaml
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
    static public String fileToString(File file) {
        try {
            final String str = FileUtils.readFileToString(file);
            return str;
        }
        catch (IOException e) {
            log.error(e);
        }
        return "";
    }
    
    private static boolean strCmp(String arg0, String arg1) {
        if (arg0 == null && arg1 == null) {
            return true;
        }
        if (arg0 == null) {
            return false;
        }
        return arg0.equals(arg1);                         
    }

    /**
     * Implement equals based on moduleLsid and license
     */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof EulaInfo)) {
            return false;
        }
        
        EulaInfo eulaInfo = (EulaInfo) obj;
        return strCmp(moduleLsid, eulaInfo.moduleLsid) && strCmp(license, eulaInfo.license);
    }

    /**
     * Implement hashCode based on moduleLsid and license
     */
    public int hashCode() {
        String key = "lsid="+moduleLsid+", license="+license;
        return key.hashCode();
    }
    
    /**
     * Implement compare to based on name, lsid, and lsid version
     */
    public int compareTo(EulaInfo eulaInfo) {
        int i=theLsid.compareTo(eulaInfo.theLsid);
        if (i != 0) {
            return i;
        }
        return license.compareTo(eulaInfo.license);
    }

}