package org.genepattern.server.util;

import junit.framework.TestCase;

public class AuthorizationManagerTest extends TestCase {

    public void testCreateAuthorizationManager() {
        AuthorizationManager manager = new AuthorizationManager();
        
        //<url link="pipelineDesigner.jsp" permission="createPipeline" /> 
        assertTrue(manager.actionPermission.size() > 0);

    }

}
