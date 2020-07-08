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
        
        //RepositoryInfo beta=map.get("https://modulerepository.genepattern.org/gpModuleRepository/");
        assertEquals("GenePattern production (new)", 
                map.get(RepositoryInfo.GP_PROD_URL).getLabel());
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
            final boolean success=ConfigRepositoryInfoLoader.ping(repoUrlStr, ping_timeout_millis);
            assertTrue("ping "+repoUrlStr, success);
            return ConfigRepositoryInfoLoader.initDetailsFromRepo(repoUrl);
        }
        catch (MalformedURLException e) {
            fail("MalformedURL: '"+repoUrlStr+"'");
        }
        fail("Unexpected error initializing details from '"+repoUrlStr+"'");
        return null;
    }
    
    @Test
    public void checkGPProdUrl() throws MalformedURLException {
        final RepositoryInfo repoInfo=checkRepoInfoFromUrl(RepositoryInfo.GP_PROD_URL);
        assertEquals("repoInfo.label", "GenePattern production (new)", repoInfo.getLabel());
    }

    @Test
    public void checkGparcUrl() throws MalformedURLException {
        final RepositoryInfo repoInfo=checkRepoInfoFromUrl(RepositoryInfo.GPARC_URL);
        assertEquals("repoInfo.label", "GParc (GenePattern Archive)", repoInfo.getLabel());
    }

    @Test
    public void checkGPBetaUrl() throws MalformedURLException {
        final RepositoryInfo repoInfo=checkRepoInfoFromUrl(RepositoryInfo.GP_BETA_URL);
        assertEquals("repoInfo.label", "GenePattern beta", repoInfo.getLabel());
    }

    @Test
    public void checkGPDevUrl() throws MalformedURLException {
        final RepositoryInfo repoInfo=checkRepoInfoFromUrl(RepositoryInfo.GP_DEV_URL);
        assertEquals("repoInfo.label", "GenePattern dev", repoInfo.getLabel());
    }

}
