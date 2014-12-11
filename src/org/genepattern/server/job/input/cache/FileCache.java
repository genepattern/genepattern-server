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
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;

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
    
    /**
     * Special userid for cached data files from external URL.
     * These shared files are saved in the user uploads tab for the '.cache' user account. 
     */
    public static final String CACHE_USER_ID=".cache";
    
    private static final FileCache instance=new FileCache();
    public static FileCache instance() {
        return instance;
    }
    private FileCache() {
    }

    //max number of simultaneous downloads
    int maxNumOfThreads=5;
    private final ExecutorService downloadService=Executors.newFixedThreadPool(maxNumOfThreads, new ThreadFactory() {
        long i=0;
        @Override
        public Thread newThread(final Runnable r) {
            final Thread t = new Thread(r);
            t.setName("DownloadService-"+i++);
            return t;
        }
    });
    private final ScheduledExecutorService evictionService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(final Runnable r) {
            final Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName(""+FileCache.class.getSimpleName()+"-EvictionService");
            return t;
        }
    });
    
    
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
    private CachedFile initCachedFileObj(final GpConfig gpConfig, final GpContext jobContext, final String externalUrl, final boolean isRemoteDir) {
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
        
        /*
         * change the default ftp downloader by making an edit to the config.yaml file, e.g.  
         *     ftpDownloader.type: JAVA_6
         *     ftpDownloader.type: COMMONS_NET_3_3
         *     ftpDownloader.type: EDT_FTP_J
         *     ftpDownloader.type: EDT_FTP_J_SIMPLE
         */
        if (!isRemoteDir) {
            return CachedFtpFile.Factory.instance().newCachedFtpFile(gpConfig, externalUrl);
        }
        else {
            return new CachedFtpDir(gpConfig, jobContext, externalUrl);
        }
     }
    
    public void shutdownNow() {
        downloadService.shutdownNow();
        evictionService.shutdownNow();
        CachedFtpFile.Factory.instance().shutdownNow();
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

    public synchronized Future<CachedFile> getFutureObj(final GpConfig gpConfig, final GpContext jobContext, final String externalUrl, final boolean isRemoteDir) {
        if (log.isDebugEnabled()) {
            StringBuffer sb=new StringBuffer();
            sb.append("checking for cached ");
            if (isRemoteDir) {
                sb.append("directory ");
            }
            else {
                sb.append("file ");
            }
            sb.append(" from "+externalUrl+" ... ");
            log.debug(sb.toString());
        }
        final CachedFile obj = initCachedFileObj(gpConfig, jobContext, externalUrl, isRemoteDir);
        if (obj.isDownloaded()) {
            //already downloaded
            if (log.isDebugEnabled()) {
                log.debug("already downloaded");
            }
            return new AlreadyDownloaded(obj);
        }
        final String key=obj.getUrl().toExternalForm();
        if (cache.containsKey(key)) {
            if (log.isDebugEnabled()) {
                log.debug("already downloading");
            }
            return cache.get(key);
        }
        //otherwise start the download and add to the cache
        Future<CachedFile> f = downloadService.submit(new Callable<CachedFile>() {
            @Override
            public CachedFile call() throws Exception {
                DownloadException ex=null;
                try {
                    log.debug("starting download, downloader="+obj.getClass().getName());
                    obj.download();
                }
                catch (DownloadException e) {  
                    //swallow it, we'll deal later
                    ex=e;
                }  
                catch (Throwable t) {
                    ex = new DownloadException("Unexpected exception during download - "+t.getClass().getName(), t);
                }
                //schedule removal of Future from cache
                final int evictionInterval_sec = ex!=null ? 
                        30 //evict after 30 seconds if there was an Exception during the file download
                        : 
                        300; //otherwise after 5 minutes
                scheduleForEviction(key, evictionInterval_sec);

                if (ex != null) {
                    //TODO: implement pause and retry
                    if (log.isDebugEnabled()) {
                        log.debug("Error downloading from "+externalUrl, ex);
                    }
                    throw ex;
                }

                if (log.isDebugEnabled()) {
                    log.debug("completed download from "+externalUrl);
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
    
    /**
     * Remove the item from the cache at some time in the future, roughly 
     * equivalent to the secondsFromNow value.
     * 
     * @param key, an externalUrl as a string
     * @param secondsFromNow, the amount of time to wait before removing from the cache
     */
    private void scheduleForEviction(final String key, int secondsFromNow) {
        evictionService.schedule(new Runnable() {
            @Override
            public void run() {
                cache.remove(key);
            }
        },
        //remove from cache N seconds from now (30 for failed downloades, 300 for successful downloads)
        secondsFromNow, TimeUnit.SECONDS);
    }
}
