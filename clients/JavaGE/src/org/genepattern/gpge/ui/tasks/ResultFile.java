package org.genepattern.gpge.ui.tasks;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * @author Hui Gong
 * @version 1.0
 */
 
public class ResultFile{
    private String _site;
    private int _id;
    private String _filename;

    public ResultFile(String siteName, int jobID, String filename) {
        this._site = siteName;
        this._id = jobID;
        this._filename = filename;
    }

    public String getSite(){
        return this._site;
    }

    public int getJobID(){
        return this._id;
    }

    public String getFileName(){
        return this._filename;
    }

    public String toString(){
        return this._filename;
    }
}