/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.choice;


import org.genepattern.junitutil.ParameterInfoUtil;
import org.genepattern.webservice.ParameterInfo;
import org.junit.Assert;
import org.junit.Test;

/**
 * junit tests for the ChoiceInfo class.
 * @author pcarr
 *
 */    
@SuppressWarnings("unchecked")
public class TestChoiceInfo {
    public static final String pname="input.file";
    public static final String dropDownDir="ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.dir/";
    
    @Test
    public void testFtpPassiveMode_default() {
        final ParameterInfo pinfo=ParameterInfoUtil.initFileDropdownParam(pname, dropDownDir);
        Assert.assertEquals("by default, passiveMode is true", true, 
                ChoiceInfo.getFtpPassiveMode(pinfo));
    }
    
    @Test
    public void testFtpPassiveMode_declareTrue() {
        final ParameterInfo pinfo=ParameterInfoUtil.initFileDropdownParam(pname, dropDownDir);
        pinfo.getAttributes().put(ChoiceInfo.PROP_CHOICE_DIR_FTP_PASSIVE_MODE, "true");
        Assert.assertEquals("passiveMode=true", true, 
                ChoiceInfo.getFtpPassiveMode(pinfo));
    }

    @Test
    public void testFtpPassiveMode_declareFalse() {
        final ParameterInfo pinfo=ParameterInfoUtil.initFileDropdownParam(pname, dropDownDir);
        pinfo.getAttributes().put(ChoiceInfo.PROP_CHOICE_DIR_FTP_PASSIVE_MODE, "false");
        Assert.assertEquals("passiveMode=false", false, 
                ChoiceInfo.getFtpPassiveMode(pinfo));
    }

    @Test
    public void testFtpPassiveMode_caseInsensitive() {
        final ParameterInfo pinfo=ParameterInfoUtil.initFileDropdownParam(pname, dropDownDir);
        pinfo.getAttributes().put(ChoiceInfo.PROP_CHOICE_DIR_FTP_PASSIVE_MODE, "FALSE");
        Assert.assertEquals("passiveMode=false", false, 
                ChoiceInfo.getFtpPassiveMode(pinfo));
    }

}
