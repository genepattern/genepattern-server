/*
 * AbstractDataSource.java
 *
 * Created on February 18, 2003, 3:52 PM
 */

package org.genepattern.gpge.io;

import java.io.InputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.genepattern.data.DataObjector;
import org.genepattern.gpge.GenePattern;
import org.genepattern.io.parsers.DataParser;
import org.genepattern.util.GPpropertiesManager;

/**
 * Partial implemention of the DataSource interface
 * 
 * @author keith
 */
public abstract class AbstractDataSource implements DataSource {

	/** Creates a new instance of AbstractDataSource */
	protected AbstractDataSource(final DataParser parser) {
		this(parser, DataSourceUpdateListener.NO_PARENTS);
	}

	/** Creates a new instance of AbstractDataSource */
	protected AbstractDataSource(final DataParser parser, final Object[] parents) {
		this.parents = parents;
		this.proxies_w_path = new ProxiesWithPath[1];
		this.data_proxy = new HashMap();
		this.proxy_data = new HashMap();
		this.parser = parser;
	}

	// abstract methods
	/** creates a DataObjectProxy */
	abstract protected DataObjectProxy createDataObjectProxy(final Object data);

	/**
	 * gets the DataObject from the specified DataObjectProxy
	 * 
	 * @exception IllegalArgumentException
	 *                if the specified DataObjectProxy is not found from this
	 *                source
	 * @exception IOException
	 *                if there was a problem reading the data
	 * @exception ParseException
	 *                if there was some problem with the content of the data
	 */
	abstract public DataObjector getDataObject(final DataObjectProxy proxy)
			throws java.io.IOException, java.text.ParseException,
			IllegalArgumentException;

	/**
	 * returns a description of the source- i.e. if it reads gct data from a
	 * local directory, it reads sdf data from OmniGene, etc
	 */
	public abstract String getDescription(DataObjectProxy proxy);

	/** returns an InputStream for reading the raw data */
	abstract public InputStream getRawInputStream(final DataObjectProxy proxy)
			throws IOException;

	/**
	 * returns a File on the local system where the data can be read note this
	 * could be an expensive operation if the file is large and on the server
	 * (at least initially read from the server to create the file)
	 */
	abstract public java.io.File getAsLocalFile(final DataObjectProxy proxy)
			throws IOException;

	// implementation of DataSource signature methods

	/**
	 * gets a copy of the array of DataObjectProxy objects note that this is not
	 * dynamically updated but just a static array
	 */
	public DataObjectProxy[] getDataProxies() {
		final java.util.Set key_set = proxy_data.keySet();
		return (DataObjectProxy[]) key_set.toArray(new DataObjectProxy[key_set
				.size()]);
	}

	/** returns the parents */
	public Object[] getParents() {
		return (Object[]) parents.clone();
	}

	/** updates the list of new ones and ones removed */
	public void updateList(final List new_data, final List removed_data) {
		if (new_data != null) {
			Collections.sort(new_data, getComparator());
			Thread.yield();
			try {
				show_exts = GPpropertiesManager.getBooleanProperty(
						"databrowser.show.file.exts", true);
				//System.out.println("show.file.exts="+GenePattern.getProperty("show.file.exts")+"
				// show_exts="+show_exts);
			} catch (java.text.ParseException ex) {
				show_exts = true;//default
				GenePattern.logWarning(ex.getMessage());
			}

			final int new_cnt = new_data.size();
			//System.out.println("A total of "+new_cnt+" data were accepted by
			// the filer");
			final List new_proxies = new ArrayList(new_cnt);
			for (int i = 0; i < new_cnt; i++) {
				final Object dat = new_data.get(i);
				final DataObjectProxy proxy = createDataObjectProxy(dat);
				data_proxy.put(dat, proxy);
				proxy_data.put(proxy, dat);
				new_proxies.add(proxy);
			}
			Thread.yield();
			if (new_cnt > 0) {
				final DataObjectProxy[] new_proxies_array = (DataObjectProxy[]) new_proxies
						.toArray(new DataObjectProxy[new_cnt]);
				notifyDataSourceListenersAdd(new_proxies_array, parents);
				Thread.yield();
			}
		}

		// *** the missing ones ***
		if (removed_data != null) {
			final int missing_cnt = removed_data.size();
			if (missing_cnt == 0)
				return;
			Collections.sort(removed_data, getComparator());
			Thread.yield();
			//final DataObjectProxy[] missing_proxies = new
			// DataObjectProxy[missing_cnt];
			final List missing_proxies = new ArrayList(missing_cnt);
			// remove the missing data_proxy from the Maps
			for (int j = 0; j < missing_cnt; j++) {
				final DataObjectProxy proxy = (DataObjectProxy) data_proxy
						.remove(removed_data.get(j));
				//missing_proxies[j] = proxy;
				if (proxy != null) {
					missing_proxies.add(proxy);
					proxy_data.remove(proxy);
				}
			}
			Thread.yield();
			//notifyDataSourceListenersRemove(missing_proxies, parents);
			notifyDataSourceListenersRemove((DataObjectProxy[]) missing_proxies
					.toArray(new DataObjectProxy[missing_proxies.size()]),
					parents);

		}
	}

	/** returns the DataParser */
	public DataParser getDataParser() {
		return parser;
	}

	/**
	 * returns the type of data that will be read from the raw input stream For
	 * example character (ASCII), or binary, or unknown, data. FIXME this info
	 * should be had from the parser
	 */
	public StreamType getStreamType(DataObjectProxy proxy) {
		return this.stream_type; //FIXME
	}

