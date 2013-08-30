package org.genepattern.server.job.input.cache;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Maintain a cache of input files downloaded from an external source. 
 * 
 * Use-cases:
 * 1) a single run of a job with a cached input file
 * 2) two different jobs (running concurrently) each using the same exact cached input file.
 * 
 * States of a File Choice file
 * 1) INIT (no local copy)
 * 2) TRANSFERRING
 * 2a)     TRANSFERRING_TO_TMP
 * 2b)     MOVING_FROM_TMP_TO_ACTUAL
 * 2c)     RECORDING_TO_DB
 * 3) CACHED (an up-to-date local copy)
 * 4) OUT_OF_DATE (the local copy is older than the remote version)
 * 5) DOWNLOAD_ERROR (there was an error during the TRANSFER)
 * 
 * @author pcarr
 *
 */
public class FileCache {
    private static final FileCache instance=new FileCache();
    public static FileCache instance() {
        return instance;
    }
    private FileCache() {
    }

    //max number of simultaneous downloads
    int maxNumOfThreads=5;
    private final ExecutorService downloadService=Executors.newFixedThreadPool(maxNumOfThreads);
    private final ScheduledExecutorService scheduledService = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentMap<String, Future<CachedFileObj>> cache = new ConcurrentHashMap<String, Future<CachedFileObj>>();
    
    public CachedFileObj initCachedFileObj(final String externalUrl) {
        return new CachedFileObj(externalUrl);
    }
    
    public void shutdownNow() {
        downloadService.shutdownNow();
        scheduledService.shutdownNow();
    }
    
    public synchronized Future<CachedFileObj> getFutureObj(final String externalUrl) {
        final CachedFileObj obj = initCachedFileObj(externalUrl);
        if (obj.isDownloaded()) {
            //already downloaded
            return new Future<CachedFileObj>() {

                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return false;
                }

                @Override
                public CachedFileObj get() throws InterruptedException, ExecutionException {
                    return obj;
                }

                @Override
                public CachedFileObj get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    return obj;
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }

                @Override
                public boolean isDone() {
                    return true;
                }
            };
        }
        final String key=obj.getUrl().toExternalForm();
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        //otherwise start the download and add to the cache
        Future<CachedFileObj> f = downloadService.submit(new Callable<CachedFileObj>() {
            @Override
            public CachedFileObj call() throws Exception {
                DownloadException ex=null;
                try {
                    obj.download();
                }
                catch (DownloadException e) {                    
                    //swallow it, we'll deal later
                    ex=e;
                }
                //schedule removal of Future from cache
                scheduledService.schedule(new Runnable() {
                    @Override
                    public void run() {
                        cache.remove(key);
                    }
                },
                //remove from cache 5 minutes from now
                300, TimeUnit.SECONDS);
                
                if (ex != null) {
                    //TODO: implement pause and retry
                    throw ex;
                }
                return obj;
            }
        });
        Future<CachedFileObj> f2 = cache.putIfAbsent(key, f);
        if (f2 == null) {
            f2 = f;
        }
        return f2;
    }
}
