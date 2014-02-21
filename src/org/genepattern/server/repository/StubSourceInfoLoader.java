package org.genepattern.server.repository;

import java.util.List;

import org.genepattern.server.config.GpContext;
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
    private SourceInfo getSourceInfoByIdx(final int idx) {
        final GpContext serverContext=GpContext.getServerContext();
        final RepositoryInfoLoader loader = RepositoryInfo.getRepositoryInfoLoader(serverContext);
        final List<RepositoryInfo> repositories=loader.getRepositories();
        if (repositories != null && repositories.size()>idx) {
            final RepositoryInfo repoInfo=repositories.get(idx);
            final SourceInfo sourceInfo = new FromRepo(repoInfo);
            return sourceInfo;
        }
        //everything else is unknown
        return new FromUnknown();
    }

    @Override
    public SourceInfo getSourceInfo(final TaskInfo taskInfo) {
        if (taskInfo.getName().equalsIgnoreCase("ConvertLineEndings")) {
            //hard-coded as if CLE was installed from the public repository
            return getSourceInfoByIdx(0);
        }
        if (taskInfo.getName().equalsIgnoreCase("ComparativeMarkerSelection")) {
            //hard-coded as if CMS was installed from GParc
            return getSourceInfoByIdx(1);
        }
        if (taskInfo.getName().equalsIgnoreCase("PreprocessDataset")) {
            //hard-coded as if PD was installed from beta repository
            return getSourceInfoByIdx(2);
        }

        //everything else is unknown
        return new FromUnknown();
    }

}
