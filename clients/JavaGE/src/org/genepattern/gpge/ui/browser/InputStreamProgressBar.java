/*
 * InputStreamProgressBar.java
 *
 * Created on June 12, 2002, 8:48 AM
 */

package org.genepattern.gpge.ui.browser;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

import javax.swing.AbstractButton;
import javax.swing.JProgressBar;

import org.genepattern.util.Messenger;

/**
 * 
 * @author KOhm
 * @version
 */
public class InputStreamProgressBar extends FilterInputStream implements
		java.awt.event.ActionListener {
	/**
	 * Constructs an object to monitor the progress of an input stream.
	 * 
	 * @param in
	 *            The input stream to be monitored.
	 * @param monitor
	 * @param stopButton
	 */
	public InputStreamProgressBar(InputStream in, JProgressBar monitor,
			AbstractButton stopButton, Messenger messenger) {
		super(in);
		this.monitor = monitor;
		this.stopButton = stopButton;
		this.messenger = messenger;
		try {
			size = in.available();
		} catch (IOException ioe) {
			size = 0;
		}
		System.out.println("available = " + size);
		monitor.setMinimum(0);
		monitor.setMaximum(size);
		monitor.setStringPainted(true);
		monitor.setValue(0);

		stopButton.setEnabled(true);
		stopButton.addActionListener(this);
	}

	/**
	 * Get the JProgressBar object being used by this stream. Normally this
	 * isn't needed unless you want to do something like change the descriptive
	 * text partway through reading the file.
	 * 
	 * @return the JProgressBar object used by this object
	 */
	public JProgressBar getMonitor() {
		return monitor;
	}

	/**
	 * Overrides <code>FilterInputStream.read</code> to update the progress
	 * bar after the read.
	 */
	public int read() throws IOException {
		int c = in.read();
		if (c >= 0)
			monitor.setValue(++nread);
		if (is_canceled) {
			InterruptedIOException exc = new InterruptedIOException("progress");
			exc.bytesTransferred = nread;
			messenger.setMessage("Stopped...");
			throw exc;
		}
		return c;
	}

	/**
	 * Overrides <code>FilterInputStream.read</code> to update the progress
	 * bar after the read.
	 */
	public int read(byte b[]) throws IOException {
		int nr = in.read(b);
		if (nr > 0)
			monitor.setValue(nread += nr);
		if (is_canceled) {
			InterruptedIOException exc = new InterruptedIOException("progress");
			exc.bytesTransferred = nread;
			messenger.setMessage("Stopped...");
			throw exc;
		}
		return nr;
	}

	/**
	 * Overrides <code>FilterInputStream.read</code> to update the progress
	 * bar after the read.
	 */
	public int read(byte b[], int off, int len) throws IOException {
		int nr = in.read(b, off, len);
		if (nr > 0)
			monitor.setValue(nread += nr);
		if (is_canceled) {
			InterruptedIOException exc = new InterruptedIOException("progress");
			exc.bytesTransferred = nread;
			messenger.setMessage("Stopped...");
			throw exc;
		}
		return nr;
	}

	/**
	 * Overrides <code>FilterInputStream.skip</code> to update the progress
	 * bar after the skip.
	 */
	public long skip(long n) throws IOException {
		long nr = in.skip(n);
		if (nr > 0)
			monitor.setValue(nread += nr);
		return nr;
	}

	/**
	 * Overrides <code>FilterInputStream.close</code> to close the progress
	 * bar as well as the stream.
	 */
	public void close() throws IOException {
		in.close();
		monitor.setValue(monitor.getMinimum());
		monitor.setStringPainted(false);
		stopButton.removeActionListener(this);
		stopButton.setEnabled(false);
		messenger.setMessage("Done...");
	}

	/**
	 * Overrides <code>FilterInputStream.reset</code> to reset the progress
	 * bar as well as the stream.
	 */
	public synchronized void reset() throws IOException {
		in.reset();
		nread = size - in.available();
		monitor.setValue(nread);
	}

	/** listener for the stop button */
	public final void actionPerformed(java.awt.event.ActionEvent actionEvent) {
		is_canceled = true;
	}

	// Fields
	/** when the stopButton is pressed this becomes true */
	private boolean is_canceled = false;

	/** the progress bar */
	private final JProgressBar monitor;

	/** bytes read */
	private int nread = 0;

	/** total bytes */
	private int size = 0;

	/** the stop button */
	private final AbstractButton stopButton;

	/** where messages are sent */
	private final Messenger messenger;
}