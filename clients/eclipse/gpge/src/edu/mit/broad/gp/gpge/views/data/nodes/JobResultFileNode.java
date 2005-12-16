/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/


/*
 * Created on Jun 19, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.broad.gp.gpge.views.data.nodes;


import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;



public class JobResultFileNode extends AbstractFileNode{
    String fileName;
    Date lastModificationDate;
    JobResultNode parent;
    /** Whether this file has been deleted from the server */
    private boolean deleted = false;
    
    public String getFileName() {
        return fileName;
    }
    
    /** 
     * 
     * Gets whether this file exists on the server 
     * 
     * */
    public boolean exists() {
        return !deleted;
    }
    
    public void downloadFile(java.io.File f) throws IOException {
        java.io.InputStream is = getURL()
        .openStream();
        byte[] bytes = new byte[10000];
        int bytesRead = 0;

        java.io.FileOutputStream fos = new java.io.FileOutputStream(
        f);
        	while ((bytesRead = is.read(bytes)) != -1) {
        	    fos.write(bytes, 0, bytesRead);
        	}
    }
    
    public URL getURL() {
        String fileName = this.getFileName();
	    String server = parent.getServer();
	    String url = "http://" + server + "/gp/retrieveResults.jsp?job=" + parent.getJobNumber() + "&filename=" + fileName; 
	    try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new Error(e);
        }
    }
    
    public JobResultFileNode(JobResultNode parent, String fileName) {
        this.parent = parent;
        this.fileName =fileName;
        lastModificationDate = new Date();
    }
    
    public TreeNode parent() {
		return parent;
	}

	
	public String getColumnText(int column) {
		if(column==0){
		    return fileName;
		} else if(column==1) {
			return getFileExtension();
		} else {
		    return DateFormat.getInstance().format(lastModificationDate);
		}
	}

    /**
     * 
     */
    public void refresh() {
        URL url = getURL();
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if(conn.getResponseCode()==HttpURLConnection.HTTP_NOT_FOUND) {
                deleted = true;
            } else {
                deleted = false;
            }
        } catch (IOException e) {
            deleted = true;
            e.printStackTrace();
        }
        
    }

    /**
     * 
     */
    public void removeFromParent() {
        parent.remove(this);
    }
}