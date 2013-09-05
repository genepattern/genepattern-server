package org.genepattern.server.job.input.cache;

import java.io.File;
import java.net.MalformedURLException;
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

import org.apache.log4j.Logger;

/**
 * Maintain a cache of input files downloaded from an external source. 
 * 
 * Note: May want to replace this hand-coded class with an implementation from the 
 * Google guava library (https://code.google.com/p/guava-libraries/wiki/CachesExplained).
 * 
 * Use-cases:
 * 1) a single run of a job with a cached input file
 * 2) two different jobs (running concurrently) each using the same exact cached input file.
 * 
 * @author pcarr
 *
 */
public class FileCache {
    private static Logger log = Logger.getLogger(FileCache.class);

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
    private final ConcurrentMap<String, Future<CachedFile>> cache = new ConcurrentHashMap<String, Future<CachedFile>>();
    
    /**
     * For the given url, we have some options:
     *     a) it's mapped (by the gp-admin) to a local path
     *     b) it's a cached data file in one of the following states:
     *         i) not yet downloaded
     *         ii) downloading
     *         iii) download complete, with error
     *         iv) download complete, no errors
     *         v) download complete, out of date (Note: this state is not implemented)
     * 
     * @param externalUrl
     * @return
     */
    private CachedFile initCachedFileObj(final String externalUrl) {
        final File mappedFile=MapLocalEntry.initLocalFileSelection(externalUrl);
        if (mappedFile!=null) {
            if (!mappedFile.exists()) {
                log.error("mappedFile does not exist, mapping from "+externalUrl+" to "+mappedFile);
            }
            else {
                try {
                    return new MapLocalFile(externalUrl, mappedFile);
                }
                catch (MalformedURLException e) {
                    log.error("Invalid externalUrl="+externalUrl, e);
                }
            }
        }
        return new CachedFtpFile(externalUrl);
    }
    
    public void shutdownNow() {
        downloadService.shutdownNow();
        scheduledService.shutdownNow();
    }
    
    static class AlreadyDownloaded implements Future<CachedFile> {
        final CachedFile obj;
        public AlreadyDownloaded(final CachedFile obj) {
            this.obj=obj;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public CachedFile get() throws InterruptedException, ExecutionException {
            return obj;
        }

        @Override
        public CachedFile get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
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
    }

    public synchronized Future<CachedFile> getFutureObj(final String externalUrl) {
        final CachedFile obj = initCachedFileObj(externalUrl);
        if (obj.isDownloaded()) {
            //already downloaded
            return new AlreadyDownloaded(obj);
        }
        final String key=obj.getUrl().toExternalForm();
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        //otherwise start the download and add to the cache
        Future<CachedFile> f = downloadService.submit(new Callable<CachedFile>() {
            @Override
            public CachedFile call() throws Exception {
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
        Future<CachedFile> f2 = cache.putIfAbsent(key, f);
        if (f2 == null) {
            f2 = f;
        }
        return f2;
    }
}
