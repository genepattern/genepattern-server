/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.jsf;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.apache.log4j.Logger;

/**
 * Converter for formatting file size, as File.length in bytes, in human readable form.
 * E.g. 12.3 GB, 120 MB, 1 KB et cetera
 * 
 * @author pcarr
 */
public class FileLengthConverter implements Converter {
    private static Logger log = Logger.getLogger(FileLengthConverter.class);

    //not implemented
    public Object getAsObject(FacesContext arg0, UIComponent arg1, String arg2) {
        log.error("getAsObject not implemented");
        return null;
    }

    public String getAsString(FacesContext arg0, UIComponent arg1, Object param) {
        Long lVal = paramToLong(param);
        if (lVal == null) {
            return null;
        }
        return JobHelper.getFormattedSize(lVal);
    }
    
    private Long paramToLong(Object param) {
        if (param == null) {
            return null;
        }
        if (param instanceof Long) {
            return (Long) param;
        }
        try {
            return Long.parseLong(param.toString());
        }
        catch (NumberFormatException e) {
            
        }
        return null;
    }

}
