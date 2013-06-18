package org.genepattern.server.repository;

import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.repository.SourceInfo.FromRepo;
import org.genepattern.server.repository.SourceInfo.FromUnknown;
import org.genepattern.webservice.TaskInfo;

/**
 * Dummy implementation of the SourceInfoLoader interface,
 * with hard-coded mapping from a module name to a SourceInfo.
 * 
 * @author pcarr
 *
 */
public class StubSourceInfoLoader implements SourceInfoLoader {

    @Override
    public SourceInfo getSourceInfo(final TaskInfo taskInfo) {
        if (taskInfo.getName().equalsIgnoreCase("ConvertLineEndings")) {
            //hard-coded as if CLE was installed from the public repository
            final Context serverContext=ServerConfiguration.Context.getServerContext();
            final RepositoryInfo repoInfo=RepositoryInfo.getRepositoryInfoLoader(serverContext).getRepositories().get(0);
            final SourceInfo sourceInfo = new FromRepo(repoInfo);
            return sourceInfo;
        }
        if (taskInfo.getName().equalsIgnoreCase("ComparativeMarkerSelection")) {
            //hard-coded as if CMS was installed from GParc
            final Context serverContext=ServerConfiguration.Context.getServerContext();
            final RepositoryInfo repoInfo=RepositoryInfo.getRepositoryInfoLoader(serverContext).getRepositories().get(1);
            final SourceInfo sourceInfo = new FromRepo(repoInfo);
            return sourceInfo;
        }
        if (taskInfo.getName().equalsIgnoreCase("PreprocessDataset")) {
            //hard-coded as if PD was installed from beta repository
            final Context serverContext=ServerConfiguration.Context.getServerContext();
            final RepositoryInfo repoInfo=RepositoryInfo.getRepositoryInfoLoader(serverContext).getRepositories().get(2);
            final SourceInfo sourceInfo = new FromRepo(repoInfo);
            return sourceInfo;
        }

        //everything else is unknown
        return new FromUnknown();
    }

}
