package org.genepattern.server.webapp.rest.api.v1;

/**
 * Enumeration of link relations, values for the 'rel' property in
 * JSON Link representations.
 * 
 * See: http://www.iana.org/assignments/link-relations/link-relations.xhtml
 * 
 * @author pcarr
 */
public enum Rel {
    // IANA standard link relations
    first,
    last,
    next,
    prev,
    self,
    related,
    
    // GP custom link relations
    gp_job, // a link to a GP job result
    gp_inputFile,
    gp_outputFile,
    gp_stdout,
    gp_stderr,
    gp_logFile,
    gp_status // a link to the status.json representation for a GP job
    
}