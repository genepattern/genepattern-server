package org.genepattern.server.eula.remote;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.log4j.Logger;
import org.genepattern.server.eula.EulaInfo;
import org.genepattern.server.eula.RecordEula;

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
public class RecordEulaToRemoteServerAsync implements RecordEula {
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

    //@Override
    public void recordLicenseAgreement(final String userId, final EulaInfo eula) throws Exception {
        log.debug("asynchronous POST to recordLicenseAgreement ..."); 
        // (original implementation) runAsNewThread(userId, eula);
        runWithExecutor(userId, eula);
    }

    private void runWithExecutor(final String userId, final EulaInfo eula) {
        ExecutorService exec=getExec();
        Runnable r = new Runnable() {
            //@Override
            public void run() {
                log.debug("running thread...");
                RecordEulaToRemoteServer record = new RecordEulaToRemoteServer();
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

//
//    The original implementation, created a new Thread for each POST
//
//    private void runAsNewThread(final String userId, final EulaInfo eula) {
//        //create a new thread for each POST
//        Runnable r = new Runnable() {
//            //@Override
//            public void run() {
//                log.debug("running thread...");
//                RecordEulaToRemoteServer record = new RecordEulaToRemoteServer();
//                try {
//                    record.recordLicenseAgreement(userId, eula);
//                }
//                catch (Exception e) {
//                    //ignore
//                }
//            }
//        };        
//        Thread t = new Thread(r);
//        log.debug("starting thread...");
//        t.start();
//    }

    //@Override
    public boolean hasUserAgreed(final String userId, final EulaInfo eula) throws Exception {
        throw new Exception("Not implemented!");
    }

    //@Override
    public Date getUserAgreementDate(final String userId, final EulaInfo eula) throws Exception {
        throw new Exception("Not implemented!");
    }

}
