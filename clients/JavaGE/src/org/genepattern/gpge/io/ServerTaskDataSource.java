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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileFilter;

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
import org.genepattern.io.parsers.GctParser;
import org.genepattern.server.analysis.ParameterInfo;
import org.genepattern.util.ArrayUtils;
import org.genepattern.util.StringUtils;



import org.genepattern.gpge.ui.analysis.*;

/**
 *  Knows how to create ServerJobDataSource objects from the server
 * @author  keith
 */
public class ServerTaskDataSource extends GroupDataSource{
    
    /** Creates a new instance of ServerTaskDataSource 
     * This will be aware of the specified directory and all
     * data files there.
     */
    public ServerTaskDataSource(final String name, final DataParser[] parsers, final GroupDataSource parent, final DataSourceManager manager) {
        super(createDataSources(parsers), parent, manager, name);
        this.name = name;
        this.name_ref = name;// + " " + parent.getLocation();
        id_jobs = new HashMap();
    }
    
	 /**
	 * Returns the name of this task, e.g. TransposeDataset
	 @return the name.
	 */
	 public String getName() {
		 return name;
	 }
	 
    /** helper for the constructor */
    protected static final DataSource[] createDataSources(final DataParser[] parsers) {
        return new DataSource[0];
    }
    
    /** overriden */
    protected void updateList(final List data) throws java.io.IOException{
        int job_cnt = data.size();
		 
        for(int j = 0; j < job_cnt; j++) {
			  AnalysisJob anal_job = null;
			  try {
				  anal_job = (AnalysisJob)data.get(j);
			  } catch(ArrayIndexOutOfBoundsException e) { // jgould, hack to fix synchronization problem
				  break;
			  }
            if(anal_job.getJobInfo().containsOutputFileParam()){
                final String id = String.valueOf(anal_job.getJobInfo().getJobNumber());
                
                ServerJobDataSource job = (ServerJobDataSource)id_jobs.get(id);
                if( job == null ) {
						 	System.out.println("createServerJobDataSource "+id);
                    job = createServerJobDataSource(id, anal_job);
                }
                
                job.updateList(onlyFileParams(anal_job.getJobInfo().getParameterInfoArray()), PARAMINFO_COMPARATOR);
            } else {
                GenePattern.logWarning("Does not contain ouput files: "+anal_job);
            }
        }
    }
         /** overriden to eliminate the non-File parameters */
    protected ParameterInfo[] onlyFileParams(final ParameterInfo[] infos) {
        final int limit = infos.length;
        final List data = new ArrayList(limit);
        for(int i = 0; i < limit; i++) {
            final ParameterInfo info = infos[i];
            //if( info.isInputFile() || info.isOutputFile() )
            if( info.isOutputFile() )
                data.add(info);
        }
        return (ParameterInfo[])data.toArray(new ParameterInfo[data.size()]);
    }
    /** returns a description of the source- i.e. if it reads data files from a local
     * directory, it reads sdf files from OmniGene, etc
     */
     public String getDescription() {
         return DESCRIPTION + name;
     }
     
     /** returns a parser for specified data  */
     protected DataParser getParserToDecode(final Object data) throws java.io.IOException{
         throw new UnsupportedOperationException("Not implemented");
//         final String remote_file_name = (String)data;
//         DataParser parser = UniversalDecoder.getParserWithExt(remote_file_name);
//         
//         //FIXME inefficient design
//         // "null" means try all parsers on the file
//         if( parser == null ) {
//             return getParserThatDecodesRemote(remote_file_name);
//             // if this is null the file will map to the null object in next step
//             // file could be ignored or reported back as error
//         }
//         return parser;
     }
     
//     // helpers
//     
//     /** gets the parser that can decode the remote file */
//     protected DataParser getParserThatDecodesRemote(final String remote_name) throws java.io.IOException {
//         final String string_url = StringUtils.replaceAll(FILE_HEADER_URL, "[resultfilename]", remote_name);
//         System.out.println("getDataObject()--URL="+string_url);
//         final java.net.URL url = new java.net.URL(string_url);
//         final InputStream in = url.openStream();
//         final DataParser parser = UniversalDecoder.getParser(in, remote_name);
//         in.close();
//         return parser;
//     }
     // helpers
     
     /** creates a ServerTaskDataSource and adds the listeners to it */
     protected final ServerJobDataSource createServerJobDataSource(final String id, final AnalysisJob job) {
         final ServerJobDataSource source = new ServerJobDataSource(job, parsers, this, manager);
         id_jobs.put(id, source);
         // add all listeners FIXME
         //for(final Iterator iter = listeners.iterator(); iter.hasNext(); ) {
         //  final DataSourceUpdateListener listener = (DataSourceUpdateListener)iter.next();
         //  task.addDataSourceUpdateListener(listener);
         //}
         return source;
     }
     
     /** called when this should refresh/rescan its data and update  */
     public void refresh() {
     }
     
    // fields
    /** describes what this DataSource does */
    private static final String DESCRIPTION = "Task: ";
    /** maps the id to the AnalysisJob */
    private final Map id_jobs;
    /** the ParameterInfo Compatator - Compares two ParameterInfo objs by their names */
    protected static final java.util.Comparator PARAMINFO_COMPARATOR = new java.util.Comparator() {
        /** Compares its two arguments for order.*/
        public final int compare(final Object o1, final Object o2) {
            final ParameterInfo pi1 = (ParameterInfo)o1, pi2 = (ParameterInfo)o2;
            return pi1.getName().compareTo(pi2.getName());
        }
          
    };
    /** the URL of the file source jsp page */
    protected static final String FILE_HEADER_URL;
    
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
    }
}
