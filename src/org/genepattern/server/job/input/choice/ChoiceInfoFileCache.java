package org.genepattern.server.job.input.choice;

import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.JobInputHelper;
import org.genepattern.server.job.input.ParamListHelper;

/**
 * Helper method for a caching files associated with file drop-down items.
 * This is for modules which have File Choice parameters, for the case when a user selects
 * a file from (static or dynamically generated) drop-down menu for a file input parameter.
 * 
 * Use-cases:
 * 1) a single run of a job with a File Choice selection
 * 2) two different jobs (running concurrently) each using the same exact File Choice selection value.
 * 
 * States of a File Choice file
 * 1) INIT (no local copy)
 * 2) TRANSFERRING
 * 2a)     TRANSFERRING_TO_TMP
 * 2b)     MOVING_FROM_TMP_TO_ACTUAL
 * 2c)     RECORDING_TO_DB
 * 3) CACHED (an up-to-date local copy)
 * 4) OUT_OF_DATE (the local copy is older than the remote version)
 * 
 * Helper methods,
 *     GpFilePath actual=getActualValue(URL url);
 *     Future<GpFilePath> future=getFileDownloader(URL url);
 *     
 *     GpFilePath actualPath=initActualPath(URL url);
 *     // to check if we have a local copy, actualPath.getServerFile().exists();
 *     // to check if the local copy is out of date, actualPath.getServerFile().getTime();
 *     Date getRemoteTimestamp(URL url);
 *     
 * 
 * @author pcarr
 *
 */
public class ChoiceInfoFileCache {
    private static Demo demo=new Demo();
    public static Demo instance() {
        return demo;
    }
    
//    private static class Wrapper {
//        final GpFilePath localPath;
//        final Exception ex;            
//        public Wrapper(final GpFilePath localPath, Exception ex) {
//            this.localPath=localPath;
//            this.ex=ex;
//        }
//    }
//    static interface MyInterface {
//        /**
//         * Gets the cached local copy for the given URL. This method waits
//         * for the file to be transferred.
//         * 
//         * @param url
//         * @return
//         */
//        public GpFilePath getLocalCopy(URL url) throws Exception;
//        public Future<GpFilePath> getLocalCopier(URL url);
//
//        /**
//         * Gets the method for transferring the file.
//         * @param url
//         * @return
//         */
//        public Computable<URL,Wrapper> getComputation(URL url);
//    }
    
    
    public static class Demo {
        /**
         * Map of cached files, indexed by external URL.
         * store the process (aka Future) in the cache so that we can get the status of a
         * file while it is being transferred.
         */
        private final ConcurrentMap<URL, Future<GpFilePath>> cache = new ConcurrentHashMap<URL, Future<GpFilePath>>();

        private Callable<GpFilePath> initCallable(final Choice selectedChoice) {
            Callable<GpFilePath> eval = new Callable<GpFilePath>() {
                public GpFilePath call() throws Exception {
                    final URL url=JobInputHelper.initExternalUrl(selectedChoice.getValue());
                    GpFilePath actualPath=null;
                    actualPath=ChoiceInfoHelper.getLocalPathFromSelection(selectedChoice);
                    ParamListHelper.copyExternalUrlToUserUploads(actualPath, url);
                    return actualPath;
                }
            };
            return eval;
        }

//        private Callable<GpFilePath> initCallable(final GpFilePath actualPath, final URL url) {
//            Callable<GpFilePath> eval = new Callable<GpFilePath>() {
//                public GpFilePath call() throws Exception {
//                    ParamListHelper.copyExternalUrlToUserUploads(actualPath, url);
//                    return actualPath;
//                }
//            };
//            return eval;
//        }
        
        private Future<GpFilePath> initFuture(final Choice selectedChoice) {
            final URL url=JobInputHelper.initExternalUrl(selectedChoice.getValue());
            Future<GpFilePath> f = cache.get(url);
            if (f == null) {
                Callable<GpFilePath> eval = initCallable(selectedChoice);
                FutureTask<GpFilePath> ft = new FutureTask<GpFilePath>(eval);
                f = cache.putIfAbsent(url, ft);
                if (f == null) {
                    f = ft;
                    ft.run();
                }
            }
            return f;
        }

//        private Future<GpFilePath> initFuture(final GpFilePath actualPath, final URL url) {
//            Future<GpFilePath> f = cache.get(url);
//            if (f == null) {
//                Callable<GpFilePath> eval = initCallable(actualPath, url);
//                FutureTask<GpFilePath> ft = new FutureTask<GpFilePath>(eval);
//                f = cache.putIfAbsent(url, ft);
//                if (f == null) {
//                    f = ft;
//                    ft.run();
//                }
//            }
//            return f;
//        }

        
        //call this if you want an TimeoutException after 0.1 second, instead of waiting for the transfer to complete
        public GpFilePath getCachedGpFilePath(final Choice selectedChoice)  throws InterruptedException, ExecutionException, TimeoutException {
            Future<GpFilePath> f = initFuture(selectedChoice);
            if (f.isDone()) {
                return f.get();
            }
            return f.get(100, TimeUnit.MILLISECONDS);
        }

//        //call this if you want to wait for the transfer to complete
//        public GpFilePath getCachedGpFilePathWait(final GpFilePath actualPath, final URL url) throws InterruptedException {
//            while(true) {
//                Future<GpFilePath> f=initFuture(actualPath, url);
//            
//                //copied from memoizer
//                try {
//                    return f.get();
//                }
//                catch (CancellationException e) {
//                    cache.remove(url, f);
//                }
//                catch (ExecutionException e) {
//                    throw LaunderThrowable.launderThrowable(e.getCause());
//                }
//            }
//        }
    }

}
