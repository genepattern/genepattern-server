package org.genepattern.server.webapp;

import java.util.ArrayList;
import java.util.List;

import org.genepattern.webservice.ParameterInfo;

public class ParameterInfoWrapper {
    private ParameterInfo parameterInfo = null;
    
    public ParameterInfoWrapper(ParameterInfo pi) {
        parameterInfo = pi;
    }
    
    public String getName() {
        return parameterInfo.getName();
    }
    
    public String getDisplayName() {
        return parameterInfo._getDisplayName();
    }
    
    public String getDescription() {
        return parameterInfo._getDisplayDescription();
    }
    
    public static List<ParameterInfoWrapper> wrapList(List<ParameterInfo> list) {
        List<ParameterInfoWrapper> returnList = new ArrayList<ParameterInfoWrapper>();
        for (ParameterInfo pi : list) {
            returnList.add(new ParameterInfoWrapper(pi));
        }
        return returnList;
    }
}
