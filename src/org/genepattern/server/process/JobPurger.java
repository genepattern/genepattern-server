package org.genepattern.server.process;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Timer;

public class JobPurger implements Runnable {

	final static int DEFAULT_PURGE_JOBS_INTERVAL = 7;
	final static int DEFAULT_PURGE_HOUR = 23;
	final static int DEFAULT_PURGE_MINUTE = 0;
	
	protected int purgeInterval = -1;
	Timer timer = new Timer(true);
	Purger purger = null;
	boolean DEBUG = false;
	
	public JobPurger(String purgeInterval, String purgeTime) {
		GregorianCalendar nextPurgeTime = new GregorianCalendar();
		GregorianCalendar purgeTOD = new GregorianCalendar();
		SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
		try {
			this.purgeInterval = Integer.parseInt(purgeInterval);
		} catch (NumberFormatException nfe) {
			this.purgeInterval = DEFAULT_PURGE_JOBS_INTERVAL;
		}
		try {
			purgeTOD.setTime(dateFormat.parse(purgeTime));
		} catch (ParseException pe) {
			purgeTOD.set(GregorianCalendar.HOUR_OF_DAY, DEFAULT_PURGE_HOUR);
			purgeTOD.set(GregorianCalendar.MINUTE, DEFAULT_PURGE_MINUTE);
		}
		nextPurgeTime.set(GregorianCalendar.HOUR_OF_DAY, purgeTOD.get(GregorianCalendar.HOUR_OF_DAY));
		nextPurgeTime.set(GregorianCalendar.MINUTE, purgeTOD.get(GregorianCalendar.MINUTE));
		nextPurgeTime.set(GregorianCalendar.SECOND, 0);
		nextPurgeTime.set(GregorianCalendar.MILLISECOND, 0);

		if (nextPurgeTime.getTime().before(new Date())) {
			// it's already after today's purge time, wait until tomorrow's
			nextPurgeTime.add(GregorianCalendar.DATE, 1);
		}
		if (DEBUG) System.out.println("next purge will be at " + nextPurgeTime.getTime());
		long MILLISECONDS_IN_A_DAY = 24*60*60*1000;
		purger = new Purger(this.purgeInterval);
		timer.scheduleAtFixedRate(purger, nextPurgeTime.getTime(), MILLISECONDS_IN_A_DAY);
	}
	
	public void run() {
		try {
			synchronized (this) { this.wait(); }
		} catch (InterruptedException ie) {
			System.err.println("JobPurger shutting down");
			timer.cancel();
		}
	}
	
	public void runPurger() {
		purger.run();
	}
	
	public static void main(String[] args) {
		System.out.println(new Date() + ": Starting JobPurger");
		try {
			JobPurger jobPurger = new JobPurger("7", "23:00");
			jobPurger.runPurger();
		} catch (Exception e) {
			System.err.println(e);
		}
	}
}
