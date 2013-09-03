package org.genepattern.server.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
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
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.user.UserDAO;
import org.yaml.snakeyaml.Yaml;

/**
 * A RepositoryInfoLoader based on system properties and server configuration files.
 * 
 * The current repository and the list of repositories are set in the genepattern.properties file,
 * and can be edited on the Server Settings -> Repositories page.
 * 
 * Additional details, such as label, icon, brief and full description are optionally loaded from the 
 * module repository if an about.yaml file is present.
 * <pre>
 *     <repoUrl>/about/about.yaml
 * e.g. for the Broad production repository,
 *     http://www.broadinstitute.org/webservices/gpModuleRepository/about/about.yaml
 * e.g. for the Broad dev repository,
 *     http://www.broadinstitute.org/webservices/gpModuleRepository/about/dev/about.yaml
 * </pre>
 * 
 * If an about.yaml file is not present in the repository, 
 * details are loaded from the 'repo.yaml' and/or 'repo_custom.yaml' file.
 * 
 * @author pcarr
 */
public class ConfigRepositoryInfoLoader implements RepositoryInfoLoader {
    final static private Logger log = Logger.getLogger(ConfigRepositoryInfoLoader.class);
    
    //cached map of repositoryUrl to RepositoryInfo
    final static private Map<String, RepositoryInfo> cache=new ConcurrentHashMap<String, RepositoryInfo>();
    final static public void clearCache() {
        cache.clear();
    }

