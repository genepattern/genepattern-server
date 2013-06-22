package org.genepattern.server.repository;

import java.util.List;

public interface RepositoryInfoLoader {
    public RepositoryInfo getCurrentRepository();
    public List<RepositoryInfo> getRepositories();
    public RepositoryInfo getRepository(String repositoryUrl);
}
