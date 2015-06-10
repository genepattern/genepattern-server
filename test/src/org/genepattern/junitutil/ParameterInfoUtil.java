package org.genepattern.junitutil;

import java.util.HashMap;

import org.genepattern.webservice.ParameterInfo;

/**
 * Helper class for initializing ParameterInfo instances for junit tests.
 * @author pcarr
 *
 */
public class ParameterInfoUtil {

    /**
     * For junit testing, initialize a ParameterInfo which has a choiceDir.
     * @param pname, the name of the parameter
     * @param choiceDir, the remote FTP directory
     * @return
     */
    @SuppressWarnings("unchecked")
    public static final ParameterInfo initFileDropdownParam(final String pname, final String choiceDir) {
        final String value="";
        final String description="A file drop-down";
    
        final ParameterInfo pinfo=new ParameterInfo(pname, value, description);
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

    @SuppressWarnings("unchecked")
    public static final ParameterInfo initFileParam(String name, String value, String description) {
        final ParameterInfo pinfo=new ParameterInfo(name, value, description);
        pinfo.setAttributes(new HashMap<String,String>());        
        pinfo.getAttributes().put("MODE", "IN");
        pinfo.getAttributes().put("TYPE", "FILE");
        pinfo.getAttributes().put("default_value", "");
        pinfo.getAttributes().put("optional", "");
        pinfo.getAttributes().put("prefix_when_specified", "");
        pinfo.getAttributes().put("type", "java.io.File");
    
        return pinfo;
    }

}
