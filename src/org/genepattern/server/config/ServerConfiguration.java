package org.genepattern.server.config;

import org.apache.log4j.Logger;
import org.genepattern.server.executor.BasicCommandManager;
import org.genepattern.server.executor.CommandManager;
import org.genepattern.server.executor.CommandManagerFactory;
import org.genepattern.server.executor.CommandProperties;
import org.genepattern.server.user.User;
import org.genepattern.webservice.JobInfo;

/**
 * Server configuration.
 * 
 * @author pcarr
 */
public interface ServerConfiguration {

    public static class Context {
        //hard-coded default value is true for compatibility with GP 3.2.4 and earlier
        private boolean initFromSystemProperties = true;
        private User user = null;
        private JobInfo jobInfo = null;
        
        public static Context getContextForUser(String userId) {
            Context context = new Context();
            User user = new User();
            user.setUserId(userId);
            context.setUser(user);
            return context;
        }
        
        public void setInitFromSystemProperties(boolean b) {
            this.initFromSystemProperties = b;
        }
        public boolean getInitFromSystemProperties() {
            return initFromSystemProperties;
        }
        
        public void setUser(User user) {
            this.user = user;
        }
        public User getUser() {
            return user;
        }
        
        public void setJobInfo(JobInfo jobInfo) {
            this.jobInfo = jobInfo;
        }
        public JobInfo getJobInfo() {
            return jobInfo;
        }
        
    }

    public CommandProperties getGPProperties(Context context);

    public static class Factory {
        public static ServerConfiguration instance() {
            return Impl.serverConfiguration;
        }
    }
    
    static class Impl implements ServerConfiguration {
        private static Logger log = Logger.getLogger(Impl.class);
        static ServerConfiguration serverConfiguration = new Impl();
        
        public CommandProperties getGPProperties(Context context) {
            CommandManager cmdMgr = CommandManagerFactory.getCommandManager();
            //HACK: until I change the API for the CommandManager interface to take a Context arg
            if (cmdMgr instanceof BasicCommandManager) {
                BasicCommandManager defaultCmdMgr = (BasicCommandManager) cmdMgr;
                return defaultCmdMgr.getConfigProperties().getCommandProperties(context);
            }
            
            //this should not be called unless the default (BasicCommandManager) implementation is replaced
            log.error("getCommandProperties(jobInfo) is deprecated; GP 3.3.2 uses getCommandProperties(Context)");
            if (context.jobInfo != null) {
                return CommandManagerFactory.getCommandManager().getCommandProperties(context.jobInfo);
            }
            CommandProperties defaultProperties = CommandManagerFactory.getCommandManager().getCommandProperties(null);
            return defaultProperties;
        }
    }
}
