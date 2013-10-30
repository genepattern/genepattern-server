package org.genepattern.server.purger;

/**
 * Top level factory class for accessing the purger instance for the system.
 * 
 * @author pcarr
 *
 */
public class PurgerFactory {
    public static final Purger instance() {
        return Singleton.instance;
    }
    
    private PurgerFactory() {
    }
    
    private static class Singleton {
        static Purger instance=new DefaultPurgerImpl01();
    }
}
