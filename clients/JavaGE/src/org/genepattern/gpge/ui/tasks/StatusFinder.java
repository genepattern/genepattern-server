package org.genepattern.gpge.ui.tasks;

import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import org.apache.log4j.Category;
import org.genepattern.gpge.GenePattern;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.JobStatus;
import org.genepattern.webservice.RequestHandler;
import org.genepattern.util.RequestHandlerFactory;
import org.genepattern.webservice.WebServiceException;

/**
 * <p>
 * 
 * Title: StatusFinder.java
 * </p>
 * <p>
 * 
 * Description: Checks the status of unfinished job.
 * </p>
 * 
 * @author Hui Gong
 * @created May 4, 2004
 * @version $Revision$
 */

public class StatusFinder implements Runnable, Observer {
	private DataModel _data;

	private static Category cat = Category.getInstance(StatusFinder.class
			.getName());

	//fields
	private final int polling_delay;

	/**
	 * Constructor, takes data from DataModel
	 * 
	 * @param model
	 *            Description of the Parameter
	 */
	public StatusFinder(final DataModel model) {
		this(model, 15000); // 15000 ms = 15 sec
	}

	/**
	 * Constructor, takes data from DataModel
	 * 
	 * @param model
	 *            Description of the Parameter
	 * @param delay_ms
	 *            Description of the Parameter
	 */
	public StatusFinder(final DataModel model, final int delay_ms) {
		this._data = model;
		if (delay_ms <= 0) {
			throw new IllegalArgumentException(
					"Polling delay must be a positive time in milliseconds not "
							+ delay_ms);
		}
		this.polling_delay = delay_ms;
	}

	public void run() {
		long time = 0;

		while (true) {
			// record start
			time = System.currentTimeMillis();
			Vector history = (Vector) this._data.getJobs().clone();
			try {
				Iterator iter = history.iterator();
				while (iter.hasNext()) {
					AnalysisJob job = (AnalysisJob) iter.next();
					String status = job.getJobInfo().getStatus();
					if (!status.equals(JobStatus.FINISHED)
							&& !status.equals(JobStatus.ERROR)) {
						try {
							RequestHandlerFactory factory = RequestHandlerFactory
									.getInstance(job.getJobInfo().getUserId(),
											null);
							RequestHandler handler = factory
									.getRequestHandler(job.getServer());
							JobInfo info = handler.checkStatus(job.getJobInfo()
									.getJobNumber());

							if (!info.getStatus().equals(
									job.getJobInfo().getStatus())) {
								job.setJobInfo(info);
								this._data.updateStatus(job);
							}
						} catch (WebServiceException wse) {
							System.out.println("ERROR ROOT: "
									+ wse.getMessage());
							// if it is a disconnect, notify the system
							if (wse.getMessage().indexOf("ConnectException") >= 0) {
								GenePattern.getDataObjectBrowser()
										.disconnectedFromServer();
								job.getJobInfo().setStatus(JobStatus.ERROR);
							}
							//cat.error("error in checking status " + wse);

						} catch (
						/*
						 * WebService
						 */
						Exception e) {
							System.out.println("Plain exception: "
									+ e.getClass());
							e.printStackTrace();
							cat.error("error in checking status " + e);
						}
					}
				}

			} catch (java.util.ConcurrentModificationException ccme) {
				// kludge if ccme then sleep
				System.err
						.println("StatusFinder: ConcurrentModificationException"
								+ " while trying to iterate though a history Vector");
			} catch (Throwable t) {
			}

			// elapsed
			final long before_sleep = System.currentTimeMillis();
			try {
				//Thread.sleep(1000*15);
				Thread.yield();
				Thread.sleep(polling_delay);
			} catch (InterruptedException e) {
				cat.error("", e);
			}
			final long after_sleep = System.currentTimeMillis();
			time = after_sleep;
		}
	}

	/**
	 * implements the method from interface Observer.
	 * 
	 * @param o
	 *            Description of the Parameter
	 * @param arg
	 *            Description of the Parameter
	 */
	public void update(Observable o, Object arg) {
		this._data = (DataModel) o;
	}
}