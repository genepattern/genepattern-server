/*
 * DataSource.java
 *
 * Created on February 18, 2003, 10:18 AM
 */

package org.genepattern.gpge.io;

import java.util.Iterator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.genepattern.data.DataObjector;
import org.genepattern.gpge.GenePattern;
import org.genepattern.io.UniversalDecoder;
import org.genepattern.io.parsers.ClsDataParser;
import org.genepattern.io.parsers.DataParser;
import org.genepattern.io.parsers.GctParser;
import org.genepattern.io.parsers.ResParser;
import org.genepattern.util.ArrayUtils;
import org.genepattern.util.PeriodicUpdater;
import org.genepattern.util.Warning;
 


/**
 * Discovers all the known and available DataObjects from local files,
 * OmniGene server, Web Services, ftp site, html site, DataBase, and etc.
 *
 * Note that DataSources does not implement getDataProxies() and will throw a NullPointerException
 * if invoked.
 * @author  keith
 */
public class DataSources extends AbstractDataSource implements DataSourceUpdateListener {
    
    /** Creates a new instance of DataSource */
    protected DataSources(final GroupDataSource[] sources) {
        super(null);
        data_sources = new java.util.HashSet();
        dir_sources  = new java.util.HashSet();
        if( sources != null ) {
            final int limit = sources.length;
            for(int i = 0; i < limit; i++) {
                addDataSource(sources[i]);
            }
        }
        to_be_updated = new ArrayList();
        updater = new PeriodicUpdater("gp.project.dirs.refresh.milisecs", 15000){
            public final void execute() {
                refresh();
            }
        };
            
    }
    /** retrieves the singleton instance of DataSources */
    public static final DataSources instance() {
        return INSTANCE;
    }
    
    /** adds the specified DataSource */
    public void addDataSource(final GroupDataSource source) {// GroupDataSource
	
        if( data_sources.add(source) ) { // only unique datasources
            source.addDataSourceUpdateListener(this);
            //alert all listeners that there are some more DataObjects available
            final DataObjectProxy[] proxies = source.getDataProxies();
            if(proxies != null && proxies.length > 0) {
                System.out.println("Source has "+proxies.length+" proxies!");
                    notifyDataSourceListenersAdd(proxies, DataSourceUpdateListener.NO_PARENTS);
            }
        } else {
            System.err.println(getClass()+".addDataSource(DataSource) was trying to add the same source again: "+source);
        }

    }
    /** removes the specified DataSource */
    public void removeDataSource(final GroupDataSource source) {// GroupDataSource
        if( data_sources.remove(source) ) { // only unique datasources
	    if(source instanceof DirDataSource) {
		 DirDataSource temp = (DirDataSource) source;
		 dir_sources.remove(temp.getDir());
	    }
            source.removeDataSourceUpdateListener(this);
            to_be_updated.remove(source);
            //alert all listeners that there are some more DataObjects available
            final DataObjectProxy[] proxies = source.getDataProxies();
            if(proxies != null && proxies.length > 0) {
                    notifyDataSourceListenersRemove(proxies, DataSourceUpdateListener.NO_PARENTS);
            }
        } else {
            System.err.println(getClass()+".removeDataSource(DataSource) was trying to remove an absent data source: "+source);
        }

    }
    /** causes all datasources that can update to do so - note this could take some time*/
    public final void refresh() {
        final int limit = to_be_updated.size();
        //System.out.println("Refreshing "+limit+" GroupDataSource objs.");
        for(int i = 0; i < limit; i++) {
            final GroupDataSource source = (GroupDataSource)to_be_updated.get(i);
            try {
                source.refresh();
            } catch (java.io.IOException ioe) {
                GenePattern.showWarning(null, "While refreshing "
                    +source.getLocation()+" an error occured:\n"+ioe.getMessage());
            }
        }
    }
    /** starts the Timer */
    public final void startPeriodicUpdate() {
        updater.restartTimer();
    }
    
   /** sets the server data source and creates the new ServerSiteDataSource */
    public ServerSiteDataSource setAnalysisDataModel(final String site_name, final org.genepattern.gpge.ui.tasks.DataModel analysis_model, final DataSourceManager manager) throws java.io.IOException, org.genepattern.analysis.PropertyNotFoundException{
        final DataParser[] parsers = (DataParser[])UniversalDecoder.PARSER_LIST.toArray(new DataParser[UniversalDecoder.PARSER_LIST.size()]);
        final ServerSiteDataSource server_datasource = new ServerSiteDataSource(analysis_model, site_name, parsers, null, manager);
        addDataSource(server_datasource);
        return server_datasource;
    }
	 
	
    /** adds the specified directory as a datasource
     */
    public synchronized final DirDataSource addDirectory(final java.io.File file, final DataSourceManager manager, org.genepattern.modules.ui.graphics.PeriodicProgressObserver observer) throws java.io.IOException, java.text.ParseException{
        final java.io.File dir = DirDataSource.getDirectory(file);
 	  if (dir == null){
		throw new Warning("Tried to add a Project Directory that doesn't exist: "+file.getAbsolutePath());
	  }


        if( !dir_sources.add(dir) ) {
            throw new Warning("Tried to add Project Directory again: " + dir);
        }
	  if (!dir.exists()){
		throw new Warning("Tried to add a Project Directory that doesn't exist: "+file.getAbsolutePath());

	  }


        final List parsers = UniversalDecoder.PARSER_LIST;//unmutable
        final DataParser[] parser_array = (DataParser[])parsers.toArray(new DataParser[parsers.size()]);
        final DirDataSource dsource = new DirDataSource(parser_array, dir, manager, observer);
            
        to_be_updated.add(dsource);
        this.addDataSource(dsource);
        //pastDropTarget.add(dsource);
        return dsource;
    }

