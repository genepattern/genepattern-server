/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.server.repository.ConfigRepositoryInfoLoader;
import org.genepattern.server.repository.RepositoryInfo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * See RepositoryInfoLoader class, which is runtime interface for reading module repository information from the
 * server. This helper interface is to assist with loading the repository details from the configuration file(s).
 * 
 * @author pcarr
 *
 */
public class GpRepositoryProperties {
    private static Logger log = Logger.getLogger(GpRepositoryProperties.class);

    private final List<Throwable> initErrors;
    private final Map<String,RepositoryInfo> repositoryDetails;
    
    private GpRepositoryProperties(Builder in) {
        this.initErrors=ImmutableList.copyOf(in.initErrors);
        this.repositoryDetails=ImmutableMap.copyOf(in.repositoryDetails);
    }
    
    public List<Throwable> getInitErrors() {
        return initErrors;
    }

    public Set<String> getRepositoryUrls() {
        if (repositoryDetails==null || repositoryDetails.size()==0) {
            return Collections.emptySet();
        }
        return repositoryDetails.keySet();
    }

    public RepositoryInfo getRepositoryInfo(final String url) {
        if (repositoryDetails==null) {
            return null;
        }
        return repositoryDetails.get(url);
    }
    
    public static final class Builder {
        private List<Throwable> initErrors=null;
        private File resourcesDir=null;
        private File repoYaml=null;
        private File repoCustomYaml=null;
        private Map<String, RepositoryInfo> repositoryMapIn=null;
        private Map<String, RepositoryInfo> repositoryDetails=null;

        public Builder resourcesDir(final File resourceDir) {
            this.resourcesDir=resourceDir;
            return this;
        }
        
        public Builder repoConfigFile(final File repoYaml) {
            this.repoYaml=repoYaml;
            return this;
        }
        
        public Builder repoCustomConfigFile(final File repoCustomYaml) {
            this.repoCustomYaml=repoCustomYaml;
            return this;
        }
        
        public Builder addRepositoryInfo(final String key, final RepositoryInfo repositoryInfo) {
            if (repositoryMapIn==null) {
                repositoryMapIn=new LinkedHashMap<String,RepositoryInfo>();
            }
            repositoryMapIn.put(key, repositoryInfo);
            return this;
        }
        
        public GpRepositoryProperties build() {
            initRepositoryDetails();
            return new GpRepositoryProperties(this);
        }
        
        private void initRepositoryDetails() {
            this.initErrors=new ArrayList<Throwable>();
            this.repositoryDetails=new LinkedHashMap<String,RepositoryInfo>();
            if (resourcesDir != null) {
                if (repoYaml == null) {
                    repoYaml = new File(resourcesDir, "repo.yaml");
                }
                else {
                    if (!repoYaml.isAbsolute()) {
                        repoYaml=new File(resourcesDir, repoYaml.getPath());
                    }
                }
                if (repoCustomYaml == null) {
                    repoCustomYaml = new File(resourcesDir, "repo_custom.yaml");
                }
                else {
                    if (!repoCustomYaml.isAbsolute()) {
                        repoCustomYaml=new File(resourcesDir, repoCustomYaml.getPath());
                    }
                }
            }
            if (repoYaml != null) {
                try {
                    this.repositoryDetails.putAll(ConfigRepositoryInfoLoader.parseRepositoryDetailsYaml(repoYaml));
                }
                catch (Throwable t) {
                    initErrors.add(t);
                }
            }
            if (repoCustomYaml != null) {
                try {
                    this.repositoryDetails.putAll(ConfigRepositoryInfoLoader.parseRepositoryDetailsYaml(repoCustomYaml));
                }
                catch (Throwable t) {
                    initErrors.add(t);
                }
            }
            if (repositoryMapIn != null) {
                this.repositoryDetails.putAll(repositoryMapIn);
            }
        }
    }
}
