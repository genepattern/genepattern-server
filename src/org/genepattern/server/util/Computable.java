/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.util;

public interface Computable<A,V> {
    V compute(A arg) throws InterruptedException;
}
