/*
 * GroupDataSource.java
 *
 * Created on February 18, 2003, 4:48 PM
 */

package org.genepattern.gpge.io;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.genepattern.data.DataObjector;
import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.io.parsers.AnythingParser;
import org.genepattern.io.UniversalDecoder;
import org.genepattern.io.parsers.DataParser;
import org.genepattern.util.ArrayUtils;
import org.genepattern.util.GPpropertiesManager;
import org.genepattern.util.StringUtils;

import com.oroinc.text.regex.Pattern;
import com.oroinc.text.regex.PatternCompiler;
import com.oroinc.text.regex.PatternMatcher;
import com.oroinc.text.regex.Perl5Compiler;
import com.oroinc.text.regex.Perl5Matcher;

/**
 *  Knows how to efficiently identify local Data Object files within a directory
 * @author  keith
 */
abstract public class GroupDataSource implements DataSource, org.genepattern.util.ProgressObservable {
    
    /** Creates a new instance of GroupDataSource 
     */
    public GroupDataSource(final DataSource[] srcs, final GroupDataSource parent, final DataSourceManager manager, final Object hashobject) {//throws java.io.FileNotFoundException {
        //this.sources = sources;
        this.manager = manager;
        this.parent  = parent;
        this.hashobject = hashobject;
        this.parser_source = new HashMap();
        
        final int num = srcs.length;
        if( GET_UNKNOWNS && num > 0) { // then one more DataSource with AnythingParser
            this.sources = new DataSource[num + 1];
            System.arraycopy(srcs, 0, this.sources, 0, num);
            this.sources[num] = createDataSource(ANYTHINGPARSER);
        } else
            this.sources = srcs;
        
        if( manager != null )
            manager.addDataSource(this);
        
        final int num_sources = this.sources.length;
        this.parsers = new DataParser[num_sources];
        for(int i = 0; i < num_sources; i++) {
            final DataSource source = this.sources[i];
            final DataParser parser = source.getDataParser();
            parsers[i] = parser;
            parser_source.put(parser, source);
        }
        
        added_list   = new ArrayList();
        removed_list = new ArrayList();
        old_list     = new ArrayList();
    }
    
    /** create a DataSource from the parser
     * This should be overridden by subclasses if needed (if GET_UNKNOWNS could be true 
     * and the sources array is not length 0 => don't use DataSource objects)
     */
    protected DataSource createDataSource(final DataParser parser) {
        throw new UnsupportedOperationException("This should be overridden if it's needed");
    }
    // abstract methods
    /** returns a parser for specified data */
    abstract protected DataParser getParserToDecode(Object data) throws java.io.IOException;
    /** returns a description of the source- i.e. if it reads gct files from a local
     * directory, it reads sdf files from OmniGene, etc
     */
    abstract public String getDescription();
    /** called when this should refresh/rescan its data and update */
    abstract public void refresh() throws IOException;
    
    // primary methods
    
    /** updates the list of data objects previously found
     * Note: this should be run in a seperate thread
     */
    protected final void updateList(final Object[] new_data) {
        updateList(new_data, null);
    }
    protected final void updateList(final Object[] new_data, final java.util.Comparator comparer) {
		 try {
			 _updateList(new_data, comparer);
		 } catch(Throwable t){}
	 }
		 
	private void _updateList(final Object[] new_data, final java.util.Comparator comparer) {
            // ProgressObservable stuff
            total = 7; // set in the fields
            current = 1;
            
            Arrays.sort(new_data, comparer); //if comparer is null the elements' natural ordering will be used
            added_list.clear();
            removed_list.clear();
            
            current++; // ProgressObservable stuff
            Thread.yield();
            
            // find the ones that are missing and the new ones
            final int old_cnt = old_list.size();
            for(int i = 0; i < old_cnt; i++) {
                final Object old = old_list.get(i);
                if( Arrays.binarySearch(new_data, old, comparer) < 0 )
                    removed_list.add(old);
            }
            
            current++; // ProgressObservable stuff
            Thread.yield();
            
            final int new_cnt = new_data.length;
            for(int i = 0; i < new_cnt; i++) {
                final Object neu = new_data[i];
                if( Collections.binarySearch(old_list, neu, comparer) < 0 )
                    added_list.add(neu);
            }
            
            current++; // ProgressObservable stuff
            Thread.yield();
            
            //classify them by extension
            
            // map parser to list of data
            final Map parser_list = new HashMap();
            
            // map parser to list of files to remove
            final Map parser_list_removed = new HashMap();
            
            final int num_parsers = parsers.length;
             
            current++; // ProgressObservable stuff
            Thread.yield();
            
				
            final int limit = added_list.size();
            for(int i = 0; i < limit; i++) {
                final Object data = added_list.get(i);
					 DataParser parser = null;
					 try {
						 parser = getParserToDecode(data);
					 } catch(IOException ioe) {
							removed_list.add(data);
							continue; 
					 }
                if( parser == null ){
                    System.out.println("\n************ No parser for "+data+" ************** GET_UNKNOWNS="+GET_UNKNOWNS+"\n");
                    if( GET_UNKNOWNS )
                        parser = ANYTHINGPARSER;
                    else
                        continue;
                }
                // organize them into parsers associated with List of data objs
                List list = (List)parser_list.get(parser);
                if( list == null ) {
                    list = new ArrayList();
                    parser_list.put(parser, list);
                }
                list.add(data);
            }
            
            current++; // ProgressObservable stuff
            Thread.yield();
            
            for(int i = 0; i < num_parsers; i++) {
                final DataParser parser = parsers[i];
                final List added = (List)parser_list.get(parser);
                //final List removed = (List)parser_list_removed.get(parser);
                if( added != null || removed_list != null) {
                    final DataSource source = (DataSource)parser_source.get(parser);
                    source.updateList(added, removed_list);
                    // these take awhile so let other threads have some cycles
                    Thread.yield();//java.lang.Thread.currentThread().yield();
                }
            }
            
            // finally update the old_list with the new
            old_list.clear();
            ArrayUtils.addToCollection(new_data, old_list);
            
            current++; // ProgressObservable stuff
            Thread.yield();

            //System.out.println(this+"\n old_cnt"+old_cnt+" new_cnt"+new_cnt+" added list="+limit+" removed list="+/*cnt*/+" parser="+num_parsers);
           // System.out.println(this+"\n old_cnt"+old_cnt+" new_cnt"+new_cnt+" added list="+limit+" removed list="+removed_list.size()+" parser="+num_parsers);
       // } finally { // ProgressObservable stuff
			 //total = -1;
         //   current = 7;
          //  Thread.yield();
       // }
    }
    
