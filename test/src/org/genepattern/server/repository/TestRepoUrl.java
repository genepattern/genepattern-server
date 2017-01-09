package org.genepattern.server.repository;

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.genepattern.server.config.GpContext;
import org.junit.Test;

/**
 * Integration test for the default module repository urls.
 * 
 * @author pcarr
 */
public class TestRepoUrl {

    /**
     * Integration test for the built-in list of module repositories.
     * Invokes the RepositoryInfo.getRepositoryInfoLoader method. 
     * 
     * The other tests in this file are more straightforwar. They directly
     * load the details from the remote URL, without first initializing a server config instance
     * to get the list of repo urls.
     */
    @Test
    public void loadRepositories() {
        final GpContext serverContext=GpContext.getServerContext();
        final RepositoryInfoLoader loader = RepositoryInfo.getRepositoryInfoLoader(serverContext);
        final List<RepositoryInfo> repositories=loader.getRepositories();
        
        final Map<String,RepositoryInfo> map=new HashMap<String,RepositoryInfo>();
        for(final RepositoryInfo repoInfo : repositories) {
            map.put(repoInfo.getUrl().toExternalForm(), repoInfo);
        }
        
        //RepositoryInfo beta=map.get("http://software.broadinstitute.org/webservices/gpModuleRepository");
        assertEquals("Broad production (new)", 
                map.get(RepositoryInfo.BROAD_PROD_URL).getLabel());
        assertEquals("Broad beta (new)", 
                map.get(RepositoryInfo.BROAD_BETA_URL).getLabel());
        assertEquals("GParc (GenePattern Archive)", 
                map.get(RepositoryInfo.GPARC_URL).getLabel());
    }

    /**
     * Helper method to test a module repository url. First it pings the url, then it
     * loads the RepositoryInfo.
     * 
     * @param repoUrlStr, the module repository url
     * @return the details loaded from the remote url
     */
    protected static RepositoryInfo checkRepoInfoFromUrl(final String repoUrlStr) {
        final int ping_timeout_millis=5000;
        URL repoUrl;
        try {
            repoUrl = new URL(repoUrlStr);
            final boolean success=ConfigRepositoryInfoLoader.ping(repoUrl.toExternalForm(), ping_timeout_millis);
            assertTrue("ping", success);
            return ConfigRepositoryInfoLoader.initDetailsFromRepo(repoUrl);
        }
        catch (MalformedURLException e) {
            fail("MalformedURL: '"+repoUrlStr+"'");
        }
        fail("Unexpected error initializing details from '"+repoUrlStr+"'");
        return null;
    }
    
    @Test
    public void checkBroadProdUrl() throws MalformedURLException {
        final RepositoryInfo repoInfo=checkRepoInfoFromUrl(RepositoryInfo.BROAD_PROD_URL);
        assertEquals("repoInfo.label", "Broad production (new)", repoInfo.getLabel());
    }

    @Test
    public void checkBroadBetaUrl() throws MalformedURLException {
        final RepositoryInfo repoInfo=checkRepoInfoFromUrl(RepositoryInfo.GPARC_URL);
        assertEquals("repoInfo.label", "GParc (GenePattern Archive)", repoInfo.getLabel());
    }

    @Test
    public void checkGparcUrl() throws MalformedURLException {
        final RepositoryInfo repoInfo=checkRepoInfoFromUrl(RepositoryInfo.BROAD_BETA_URL);
        assertEquals("repoInfo.label", "Broad beta (new)", repoInfo.getLabel());
    }

    @Test
    public void checkBroadDevUrl() throws MalformedURLException {
        final RepositoryInfo repoInfo=checkRepoInfoFromUrl(RepositoryInfo.BROAD_DEV_URL);
        assertEquals("repoInfo.label", "Broad dev", repoInfo.getLabel());
    }

}
