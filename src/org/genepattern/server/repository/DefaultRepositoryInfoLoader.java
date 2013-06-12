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
    final static private Map<String, RepositoryInfo> repositoryMap;
    final static private List<RepositoryInfo> repositoryList;
    static {
         repositoryMap=new LinkedHashMap<String, RepositoryInfo>();
         
         //Broad public repository
         try {
             broadPublic=new RepositoryInfo("Broad public", new URL("http://www.broadinstitute.org/webservices/gpModuleRepository"));
             broadPublic.setDescription("Modules developed and tested by the GenePattern development team");
             repositoryMap.put(broadPublic.getUrl().toExternalForm(), broadPublic);
         }
         catch (MalformedURLException e) {
             log.error(e);
         }

         //GParc repository
         try {
             gparc=new RepositoryInfo("GParc", new URL("http://vgpprod01.broadinstitute.org:4542/gparcModuleRepository"));
             gparc.setDescription("Modules developed by GenePattern users are available on GParc, the GenePattern Archive.");
             repositoryMap.put(gparc.getUrl().toExternalForm(), gparc);
         }
         catch (MalformedURLException e) {
             log.error(e);
         }
         
         //Broad dev repository (only available via Broad internal network)
         final Context serverContext=ServerConfiguration.Context.getServerContext();
         final boolean includeDevRepository=ServerConfiguration.instance().getGPBooleanProperty(serverContext, "includeDevRepository", true);
         if (includeDevRepository) {
         try {
             RepositoryInfo dev=new RepositoryInfo("Broad dev", new URL("http://www.broadinstitute.org/webservices/gpModuleRepository?env=dev"));
             dev.setDescription("Broad internal dev repository, only available via Broad internal network");
             repositoryMap.put(dev.getUrl().toExternalForm(), dev);
         }
         catch (MalformedURLException e) {
             log.error(e);
         }
         }
         
         repositoryList=new ArrayList<RepositoryInfo>();
         repositoryList.addAll(repositoryMap.values());
    }
    
    
    final Context userContext;
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
        return broadPublic;
    }

    @Override
    public List<RepositoryInfo> getRepositories() {
        return Collections.unmodifiableList(repositoryList);
    }

}