    /** helper that determines which parser can decode the file */
    protected final DataParser getParserThatCanDecode(final File file) {
		 // jgould What is the point of this method-returning null works here too
		 FileInputStream fis = null;
		 try {
			fis = new FileInputStream(file);
			return UniversalDecoder.getParserWithExt(file.getName());
		 } catch(IOException ioe) {
		 } finally {
			if(fis!=null) {
				try {
					fis.close();	
				} catch(IOException x){}
			}
		 }
		 return null;		 
    }
    /** gets the DataObject from the specified DataObjectProxy 
     * @exception IllegalArgumentException if the specified DataObjectProxy is not found from this source
     * @exception IOException if there was a problem reading the data
     * @exception ParseException if there was some problem with the content of the data
     */
    public final DataObjector getDataObject(final DataObjectProxy proxy) throws java.io.IOException, java.text.ParseException, IllegalArgumentException {
        throw new UnsupportedOperationException("Should not be called. One of the data sources should have been instead.");
    }
    
     /** returns a copy of the array of DataObjectProxy objects
      * note that this is not dynamically updated but just a static array
      *
      *
      */
     public final org.genepattern.gpge.io.DataObjectProxy[] getDataProxies() {
         //throw new UnsupportedOperationException("This shouldn't be used see the variable 'sources'.");
         final int limit = this.sources.length;
         final List list = new ArrayList();
         for(int i = 0; i < limit; i++) {
             final DataObjectProxy[] proxies = sources[i].getDataProxies();
             if( proxies != null )
             ArrayUtils.addToCollection(proxies, list);
         }
         return (DataObjectProxy[])list.toArray(new DataObjectProxy[list.size()]);
     }
     
     // required but not implemented methods
     
          /** returns a description of the source- i.e. if it reads gct files from a local
      * directory, it reads sdf files from OmniGene, etc
      *
      */
     public final String getDescription(final DataObjectProxy proxy) {
         throw new UnsupportedOperationException("Not implemented");
     }
     /** returns the type of data that will be read from the raw input stream
      * For example character (ASCII), or binary, or unknown, data.
      */
     public final StreamType getStreamType(final DataObjectProxy proxy) {
         throw new UnsupportedOperationException("Not implemented");
     }
     /** returns an InputStream for reading the raw data */
     public final java.io.InputStream getRawInputStream(final DataObjectProxy proxy) throws java.io.IOException {
         throw new UnsupportedOperationException("Not implemented");
     }
     /** updates the list of new ones and ones removed  */
     public final void updateList(List new_data, List removed_data) {
         throw new UnsupportedOperationException("Not implemented");
     }
     /** returns the DataParser  */
     public final DataParser getDataParser() {
         throw new UnsupportedOperationException("Not implemented");
     }
     // interface DataSource methods
     
     /** adds the DataSourceUpdateListener to the collection of listeners
      * these listeners will be notified when a new DataObjectProxy
      * object has become available
      *
      */
     public final void addDataSourceUpdateListener(final DataSourceUpdateListener listener) {
         final int limit = sources.length;
         for(int i = 0; i < limit; i++) {
            sources[i].addDataSourceUpdateListener(listener);
        }
     }
     
     /** removes the specified DataSourceUpdateListener from the collection of listeners  */
     public final void removeDataSourceUpdateListener(final DataSourceUpdateListener listener) {
         final int limit = sources.length;
         for(int i = 0; i < limit; i++) {
            sources[i].removeDataSourceUpdateListener(listener);
        }
     }
     