    /**
     * Pings a HTTP URL. This effectively sends a HEAD request and returns <code>true</code> if the response code is in 
     * the 200-399 range.
     * @param url The HTTP URL to be pinged.
     * @param timeout The timeout in millis for both the connection timeout and the response read timeout. Note that
     * the total timeout is effectively two times the given timeout.
     * @return <code>true</code> if the given HTTP URL has returned response code 200-399 on a HEAD request within the
     * given timeout, otherwise <code>false</code>.
     * 
     * Method contributed to StackOverflow by BalusC.
     */
    final static public boolean ping(String url, int timeout) {
        url = url.replaceFirst("https", "http"); // Otherwise an exception may be thrown on invalid SSL certificates.

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            return (200 <= responseCode && responseCode <= 399);
        } 
        catch (IOException exception) {
            return false;
        }
        catch (Throwable t) {
            log.error("Unexpected exception", t);
            return false;
        }
    }
    
    final static private List<String> getModuleRepositoryUrlsFromGpProps() { 
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

    /**
     * Load the repository details from a the given configuration file,
     * for example 'resources/repo.yaml'.
     * 
     * @param repositoryDetailsFile
     * @return a Map, possibly empty if there was an error reading the file.
     */
    final static public Map<String,RepositoryInfo> parseRepositoryDetailsYaml(final File repositoryDetailsFile) throws Exception {
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
    
    final static private Map<String,RepositoryInfo> loadDetailsFromUrl(final URL url) throws Exception {  
        if (url==null) {
            log.error("url==null");
            return Collections.emptyMap();
        }        
        log.debug("loading repository details from "+url);

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
        Iterable<Object> yamlFiles;
        try {
            yamlFiles=yaml.loadAll(yamlStr);
        }
        catch (Throwable t) {
            log.error("Error parsing repository file: "+url.toExternalForm(), t);
            throw new Exception("Error parsing "+url.toExternalForm()+": "+t.getLocalizedMessage());
        }
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
     * Can be either in JSON or YAML format.
     * See the 'repo.yaml' file in the resources directory for an example.
     * 
     * @param yamlDocument, the contents of the yaml (or json) file loaded from the file system
     *     or via external url.
     *     
     * @return a RepositoryInfo instance or null if there are parser errors
     */
    final static private RepositoryInfo initFromYaml(final Object yamlDocument) {
        return initFromYaml(null, yamlDocument);
    }

    final static private RepositoryInfo initFromYaml(final String urlIn, final Object yamlDocument) {
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
    
    final private Context userContext; 
    private String currentRepository=RepositoryInfo.BROAD_PROD_URL;

    public ConfigRepositoryInfoLoader(Context userContext) {
        if (userContext==null) {
            log.debug("User context==null");
            this.userContext=ServerConfiguration.Context.getServerContext();
        }
        else {
            this.userContext=userContext;
            if (userContext.getUserId()==null || userContext.getUserId().length()==0) {
                log.debug("userContext.userId is not set");
            }
            else {
                boolean inTransaction=HibernateUtil.isInTransaction();
                try {
                    UserDAO userDao=new UserDAO();
                    this.currentRepository=userDao.getPropertyValue(userContext.getUserId(), RepositoryInfo.PROP_MODULE_REPOSITORY_URL, RepositoryInfo.BROAD_PROD_URL);
                }
                finally {
                    if (!inTransaction) {
                        HibernateUtil.closeCurrentSession();
                    }
                }
            }            
        }
    }

    @Override
    public void setCurrentRepository(final String repositoryUrl) {
        this.currentRepository=repositoryUrl;
        if (userContext==null || userContext.getUserId()==null || userContext.getUserId().length()==0) {
            //when userId us not set, match previous <= 3.6.0 functionality by saving as a global system property
            log.error("userContext is null or userContext.userId is not set");
            System.setProperty(RepositoryInfo.PROP_MODULE_REPOSITORY_URL, repositoryUrl);
            return;
        }
        
        boolean inTransaction=HibernateUtil.isInTransaction();
        try {
            UserDAO userDao = new UserDAO();
            userDao.setProperty(userContext.getUserId(), RepositoryInfo.PROP_MODULE_REPOSITORY_URL, repositoryUrl);
            if (!inTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error("Error in setCurrentRepository("+repositoryUrl+") for user="+userContext.getUserId(), t);
            HibernateUtil.rollbackTransaction();
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    @Override
    public RepositoryInfo getCurrentRepository() {
        //final String moduleRepositoryUrl=System.getProperty(RepositoryInfo.PROP_MODULE_REPOSITORY_URL, RepositoryInfo.BROAD_PROD_URL);
        RepositoryInfo info=getRepository(currentRepository);
        if (info != null) {
            return info;
        }
        log.error("Error initializing repository info for current repository: "+currentRepository);
        return getRepository(RepositoryInfo.BROAD_PROD_URL);
    }

    @Override
    public List<RepositoryInfo> getRepositories() {
        final LinkedHashSet<String> repoUrls=new LinkedHashSet<String>();
        
        //check for entries in the server configuration
        final Set<String> urlsFromConfig=ServerConfiguration.instance().getRepositoryUrls();
        for(final String urlFromConfig : urlsFromConfig) {
            repoUrls.add(urlFromConfig);
        }

        //check for repos from the gp.properties file (also set via Server Settings -> Repositories page)
        final List<String> fromProps=getModuleRepositoryUrlsFromGpProps();
        for(final String fromProp : fromProps) {
            if (!repoUrls.contains(fromProp)) {
                repoUrls.add(fromProp);
            }
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
        info = initDetailsFromRepo(url);
        
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
     * Optionally get the details directly from the repository. By convention the details are checked for in the
     * following pre-set locations, relative the the repository URL.
     * 
     *     <repoUrl>/about/about.yaml for a production repository
     *     <repoUrl>/about/dev/about.yaml for a development repository
     * 
     * If there is no about.yaml file the module repository will return the full XML catalog. 
     * We don't want this, so we first check for the following,
     * 
     *     <repoUrl>/about/about.jsp
     *     or
     *     <repoUrl>/about/dev/about.jsp
     * 
     * If this file is not present, the repository will return a 404 Not Found.
     * 
     * @param repoUrl, the url of the repository
     * 
     * @return
     */
    private RepositoryInfo initDetailsFromRepo(final URL repoUrl) {
        if (repoUrl==null) {
            log.error("repoUrl==null");
            return null;
        }
        log.debug("remote check for repository details: "+repoUrl);
        
        //if necessary, strip the query string from the base url
        String base=repoUrl.toExternalForm();
        String query=repoUrl.getQuery();
        if (query != null) {
            base=base.substring(0, base.lastIndexOf("?"));
        }
        //check for dev, queryString will be "env=dev"
        boolean isDev=false;
        if ("env=dev".equals(query)) {
            isDev=true;
        }
        base += "/about";
        if (isDev) {
            base += "/dev";
        }
        
        String pingLink = base+"/about.jsp";
        
        //1st ping
        boolean exists=ping(pingLink, 5000);
        if (!exists) {
            log.debug("No details available in repository: "+repoUrl);
            return null;
        }

        URL aboutUrl;
        try {
            final String aboutLink = base+"/about.yaml";
            aboutUrl=new URL(aboutLink);
        }
        catch (MalformedURLException e) {
            log.error(e);
            return null;
        }
        log.debug("aboutUrl="+aboutUrl);
        
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

}
