/*
 * SafeListenerWrapper.java
 *
 * Created on January 30, 2002, 9:58 PM
 */

package org.genepattern.modules.ui.listeners;

import java.util.Observable;
import java.util.Observer;

import org.genepattern.util.RunLater;

/**
 * 
 * @author KOhm
 * @version
 */
public abstract class SafeObserverListener extends RunLater implements Observer {
	/** Creates new SafeListenerWrapper */
	public SafeObserverListener() {
	}

	/** is called whenever an update occures */
	protected abstract void updateIt(Observable observable, Object obj)
			throws Exception;

	public final void update(Observable observable, Object obj) {
		this.observable = observable;
		this.obj = obj;
		run();
	}

	/**
	 * this is called by run() but in an environment where Exceptions and Errors
	 * are logged and/or displayed to the user
	 */
	protected final void runIt() throws Throwable {
		updateIt(observable, obj);
	}

	// fields
	private Observable observable;

	private Object obj;
}