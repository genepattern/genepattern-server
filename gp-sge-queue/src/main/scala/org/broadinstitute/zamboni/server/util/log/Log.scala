package org.broadinstitute.zamboni.server.util.log

import org.apache.log4j.Logger;

/**
 * General logging mechanism for genepattern-sqe-plugin. It's implemented (ad-hoc) to be source compatible with the logging calls
 * in the copy of the source code provided by Alec (circa July 2011).
 * It is a thin wrapper around the log4j library, which is the logger used in GenePattern.
 * 
 * @author Peter Carr
 */
object Log {
    lazy val logger = Logger.getLogger("org.broadinstitute.zamboni.server.util");

    def info(message: String) {
      logger.info(message);
    }
    
    def debug(message: String) {
      logger.debug(message);
    }
    
    def warning(message: String) {
      logger.warn(message);
    }
    
    def warning(message: String, t: Throwable) {
      logger.warn(message, t);
    }

    def error(message: String) {
      logger.error(message);
    }
    
    def error(message: String, t: Throwable) {
      logger.error(message, t);
    }
    
    def internalError(message: String) {
      logger.error(message);
    }

    def internalError(message: String, t: Throwable) {
      logger.error(message, t);
    }
}