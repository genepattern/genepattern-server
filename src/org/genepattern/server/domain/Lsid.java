/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2008) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.domain;

import java.net.MalformedURLException;

import org.apache.log4j.Logger;
import org.genepattern.util.LSID;

public class Lsid {
    private static Logger log = Logger.getLogger(Lsid.class);
    private String lsid;
    private String lsidNoVersion;
    private String version;
    
    public Lsid() {
    	
    }

    /**
     * Create an instance from a string representation.  
     */
    public Lsid(String lsidString) {
        try {
        	org.genepattern.util.LSID tmp = new LSID(lsidString);
			this.setLsid(tmp.toString());
			this.setLsidNoVersion(tmp.toStringNoVersion());
			this.setVersion(tmp.getVersion());
		} catch (MalformedURLException e) {
			log.error("Error creating lsid", e);
		}

    	
    }
    
    public String getLsid() {
        return lsid;
    }

    public void setLsid(String lsid) {
        this.lsid = lsid;
    }

    public String getLsidNoVersion() {
        return lsidNoVersion;
    }

    public void setLsidNoVersion(String lsidNoVersion) {
        this.lsidNoVersion = lsidNoVersion;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String lversion) {
        this.version = lversion;
    }

}
