package org.genepattern.server.job.input.choice;

import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
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
import org.genepattern.server.util.LaunderThrowable;

/**
 * Helper method for a caching files associated with file drop-down items.
 * This is for modules which have File Choice parameters, for the case when a user selects
 * a file from the (static or dynamically generated) drop-down menu for a file input parameter.
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
 *     GpFilePath actualPath=initActualPath(URL url);
 *     Future<GpFilePath> future=getFileDownloader(URL url);
 *     
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
    
    public static class Demo {
        /**
         * Map of cached files, indexed by external URL.
         * Note: URL is not a good key for a hashmap. It's hashcode method performs a DNS lookup.
         * Store the process (aka Future) in the cache so that we can get the status of a
         * file while it is being transferred.
         * 
         */
        private final ConcurrentMap<String, Future<GpFilePath>> cache = new ConcurrentHashMap<String, Future<GpFilePath>>();

        private Callable<GpFilePath> initCallableFileDownloader(final Choice selectedChoice) {
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

        private Future<GpFilePath> initOrGetFileDownloader(final Choice selectedChoice) {
            final URL _url=JobInputHelper.initExternalUrl(selectedChoice.getValue());
            final String urlKey=_url.toExternalForm();
            Future<GpFilePath> f = cache.get(urlKey);
            if (f == null) {
                Callable<GpFilePath> eval = initCallableFileDownloader(selectedChoice);
                FutureTask<GpFilePath> ft = new FutureTask<GpFilePath>(eval);
                f = cache.putIfAbsent(urlKey, ft);
                if (f == null) {
                    f = ft;
                    ft.run();
                }
            }
            return f;
        }

        /**
         * Call this if you want a TimeoutException after 0.1 second, instead of waiting for the transfer to complete.
         * 
         * @param selectedChoice
         * @return
         * @throws InterruptedException
         * @throws ExecutionException
         * @throws TimeoutException
         */
        public GpFilePath getCachedGpFilePath(final Choice selectedChoice)  throws InterruptedException, ExecutionException, TimeoutException {
            return getCachedGpFilePath(selectedChoice, 100, TimeUnit.MILLISECONDS);
        }

        public GpFilePath getCachedGpFilePath(final Choice selectedChoice, final long timeout, final TimeUnit unit)  throws InterruptedException, ExecutionException, TimeoutException {
            Future<GpFilePath> f = initOrGetFileDownloader(selectedChoice);
            if (f.isDone()) {
                return f.get();
            }
            return f.get(timeout, unit);
        }

        /**
         * Call this if you want to wait for the transfer to complete.
         * 
         * @param selectedChoice
         * @return
         * @throws InterruptedException
         */
        public GpFilePath getCachedGpFilePathWait(final Choice selectedChoice) throws InterruptedException {
            while(true) {
                Future<GpFilePath> f=initOrGetFileDownloader(selectedChoice);
            
                //copied from memoizer
                try {
                    return f.get();
                }
                catch (CancellationException e) {
                    final URL _url=JobInputHelper.initExternalUrl(selectedChoice.getValue());
                    final String urlKey=_url.toExternalForm();
                    cache.remove(urlKey, f);
                }
                catch (ExecutionException e) {
                    throw LaunderThrowable.launderThrowable(e.getCause());
                }
            }
        }
    }

}
