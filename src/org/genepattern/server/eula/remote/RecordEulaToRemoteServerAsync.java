package org.genepattern.server.eula.remote;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.log4j.Logger;
import org.genepattern.server.eula.EulaInfo;

/**
 * POST eula to remote server, asynchronously, by using a background thread.
 * 
 * Note: When using java concurrency Executors class, make sure to shut down the executors properly.
 * See this for more info,
 *     http://stackoverflow.com/questions/1211657/how-to-shut-down-all-executors-when-quitting-an-application
 * Or, use a custom ThreadFactory, and create daemon thread, which is how it's implemented.
 * 
 * @author pcarr
 *
 */
public class RecordEulaToRemoteServerAsync  {
    final static private Logger log = Logger.getLogger(RecordEulaToRemoteServerAsync.class);

    final private ExecutorService getExec() {
        return ExecSingleton.INSTANCE.getExec();
    }

    //lazy-init ExecutorService singleton
    final static private class ExecSingleton {
        final static public ExecSingleton INSTANCE=new ExecSingleton();
        private ExecutorService exec=null;
        private ExecSingleton() {
            exec=initExecutorService();
        }
        public ExecutorService getExec() {
            return exec;
        }
        private static ExecutorService initExecutorService() {
            //use a ThreadFactory, so that we can force each thread to be a daemon thread
            ThreadFactory threadFactory=new ThreadFactory() {
                private long i=0L;
                //@Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    t.setName(""+RecordEulaToRemoteServerAsync.class.getSimpleName()+"-"+i);
                    ++i;
                    return t;
                }
            };
            return Executors.newSingleThreadExecutor(threadFactory);
        }
    } 

    public void recordLicenseAgreement(final String userId, final EulaInfo eula, final String remoteUrl) throws Exception {
        log.debug("asynchronous POST to recordLicenseAgreement ..."); 
        runWithExecutor(userId, eula, remoteUrl);
    }

    private void runWithExecutor(final String userId, final EulaInfo eula, final String remoteUrl) {
        ExecutorService exec=getExec();
        Runnable r = new Runnable() {
            //@Override
            public void run() {
                log.debug("running thread...");
                RecordEulaToRemoteServer record = new RecordEulaToRemoteServer(remoteUrl);
                try {
                    record.recordLicenseAgreement(userId, eula);
                }
                catch (Exception e) {
                    //ignore
                }
            }
        };
        log.debug("submitting thread...");
        exec.submit(r);
    }

}
