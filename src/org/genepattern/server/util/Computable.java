/*******************************************************************************
 * Copyright (c) 2003-2021 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.util;

public interface Computable<A,V> {
    V compute(A arg) throws InterruptedException;
}
