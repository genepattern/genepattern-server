package org.genepattern.server.repository;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;

/**
 * 
 * @author pcarr
 * @deprecated Use the ConfigRepositoryInfo instead.
 */
public class DefaultRepositoryInfoLoader implements RepositoryInfoLoader {
    final static private Logger log = Logger.getLogger(DefaultRepositoryInfoLoader.class);
    
    //
    // Initialize default repositories.
    //
    //Broad public repository
    final static private RepositoryInfo broadPublic=init("Broad production", "/gp/images/broad-symbol.gif", "http://www.broadinstitute.org/webservices/gpModuleRepository",
            "A repository of GenePattern modules curated by the GenePattern team.",  
            "The GenePattern production repository containing curated modules which have been developed and fully tested by the Broad Institute’s GenePattern team.");

    //GParc repository
    final static private RepositoryInfo gparc=init("GParc (GenePattern Archive)", "/gp/images/gparc_logo.png", RepositoryInfo.GPARC_URL,
            "A GenePattern module repository whose content is contributed by members of the GenePattern community.  These modules are not curated by the GenePattern team.",
                "GParc, short for GenePattern Archive, is both a GenePattern module repository and a web-based community. "+
                "The repository contains GenePattern modules contributed by members of the GenePattern community, for use by that community. "+
                "The GenePattern team does not curate GParc modules.  Community members can go to the <a href=\"http://www.broadinstitute.org/software/gparc/\">GParc website</a> "+
                "to share, browse, discuss and rate the repository’s modules.  The GParc website also provides resources to assist in the authoring and testing of GenePattern modules.");

    //Broad beta repository
    final static private RepositoryInfo broadBeta=init("Broad beta", "/gp/images/broad_beta.png", "http://www.broadinstitute.org/webservices/betaModuleRepository",
            "A GenePattern module repository containing beta releases of GenePattern modules.", 
            "The GenePattern beta repository containing modules which have been developed by the Broad Institute’s GenePattern team but have undergone limited testing. "+
            "Module testing is sufficient to verify it will not harm the GenePattern server in which they run. "+
            "The GenePattern team, however, does not guarantee the module is computationally correct or that its interface will not be subject to change. "+
            "The documentation that accompanies beta modules is likely to be incomplete." );
    
    //Broad dev repository (only available via Broad internal network)
    final static private RepositoryInfo broadDev=init("Broad dev", null, "http://www.broadinstitute.org/webservices/gpModuleRepository?env=dev",
            "A repository of internal development quality modules developed by the GenePattern team." ,
                "A repository of internal development quality modules developed by the GenePattern team (full description).");
    
    final static private RepositoryInfo init(final String label, final String iconImgSrc,
            final String urlStr,
            final String briefDescription,
            final String fullDescription) {
        try {
            RepositoryInfo repoInfo=new RepositoryInfo(label, new URL(urlStr));
            repoInfo.setBriefDescription(briefDescription);
            repoInfo.setFullDescription(fullDescription);
            repoInfo.setIconImgSrc(iconImgSrc);
            return repoInfo;
        }
        catch (MalformedURLException e) {
            log.error(e);
            return null;
        }
    }

