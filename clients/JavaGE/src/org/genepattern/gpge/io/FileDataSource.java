/*
 * FileDataSource.java
 *
 * Created on February 18, 2003, 4:48 PM
 */

package org.genepattern.gpge.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import org.genepattern.data.DataObjector;
import org.genepattern.io.SummaryError;
import org.genepattern.io.SummaryInfo;
import org.genepattern.io.parsers.AbstractDataParser;
import org.genepattern.io.parsers.DataParser;



/**
 *  Knows how to identify local Data files and wrap them in a DataObjectProxy
 * @author  keith
 */
public class FileDataSource extends AbstractDataSource {
    
    /** Creates a new instance of GctFileDataSource 
     * This will be aware of the specified directory and all
     * files that the parser handles there.
     */
    public FileDataSource(final DataParser parser) {
        super(parser);
        //updateList();
    }
    
    /** creates a DataObjectProxy */
    protected DataObjectProxy createDataObjectProxy(final Object data) {
          final File file = (File)data;
          //System.out.println("A new file "+file);
          Exception exception = null;
          SummaryInfo summary = null;
          try {
              final FileInputStream in = new FileInputStream(file);
              summary = parser.createSummary(in);
              in.close();
          } catch (IOException ex) {
              //in.close();
              exception = ex;
          } catch (ParseException ex) {
              exception = ex;
          } catch (java.lang.RuntimeException ex) {
              exception = ex;
          }
          if( summary == null )
              summary = new SummaryError(file, exception, parser);
          
          final String name = ( show_exts ) ? file.getName() : AbstractDataParser.getFileNameNoExt(file);
          final DataObjectProxy proxy = new DefaultDataObjectProxy(name, this, summary);
          return proxy;
    }
    /** gets the DataObject from the specified DataObjectProxy 
     * @exception IllegalArgumentException if the specified DataObjectProxy is not found from this source
     * @exception IOException if there was a problem reading the data
     * @exception ParseException if there was some problem with the content of the data
     */
    public DataObjector getDataObject(final DataObjectProxy proxy) throws java.io.IOException, java.text.ParseException, IllegalArgumentException {
        final File file = (File)proxy_data.get(proxy);
        if( file == null ) 
            throw new IllegalArgumentException("The DataObjectProxy is not found!\n"+proxy);
        final FileInputStream in = new FileInputStream(file);
        DataObjector data = parser.parse(in, AbstractDataParser.getFileNameNoExt(file));
        in.close();
        return data;
    }
    /** returns a description of the source- i.e. if it reads gct files from a local
     * directory, it reads sdf files from OmniGene, etc
     */
     public String getDescription(DataObjectProxy proxy) {
         final File file = (File)proxy_data.get(proxy);
         return DESCRIPTION + file.getParent();
     }
    
    /** returns an InputStream for reading the raw data */
    public InputStream getRawInputStream(final DataObjectProxy proxy) throws IOException {
        final File file = (File)proxy_data.get(proxy);
        return new FileInputStream(file);
    }
     
    /** returns a File on the local system where the data can be read
     * note this could be an expensive operation if the file is large and on the server
     * (at least initially read from the server to create the file)
     *
     */
    public java.io.File getAsLocalFile(DataObjectProxy proxy) throws IOException {
        return (File)proxy_data.get(proxy);
    }
    
    // fields
    /** describes what this DataSource does */
    protected static final String DESCRIPTION = "Files from directory: ";
    
    
}
