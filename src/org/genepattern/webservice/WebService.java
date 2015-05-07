/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.webservice;

/**
 *  Defines methods that all Web Services must implement. <p>
 *
 *  A Web Service is a program that runs within the OmniGene container. Clients
 *  can make RPC calls to Web Service methods by SOAP messages. A SOAP message
 *  will contain the method name to call and the parameters the method is
 *  expecting. Another SOAP message will be sent back to the client containing
 *  the result from the Web Service method. <p>
 *
 *  To implement this interface you can write a generic Web Service by
 *  extending GenericWebService or one of the known subclasses. <p>
 *
 *  This interface defines methods that get the version of the Web Service, the
 *  encoding scheme that you want data sent back as, and Web Service
 *  information,
 *
 * @author     David Turner, Brian Gilman
 */

public interface WebService {
   /**
    *  Returns the name of the web service.
    *
    * @return    a <code>String</code> object containing the name of the web
    *      service.
    */
   public String getWebServiceName();


   /**
    *  Returns information about the web service such as description, version,
    *  and author. <p>
    *
    *  The string that this method returns should be plain text and not markup
    *  like XML.
    *
    * @return    a <code>String</code> containing web service information
    */
   public String getWebServiceInfo();


   /**
    *  Sets the encoding scheme of the web service. The encoding scheme is the
    *  XML format that the data will be returned as (i.e. AGAVE, BSML).
    *
    * @param  scheme  a <code>String</code> object containing the encoding
    *      scheme name
    */
   public void setEncodingScheme(String scheme);


   /**
    *  Returns the encoding scheme name of the web service.
    *
    * @return    a <code>String</code> object containing the encoding scheme
    *      name
    */
   public String getEncodingScheme();


   /**
    *  Sets the encoding scheme version.
    *
    * @param  version  a <code>float</code> containing the encoding scheme
    *      version
    */
   public void setEncodingSchemeVersion(float version);


   /**
    *  Returns the encoding scheme version.
    *
    * @return    the version of the encoding scheme
    */
   public float getEncodingSchemeVersion();


   /**
    *  Pings the webservice.
    *
    * @return    a String object containing "OK" if the service is available.
    */
   public String ping();
}
