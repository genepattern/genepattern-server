package org.genepattern.server.analysis.webservice.server.dao;

public class AdminDAOSysException extends Exception {
   public  AdminDAOSysException(String message) {
      super(message);  
     
   }
   
   public AdminDAOSysException(String message, Throwable cause) {
      super(message, cause);  
   }
}