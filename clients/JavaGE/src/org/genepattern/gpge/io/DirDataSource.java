/*
 * DirDataSource.java
 *
 * Created on February 18, 2003, 4:48 PM
 */

package org.genepattern.gpge.io;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileFilter;

import java.text.ParseException;

import org.genepattern.data.DataModel;
import org.genepattern.data.DataObjector;
import org.genepattern.gpge.GenePattern;
import org.genepattern.io.SummaryError;
import org.genepattern.io.SummaryInfo;
import org.genepattern.io.UniversalDecoder;
import org.genepattern.io.parsers.AbstractDataParser;
import org.genepattern.io.parsers.DataParser;
import org.genepattern.io.parsers.GctParser;
import org.genepattern.util.ArrayUtils;
import org.genepattern.util.GPpropertiesManager;
import org.genepattern.util.StringUtils;

import com.oroinc.text.regex.*;



/**
 *  Knows how to efficiently identify local Data Object files within a directory
 * @author  keith
 */
public class DirDataSource extends GroupDataSource/*implements DataSource*/ {
    
    /** Creates a new instance of GctFileDataSource 
     * This will be aware of the specified directory and all
     * data files there.
     */
    public DirDataSource(final DataParser[] parsers, final File dir, final DataSourceManager manager, final org.genepattern.modules.ui.graphics.PeriodicProgressObserver observer) throws java.io.IOException {
        super(createDataSources(parsers), null, manager, dir);        
        this.file_only_filter = DEFAULT_DIR_FILE_FILTER;
        observer.addProgressObservable(this);
 	  setName(dir);

        setDir(dir);
    }
    
    /** creates FileDataSource array */
    protected static final FileDataSource[] createDataSources(final DataParser[] parsers) {
        final int limit = parsers.length;
        final FileDataSource[] file_sources = new FileDataSource[limit];
        
        for(int i = 0; i < limit; i++) {
            file_sources[i] = new FileDataSource(parsers[i]);
        }
        return file_sources;
    }
    /** create a DataSource from the parser */
    protected final DataSource createDataSource(final DataParser parser) {
        return new FileDataSource(parser);
    }
    /** helper method that converts the file to a directory or returns the file if
     * it already is a dir */
    protected static final File getDirectory(final File file) {
        return ( file.isDirectory() ) ? file : file.getParentFile();
    }
    /** sets the directory
     * note this runs in its own thread because it takes awhile
     */
    private synchronized final void setDir(final File file) throws java.io.IOException {
        if( file == null )
            throw new NullPointerException("The directory cannot be null!");
        this.dir = getDirectory(file);
        if( !dir.exists() )
            throw new java.io.FileNotFoundException("The directory, ("+dir+"), does not exist!");
        
        setName(dir);

        this.path = this.dir.getAbsolutePath();
        
        // keep track of the dir
        String dir_props = org.genepattern.util.GPpropertiesManager.getProperty("gp.project.dirs");
        System.out.println("In "+this+" gp.project.dirs='"+dir_props+"'");
        dir_props = (dir_props != null && dir_props.trim().length() > 0) ? dir_props+", "+this.dir.getCanonicalPath() : this.dir.getCanonicalPath();
        System.out.println("after='"+dir_props+"'");
        org.genepattern.util.GPpropertiesManager.setProperty("gp.project.dirs", dir_props);
        
        refresh();
    }
    
  private synchronized final void setName(final File file) throws java.io.IOException {
        if( file == null )
            throw new NullPointerException("The directory cannot be null!");
       
        this.name = file.getName();
	  if ((this.name.trim()).length() == 0){
		this.name = file.getPath();
	  }

        this.name_ref = name + " (local directory)";
	}

    public synchronized final File getDir() {
	return dir;    
    }
    
    
    /** returns a description of the source- i.e. if it reads gct files from a local
     * directory, it reads sdf files from OmniGene, etc
     */
     public String getDescription() {
         return DESCRIPTION + dir;
     }
         