    // DataSourceUpdateListener interface methods 
    /** called when some data objects proxies are available
     * The parents can be a zero length array if no parents or defines the parent
     * nodes to be created.  The first parent in the array, index 0,
     * is the top node which will have a child of parents[1], etc.  The last node will
     * have the DataModel nodes that contain the DataObjectProxy objects.
     *
     * @param proxies  array of new DataObjectProxy that are to be added
     * @param parents  (not null!) The hierarchy of user objects to be turned into
     *                 nodes
     */
    public void updateAddDataObjects(DataObjectProxy[] proxies, Object[] parents) {
         notifyDataSourceListenersAdd(proxies, parents);
    }
    
    /** called when some data objects proxies have become unavailable
     *
     * @param proxies  array of new DataObjectProxy that are to be 
     * @param parents  (not null!) The hierarchy of user objects whose nodes should contain
     *                 the proxies to be removed
     * @see  updateAddDataObjects(DataObjectProxy[] proxies, Object[] parents)
     */
    public void updateRemoveDataObjects(DataObjectProxy[] proxies, Object[] parents) {
        notifyDataSourceListenersRemove(proxies, parents);
    }
    
    /** gets the DataObject from the specified DataObjectProxy
     * @exception IllegalArgumentException if the specified DataObjectProxy is not found from this source
     * @exception IOException if there was a problem reading the data
     * @exception ParseException if there was some problem with the content of the data
     *
     */
    public DataObjector getDataObject(DataObjectProxy proxy) throws java.io.IOException, IllegalArgumentException {
        throw new UnsupportedOperationException("Not implemented");
    }
    
    /** returns a description of the source- i.e. if it reads gct files from a local
     * directory, it reads sdf files from OmniGene, etc
     *
     */
    public String getDescription(DataObjectProxy proxy) {
        throw new UnsupportedOperationException("Not implemented");
    }
    /** returns a copy of the array of DataObjectProxy objects
     * note that this is not dynamically updated but just a static array
     *
     */
    public synchronized DataObjectProxy[] getDataProxies() {
        final ArrayList list = new ArrayList();
        for(final Iterator iter = data_sources.iterator(); iter.hasNext(); ) {
            DataSource source = (DataSource)iter.next();
            final Object[] prxs = source.getDataProxies();
            list.ensureCapacity(prxs.length * 2 + list.size());
            ArrayUtils.addToCollection(prxs, list);
        }
        return (DataObjectProxy[])list.toArray(new DataObjectProxy[list.size()]);
         
    }
    /** returns the type of data that will be read from the raw input stream 
     * For example character (ASCII), or binary, or unknown, data.
     */
    public StreamType getStreamType(DataObjectProxy proxy) {
        throw new UnsupportedOperationException("Not implemented");
    }
    /** returns an InputStream for reading the raw data */
    public java.io.InputStream getRawInputStream(final DataObjectProxy proxy) throws java.io.IOException {
        throw new UnsupportedOperationException("Not implemented");
    }
    
    /** creates a DataObjectProxy  */
    protected DataObjectProxy createDataObjectProxy(Object data) {
        throw new UnsupportedOperationException("Not implemented");
    }
    
    /** returns a File on the local system where the data can be read
     * note this could be an expensive operation if the file is large and on the server
     * (at least initially read from the server to create the file)
     *
     */
    public java.io.File getAsLocalFile(DataObjectProxy proxy) throws java.io.IOException {
        throw new UnsupportedOperationException("Not implemented");
    }
    
    // end interface methods
    
    
    // fields
    /** the singleton instance */
    public static DataSources INSTANCE;
    /** the collection of DataSources */
    private final java.util.Set data_sources;
    /** the list of GroupDataSource objs that need to be periopically updated */
    private final List to_be_updated;
    /** the collection of unique directories */
    private final java.util.Set dir_sources;
    /** the Updater*/
    private final PeriodicUpdater updater;
    /** handles paste and drops of Proxies (Files?) etc */
    
	 public static void reset() {
		  // initialize the INSTANCE
        INSTANCE = new DataSources(null);
	 }
    /** static initializer */
    static {
       reset();
    }
}
