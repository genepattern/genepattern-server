package org.genepattern.server;

/**
 * <p>Title: AnalysisServiceException.java </p>
 * <p>Description: Super Exception class for all analysis service exception.</p>
 * @author Hui Gong
 * @version 1.0
 */

public class AnalysisServiceException extends Exception{

    public AnalysisServiceException() {
        super();
    }

    public AnalysisServiceException(String s){
        super(s);
    }
}