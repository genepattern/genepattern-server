package org.genepattern.server.job.input.collection;

import java.io.File;

/**
 * Interface for writing tabular data, for example into a TAB delimited text file.
 * @author pcarr
 *
 */
public interface TableWriter {
    /**
     * Get the suggesting file extension for the generated file.
     * @return
     */
    String getExtension();

    /**
     * Initialize the writer, called before the first write operation.
     * @param toFile
     * @throws Exception
     */
    void init(final File toFile) throws Exception;
    /**
     * Append a comment.
     * @param comment
     * @throws Exception
     */
    void writeComment(final String comment) throws Exception;
    /**
     * Append a row of values.
     * @param values
     * @throws Exception
     */
    void writeRow(final String[] values) throws Exception;
    /**
     * Close the writer, called after the last write operation.
     * @throws Exception
     */
    void finish() throws Exception;
}
