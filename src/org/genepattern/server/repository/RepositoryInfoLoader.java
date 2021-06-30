/*******************************************************************************
 * Copyright (c) 2003-2021 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.repository;

import java.util.List;

public interface RepositoryInfoLoader {
    public void setCurrentRepository(String repositoryUrl);
    public RepositoryInfo getCurrentRepository();
    public List<RepositoryInfo> getRepositories();
    public RepositoryInfo getRepository(String repositoryUrl);
}
