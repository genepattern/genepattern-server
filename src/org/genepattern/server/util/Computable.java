package org.genepattern.server.util;

public interface Computable<A,V> {
    V compute(A arg) throws InterruptedException;
}
