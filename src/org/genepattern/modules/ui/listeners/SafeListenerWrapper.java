/*
 * SafeListenerWrapper.java
 *
 * Created on January 30, 2002, 9:58 PM
 */

package org.genepattern.modules.ui.listeners;

import java.awt.AWTEvent;

import org.genepattern.util.RunLater;

/**
 * 
 * @author KOhm
 * @version
 */
public abstract class SafeListenerWrapper extends RunLater {
	protected AWTEvent event;

	/** Creates new SafeListenerWrapper */
	public SafeListenerWrapper() {
	}

	public final void doIt(AWTEvent event) {
		this.event = event;
		run();
	}

	/**
	 * this is where the subclasses implement whatever code they want to be run
	 * in a thread
	 */
	abstract protected void runIt() throws Throwable;

}