package org.genepattern.server.queue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.genepattern.webservice.JobInfo;

/**
 * Factory class for generating instances of CommandExecutorService, based on a given JobInfo.
 * This is here to enable configurable routing of jobs to different types of command executors.
 * By default, jobs are run with calls to RuntimeExec. This factory class can be updated to send jobs to LSF.
 * 
 * @author pcarr
 */
public class CommandExecutorServiceFactory {
    private static Logger log = Logger.getLogger(CommandExecutorServiceFactory.class);

    public static CommandExecutorServiceFactory instance() {
        return Singleton.commandExecutorServiceFactory;
    }

    private static class Singleton {
        static CommandExecutorServiceFactory commandExecutorServiceFactory = new CommandExecutorServiceFactory();
    }
    
    private String defaultQueueId="queue.default";
    private Map<String,CommandExecutorService> map = new HashMap<String,CommandExecutorService>();
    private Map<String,String> map2 = new HashMap<String,String>();

    private CommandExecutorServiceFactory() {
    }
    
    private void init() {
        File queuePropertiesFile = new File(System.getProperty("genepattern.properties"), "queue.properties");
        Properties queueProperties = new Properties();
        try {
            queueProperties.load(new FileInputStream(queuePropertiesFile));
            init(queueProperties);
        } 
        catch (IOException e) {
            log.error("Failed to initialize job queue: "+e.getLocalizedMessage(), e);
        }
    }
    
    private void init(Properties props) {
        Enumeration e = props.propertyNames();
        while(e.hasMoreElements()) {
            String propName = (String) e.nextElement();
            String propValue = props.getProperty(propName);
            if (isQueueId(propName)) {
                loadQueue(propName, propValue);
            }
            else if ("default".equals(propName)) {
                setDefaultQueueId(propValue);
            }
            else {
                appendTask(propName, propValue);
            }
        }
    }
    
    private boolean isQueueId(String propName) {
        return propName.startsWith("queue.");
    }
    
    private void loadQueue(String queueId, String classname) {
        CommandExecutorService svc = loadCommandExecutorService(classname);
        if (svc != null) {
            map.put(queueId, svc);
        }
        else {
            log.error("Failed to initialize queue, queue.id="+queueId+", classname="+classname);
        }
    }
    
    private void setDefaultQueueId(String defaultQueueId) {
        this.defaultQueueId=defaultQueueId;
    }
    
    private void appendTask(String taskName, String queueId) {
        map2.put(taskName, queueId);
    }
    
    private CommandExecutorService loadCommandExecutorService(String svcClassname) {
        CommandExecutorService svc = null;
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Class svcClass = Class.forName(svcClassname, false, classLoader);
            if (!CommandExecutorService.class.isAssignableFrom(svcClass)) {
                log.error(""+svcClass.getCanonicalName()+" does not implement "+CommandExecutorService.class.getCanonicalName());
                return svc;
            }
            svc = (CommandExecutorService) svcClass.newInstance();
            return svc;
        }
        catch (Throwable t) {
            log.error("Error loading CommandExecutorService for classname: "+svcClassname+", "+t.getLocalizedMessage(), t);
        }
        return svc;
    }
    
    public void start() {
        init();
        
        for(CommandExecutorService svc : map.values()) {
            try {
                svc.start();
            }
            catch (Throwable t) {
                log.error("Error starting CommandExecutorService, for class: "+svc.getClass().getCanonicalName()+": "+t.getLocalizedMessage(), t);
            }
        }
    }
    
    public void stop() {
        for(CommandExecutorService svc : map.values()) {
            try {
                svc.stop();
            }
            catch (Throwable t) {
                log.error("Error stopping CommandExecutorService, for class: "+svc.getClass().getCanonicalName()+": "+t.getLocalizedMessage(), t);
            }
        }
    }

    public CommandExecutorService getCommandExecutorService(JobInfo jobInfo) {
        String taskName = jobInfo.getTaskName();
        CommandExecutorService svc = null;
        String queueId = null;

        queueId = map2.get(taskName);
        if (queueId == null) {
            queueId = this.defaultQueueId;
        }

        svc = map.get(queueId);
        //TODO: handle null
        if (svc == null) {
            log.error("no service found for job: "+jobInfo.getJobNumber()+". "+jobInfo.getTaskName());
        }
        return svc;
    }
}
