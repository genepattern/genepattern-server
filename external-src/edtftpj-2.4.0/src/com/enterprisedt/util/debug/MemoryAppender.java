package com.enterprisedt.util.debug;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;

/**
 * Appends log statements to an in-memory buffer.
 *
 *  @author      Hans Andersen
 *  @version     $Revision$
 */
public class MemoryAppender extends StreamAppender {

	/**
	 * Default size of memory-buffer (100k).
	 */
	public final static int DEFAULT_BUFFER_SIZE = 100*1024;
	
	/**
	 * Create a MemoryAppender whose buffer has the given initial size.
	 * @param size Initial size of buffer.
	 */
	public MemoryAppender(int initSize) {
		super(new ByteArrayOutputStream(initSize));
	}
	
	/**
	 * Create a MemoryAppender whose buffer is initially set to 100k.
	 */
	public MemoryAppender() {
		this(DEFAULT_BUFFER_SIZE);
	}
	
	/**
	 * Returns the current buffer.
	 * @return the current buffer.
	 */
	public byte[] getBuffer() {
		synchronized (log) {
			log.flush();
			return ((ByteArrayOutputStream)outStr).toByteArray();
		}
	}
	
	/**
	 * Returns a reader that may be used to access the current buffer.
	 * A snapshot of the buffer is taken at the time of invocation;
	 * any logging written to the appender after the invocation will not
	 * be accessible through the reader.
	 * @return A reader for the current buffer.
	 */
	public Reader getReader() {
		return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(getBuffer())));
	}
	
    /* (non-Javadoc)
     * @see java.lang.String.toString()
     */
	public String toString() {
		return new String(getBuffer());
	}
	
	/**
	 * Writes the entire buffer to the given writer.
	 * @param writer Writer to write to.
	 */
	public void print(PrintStream stream) {
		BufferedReader reader = new BufferedReader(getReader());
		String line = null;
		try {
			while ((line=reader.readLine())!=null)
				stream.println(line);
		} catch (IOException e) {
			e.printStackTrace(stream);
		}
	}
	
	/**
	 * Writes the entire buffer to stdout.
	 */
	public void print() {
		print(System.out);
	}
}
