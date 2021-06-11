package com.enterprisedt.util.debug;

import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Appends log statements to a given stream.
 *
 *  @author      Hans Andersen
 *  @version     $Revision$
 */
public class StreamAppender implements Appender {
	
	/**
	 * Writer used to write to the buffer.
	 */
	protected PrintWriter log;
	
	protected OutputStream outStr;
	
	/**
	 * Creates a StreamAppender using the given stream.
	 * @param outStr Stream to write logging to.
	 */
	public StreamAppender(OutputStream outStr) {
		this.outStr = outStr;
		this.log = new PrintWriter(outStr);
	}
	
    /* (non-Javadoc)
     * @see com.enterprisedt.util.debug.Appender#close()
     */
	public void close() {
		synchronized (log) {
	        log.flush();
	        log.close();
		}
	}

    /* (non-Javadoc)
     * @see com.enterprisedt.util.debug.Appender#log(java.lang.String)
     */
	public void log(String msg) {
		synchronized (log) {
			log.println(msg);
		}
	}

    /* (non-Javadoc)
     * @see com.enterprisedt.util.debug.Appender#log(java.lang.Throwable)
     */
	public void log(Throwable t) {
		synchronized (log) {
	        t.printStackTrace(log);
	        log.println();
		}
	}
}
