/*
 * DirDataSource.java
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
import java.util.Properties;
import java.util.Set;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileFilter;

import java.net.URLEncoder;

import java.text.ParseException;

import org.genepattern.data.DataModel;
import org.genepattern.data.DataObjector;
import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.ui.analysis.AnalysisJob;
import org.genepattern.io.SummaryError;
import org.genepattern.io.SummaryInfo;
import org.genepattern.io.UniversalDecoder;
import org.genepattern.io.parsers.AbstractDataParser;
import org.genepattern.io.parsers.DataParser;
import org.genepattern.io.parsers.TextParser;
import org.genepattern.server.analysis.JobInfo;
import org.genepattern.server.analysis.ParameterInfo;
import org.genepattern.util.ArrayUtils;
import org.genepattern.util.StringUtils;


//import org.genepattern.io.parsers.GctParser;

import org.genepattern.gpge.ui.analysis.*;

/**
 *  Knows how to identify remote Jobs from the server and create ServerFileDataSource
 * objects. Remote file proxies are then stored in the ServerFileDataSource by 
 * the parser that accepted them.
 * @author  keith
 */
public class ServerJobDataSource extends GroupDataSource{
    
    /** Creates a new instance of ServerJobDataSource 
     * This will be aware of the specified directory and all
     * data files there.
     */
    public ServerJobDataSource(final AnalysisJob job, final DataParser[] parsers, final GroupDataSource parent, final DataSourceManager manager) {
        super(createDataSources(parsers), parent, manager, new Integer(job.getJobInfo().getJobNumber()));
        this.job = job;
        resetName();
    }
    
    /**
    * Returns the job for this data source 
    * @return the job
   	*/
    public AnalysisJob getJob() {
    	return job;
    }
    
    /** helper for the constructor */
    protected static final DataSource[] createDataSources(final DataParser[] prsrs) {
        // if parsers arn't available get some...
        final DataParser[] parsers = ( prsrs.length > 0)? prsrs: (DataParser[])UniversalDecoder.PARSER_LIST.toArray(new DataParser[UniversalDecoder.PARSER_LIST.size()]);
        final int limit = parsers.length;
        final ServerFileDataSource[] sources = new ServerFileDataSource[limit + 1];
        for(int i = 0; i < limit; i++) {
            sources[i] = new ServerFileDataSource(parsers[i]);
        }
        sources[limit] = new ServerFileDataSource(TEXT_PARSER);
        return sources;
    }
    /** create a DataSource from the parser */
    protected final DataSource createDataSource(final DataParser parser) {
        return new ServerFileDataSource(parser);
    }
    /** resets the name given the preference */
    protected void resetName() {
        final JobInfo info = job.getJobInfo();
        this.name = String.valueOf(info.getJobNumber());
        //if( prefs.equals("end.date") )
        //this.name_ref = info.getDateCompleted().toString();
        java.util.Date date = info.getDateCompleted();
        this.name_ref  = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(date) + " (job " + info.getJobNumber() + ")";
    }
    
//    /** overriden */
//    protected void updateList(final List data) {
//        final int param_cnt = data.size();
//        for(int j = 0; j < job_cnt; j++) {
//            final AnalysisJob job = (AnalysisJob)data.get(j);
//            if(job.getJobInfo().containsOutputFileParam()){
//                final ParameterInfo[] params = job.getJobInfo().getParameterInfoArray();
//                final int params_cnt = params.length;
//                for(int i=0; i < params_cnt; i++){
//                    if(params[i].isOutputFile()){
//                        final String file_path = params[i].getValue();
//                        System.out.println("file path: "+file_path);
//                        //final String filename = file_path.substring(file_path.lastIndexOf("__")+2);
//                        final String filename = file_path;
//                        //final ResultFile file = new ResultFile(siteName, job.getJobInfo().getJobNumber(), filename);
//                        //final ResultTreeNode fileNode = new FileNode(file, this._dataModel, this._treeModel, this._result);
//                        //final MutableTreeNode fileNode = node_factory.createNode(new FileNode(file, _dataModel, _treeModel, _result));
//                        //jobNode.add(fileNode);
//                    } else
//                        System.out.println("Job "+job+" params["+i+"] does not have output file: "+params[i]);
//                }
//            } else {
//                GenePattern.logWarning("Does not contain ouput files: "+job);
//            }
//        }
//    }
    /** returns a description of the source- i.e. if it reads data files from a local
     * directory, it reads sdf files from OmniGene, etc
     */
     public String getDescription() {
         return DESCRIPTION + name;
     }
     
