/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input;

/**
 * Unique identifier for a parameter in a module.
 * @author pcarr
 *
 */
public class ParamId {
    /** reserved parameter id for saving the baseGpHref to which the given job was submitted */
    public static final ParamId BASE_GP_HREF=new ParamId("__GP_BASE_GP_HREF");
    
    transient int hashCode;
    private final String fqName;
    public ParamId(final String fqName) {
        if (fqName==null) {
            throw new IllegalArgumentException("fqName==null");
        }
        if (fqName.length()==0) {
            throw new IllegalArgumentException("fqName is empty");
        }
        this.fqName=fqName;
        this.hashCode=fqName.hashCode();
    }
    //copy constructor
    public ParamId(final ParamId in) {
        this.fqName=in.fqName;
        this.hashCode=fqName.hashCode();
    }
    public String getFqName() {
        return fqName;
    }

    public boolean equals(Object obj) {
        if (obj instanceof ParamId) {
            return fqName.equals( ((ParamId) obj).fqName );
        }
        return false;
    }
    public int hashCode() {
        return hashCode;
    }
}