	// methods for notifying, adding, and removing, listeners

	/**
	 * adds the DataSourceUpdateListener to the collection of listeners these
	 * listeners will be notified when a new DataObjectProxy object has become
	 * available
	 *  
	 */
	public void addDataSourceUpdateListener(
			final DataSourceUpdateListener listener) {
		if (listeners.add(listener)) {
			// run it
			new org.genepattern.util.SwingWorker() {
				public Object construct() {
					final ProxiesWithPath[] pwps = getProxiesWithPath();
					final int limit = pwps.length;
					//final DataObjectProxy[] proxies = getDataProxies();
					for (int i = 0; i < limit; i++) {
						final ProxiesWithPath pwp = pwps[i];
						final Object[] parents = pwp.parents;
						final DataObjectProxy[] proxies = pwp.proxies;
						if (proxies != null || proxies.length != 0) {
							// this must be called on swing event thread

							listener.updateAddDataObjects(proxies, parents);
						}

					}
					return null;
				}
			}.start();
		} else {
			System.err.println(this.getClass()
					+ " already had added DataSourceUpdateListener: "
					+ listener);
		}
	}

	/**
	 * removes the specified DataSourceUpdateListener from the collection of
	 * listeners
	 */
	public void removeDataSourceUpdateListener(
			final DataSourceUpdateListener listener) {
		if (listeners.remove(listener)) {
			// run it
			//            new edu.mit.genome.util.SwingWorker() {
			//                public Object construct() {
			final ProxiesWithPath[] pwps = getProxiesWithPath();
			final int limit = pwps.length;
			//final DataObjectProxy[] proxies = getDataProxies();
			for (int i = 0; i < limit; i++) {
				final ProxiesWithPath pwp = pwps[i];
				final Object[] parents = pwp.parents;
				final DataObjectProxy[] proxies = pwp.proxies;
				if (proxies != null || proxies.length != 0) {
					// this must be called on swing event thread
					listener.updateRemoveDataObjects(proxies, parents);
					Thread.yield();
				}
			}
			//                    return null;
			//                }
			//            }.start();
		} else {
			System.err.println(getClass()
					+ " didn't have the DataSourceUpdateListener to remove: "
					+ listener);
		}
	}

	/**
	 * notifies all DataSourceUpdateListener objects that some DataObjectProxy
	 * objects have become available
	 */
	protected void notifyDataSourceListenersAdd(
			final DataObjectProxy[] proxies, final Object[] parents) {
		//        new edu.mit.genome.util.SwingWorker() {
		//            public Object construct() {
		for (final Iterator iter = listeners.iterator(); iter.hasNext();) {
			final DataSourceUpdateListener listener = (DataSourceUpdateListener) iter
					.next();
			listener.updateAddDataObjects(proxies, parents);
			Thread.yield();
		}
		//                return null;
		//            }
		//        }.start();
	}

	/**
	 * notifies all DataSourceUpdateListener objects that some DataObjectProxy
	 * objects are no longer available or have been removed
	 */
	protected void notifyDataSourceListenersRemove(
			final DataObjectProxy[] proxies, final Object[] parents) {
		//        // run it
		//        new edu.mit.genome.util.SwingWorker() {
		//            public Object construct() {
		// this must be called on swing event thread
		for (final Iterator iter = listeners.iterator(); iter.hasNext();) {
			final DataSourceUpdateListener listener = (DataSourceUpdateListener) iter
					.next();
			listener.updateRemoveDataObjects(proxies, parents);
			Thread.yield();
		}
		//                return null;
		//            }
		//        }.start();
	}

	// helper methods

	protected ProxiesWithPath[] getProxiesWithPath() {
		if (proxies_w_path[0] == null) {
			proxies_w_path[0] = new ProxiesWithPath();
			proxies_w_path[0].parents = this.parents;
		}
		proxies_w_path[0].proxies = this.getDataProxies();
		return proxies_w_path;
	}

	/** this can be overridden by subclasses to supply a Comparator */
	protected java.util.Comparator getComparator() {
		return null;
	}

	// fields
	/**
	 * the collection of listenrs FIXME this should be WeakHashMap so that Weak
	 * references get GCed
	 */
	private final java.util.Set listeners = new java.util.HashSet();

	//    /** this is the collection of proxies aquired */
	//    protected final Collection proxies;
	/**
	 * the parent user objects that these proxies are members of in a tree
	 * structure
	 */
	protected final Object[] parents;

	/**
	 * array of ProxiesWithPath objects associating parents with an array of
	 * proxies only one of these by default
	 */
	protected final ProxiesWithPath[] proxies_w_path;

	/** the data mapped to their DataObjectProxy */
	protected final Map data_proxy;

	/** the DataObjectProxy objects are mapped to their data */
	protected final Map proxy_data;

	/** the parser that knows how to identify certain data data */
	protected final DataParser parser;

	/**
	 * the stream type FIXME this information needs to come from the DataParser
	 */
	private final StreamType stream_type = StreamType.TEXT;

	/** if true the names of the nodes should display the file extensions */
	protected boolean show_exts = true;

	// I N N E R C L A S S E S
	/**
	 * contains the association of a group of proxies and the user objects that
	 * become the parent nodes
	 */
	private static class ProxiesWithPath {
		private DataObjectProxy[] proxies;

		private Object[] parents;
	}
}