     // getters
     
     /** gets the number of objects available from this DataSource at a particular
      * mement in time
      * @return int the number of available objects
      */
     public final int getCount() {
         return old_list.size();
     }
     /** returns the path that this node represents */
     public final String getLocation() {
         return path;
     }
     /** returns the parent DataSource that spawned this one */
     public GroupDataSource getParent() {
         return parent;
     }
     // overridden methods
     
     /** returns the name of this object */
     public String toString() {
         return name_ref;
     }
     /** the hash */
     public final int hashCode() {
         return hashobject.hashCode() + 17;
     }
     //FIXME these should be implemented for the various Set and Map instances to work
     // predictably  also need to know if two instances are the
     // same i.e. need equals(Object)
     //abstract public int hashCode();
     //abstract public boolean equals(Object other);
     
     // ProgressObservable interface signature
     
    /**
     * @return int negative if not ready or non-negative when total has been calculated
     */    
    public synchronized int getTotal() {
        return total;
    }
    /**
     * @return int the current state of progress of the task relative to the total
     */    
    public synchronized int getCurrent() {
        return current;
    }
     
    // fields
   
    /** if true then collect the unkown files too */
    protected static boolean GET_UNKNOWNS;
    /** the parser for the unkown files */
    protected static final AnythingParser ANYTHINGPARSER = new AnythingParser();
    /** the file filter */
    protected static final FileFilter DEFAULT_DIR_FILE_FILTER;
    /** the hash code creating object*/
    private final Object hashobject;
    /** the parser-based data sources */
    protected final DataSource[] sources;
    /** the array of parsers */
    protected final DataParser[] parsers;
    /** mapping DataParser objs to DataSource objs */
    protected final Map parser_source;
    /** current list of new data objects */
    protected final List added_list;
    /** current list of data objects that have been removed */
    protected final List removed_list;
    /** the list of data objects from the last time updated */
    private final List old_list;
    /** the name of this node */
    protected String name = "Not named yet";
    /** the modified name of this node */
    protected String name_ref = "Not named yet";
    /** the path that this node represents */
    protected String path = "no path yet";
    /** the manager that needs to know about any additional GroupDataSource objs created */
    protected final DataSourceManager manager;
    /** total progress or -1 if not set */
    protected int total = 7;
    /** current state of progress relative to total */
    protected int current;
    /** The parent of this (could be its' parent node) */
    protected final GroupDataSource parent;
    /** static initializer */
    static {
        // determine if unkown files get collected
        boolean get_unknowns = true;
        final String unknown_prop = "gp.project.files.unknown.show";
        try {
            get_unknowns = GPpropertiesManager.getBooleanProperty(unknown_prop, true);
        } catch (java.text.ParseException ex) { // FIXME move this code into GPpropertiesManager
            GenePattern.logWarning("The value for property \""+unknown_prop
                +"\" is not a valid boolean ("
                +GPpropertiesManager.getProperty(unknown_prop)+")");
        }
        GET_UNKNOWNS = get_unknowns;
        // create the FileFilter with it's excludes
        final List list = new ArrayList();
        final String  excludes_text = GPpropertiesManager.getProperty("gp.file.excludes");
        if( excludes_text != null && excludes_text.trim().length() > 0 ) {
            final String[] excludes_array = StringUtils.splitStrings(excludes_text, ',');
            final int limit = excludes_array.length;
            
            final PatternCompiler compiler = new Perl5Compiler();
            
            for(int i = 0; i < limit; i++) {
                try {
                    final Pattern srchPattern = compiler.compile(excludes_array[i]);
                    list.add(srchPattern);
                } catch (com.oroinc.text.regex.MalformedPatternException ex) {
                    GenePattern.logWarning("While compiling regular expressions: '"+excludes_array[i]+"'\n"+ex.getMessage());
                }
            }
        }
        //FIXME compile regexps
        
        DEFAULT_DIR_FILE_FILTER = new FileFilter() {
            /** accepts the file if it is a file 
             * and passes some of the file name exludes
             */
            public final boolean accept(final File pathname) {
                if( !(pathname.isFile()) )
                    return false;
                if( excludes_cnt > 0 ) {
                    final String name = pathname.getAbsolutePath();
                    final int limit = excludes_cnt;
                    for(int i = 0; i < limit; i++) {
                        final Pattern pattern = excludes[i];
                        if( matcher.contains (name, pattern) )
                            return false;
                    }
                }
                return true;
            }
            /** array of reg expresson Patterns to exclude files
             * see http://www.oreilly.com/catalog/regex/
             * for a good tutor
             */
            private final Pattern[] excludes = (Pattern[])list.toArray(new Pattern[list.size()]);
            /** the excludes count */
            private final int excludes_cnt       = excludes.length;
            /** the reg exp pattern matcher */
            private final PatternMatcher matcher = new Perl5Matcher ();
        };
    }
}
