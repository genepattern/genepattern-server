package org.genepattern.util;

import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Category;
import org.genepattern.util.PropertyFactory;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.RequestHandler;
import org.genepattern.webservice.WebServiceException;

/**
 * <p>
 * 
 * Title: RequestHandlerFactory.java
 * </p>
 * <p>
 * 
 * Description: creates and stores the RequestHandler for sending out requests.
 * </p>
 * 
 * @author Hui Gong
 * @version $Revision 1.5 $
 * @deprecated use AnalysisWebServiceProxy instead
 */

public class RequestHandlerFactory {
	private static RequestHandlerFactory instance;

	private String userName, password;

	private String siteName;

	private String url;

	private static Category _cat = Category
			.getInstance(RequestHandlerFactory.class.getName());

	private RequestHandlerFactory(String username, String password) {
		this.userName = username;
		this.password = password;
		PropertyFactory property = PropertyFactory.getInstance();
		try {
			Properties p = property.getProperties("omnigene.properties");
			this.url = p.getProperty("analysis.service.URL");
			this.siteName = p.getProperty("analysis.service.site.name",
					"Broad Institute");
		} catch (java.io.IOException ioe) {
			ioe.printStackTrace();
		} catch (org.genepattern.webservice.PropertyNotFoundException pnfe) {
			pnfe.printStackTrace();
		}
	}

	public synchronized static RequestHandlerFactory getInstance() {
		if (instance == null) {
			instance = new RequestHandlerFactory(null, null);
		}
		return instance;
	}

	public synchronized static RequestHandlerFactory getInstance(
			String username, String password) {
		if (instance == null) {
			instance = new RequestHandlerFactory(username, password);
		}
		return instance;
	}

	private RequestHandler createRequestHandler() {
		//get all analysis web services from uddi
		//needs to change after the look up service is done.
		return new RequestHandler(siteName, userName);
	}

	/**
	 * Gets all available analysis service tasks
	 * 
	 * @return a list of AnalysisService object.
	 * @exception WebServiceException
	 *                Description of the Exception
	 */
	public Vector getAllServices() throws WebServiceException {
		Vector tasks = new Vector();
		RequestHandler handler = createRequestHandler();
		try {
			AnalysisService[] services = handler.getAnalysisServices();
			int size = services.length;
			for (int i = 0; i < size; i++) {
				tasks.add(services[i]);
			}
		} catch (WebServiceException wse) {
			wse.printStackTrace();
			_cat.error("error in sending request to: " + handler.getURL());
			throw wse;
		}

		return tasks;
	}

	/**
	 * Gets the RequestHandler based on the siteName which is unique and stored
	 * in omnigene loopup service.
	 * 
	 * @param siteName
	 */
	public static RequestHandler getRequestHandler(String siteName) {
		// FIXME siteName ignored
		return instance.createRequestHandler();
	}

}