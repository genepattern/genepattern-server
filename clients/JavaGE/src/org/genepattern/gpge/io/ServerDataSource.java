/*
 * DirDataSource.java
 *
 * Created on February 18, 2003, 4:48 PM
 */

package org.genepattern.gpge.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.Properties;

import org.genepattern.gpge.ui.tasks.FileNode;
import org.genepattern.gpge.ui.tasks.ResultFile;
import org.genepattern.gpge.ui.tasks.TreeNodeFactory;
import org.genepattern.io.SummaryError;
import org.genepattern.io.SummaryInfo;
import org.genepattern.io.UniversalDecoder;
import org.genepattern.io.parsers.DataParser;
import org.genepattern.util.StringUtils;

/**
 *  Knows how to identify remote Data Object files on a server
 * @author  keith
 */
public class ServerDataSource extends GroupDataSource implements java.util.Observer {
    
    /** Creates a new instance of ServerDataSource 
     * This will be aware of the specified directory and all
     * data files there.
     */
    public ServerDataSource(final org.genepattern.gpge.ui.tasks.DataModel analysis_model, final org.genepattern.gpge.ui.tasks.ResultsPanel analysis_results_panel, final DataParser[] parsers/*DataSource[] sources*/) throws java.io.IOException, org.genepattern.webservice.PropertyNotFoundException {
        super(createSources(parsers), null, null, parsers);
        this.analysis_model = analysis_model;
        this.analysis_results_panel = analysis_results_panel;
        server_node = new javax.swing.tree.DefaultMutableTreeNode("Server (remote access)", true/*allows childern*/);
        
        final Properties p = org.genepattern.util.PropertyFactory.getInstance().getProperties("omnigene.properties");
        file_header_source_url = p.getProperty("result.file.header.source");
        if( file_header_source_url == null )
            throw new IllegalStateException("Cannot get the result file header source URL!");
        
        this.name = "Server";
        this.name_ref = name + " (remote access)";
        
        // last thing
        analysis_model.addObserver(this);
    }
    
    /** helper for constructor */
    protected static final DataSource[] createSources(final DataParser[] parsers) {
        return new DataSource[0];
    }

    // java.util.Observer interface method signature
    /**
     * implements the method from interface Observer
     * @param o <code>Observable<code> object is the omiview.analysis
     *          package's <code>DataModel<code>
     * @param arg Unused...
     */
    public void update(java.util.Observable observable, Object obj) {
        System.out.println("updating observer ServerDataSource obj="+obj+" (should be null...)");
        this.updateList();
    }
    /** does nothing no need to update */
    public void refresh() { /*no op*/ }
    
    /** updates the list of data files previously found
     * Note: this should be run in a seperate thread
     */
    protected void updateList() {
        throw new UnsupportedOperationException("Not implemented");
        //notifyDataSourceListenersRemove(old_nodes);
        //notifyDataSourceListenersAdd(nodes);
    }

    /** returns a description of the source- i.e. if it reads data files from a local
     * directory, it reads sdf files from OmniGene, etc
     */
     public String getDescription() {
         return DESCRIPTION + name;
     }
     
     /** returns a parser for specified data  */
     protected DataParser getParserToDecode(Object data) {
         throw new UnsupportedOperationException("Not implemented");
     }
     
    // fields
    /** describes what this DataSource does */
    private static final String DESCRIPTION = "Retrives data from the server, \n";
    /** the edu....omniview.analysis package's DataModel */
    protected final org.genepattern.gpge.ui.tasks.DataModel analysis_model;
    /** the tree model this should be deleted Kludge time... */
    protected javax.swing.tree.DefaultTreeModel tree_model;
    /** the edu....omniview.analysis package's ResultsPanel */
    protected final org.genepattern.gpge.ui.tasks.ResultsPanel analysis_results_panel;
    /** the URL of the header source jsp page */
    private final String file_header_source_url;
    /** the stream type
     * FIXME this information needs to come from the DataParser
     */
    protected final StreamType stream_type = StreamType.TEXT;
    /** the server's node that this is a souce for */
    private final javax.swing.tree.DefaultMutableTreeNode server_node;
    /** the node factory that will create TreeNode objects with DataObjectProxy objs*/
    private final TreeNodeFactory node_factory = new TreeNodeFactory() {
        public javax.swing.tree.MutableTreeNode createNode(final javax.swing.tree.MutableTreeNode node) {
            final FileNode fnode = (FileNode)node;
            final ResultFile result_file = (ResultFile)fnode.getUserObject();
            final String server_file_name = result_file.getFileName();
            System.out.println("ResultsFile file_name="+server_file_name);
            Exception exception = null;
            SummaryInfo summary = null;
            try {
                System.out.println("before replacement: file_header_source_url="+file_header_source_url);
                final String string_url = StringUtils.replaceAll(file_header_source_url, "[resultfilename]", URLEncoder.encode(server_file_name, "UTF-8"));
                System.out.println(" after: file_header_source_url="+file_header_source_url);
                final java.net.URL url = new java.net.URL(string_url);
                final InputStream in = url.openStream();
                summary = UniversalDecoder.createSummary(in, server_file_name);
                in.close();
            } catch (IOException ex) {
                //in.close();
                exception = ex;
            } catch (ParseException ex) {
                exception = ex;
            }
            if( summary == null ) {
                final DataParser parser = UniversalDecoder.getParserWithExt(server_file_name);
                summary = new SummaryError(server_file_name, exception, (parser != null)? parser: parsers[0]);
            }
            //final String name = AbstractDataParser.getFileNameNoExt(server_file_name);
            //final DataObjectProxy proxy = new DefaultDataObjectProxy(name, this, summary);
            final DataObjectProxy proxy = new DefaultDataObjectProxy(server_file_name, ServerDataSource.this, summary);
            return new javax.swing.tree.DefaultMutableTreeNode(proxy, false/* no childern*/);
        }
    };
}
