/*
 * FileDataSource.java
 *
 * Created on February 18, 2003, 4:48 PM
 */

package org.genepattern.gpge.io;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileFilter;

import java.net.URLEncoder;

import java.text.ParseException;

import org.genepattern.data.AbstractObject;
import org.genepattern.data.DataModel;
import org.genepattern.data.DataObjector;
import org.genepattern.gpge.GenePattern;
import org.genepattern.io.SummaryError;
import org.genepattern.io.SummaryInfo;
import org.genepattern.io.parsers.AbstractDataParser;
import org.genepattern.io.parsers.DataParser;
import org.genepattern.io.parsers.GctParser;
import org.genepattern.server.analysis.ParameterInfo;
import org.genepattern.util.StringUtils;





/**
 *  Knows how to identify remote Data files and wrap them in a DataObjectProxy
 * @author  keith
 */
public class ServerFileDataSource extends AbstractDataSource {
    
    /** Creates a new instance of GctFileDataSource 
     * This will be aware of the specified directory and all
     * files that the parser handles there.
     */
    public ServerFileDataSource(final DataParser parser) {
        super(parser);
        proxy_file = new HashMap();
    }
    
    /** creates a DataObjectProxy */
    protected DataObjectProxy createDataObjectProxy(final Object data) {
        System.out.println("creating DataObjectProxy from ParameterInfo "+data);
        final ParameterInfo param = (ParameterInfo)data;
        final String remote_file = param.getValue();
        //System.out.println("A new file "+file);
        Exception exception = null;
        SummaryInfo summary = null;
        try {
            final String string_url = StringUtils.replaceAll(ServerJobDataSource.FILE_HEADER_URL, "[resultfilename]", URLEncoder.encode(remote_file, "UTF-8"));
            final java.net.URL url = new java.net.URL(getFileDownloadURL(remote_file));
				java.net.URLConnection conn = url.openConnection();
				
				String size = (long) (Math.ceil(conn.getContentLength() / 1024.0)) + " KB"; // size returned from InputStream.available is not correct
            final InputStream in = url.openStream();
            
            summary = parser.createSummary(in);
				if(summary.getPrimaryinfo()!=null && summary.getPrimaryinfo().containsKey("Size=")) {
					summary.getPrimaryinfo().put("Size=", size);
				} else if(summary.getPrimaryinfo()!=null && summary.getPrimaryinfo().containsKey("Size")) {
					summary.getPrimaryinfo().put("Size", size);
				}
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
            summary = new SummaryError(remote_file, exception, parser);
        //final String name = AbstractDataParser.getFileNameNoExt(remote_file);
        //final String better_name = getNameNoJunk(name);
        final String better_name = getNameNoJunk(remote_file);
        System.out.println("Remote file: "+remote_file+"\nbetter_name= "+better_name);
        final DataObjectProxy proxy = new DefaultDataObjectProxy(better_name, this, summary);
        return proxy;
    }
    /** gets the DataObject from the specified DataObjectProxy 
     * @exception IllegalArgumentException if the specified DataObjectProxy is not found from this source
     * @exception IOException if there was a problem reading the data
     * @exception ParseException if there was some problem with the content of the data
     */
    public DataObjector getDataObject(final DataObjectProxy proxy) throws java.io.IOException, java.text.ParseException, IllegalArgumentException {
        final InputStream in = getRawInputStream(proxy);
        final DataObjector data = parser.parse(in, proxy.getName());
        in.close();
        return data;
    }
    /** returns a description of the source- i.e. if it reads gct files from a local
     * directory, it reads sdf files from OmniGene, etc
     */
     public String getDescription(DataObjectProxy proxy) {
         final ParameterInfo info = (ParameterInfo)proxy_data.get(proxy);
         return DESCRIPTION + info.getValue();
     }
    
    /** returns an InputStream for reading the raw data */
    public InputStream getRawInputStream(final DataObjectProxy proxy) throws IOException {
        final String remote_file = getRemoteFileNameFor(proxy);
        return createRemoteFileInputStream(remote_file);
    }
    public static final InputStream createRemoteFileInputStream(final String remote_file_name) throws IOException {
        final String just_filename = getJustFileName(remote_file_name);
        String string_url = StringUtils.replaceAll(FILE_SOURCE_URL, "[just_filename]", URLEncoder.encode(just_filename, "UTF-8"));
        string_url = StringUtils.replaceAll(string_url, "[just_dir]", URLEncoder.encode(getJustPath(remote_file_name), "UTF-8"));
        
        System.out.println("getDataObject()--URL="+string_url);
        final java.net.URL url = new java.net.URL(string_url);
		  try {
			  return url.openStream();
		  } catch(java.net.ConnectException ce) {
			  javax.swing.JOptionPane.showMessageDialog(GenePattern.getDialogParent(),"Unable to connect to server " + url.getHost() + " on port " + url.getPort());
			  throw ce;
		  }
    }
	 
	 
	 /** 
	 * Gets the url to download the specified file on the server
	 */
	  public static String getFileDownloadURL(String fileOnServer) throws IOException {
        String fileName = getJustFileName(fileOnServer);
        String retrieveFileURL = StringUtils.replaceAll(FILE_SOURCE_URL, "[just_filename]", URLEncoder.encode(fileName, "UTF-8"));
        retrieveFileURL = StringUtils.replaceAll(retrieveFileURL, "[just_dir]", URLEncoder.encode(getJustPath(fileOnServer), "UTF-8"));
        return retrieveFileURL;
    }
	 
    /** gets the remote file name */
    public final String getRemoteFileNameFor(final DataObjectProxy proxy) {
        final ParameterInfo param = (ParameterInfo)proxy_data.get(proxy);
        if( param == null )
            throw new IllegalArgumentException("The DataObjectProxy is not found!\n"+proxy);
        
        return param.getValue();
    }
    /**  */
    public static final String getJustFileName(final String name) {
        final int bad = AbstractObject.getLastBadCharIndex(name);
        if( bad < 0 )
            return name;
        return name.substring(bad + 1);
    }
    /**  */
    public static final String getJustPath(final String name) {
        final int bad = AbstractObject.getLastBadCharIndex(name);
        if( bad < 0 )
            return "";
        return name.substring(0, bad);
    } 
    
    /** returns a File on the local system where the data can be read
     * note this could be an expensive operation if the file is large and on the server
     * (at least initially read from the server to create the file)
     *
     */
    public java.io.File getAsLocalFile(DataObjectProxy proxy) throws IOException {
      // File file = (File)proxy_file.get(proxy);
       // if( file == null || !file.exists()) { always get the file from the server
            //file = File.createTempFile(proxy.toString(), '.'+parser.getFileExtensions()[0]);
            //file = File.createTempFile("", proxy.toString());
           File file = org.genepattern.io.StorageUtils.createTempFileNoMung(proxy.toString());
            final InputStream in = this.getRawInputStream(proxy);
            try {
                org.genepattern.io.StorageUtils.writeToFile(file, in);
                proxy_file.put(proxy, file);
            } finally {
                in.close();
            }
      //  }
        return file;
    }
    
    /** */
    public static final String getNameNoJunk(final String name) {
        String better_name = name;
        final int index = name.lastIndexOf('\\');
        System.out.println("name="+name);
        if( index > 0 ) {
            better_name = name.substring(index + 1);
        }
        
        final int index_slash = better_name.lastIndexOf('/');
        if( index_slash > 0 ) {
            better_name = better_name.substring(index_slash + 1);
        }
        
        if( better_name.startsWith("Axis") ) {
            better_name = better_name.substring(better_name.indexOf('_') + 1);
        }
        
        final String seperator = org.genepattern.io.StorageUtils.TEMP_FILE_MARKER;
        final int sep_ind = better_name.indexOf(seperator);
        if( sep_ind >= 0 ) {
            System.out.println("Debug: seperator=\""+seperator+"\" sep_ind="+sep_ind);
            better_name = better_name.substring(sep_ind + seperator.length() + 2);
        }
        System.out.println("Fixed name: \""+better_name+"\"");
        return better_name;
        
    }
//    /**  */
//    protected static final String getNameNoJunk(final String name) {
//        final String seperator = org.genepattern.io.StorageUtils.TEMP_FILE_MARKER;
//        final int index = name.indexOf(seperator);
//        if( index < 0 ) {
//            if( name.startsWith("Axis") )
//                return name.substring(name.indexOf('_') + 1);
//            else
//                return name;
//        }
//        return name.substring(index + seperator.length() + 2);
//    }
    
    /** this can be overridden by subclasses to supply a Comparator */
    protected java.util.Comparator getComparator() {
        return ServerTaskDataSource.PARAMINFO_COMPARATOR;
    }
    
    // fields
    /** describes what this DataSource does */
    protected static final String DESCRIPTION = "Files from remote directory: ";
    /** the file source url - jsp page to get the whole file */
    protected static final String FILE_SOURCE_URL;
    /** maps a proxy to the local file of the proxy object or null if not created */
    protected final Map proxy_file;
    
    /** static initializer */
    static {
        String source_url = null;
        try {
            final java.util.Properties gp_props = org.genepattern.server.util.PropertyFactory.getInstance().getProperties("omnigene.properties");
            source_url = gp_props.getProperty("retrieve.file");
        } catch (Exception ex) {
            org.genepattern.gpge.GenePattern.showError(null, "Cannot get the result file source URL", ex);
        }
        FILE_SOURCE_URL = source_url;
        if( source_url == null )
            throw new IllegalStateException("Cannot get the result file source URL!");
        
    }
}
