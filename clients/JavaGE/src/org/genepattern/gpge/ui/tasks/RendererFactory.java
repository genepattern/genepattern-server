/*
 * RendererFactory.java
 *
 * Created on March 26, 2003, 8:27 AM
 */

package org.genepattern.gpge.ui.tasks;

import org.genepattern.client.AnalysisService;

/**
 *
 * @author  kohm
 */
public interface RendererFactory {
    /**
     * returns an UIRenderer array for rendering the params or null if couldn't process
     * any params.  After returning the input java.util.List will contain any remaining 
     * ParameterInfo objects that were not processed. Note the params can be run through
     * the next RendererFactory to produce more Renderers.
     *
     */
    public UIRenderer createRenderer(final AnalysisService service, java.util.List params);
}
