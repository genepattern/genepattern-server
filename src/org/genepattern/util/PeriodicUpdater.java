/*
 * DataSourceUpdater.java
 *
 * Created on June 12, 2003, 8:57 AM
 */

package org.genepattern.util;

import javax.swing.Timer;

//import edu.mit.genome.gp.GenePattern;

/**
 * Periodically fires the execute() method, which is run in a thread, and
 * restarts the timer only after execute is done.
 * 
 * @author kohm
 */
public abstract class PeriodicUpdater implements java.awt.event.ActionListener {

	/** Creates a new instance of DataSourceUpdater */
	public PeriodicUpdater(final String prop_key, final int default_delay) {
		this.prop_key = prop_key;
		this.default_delay = default_delay;
		timer = new Timer(getDelay(), this);
		timer.setRepeats(false);
		timer.setCoalesce(false);
		timer.setLogTimers(false);
	}

	/** subclasses implement this with whatever needs to be periodically run */
	abstract public void execute() throws Exception;

	/** restarts/starts the timer */
	public final void restartTimer() {
		timer.setDelay(getDelay());
		timer.restart();
		//        if( timer.isRunning() ) {
		//            timer.restart();
		//            System.out.println("Timer is running and just called restart()");
		//        } else {
		//            timer.start();
		//            System.out.println("Timer wasn't running so called start()");
		//        }
	}

	/**
	 * gets the amount of time to wait before the next update is started
	 * 
	 * @return int, The number of milliseconds between updates being fired
	 */
	protected final int getDelay() {
		// 150000 = 150,000 mili secs = 150 secs = 2.5 min
		final int delay = GPpropertiesManager.getIntProperty(prop_key,
				default_delay);
		//System.out.println("Timer delay = "+delay);
		return delay;
	}

	/**
	 * subclasses will implement this method to know when an event occured This
	 * is pertty much the only method that the developer needs to bother with
	 *  
	 */
	public void actionPerformed(final java.awt.event.ActionEvent actionEvent) {
		//System.out.println("Timer fired: "+actionEvent);
		timer.stop(); // stop the timer until execute() is finnished
		new Thread(new RunLater() {
			public final void runIt() throws Exception {
				try {
					// System.out.println("Executing update...");
					execute();
				} finally {
					// restarts the update timer
					restartTimer();
				}

			}
		}).start();
	}

	// fields
	/**
	 * the key for getting the property that indicates how long to wait before
	 * firing execute() again.
	 */
	private final String prop_key;

	/** the default delay */
	private final int default_delay;

	/** the timer */
	private final Timer timer;
}