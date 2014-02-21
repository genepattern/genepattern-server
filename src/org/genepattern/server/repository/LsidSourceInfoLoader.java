package org.genepattern.server.repository;

import java.net.MalformedURLException;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.repository.SourceInfo.CreatedOnServer;
import org.genepattern.server.repository.SourceInfo.FromRepo;
import org.genepattern.server.repository.SourceInfo.FromUnknown;
import org.genepattern.server.repository.SourceInfo.FromZip;
import org.genepattern.util.LSID;
import org.genepattern.util.LSIDUtil;
import org.genepattern.webservice.TaskInfo;

/**
 * Quick and dirty implementation of source info loader, using the TaskInfo.lsid to deduce
 * the installation source for the task.
 * 
 * Hint: can probably be a server-wide singleton.
 * 
 * For debugging, ConvertLineEndings is always from GParc.
 * 
 * 
 * @author pcarr
 *
 */
public class LsidSourceInfoLoader implements SourceInfoLoader {
    final static private Logger log = Logger.getLogger(LsidSourceInfoLoader.class);

    @Override
    public SourceInfo getSourceInfo(final TaskInfo taskInfo) {
        if (taskInfo==null) {
            log.error("taskInfo==null");
            return new FromUnknown();
        }

        // example LSID from Broad public repository
        // urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:9
        // urn:lsid:broad.mit.edu:cancer.software.genepattern.module.pipeline:00001:2
        
        final String lsidStr=taskInfo.getLsid();
        if (lsidStr==null || lsidStr.length()==0) {
            log.error("taskInfo.lsid is not set");
            return new FromUnknown();
        }
        
        LSID lsid=null;
        String lsidVersion=null;
        try {
            lsid=new LSID(lsidStr);
            lsidVersion=lsid.getVersion();
        }
        catch (MalformedURLException e) {
            log.error(e);
        }
        
        
        if (lsidStr.toLowerCase().startsWith("urn:lsid:broad.mit.edu:cancer.software.genepattern.module.")) {
            boolean isBeta=false;
            if (lsidVersion != null && lsidVersion.contains(LSID.VERSION_DELIMITER)) {
                isBeta=true;
            }
            else {
                isBeta=false;
            }
            if (!isBeta) {
                GpContext serverContext=GpContext.getServerContext();
                RepositoryInfo prod=
                        RepositoryInfo.getRepositoryInfoLoader(serverContext).getRepository(RepositoryInfo.BROAD_PROD_URL);
                return new FromRepo(prod);
            }
            else {
                // assume it's from Broad beta repository
                GpContext serverContext=GpContext.getServerContext();
                RepositoryInfo beta=
                        RepositoryInfo.getRepositoryInfoLoader(serverContext).getRepository(RepositoryInfo.BROAD_BETA_URL);
                return new FromRepo(beta);
            }
        }
        
        boolean createdOnServer=false;
        final String serverAuthority=LSIDUtil.getInstance().getAuthority();
        if (lsid != null && serverAuthority.equals( lsid.getAuthority() )) {
            //assume it's created/edited on this server
            createdOnServer=true;
        }
        if (createdOnServer) {
            return new CreatedOnServer();
        }
        
        return new FromZip();
    }

}
