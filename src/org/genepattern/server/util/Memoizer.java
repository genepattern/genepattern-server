package org.genepattern.server.util;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Use ConcurrentHashMap to cache results of computations.
 * 
 * Copied from JCIP (2006), p. 108
 * @author Brian Goetz and Tim Peierls
 *
 * @param <A>
 * @param <V>
 */
public class Memoizer<A,V> implements Computable<A, V> {
    private final ConcurrentMap<A, Future<V>> cache = new ConcurrentHashMap<A, Future<V>>();
    private final Computable<A,V> c;
    
    public Memoizer(Computable<A,V> c) {
        this.c = c;
    }
    
    public V compute(final A arg) throws InterruptedException {
        while(true) {
            Future<V> f = cache.get(arg);
            if (f == null) {
                Callable<V> eval = new Callable<V>() {
                    public V call() throws InterruptedException {
                        return c.compute(arg);
                    }
                };
                FutureTask<V> ft = new FutureTask<V>(eval);
                f = cache.putIfAbsent(arg, ft);
                if (f == null) {
                    f = ft;
                    ft.run();
                }
            }
            try {
                return f.get();
            }
            catch (CancellationException e) {
                cache.remove(arg, f);
            }
            catch (ExecutionException e) {
                throw LaunderThrowable.launderThrowable(e.getCause());
            }
        }
    }

    /**
     * Add precomputed value to the cache.
     * @param pre
     */
    public void put(final A key, final V pre) {
        Future<V> f = new Future<V>() {
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }
            public V get() throws InterruptedException, ExecutionException {
                return pre;
            }

            public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return pre;
            }

            public boolean isCancelled() {
                return false;
            }

            public boolean isDone() {
                return true;
            }
        };
        cache.putIfAbsent(key, f);
    }
}


