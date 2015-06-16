/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.config;


/**
 * The ValueLookup interface is the recommended way to access server configuration properties
 * from the GP runtime.
 * @author pcarr
 *
 */
public interface ValueLookup {
    Value getValue(final GpContext context, final String key);
}