     /** returns a parser for specified data  */
     protected DataParser getParserToDecode(final Object data) {
         final File file = (File)data;
         final String file_name = file.getName();
         final DataParser parser = UniversalDecoder.getParserWithExt(file_name);
         // "null" means try all parsers on the file
         if( parser == null ) {
               return getParserThatCanDecode(file);
             // if this is null the file will map to the null object in next step
             // file could be ignored or reported back as error
         } 
         return parser;
     }
     
     /** paste some Object
      * @param source the file that is to be pasted
      * @param is_temp if true then the file is renamed otherwise it is copied
      * @throws IOException if some problem
      */
     public final void paste(final File source, final boolean is_temp) throws java.io.IOException {
         if( !source.isFile() ) {
             System.out.println("Paste: warning not a file "+source.getName());
             return ; // not a file
         }
         if( source.getParentFile().equals(this.dir) ) {
             System.out.println("Paste: Warning file, "+source.getName()
                 +", not copied! Already present in this dir...");
             return ; // already in this dir
         }
         
         final String name = createUniqueName(source.getName());
         final File new_file = new File(dir, name);
         if( is_temp ) { //just rename it to be in this dir
             source.renameTo(new_file);
         } else { // write input to the new file name
             // FIXME if not text should use OutpuStream not Write
             final FileInputStream in = new FileInputStream(source);
             org.genepattern.io.StorageUtils.writeToFile(new_file, in);
             in.close();
             
             final Thread thread = new Thread(new org.genepattern.util.RunLater() {
                 public final void runIt() throws Exception {
                     updateList(DirDataSource.this.dir.listFiles(file_only_filter));
                 }
             });
             thread.start();
         }
     }
     /** creates a unique file name that doesn't conflict with the current names in this directory */
     protected String createUniqueName(final String name) {
         final String[] list = dir.list();
         Arrays.sort(list);
         final int search = Arrays.binarySearch(list, name);
         if( search < 0 ) // negative => not found
            return name;
         String new_name = name+"_1";
         for(int i = 2; Arrays.binarySearch(list, new_name) >= 0; i++) {
             new_name = name +'_'+i;
         }
         return new_name;
         
     }
     
     // overriden methods
     
     /** is this equal to another DirDataSource */
     public final boolean equals(final Object another) {
         if( another instanceof DirDataSource ) {
             final DirDataSource dds = (DirDataSource)another;
             return ( this.dir == dds.dir || (this.dir != null && this.dir.equals(dds.dir)) );
         }
         return false;
     }
     /** returns true if this has the same directory */
     public final boolean hasSameDir(final File other_dir) {
         return this.dir.equals(other_dir);
     }
     
     /** called when this should refresh/rescan its data and update 
      * refreshes the directory
      */
    public final void refresh() throws IOException {
        updateList(dir.listFiles(file_only_filter));
    }
     
    // fields
    /** describes what this DataSource does */
    private static final String DESCRIPTION = "Files from directory:\n";
    /** the file filter */
    protected static final FileFilter DEFAULT_DIR_FILE_FILTER;
    /** the directory that should have GP data files */
    protected File dir;
    /** the file filter used to root out the files that shouldn't be GCT files */
    protected final FileFilter file_only_filter;
    /** static initializer */
    static {
        // create the FileFilter with it's excludes
        final List list = new ArrayList();
        final String  excludes_text = GPpropertiesManager.getProperty("gp.file.excludes");
        if( excludes_text != null && excludes_text.trim().length() > 0 ) {
            final String[] excludes_array = StringUtils.splitStrings(excludes_text, ',');
            final int limit = excludes_array.length;
            
            final PatternCompiler compiler = new Perl5Compiler();
            
            for(int i = 0; i < limit; i++) {
                try {
                    System.out.println("compiling:'"+excludes_array[i]+"'");
                    final Pattern srchPattern = compiler.compile(excludes_array[i]);
                    list.add(srchPattern);
                } catch (com.oroinc.text.regex.MalformedPatternException ex) {
                    GenePattern.logWarning("While compiling regular expressions:\n"+ex.getMessage());
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
								try {
									if( matcher.contains (name, pattern) )
										return false;
								} catch(ArrayIndexOutOfBoundsException e) {
										return false;	
								}
                        Thread.yield();
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
            private final int excludes_cnt = excludes.length;
            private final PatternMatcher matcher   = new Perl5Matcher ();
        };
    }
}
