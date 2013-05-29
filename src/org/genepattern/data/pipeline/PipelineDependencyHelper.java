package org.genepattern.data.pipeline;

/**
 * A factory class for getting the initialized instance of the helper class
 * which implements the PipelineDependency interface.
 * 
 * @author pcarr
 * 
 * @deprecated - use the PipelineDependencyCache instead
 *
 */
public class PipelineDependencyHelper {
    final static PipelineDependencyHelperCached helper = new PipelineDependencyHelperCached();
    static boolean init = false;

    public static PipelineDependency instance() {
        return CachedImpl.INSTANCE;
    }
    
    private static class CachedImpl {
        private static final PipelineDependency INSTANCE = new PipelineDependencyHelperCached();
    }

}
