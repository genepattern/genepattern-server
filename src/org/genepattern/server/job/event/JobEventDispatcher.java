/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.event;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 * Helper class for making job completion events thread safe.
 * Similar to the Swing event dispatch thread, based on Goetz, "Java Concurrency in Practice".
 * 
 * @author pcarr
 */
public class JobEventDispatcher {
    private static final ExecutorService exec = Executors.newSingleThreadExecutor(new JobEventThreadFactory());
    private static class JobEventThreadFactory implements ThreadFactory {
        private long i=0L;
        @Override
        public Thread newThread(Runnable r) {
            //use a ThreadFactory, so that we can force each thread to be a daemon thread
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName(""+JobEventDispatcher.class.getSimpleName()+"-"+i);
            return t;
        }
    }
    
    //generic methods
    public static void invokeLater(Runnable task) {
        exec.execute(task);
    }
    public static void invokeAndWait(Runnable task) throws InterruptedException, ExecutionException {
        Future<?> f = exec.submit(task);
        f.get();
    }

}
