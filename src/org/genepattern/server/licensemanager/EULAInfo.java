package org.genepattern.server.licensemanager;

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
public class EULAInfo {
    private String ID;
    private String content;

    //the lsid of the module which requires the EULA
    private String moduleLsid;
    //the value of the license= property in the manifest for the module
    private String license;
    
    public void setLicense(String license) {
        this.license=license;
    }
    public void setModuleLsid(String lsid) {
        this.moduleLsid=lsid;
    }
    public String getModuleLsid() {
        return this.moduleLsid;
    }
    
    public String getLink() {
        //http://gpbroad.broadinstitute.org:8080/gp/getTaskDoc.jsp?name=urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:9&file=ComparativeMarkerSelection.pdf
        String rel="getTaskDoc?name="+moduleLsid+"&file="+license;
        return rel;
    }
    
    public String getContext() {
        final String NL = "\n";
        final String content=
                "To view the license agreement, click this link: "+NL+
                "    <a href=\""+getLink()+"\"></a>";
        return content; 
    }
}