    private static Map<String, RepositoryInfo> initRepositoryMap(final Context userContext) {
         final Map<String, RepositoryInfo> repositoryMap=new LinkedHashMap<String, RepositoryInfo>();
         repositoryMap.put(broadPublic.getUrl().toExternalForm(), broadPublic);
         repositoryMap.put(gparc.getUrl().toExternalForm(), gparc);
         repositoryMap.put(broadBeta.getUrl().toExternalForm(), broadBeta);
         
         final Context serverContext=ServerConfiguration.Context.getServerContext();
         //TODO: set this flag's default value to 'false', it's true for debugging
         final boolean includeDevRepository=ServerConfiguration.instance().getGPBooleanProperty(serverContext, "includeDevRepository", false);         
         if (includeDevRepository) {
             repositoryMap.put(broadDev.getUrl().toExternalForm(), broadDev);
         }
         
         //legacy support, include any entries from the Server Settings -> Module Repositories page which aren't in the list
         final List<String> fromGpProps=getModuleRepositoryUrlsFromGpProps();
         for(final String fromGpProp : fromGpProps) {
             if (repositoryMap.containsKey(fromGpProp)) {
                 //ignore, it's already on the list
             }
             else {
                 try {
                     RepositoryInfo info=new RepositoryInfo(new URL(fromGpProp));
                     repositoryMap.put(info.getUrl().toExternalForm(), info);
                 }
                 catch (MalformedURLException e) {
                     //TODO: should notify end-user when an invalid repository URL is entered
                     log.error(e);
                 }
             }
         }
         return repositoryMap;
    }
    
    
    final private Context userContext;
    final private Map<String, RepositoryInfo> repositoryMap;
    final private List<RepositoryInfo> repositoryList;
    
    /**
     * Hint: Create a new instance for each page load. The constructor initializes the list of 
     * repositories. This info could become stale if you hang onto an instance for a while.
     */
    public DefaultRepositoryInfoLoader() {
        this(null);
    }
    public DefaultRepositoryInfoLoader(final Context userContextIn) {
        if (userContextIn==null) {
            userContext=ServerConfiguration.Context.getServerContext();
        }
        else {
            userContext=userContextIn;
        }
        repositoryMap=initRepositoryMap(userContext);
        repositoryList=new ArrayList<RepositoryInfo>();
        repositoryList.addAll(repositoryMap.values());
    }

    @Override
    public void setCurrentRepository(final String repositoryUrl) {
        //TODO: must save this to the DB or else the menu will appear to have no effect
        //initial implementation matches current (<= 3.6.0 functionality) by saving as a global system property
        System.setProperty(RepositoryInfo.PROP_MODULE_REPOSITORY_URL, repositoryUrl);        
    }

    @Override
    public RepositoryInfo getCurrentRepository() {
        final String moduleRepositoryUrl=System.getProperty(RepositoryInfo.PROP_MODULE_REPOSITORY_URL, broadPublic.getUrl().toExternalForm());
        
        RepositoryInfo repositoryInfo=getRepository(moduleRepositoryUrl);
        if (repositoryInfo != null) {
            return repositoryInfo;
        }
                
        //TODO: if we're here it's an error
        log.error("Didn't find a matching RepositoryInfo for the currentRepository: "+moduleRepositoryUrl);
        return broadPublic;
    }

    @Override
    public List<RepositoryInfo> getRepositories() {
        return Collections.unmodifiableList(repositoryList);
    }
    
    @Override
    public RepositoryInfo getRepository(String moduleRepositoryUrl)  { 
        RepositoryInfo repositoryInfo=repositoryMap.get(moduleRepositoryUrl);
        if (repositoryInfo!=null) {
            return repositoryInfo;
        }
        else {
            try {
                repositoryInfo=new RepositoryInfo(new URL(moduleRepositoryUrl));
                return repositoryInfo;
            }
            catch (MalformedURLException e) {
                log.error("Invalid moduleRepositoryUrl="+moduleRepositoryUrl, e);
                return null;
            }
        }
    }

    //
    static private List<String> getModuleRepositoryUrlsFromGpProps() { 
        final String moduleRepositoryUrls=System.getProperty(RepositoryInfo.PROP_MODULE_REPOSITORY_URLS, broadPublic.getUrl().toExternalForm());
        if (moduleRepositoryUrls==null) {
            return Collections.emptyList();
        }
        //can be a single url or a comma-separated list
        final List<String> urls=new ArrayList<String>();
        final String[] splits=moduleRepositoryUrls.split(",");
        if (splits != null) {
            for(final String str : splits) { 
                    urls.add(str);
            }
        }
        return urls;
    }

}
