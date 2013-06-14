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

public class DefaultRepositoryInfoLoader implements RepositoryInfoLoader {
    final static private Logger log = Logger.getLogger(DefaultRepositoryInfoLoader.class);
    
    static RepositoryInfo broadPublic;
    static RepositoryInfo gparc;
    static RepositoryInfo broadBeta;
    static RepositoryInfo broadDev;
    
    /**
     * Initialize default repositories.
     */
    static {
         //Broad public repository
         try {
             broadPublic=new RepositoryInfo("Broad public", new URL("http://www.broadinstitute.org/webservices/gpModuleRepository"));
             broadPublic.setDescription("A repository of production quality modules developed and or curated by the GenePattern team.");
             broadPublic.setIconImgSrc("images/broad-symbol.gif");
         }
         catch (MalformedURLException e) {
             log.error(e);
         }

         //GParc repository
         try {
             gparc=new RepositoryInfo("GParc (GenePattern Archive)", new URL("http://vgpprod01.broadinstitute.org:4542/gparcModuleRepository"));
             gparc.setDescription("A repository of modules developed by GenePattern users.");
             gparc.setIconImgSrc("images/gparc_logo.png");

         }
         catch (MalformedURLException e) {
             log.error(e);
         }
         
         //Broad beta repository
         try {
             broadBeta=new RepositoryInfo("Broad beta", new URL("http://www.broadinstitute.org/webservices/betaModuleRepository"));
             broadBeta.setDescription("A repository of beta quality modules developed and or curated by the GenePattern team.");
             broadPublic.setIconImgSrc("images/broad-symbol.gif");
         }
         catch (MalformedURLException e) {
             log.error(e);
         }
         
         //Broad dev repository (only available via Broad internal network)
         try {
             broadDev=new RepositoryInfo("Broad dev", new URL("http://www.broadinstitute.org/webservices/gpModuleRepository?env=dev"));
             broadDev.setDescription("A repository of internal development quality modules developed by the GenePattern team.");
         }
         catch (MalformedURLException e) {
             log.error(e);
         }
    }
    
    private static Map<String, RepositoryInfo> initRepositoryMap(final Context userContext) {
         final Map<String, RepositoryInfo> repositoryMap=new LinkedHashMap<String, RepositoryInfo>();
         repositoryMap.put(broadPublic.getUrl().toExternalForm(), broadPublic);
         repositoryMap.put(gparc.getUrl().toExternalForm(), gparc);
         repositoryMap.put(broadBeta.getUrl().toExternalForm(), broadBeta);
         
         final Context serverContext=ServerConfiguration.Context.getServerContext();
         //TODO: set this flag's default value to 'false', it's true for debugging
         final boolean includeDevRepository=ServerConfiguration.instance().getGPBooleanProperty(serverContext, "includeDevRepository", true);         
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
    public RepositoryInfo getCurrentRepository() {
        //final String moduleRepositoryUrl=ServerConfiguration.instance().getGPProperty(userContext, "ModuleRepositoryURL", "http://www.broadinstitute.org/webservices/gpModuleRepository");
        final String moduleRepositoryUrl=System.getProperty(RepositoryInfo.PROP_MODULE_REPOSITORY_URL, broadPublic.getUrl().toExternalForm());
        
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
                log.error(e);
            }
        }
        
        //TODO: if we're here it's an error
        log.error("Didn't find a matching RepositoryInfo for the currentRepository: "+moduleRepositoryUrl);
        return broadPublic;
    }

    @Override
    public List<RepositoryInfo> getRepositories() {
        return Collections.unmodifiableList(repositoryList);
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
