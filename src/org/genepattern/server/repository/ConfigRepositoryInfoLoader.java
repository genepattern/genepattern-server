package org.genepattern.server.repository;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.executor.CommandProperties.Value;

/**
 * A RepositoryInfoLoader based on system properties and server configuration files.
 * 
 * The current repository and the list of repositories are set in the genepattern.properties file,
 * and can be edited on the Server Settings -> Repositories page.
 * 
 * Additional details, such as label, icon, brief and full description are configured with the
 * server configuration yaml file (config.yaml, config_default.yaml or config_custom.yaml depending
 * on your server).
 * 
 * @author pcarr
 */
public class ConfigRepositoryInfoLoader implements RepositoryInfoLoader {
    final static private Logger log = Logger.getLogger(ConfigRepositoryInfoLoader.class);
    final static public String BROAD_PROD_URL="http://www.broadinstitute.org/webservices/gpModuleRepository";
    final static public String BROAD_BETA_URL="http://www.broadinstitute.org/webservices/betaModuleRepository";
    final static public String BROAD_DEV_URL="http://www.broadinstitute.org/webservices/gpModuleRepository?env=dev";
    final static public String GPARC_URL="http://vgpprod01.broadinstitute.org:4542/gparcModuleRepository";
    
    final Context serverContext=ServerConfiguration.Context.getServerContext();
    
    final static Map<String, RepositoryInfo> cache=new ConcurrentHashMap<String, RepositoryInfo>();
    
    public ConfigRepositoryInfoLoader() {
    }

    @Override
    public RepositoryInfo getCurrentRepository() {
        final String moduleRepositoryUrl=System.getProperty(RepositoryInfo.PROP_MODULE_REPOSITORY_URL, BROAD_PROD_URL);
        RepositoryInfo info=getRepository(moduleRepositoryUrl);
        if (info != null) {
            return info;
        }
        log.error("Error initializing repository info for current repository: "+moduleRepositoryUrl);
        return getRepository(BROAD_PROD_URL);
    }

    @Override
    public List<RepositoryInfo> getRepositories() {
        final LinkedHashSet<String> repoUrls=new LinkedHashSet<String>();
        
        //hard-coded items
        repoUrls.add(BROAD_PROD_URL);
        repoUrls.add(GPARC_URL);
        repoUrls.add(BROAD_BETA_URL);
        
        //check for repos in the config.yaml file
        Value reposFromConfigFile=ServerConfiguration.instance().getValue(serverContext, "org.genepattern.server.repository.RepositoryUrls");
        if (reposFromConfigFile != null) {
            for(final String repoUrl : reposFromConfigFile.getValues()) {
                if (!repoUrls.contains(repoUrl)) {
                    repoUrls.add(repoUrl);
                }
            }
        }
        
        //check for repos from the gp.properties file (also set via Server Settings -> Repositories page)
        final List<String> fromProps=getModuleRepositoryUrlsFromGpProps();
        for(final String fromProp : fromProps) {
            if (!repoUrls.contains(fromProp)) {
                repoUrls.add(fromProp);
            }
        }

        //check for (and set) details in the server configuration yaml file
        final List<RepositoryInfo> repos=new ArrayList<RepositoryInfo>();
        for(final String repoUrl : repoUrls) {
            final RepositoryInfo info = initRepositoryInfo(repoUrl);
            //refresh cache if necessary
            cache.put(repoUrl, info);
            repos.add(info);
        }
        return repos;
    }
    
    @Override
    public RepositoryInfo getRepository(final String repositoryUrl) {
        log.debug("repositoryUrl="+repositoryUrl);
        RepositoryInfo cached = cache.get(repositoryUrl);
        if (cached != null) {
            return cached;
        }
        RepositoryInfo info = initRepositoryInfo(repositoryUrl);
        if (info == null) {
            log.error("Error initializing RepositoryInfo for "+repositoryUrl);
        }
        else {
            cache.put(repositoryUrl, info);
        }
        return info;
    }
    
    private RepositoryInfo initRepositoryInfo(final String repoUrl) {
        final URL url;
        try {
            url=new URL(repoUrl);
        }
        catch (MalformedURLException e) {
            log.error("Invalid moduleRepositoryUrl: "+repoUrl, e);
            return null;
        }
        
        final String label_key=repoUrl+"/about/label";
        final String brief_key=repoUrl+"/about/brief";
        final String full_key =repoUrl+"/about/full";
        final String icon_key =repoUrl+"/about/icon";
        
        final String label=ServerConfiguration.instance().getGPProperty(serverContext, label_key, repoUrl);
        final String brief=ServerConfiguration.instance().getGPProperty(serverContext, brief_key);
        final String full=ServerConfiguration.instance().getGPProperty(serverContext, full_key);
        final String icon=ServerConfiguration.instance().getGPProperty(serverContext, icon_key);
        RepositoryInfo info=new RepositoryInfo(label, url);
        if (brief != null) {
            info.setBriefDescription(brief);
        }
        if (full != null) {
            info.setFullDescription(full);
        }
        if (icon != null) {
            info.setIconImgSrc(icon);
        }
        return info;
    }

    static private List<String> getModuleRepositoryUrlsFromGpProps() { 
        final String moduleRepositoryUrls=System.getProperty(RepositoryInfo.PROP_MODULE_REPOSITORY_URLS, BROAD_PROD_URL);
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

    // ... exploring the possibility of loading a map of values directly from the config.yaml file ...
//    private static void initFromConfig(final Context userContext) {
//        Map<?,?> detailsMap=Collections.emptyMap();
//        final Value urls=ServerConfiguration.instance().getValue(userContext, "org.genepattern.server.repository.RepositoryUrls");
//        
//        final Value detailsValue=ServerConfiguration.instance().getValue(userContext, "org.genepattern.server.repository.RepositoryDetails");
//        if (detailsValue != null && detailsValue.isMap() && detailsValue.getMap() != null) {
//            detailsMap=detailsValue.getMap();
//        }
//        
//        if (urls != null) {
//            for(final String url : urls.getValues()) {
//                log.debug("adding repository: "+url);
//            }
//        }
//        
//        for(final String url : getModuleRepositoryUrlsFromGpProps()) {
//            final Object detailsForUrl=detailsMap.get(url);
//            
//            //expecting a map
//            if (detailsForUrl instanceof Map<?,?>) {
//                Map<?,?> entry = (Map<?,?>) detailsForUrl;
//                Object label=entry.get("label");
//                Object icon=entry.get("icon");
//                Object brief=entry.get("brief");
//                Object full=entry.get("full");
//            }
//        }
//    }


}
