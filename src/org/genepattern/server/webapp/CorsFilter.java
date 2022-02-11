/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Filter to enable cross-origin resource sharing (CORS) of GP data files with 3rd party Javascript libraries
 * such as the Javascript Heatmap Viewer, (https://github.com/jheatmap/jheatmap). 
 * 
 * @see <a href="http://enable-cors.org/">http://enable-cors.org/</a>
 * @see <a href="http://www.w3.org/wiki/CORS">http://www.w3.org/wiki/CORS</a>
 * @see <a href="http://www.w3.org/TR/cors/">http://www.w3.org/TR/cors/</a>
 * @author pcarr
 * 
 * Curl templates to validate CORS requests,
 * <pre>
   -H "Origin: http://www.broadinstitute.org", simulate HTTP call from jheatmap.js loaded from the 'www.broadinstitite.org' domain.
   -u <username>:<password>, use HTTP Basic Authentication credentials
   </pre>
 * <ul>
 * <li>case 1: validate HTTP Basic Auth response
   curl --verbose -H "Origin: http://www.broadinstitute.org" {gctUrl}
   Expecting a 401 response.
   <li>case 2: validate file download
   curl --verbose -u {username}:{password} -H "Origin: http://www.broadinstitute.org" {gctUrl}
 * </ul>
 * 
 * Examples
 * <pre>
   # download user uploads file
   curl --verbose -u test:test -H "Origin: http://www.broadinstitute.org" http://gpdev.broadinstitute.org/gp/users/test/all_aml_test.gct

   # download job results file
   curl --verbose -u test:test -H "Origin: http://www.broadinstitute.org" http://gpdev.broadinstitute.org/gp/jobResults/63444/all_aml_test.cvt.gct

   # download server file path file
   curl --verbose -u test:test -H "Origin: http://www.broadinstitute.org" http://gpdev.broadinstitute.org/gp/data//xchip/gpdev/public_data/all_aml_test.gct
 * </pre>
 *
 */


public class CorsFilter implements Filter {
    
    /**
     * proposed CorsFilter implementation
     */
    public static final void applyCorsHeaders(final HttpServletRequest request, final HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        }
        else {
            response.setHeader("Access-Control-Allow-Origin", "*");
        }

        String reqMethods = request.getHeader("Access-Control-Request-Method");
        if (reqMethods == null) {
            reqMethods = "GET, POST, OPTIONS";
        }

        response.setHeader("Access-Control-Allow-Methods", reqMethods);

        String reqHead = request.getHeader("Access-Control-Request-Headers");

        if(null != reqHead && !reqHead.equals("")){
            response.setHeader("Access-Control-Allow-Headers", reqHead);
        }
        // allow HTTP Basic Authentication
        response.setHeader("Access-Control-Allow-Credentials", "true");
    }
    

    @Override
    public void init(FilterConfig arg0) throws ServletException {
    }


    @Override
    public void destroy() {
    }
    
    @Override
    public void doFilter(
            final ServletRequest req, 
            final ServletResponse resp,
            final FilterChain chain
    ) 
    throws IOException, ServletException {

        final HttpServletRequest request = (HttpServletRequest)req;
        final HttpServletResponse response = (HttpServletResponse)resp;
        
        applyCorsHeaders(request, response);
        chain.doFilter(request, response);
    }


}
