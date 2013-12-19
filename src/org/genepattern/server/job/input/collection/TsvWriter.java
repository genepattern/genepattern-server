package org.genepattern.server.job.input.collection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import org.apache.log4j.Logger;


/**
 * Implementation of TableWriter interface which outputs a TAB-delimited file.
 * Newlines are hard-coded to unix-style newlines.
 * 
 * @author pcarr
 *
 */
public class TsvWriter implements TableWriter {
    private static final Logger log = Logger.getLogger(TsvWriter.class);
    
    /**
     * recommended mime-type for the generated file.
     */
    public static final String MIME_TYPE="text/tab-separated-values";
    /**
     * recommended extension for the generated file.
     */
    public static final String EXT="tsv";
    
    public static final String TAB="\t";
    public static final String COMMENT_CHAR="#";
    public static final String NL="\n"; //newline
    
    private Writer writer;

    @Override
    public void init(final File toFile) throws IOException {
        final CharsetEncoder enc=Charset.forName("UTF-8").newEncoder();
        writer = new BufferedWriter
                (new OutputStreamWriter(new FileOutputStream(toFile),enc));
    }

    @Override
    public void writeComment(final String comment) throws IOException {
        if (writer==null) {
            log.error("");
            return;
        }
        writer.write(COMMENT_CHAR);
        writer.write(comment);
        writer.write(NL);
    }

    @Override
    public void writeRow(final String[] values) throws IOException {
        boolean first=true;
        for(final String value : values) {
            if (first) {
                first=false;
            }
            else {
                writer.write(TAB);
            }
            writer.write(value);
        }
        writer.write(NL);
    }

    @Override
    public void finish() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }
}
