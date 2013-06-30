package org.genepattern.server.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.yaml.snakeyaml.Yaml;

/**
 * A RepositoryInfoLoader based on system properties and server configuration files.
 * 
 * The current repository and the list of repositories are set in the genepattern.properties file,
 * and can be edited on the Server Settings -> Repositories page.
 * 
 * Additional details, such as label, icon, brief and full description are configured in the
 * repositoryDetails.yaml file.
 * 
 * TODO: optionally check for repository details in the repository web site, 
 *     <repoUrl>/about/about.json
 *     <repoUrl>/about/about.yaml
 *     <repoUrl>/about/dev/about.json
 *     <repoUrl>/about/dev/about.yaml 
 * 
 * @author pcarr
 */
public class ConfigRepositoryInfoLoader implements RepositoryInfoLoader {
    final static private Logger log = Logger.getLogger(ConfigRepositoryInfoLoader.class);
        
    final Context serverContext=ServerConfiguration.Context.getServerContext();
    
    //cached map of repositoryUrl to RepositoryInfo
    final static Map<String, RepositoryInfo> cache=new ConcurrentHashMap<String, RepositoryInfo>();
    final static public void clearCache() {
        cache.clear();
    }
    
    public ConfigRepositoryInfoLoader() {
    }

    @Override
    public RepositoryInfo getCurrentRepository() {
        final String moduleRepositoryUrl=System.getProperty(RepositoryInfo.PROP_MODULE_REPOSITORY_URL, RepositoryInfo.BROAD_PROD_URL);
        RepositoryInfo info=getRepository(moduleRepositoryUrl);
        if (info != null) {
            return info;
        }
        log.error("Error initializing repository info for current repository: "+moduleRepositoryUrl);
        return getRepository(RepositoryInfo.BROAD_PROD_URL);
    }

