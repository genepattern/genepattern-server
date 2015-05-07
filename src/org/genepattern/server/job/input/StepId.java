/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input;

/**
 * Unique identifier for a step in a pipeline.
 * @author pcarr
 */
public class StepId {
    private String id;

    public StepId(final String id) {
        this.id=id;
    }

    //copy constructor
    public StepId(final StepId in) {
        this.id=in.id;
    }

    public String getId() {
        return id;
    }

    public int hashCode() {
        if (id==null) {
            return "".hashCode();
        }
        return id.hashCode();
    }
    public boolean equals(Object obj) {
        if (!(obj instanceof StepId)) {
            return false;
        }
        if (id==null) {
            return ((StepId)obj).id==null;
        }
        return id.equals(((StepId)obj).id);
    }
}
