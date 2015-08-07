/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.purger;

/**
 * Top level factory class for accessing the purger instance for the system.
 * 
 * @author pcarr
 *
 */
public class PurgerFactory {
    public static final Purger instance() {
        return Singleton02.instance;
    }
    
    private PurgerFactory() {
    }
    
    //this is the original (GP <= 3.7.3 implementation)
    private static class Singleton01 {
        static Purger instance=new DefaultPurgerImpl01();
    }

    private static class Singleton02 {
        static Purger instance=new DefaultPurgerImpl02();
    }
}
