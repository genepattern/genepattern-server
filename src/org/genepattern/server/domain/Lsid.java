/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

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