     /** returns a parser for specified data  */
     protected DataParser getParserToDecode(final Object data) throws java.io.IOException{
         System.out.println("getting parser to decode remote (ParameterInfo) "+data);
         final ParameterInfo param = (ParameterInfo)data;
         if(param.isOutputFile()){
             final String remote_file_name = param.getValue();
             if( isError(remote_file_name) ) {
                 final HashMap hash = param.getAttributes();
                 final Object has_seen = hash.get(IS_OLD);
                 if( has_seen == null || Boolean.FALSE.equals(has_seen) ) {// if not shown error before
                     hash.put(IS_OLD, Boolean.TRUE);
                     final String string_url = FILE_SOURCE_URL + URLEncoder.encode(remote_file_name, "UTF-8");
                     System.out.println("getParserToDecode()--URL="+string_url);
                     final java.net.URL url = new java.net.URL(string_url);
                     final InputStream in = url.openStream();
                     final StringWriter swriter = new StringWriter();
                     final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                     
                     org.genepattern.io.StorageUtils.copyReaderToWriter(reader, swriter);
                     final StringBuffer buf = swriter.getBuffer();
                     //buf.insert(0, param.getAttributes());
                     buf.insert(0, "Remote Error:\n");
                     GenePattern.showError(null, buf.toString());
                     reader.close();
                     swriter.close();
                 }
                 return TEXT_PARSER;
            }
             System.out.println("remote file name: "+remote_file_name);
             //final String filename = file_path.substring(file_path.lastIndexOf("__")+2);
             DataParser parser = null;
             
             // "null" means try all parsers on the file
             if( parser == null ) {
                 parser = getParserThatDecodesRemote(remote_file_name);
                 // if this is null the file will map to the null object in next step
                 // file could be ignored or reported back as error
             }
				 if( parser == null ) {
                 
                 if( remote_file_name.endsWith("stdout") ) {
                     parser = TEXT_PARSER;
                 } else {
                     final String ext = getExt(remote_file_name);
                     if ( ext != null && ArrayUtils.contains(TEXT_PARSER.getFileExtensions(), ext) )
                        parser = TEXT_PARSER;
                 }
             }
             if( parser == null )
                 parser = ANYTHINGPARSER;
             return parser;
         } else {
             System.err.println("ParameterInfo does not have output file: "+param);
             return null;
         }
     }
     
     // helpers
     /** determines by the name of the file if it is an error text file */
     protected static final boolean isError(final String file_name) {
         return file_name.endsWith("stderr");
     }
     /** gets the file extension */
     protected static final String getExt(final String file_name) {
         if( file_name == null )
            return null;
        final String ext = file_name.trim().toLowerCase();
        if( ext.length() == 0 )
            return null;
        
        final int dot = ext.lastIndexOf('.');
        if( dot < 0 )
            return null;
        return ext.substring(dot, ext.length());
        
     }
     /** gets the parser that can decode the remote file */
     protected DataParser getParserThatDecodesRemote(final String remote_name) throws java.io.IOException {
			final java.net.URL url = new java.net.URL(ServerFileDataSource.getFileDownloadURL(remote_name));
			java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
			if(conn.getResponseCode()==java.net.HttpURLConnection.HTTP_NOT_FOUND) {
				throw new IOException(remote_name + " deleted.");	
			} // check to see if file exists on server
		
			DataParser parser = UniversalDecoder.getParserWithExt(remote_name);
         return parser;
     }
     
     /** called when this should refresh/rescan its data and update  */
     public void refresh() {
     }
//     /** overriden to eliminate the non-File parameters */
//    protected void updateList(final List data, final java.util.Comparator comparer) throws java.io.IOException {
//        final int limit = data.size();
//        for(int i = 0; i < limit; i++) {
//            final ParameterInfo info = (ParameterInfo)data.get(i);
//            if( info.isInputFile() )
//                data.remove(i);
//        }
//        super.updateList(data, comparer);
//    }
     
    // fields
    /** describes what this DataSource does */
    private static final String DESCRIPTION = "Task: ";
    /** key for getting the attribute Boolean 
     * TRUE if the parameterinfo was seen before
     * FALSE or null if not
     */
    public static final String IS_OLD = "is old";
    /** the AnalysisJob */
    private final AnalysisJob job;
    /** the URL of the file source jsp page */
    protected static final String FILE_HEADER_URL;
    /** the URL for getting the whole thing */
    protected static final String FILE_SOURCE_URL;
    /** the text parser */
    protected static final TextParser TEXT_PARSER = new TextParser("Text Parser");
    
    /** static initializer */
    static {
        String fhu = null;
        try {
            final Properties gp_props = org.genepattern.server.util.PropertyFactory.getInstance().getProperties("omnigene.properties");
            fhu = gp_props.getProperty("result.file.header.source");
        } catch (Exception ex) {
            GenePattern.showError(null, "Cannot get the result file header source URL", ex);
        }
        FILE_HEADER_URL = fhu;
        if( fhu == null )
            throw new IllegalStateException("Cannot get the result file header source URL");
        FILE_SOURCE_URL = fhu.substring(0, fhu.lastIndexOf("[resultfilename]"));
    }
}
