package org.genepattern.server.job.input.choice;

/**
 * Interface for listing the contents of a remote directory, such as an ftp site.
 * @author pcarr
 *
 * @param <T>, the type of each entry
 * @param <E>, the type of exception thrown in case of errors.
 */
public interface RemoteDirLister<T,E extends Exception> {
    T[] listFiles(final String remoteUrl) throws E;
}
