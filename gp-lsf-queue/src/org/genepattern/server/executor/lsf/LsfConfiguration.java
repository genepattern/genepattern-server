package org.genepattern.server.executor.lsf;

import java.util.Properties;

import org.genepattern.webservice.JobInfo;

public interface LsfConfiguration {
    void reloadConfiguration() throws Exception;
    
    Properties getHibernateOptions();
    String getProperty(String key);
    String getProperty(String key, String defaultValue);
    LsfProperties getLsfProperties(JobInfo jobInfo);
}
