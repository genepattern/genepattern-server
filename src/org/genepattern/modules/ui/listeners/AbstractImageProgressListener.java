/*
 * AbstractImageProgressListener.java
 *
 * Created on May 8, 2002, 2:11 PM
 */

package org.genepattern.modules.ui.listeners;

import java.applet.Applet;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ProgressMonitor;
import javax.swing.Timer;

/**
 * 
 * @author KOhm
 * @version
 */
public abstract class AbstractImageProgressListener implements ActionListener {
	/** constructor */
	protected AbstractImageProgressListener(Component comp, String filename) {
		this.component = comp;
		monitor = new ProgressMonitor(getTopParent(comp), "Saving the "
				+ filename + " image file", "", 0, 100);
		timer = new Timer(1000 /* ONE_SECOND */, this);
		monitor.setProgress(0);
		monitor.setMillisToDecideToPopup(250 /* 1/4 sec */);
		monitor.setMillisToPopup(500 /* 1/2 sec */);
	}

	/** stops the writing process */
	abstract protected void abort();

	/**
	 * Reports the approximate degree of completion of the current write call
	 * within the associated ImageWriter.
	 */
	public final void imageStarted(int imageIndex) {
		comment("imageStarted");
		//            System.out.println("Thread: "+Thread.currentThread ());
		//            org.genepattern.data.CMJAUtil.printStackTrace ();
		timer.start();
		monitor.setProgress(1);
	}

	/** Reports that an image write operation is beginning. */
	public final void imageProgress(float percentageDone) {
		amount_completed = (int) percentageDone;
	}

	/** Reports that the image write operation has completed. */
	public final void imageComplete() {
		comment("imageComplete");
		done();
	}

	/** sets information */
	public final void comment(String message) {
		message = "Comment: " + message;
		monitor.setNote(message);
		System.out.println(message);
	}

	// ActionListener interface method signature
	/**
	 * The actionPerformed method in this class is called each time the Timer
	 * "goes off".
	 */
	public void actionPerformed(ActionEvent evt) {
		if (monitor.isCanceled()) {
			comment("User cancled");
			done();
			Toolkit.getDefaultToolkit().beep();
			abort();
		} else {
			monitor.setProgress(amount_completed);
		}
	}

	// helper methods

	/** stops eveything on this end */
	public final void done() {
		monitor.close();
		timer.stop();
	}

	// static methods

	/** helper method this should go in some utility class */
	public static final Component getTopParent(Component comp) {
		Component c = comp;
		while (c != null && !(c instanceof Window || c instanceof Applet))
			c = c.getParent();
		if (c == null)
			return comp;
		return c;
	}

	// fields
	/** the parent for the dialog */
	protected Component component;

	/** progress monitor */
	protected ProgressMonitor monitor;

	/** the timer */
	private Timer timer;

	/** amount done */
	protected int amount_completed;

}