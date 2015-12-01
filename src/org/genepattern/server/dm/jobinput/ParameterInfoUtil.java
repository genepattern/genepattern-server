/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.dm.jobinput;

import java.util.HashMap;

import org.genepattern.server.job.input.NumValues;
import org.genepattern.server.job.input.ParamListHelper.ListMode;
import org.genepattern.webservice.ParameterInfo;

/**
 * Utility class for working with ParameterInfo instances from junit tests.
 * @author pcarr
 *
 */
public class ParameterInfoUtil {

    /*
     * Example from 3.6.1 generated manifest file
    p1_MODE=
    p1_TYPE=TEXT
    p1_default_value=
    p1_description=No default value, no empty value in drop-down
    p1_fileFormat=
    p1_flag=--arg\=
    p1_name=arg
    p1_optional=
    p1_prefix=--arg\=
    p1_prefix_when_specified=--arg\=
    p1_type=java.lang.String
    p1_value=A;B;C;D
     */
    public static final ParameterInfo initTextParam(final String name, final String value, final String description, final boolean optional) {
        return ParameterInfoUtil.initTextParam(name, value, description, optional, "");
    }

    @SuppressWarnings("unchecked")
    public static final ParameterInfo initTextParam(final String name, final String value, final String description, final boolean optional, final String defaultValue) {
        ParameterInfo pinfo=new ParameterInfo(name, value, description);
        pinfo.setAttributes(new HashMap<String,String>());
        pinfo.getAttributes().put("MODE", "");
        pinfo.getAttributes().put("TYPE", "TEXT");
        pinfo.getAttributes().put("default_value", defaultValue);
        pinfo.getAttributes().put("fileFormat", "");
        pinfo.getAttributes().put("flag", "--"+name+"=");
        if (optional) {
            pinfo.getAttributes().put("optional", "on");
        }
        else {
            pinfo.getAttributes().put("optional", "");
        }
        pinfo.getAttributes().put("prefix", "--"+name+"=");
        pinfo.getAttributes().put("prefix_when_specified", "--"+name+"=");
        pinfo.getAttributes().put("type", "java.lang.String");
        return pinfo;
    }
    
    /**
     * Example file input parameter
     * <pre>
p1_MODE=IN
p1_TYPE=FILE
p1_default_value=
p1_description=
p1_fileFormat=
p1_name=input
p1_optional=
p1_prefix_when_specified=
p1_type=java.io.File
p1_value=

     * </pre>
     * @param name
     * @param description
     * @param optional
     * @return
     */
    @SuppressWarnings("unchecked")
    public static final ParameterInfo initFileParam(final String name, final String description, final boolean optional) {
        final String value="";
        ParameterInfo pinfo=new ParameterInfo(name, value, description);
        pinfo.setAttributes(new HashMap<String,String>());
        pinfo.getAttributes().put("MODE", "IN");
        pinfo.getAttributes().put("TYPE", "FILE");
        pinfo.getAttributes().put("default_value", "");
        pinfo.getAttributes().put("fileFormat", "");
        pinfo.getAttributes().put("flag", "--"+name+"=");
        setOptional(pinfo, optional);
        pinfo.getAttributes().put("prefix", "--"+name+"=");
        pinfo.getAttributes().put("prefix_when_specified", "--"+name+"=");
        pinfo.getAttributes().put("type", "java.io.File");
        return pinfo;
    }

    public static final ParameterInfo initFilelistParam(final String name) {
        final String numValues="1+";
        return initFilelistParam(name, "", numValues, ListMode.LIST);
    }

    @SuppressWarnings("unchecked")
    public static final ParameterInfo initFilelistParam(final String name, final String description, final String numValues, final ListMode listMode) {
        final boolean optional=false;
        final ParameterInfo pinfo=initFileParam(name, description, optional);
        if (numValues != null) {
            pinfo.getAttributes().put(NumValues.PROP_NUM_VALUES, numValues);
        }
        if (listMode != null) {
            pinfo.getAttributes().put(NumValues.PROP_LIST_MODE, listMode.name());
        }
        return pinfo;
    }
    
    @SuppressWarnings("unchecked")
    public static void setOptional(ParameterInfo pinfo, final boolean optional) {
        if (optional) {
            pinfo.getAttributes().put("optional", "on");
        }
        else {
            pinfo.getAttributes().put("optional", "");
        }
    }

}
