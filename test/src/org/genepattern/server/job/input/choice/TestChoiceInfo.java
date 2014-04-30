package org.genepattern.server.job.input.choice;

import java.util.HashMap;

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
    public static final String dropDownDir="ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.dir/";
    
    /**
     * For junit testing, initialize a ParameterInfo which has a choiceDir.
     * @param choiceDir
     * @return
     */
    public static final ParameterInfo initFtpParam(final String choiceDir) {
        final String name="input.file";
        final String value="";
        final String description="A file drop-down";
    
        final ParameterInfo pinfo=new ParameterInfo(name, value, description);
        pinfo.setAttributes(new HashMap<String,String>());
        pinfo.getAttributes().put("MODE", "IN");
        pinfo.getAttributes().put("TYPE", "FILE");
        pinfo.getAttributes().put("choiceDir", choiceDir);
        pinfo.getAttributes().put("default_value", "");
        pinfo.getAttributes().put("fileFormat", "txt");
        pinfo.getAttributes().put("flag", "");
        pinfo.getAttributes().put("optional", "");
        pinfo.getAttributes().put("prefix", "");
        pinfo.getAttributes().put("prefix_when_specified", "");
        pinfo.getAttributes().put("type", "java.io.File");
    
        return pinfo;
    }
    
    @Test
    public void testFtpPassiveMode_default() {
        final ParameterInfo pinfo=initFtpParam(dropDownDir);
        Assert.assertEquals("by default, passiveMode is true", true, 
                ChoiceInfo.getFtpPassiveMode(pinfo));
    }
    
    @Test
    public void testFtpPassiveMode_declareTrue() {
        final ParameterInfo pinfo=initFtpParam(dropDownDir);
        pinfo.getAttributes().put(ChoiceInfo.PROP_CHOICE_DIR_FTP_PASSIVE_MODE, "true");
        Assert.assertEquals("passiveMode=true", true, 
                ChoiceInfo.getFtpPassiveMode(pinfo));
    }

    @Test
    public void testFtpPassiveMode_declareFalse() {
        final ParameterInfo pinfo=initFtpParam(dropDownDir);
        pinfo.getAttributes().put(ChoiceInfo.PROP_CHOICE_DIR_FTP_PASSIVE_MODE, "false");
        Assert.assertEquals("passiveMode=false", false, 
                ChoiceInfo.getFtpPassiveMode(pinfo));
    }

    @Test
    public void testFtpPassiveMode_caseInsensitive() {
        final ParameterInfo pinfo=initFtpParam(dropDownDir);
        pinfo.getAttributes().put(ChoiceInfo.PROP_CHOICE_DIR_FTP_PASSIVE_MODE, "FALSE");
        Assert.assertEquals("passiveMode=false", false, 
                ChoiceInfo.getFtpPassiveMode(pinfo));
    }

}