    @Override
    public List<RepositoryInfo> getRepositories() {
        final LinkedHashSet<String> repoUrls=new LinkedHashSet<String>();
        
        //hard-coded items
        repoUrls.add(RepositoryInfo.BROAD_PROD_URL);
        repoUrls.add(RepositoryInfo.GPARC_URL);
        repoUrls.add(RepositoryInfo.BROAD_BETA_URL);
        
        //check for repos from the gp.properties file (also set via Server Settings -> Repositories page)
        final List<String> fromProps=getModuleRepositoryUrlsFromGpProps();
        for(final String fromProp : fromProps) {
            if (!repoUrls.contains(fromProp)) {
                repoUrls.add(fromProp);
            }
        }
        
        //check for entries in the repositoryDetails.yaml file
        final Set<String> urlsFromConfig=ServerConfiguration.instance().getRepositoryUrls();
        for(final String urlFromConfig : urlsFromConfig) {
            repoUrls.add(urlFromConfig);
        }

        //initialize details and update the cache
        final List<RepositoryInfo> repos=new ArrayList<RepositoryInfo>();
        for(final String repoUrl : repoUrls) {
            final RepositoryInfo info = initRepositoryInfo(repoUrl);
            if (info==null) {
                log.error("error initalizing RepositoryInfo for :"+repoUrl);
            }
            else {
                //update the cache
                cache.put(repoUrl, info);
                repos.add(info);
            }
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

    static private List<String> getModuleRepositoryUrlsFromGpProps() { 
        final String moduleRepositoryUrls=System.getProperty(RepositoryInfo.PROP_MODULE_REPOSITORY_URLS, RepositoryInfo.BROAD_PROD_URL);
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

    private RepositoryInfo initRepositoryInfo(final String repoUrl) {
        final URL url;
        try {
            url=new URL(repoUrl);
        }
        catch (MalformedURLException e) {
            log.error("Invalid moduleRepositoryUrl: "+repoUrl, e);
            return null;
        }

        RepositoryInfo info=null;
        // first, check for details in (remote) repository
        // Note: commented out for first check in
        //RepositoryInfo info = initDetailsFromRepo(repoUrl);
        
        // next, check for details in (local) config file, repositoryDetails.yaml
        if (info == null) {
            //info = repositoryDetailsYaml.get(repoUrl);
            info = ServerConfiguration.instance().getRepositoryInfo(repoUrl);
        }
        if (info != null) {
            return info;
        }
        
        info = new RepositoryInfo(url);
        return info; 
    }

    /**
     * Optionally get the details directly from the repository.
     * 
     * @param info
     * @param aboutLink
     * @return
     */
    private RepositoryInfo initDetailsFromRepo(final URL repoUrl) {
        if (repoUrl==null) {
            log.error("repoUrl==null");
            return null;
        }
        
        String aboutLink=repoUrl.toExternalForm();
        String query=repoUrl.getQuery();
        if (query != null) {
            aboutLink=aboutLink.substring(0, aboutLink.lastIndexOf("?"));
        }
        //check for dev, queryString will be "env=dev"
        boolean isDev=false;
        if ("env=dev".equals(query)) {
            isDev=true;
        }
        aboutLink += "/about";
        if (isDev) {
            aboutLink += "/dev";
        }
        aboutLink += "/about.yaml";
        
        URL aboutUrl;
        try {
            aboutUrl=new URL(aboutLink);
        }
        catch (MalformedURLException e) {
            log.error(e);
            return null;
        }
        
        try { 
            Map<String,RepositoryInfo> detailsFromUrl=loadDetailsFromUrl(aboutUrl);
            if (detailsFromUrl == null || detailsFromUrl.size()==0) {
                log.debug("no details available from url: "+aboutUrl);
                return null;
            }
            log.debug("found "+detailsFromUrl.size()+" repository detail entries from "+aboutUrl);
            return detailsFromUrl.get(repoUrl.toExternalForm());
        }
        catch (Throwable t) {
            log.error("Error getting repository details from "+aboutUrl, t);
        }
        
        return null;
        
    }

    public static Map<String,RepositoryInfo> parseRepositoryDetailsYaml() {
        final File repositoryDetailsFile=new File(System.getProperty("resources"), "repositoryDetails.yaml");
        return parseRepositoryDetailsYaml(repositoryDetailsFile);
    }
    public static Map<String,RepositoryInfo> parseRepositoryDetailsYaml(final File repositoryDetailsFile) {
        if (!repositoryDetailsFile.exists()) {
            log.debug("repositoryDetails.yaml does not exist: "+repositoryDetailsFile);
            return Collections.emptyMap();
        }
        if (!repositoryDetailsFile.canRead()) {
            log.error("repositoryDetails.yaml is not readable: "+repositoryDetailsFile);
            return Collections.emptyMap();
        }
        if (!repositoryDetailsFile.isFile()) {
            log.error("repositoryDetails.yaml is not a file: "+repositoryDetailsFile);
            return Collections.emptyMap();
        }
        
        URL url;
        try {
            url=repositoryDetailsFile.toURI().toURL();
        }
        catch (MalformedURLException e) {
            log.error("Unexpected exception getting URL for local file: "+repositoryDetailsFile);
            return Collections.emptyMap();
        }
        
        return loadDetailsFromUrl(url);
    }
    
    private static Map<String,RepositoryInfo> loadDetailsFromUrl(final URL url) {        
        String yamlStr;
        final int connectTimeout=10*1000; //10 seconds
        final int readTimeout=10*1000; //10 seconds
        
        try {
            URLConnection con = url.openConnection();
            con.setConnectTimeout(connectTimeout);
            con.setReadTimeout(readTimeout);
            InputStream in = con.getInputStream();        
            yamlStr = IOUtils.toString(in, "UTF-8");
        }
        catch (IOException e) {
            log.debug("Error loading repositoryDetails.yaml", e);
            return Collections.emptyMap();
        }

        //use linked hash map to preserve order of entries
        Map<String, RepositoryInfo> map=new LinkedHashMap<String,RepositoryInfo>();
        Yaml yaml=new Yaml();
        Iterable<Object> yamlFiles=yaml.loadAll(yamlStr);
        for(Object yamlFile : yamlFiles) {
            RepositoryInfo info=initFromYaml(yamlFile);
            if (info != null) {
                if (info.getUrl() != null) {
                    map.put(info.getUrl().toExternalForm(), info);
                }
            }
        }
        return map;
    }

    /**
     * Parse the yaml representation of the repository info.
     * Can be either in JSON or YAML format. Example format,
     * <pre>
{
"url": "http://www.broadinstitute.org/webservices/gpModuleRepository",
"label": "Broad production",
"icon": "/gp/images/broad-symbol.gif",
"brief": "A repository of GenePattern modules curated by the GenePattern team.",
"full": "The GenePattern production repository containing curated modules which have been developed and fully tested by the Broad Institute's GenePattern team."
}
     * </pre>
     * 
     * @param info, an existing RepositoryInfo instance, set values on this based on the contents of the yaml file.
     * @param yamlStr, the contents of the yaml (or json) file loaded from the file system or via external url.
     * @return
     */
    static private RepositoryInfo initFromYaml(final Object yamlDocument) {
        return initFromYaml(null, yamlDocument);
    }

    static private RepositoryInfo initFromYaml(final String urlIn, final Object yamlDocument) {
        if (!(yamlDocument instanceof Map)) {
            log.error("Expecting an object of type map, returning null RepositoryInfo");
            return null;
        }
        final Map map = (Map) yamlDocument;
        URL url=null;
        String urlStr;
        if (map.containsKey("url")) {
            urlStr = (String) map.get("url");
            try {
                url = new URL(urlStr);
            }
            catch (MalformedURLException e) {
                log.error("Invalid url: "+urlStr, e);
                return null;
            }
        }
        else {
            try {
                url = new URL(urlIn);
            }
            catch (MalformedURLException e) {
                log.error("Invalid urlIn: "+urlIn, e);
                return null;
            }
        }
        RepositoryInfo info = new RepositoryInfo(url);
        if (map.containsKey("label")) {
            info.setLabel((String) map.get("label"));
        }
        if (map.containsKey("icon")) {
            String icon= (String) map.get("icon");
            //handle url vs. relative vs. absolute path
            try {
                URL iconUrl=new URL(icon);
                info.setIconImgSrc(iconUrl.toExternalForm());
            }
            catch (MalformedURLException e) {
                if (icon.startsWith("/")) {
                    info.setIconImgSrc(icon);
                }
                //else if (localFile != null) {
                //    //TODO: handle local relative paths
                //    log.error("pathToIcon not implemented: icon="+icon);
                //}
                else {
                    //TODO: handle local relative paths
                    log.error("pathToIcon not implemented: icon="+icon);
                }
            }
        }
        if (map.containsKey("brief")) {
            info.setBriefDescription( (String) map.get("brief"));
        }
        if (map.containsKey("full")) {
            info.setFullDescription( (String) map.get("full"));
        }
        return info;
    }

}
