package org.genepattern.server.genepattern;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.NumValues;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamId;
import org.genepattern.server.job.input.ParamListHelper;
import org.genepattern.server.job.input.ParamListHelper.ListMode;
import org.genepattern.server.job.input.ParamValue;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.ParameterInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by nazaire on 7/17/15.
 */
public class ValueResolver {
    private static final Logger log = Logger.getLogger(ValueResolver.class);
 
    /**
     * use this for junit tests, where we don't need a complete propsMap or paramInfoMap.
     * @param gpConfig
     * @param gpContext
     * @param value
     * @return
     */
    public static List<String> resolveValue(final GpConfig gpConfig, final GpContext gpContext, final String value) {
        final Map<String,String> propsMap=Collections.emptyMap();
        final Map<String,ParameterInfoRecord> paramInfoMap=Collections.emptyMap();
        return resolveValue(gpConfig, gpContext, value, propsMap, paramInfoMap);
    }
    
    public static List<String> resolveValue(final GpConfig gpConfig, final GpContext gpContext, final String value, final Map<String, String> propsMap, final Map<String, ParameterInfoRecord> parameterInfoMap) {
        return resolveValue(gpConfig, gpContext, value, propsMap, parameterInfoMap, 0);
    }

    private static List<String> resolveValue(final GpConfig gpConfig, final GpContext gpContext, final String value, final Map<String, String> props, final Map<String, ParameterInfoRecord> parameterInfoMap, final int depth) {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        List<String> rval = new ArrayList<String>();

        List<String> variables = CommandLineParser.getSubstitutionParameters(value);
        //case 1: if value contains no substitutions, return a one-item list, containing the value
        if (variables == null || variables.size() == 0) {
            rval.add( value );
            return rval;
        }

        //otherwise, tokenize and return
        List<String> tokens = ValueResolver.getTokens(value);
        for(String token : tokens) {
            List<String> substitution = ValueResolver.substituteValue(gpConfig, gpContext, token, props, parameterInfoMap);

            if (substitution == null || substitution.size() == 0) {
                //remove empty substitutions
            }
            else if (substitution.size() == 1) {
                String singleValue = substitution.get(0);
                if (singleValue == null) {
                    //ignore
                }
                else if (token.equals(singleValue)) {
                    rval.add( token );
                }
                else {
                    List<String> resolvedList = resolveValue(gpConfig, gpContext, singleValue, props, parameterInfoMap, 1+depth );
                    rval.addAll( resolvedList );
                }
            }
            else {
                for(String sub : substitution) {
                    List<String> resolvedSub = resolveValue(gpConfig, gpContext, sub, props, parameterInfoMap, 1+depth);
                    rval.addAll( resolvedSub );
                }
            }
        }
        return rval;
    }

    protected static List<String> substituteValue(final GpConfig gpConfig, final GpContext gpContext, final String arg, final Map<String,String> dict, final Map<String,ParameterInfoRecord> parameterInfoMap) {
        final List<String> rval = new ArrayList<String>();
        final List<String> subs = CommandLineParser.getSubstitutionParameters(arg);
        if (subs == null || subs.size() == 0) {
            rval.add(arg);
            return rval;
        }
        String substitutedValue = arg;
        final List<String> valueList = new ArrayList<String>();

        boolean isOptional = true;
        for(final String sub : subs) {
            boolean cmdOptListMode = false;
            final String paramName = sub.substring(1, sub.length()-1);
            final ParameterInfoRecord pInfoRecord = parameterInfoMap.get(paramName);
            final ParameterInfo pInfo;
            if (pInfoRecord == null) {
                pInfo = null;
            }
            else {
                pInfo = pInfoRecord.getFormal();
            }

            String value=null;
            if (dict.containsKey(paramName)) {
                value = dict.get(paramName);
            }
            else if (gpConfig != null) {
                value = gpConfig.getGPProperty(gpContext, paramName);
            }

            if (pInfo != null) {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("resolving "+sub+"; the substitution matches an input parameter");
                    }

                    final NumValues numValues=ParamListHelper.initNumValues(pInfo);
                    if (numValues.acceptsList()) {
                        if (log.isDebugEnabled()) {
                            log.debug("for "+sub+"; acceptsList is true");
                        }
                        final ParamListHelper.ListMode listMode = ParamListHelper.initListMode(pInfoRecord);
                        if (log.isDebugEnabled()) {
                            log.debug("for "+sub+"; listMode="+listMode);
                        }
                        cmdOptListMode = ListMode.CMD_OPT == listMode;
                        if (ListMode.CMD.equals(listMode) || ListMode.CMD_OPT.equals(listMode)) {
                            final JobInput jobInput = gpContext.getJobInput();
                            if (jobInput == null) {
                                log.error("jobInput not set for param="+paramName);
                            }
                            else {
                                // ... 
                                final Param param = jobInput.getParam(paramName);
                                if (param == null) {
                                    log.error("jobInput.param not set for param="+paramName);
                                }
                                else {
                                    final List<String> results = ValueResolver.getSubstitutedValues(param, pInfoRecord) ;
                                    if(results != null && results.size() > 0) {
                                        if (log.isDebugEnabled()) {
                                            log.debug("for "+sub+"; substitutedValues="+results);
                                        }
                                        value = results.remove(0);
                                        valueList.addAll(results);
                                    }
                                }
                            }
                        }
                    }
                }
                catch (Throwable t) {
                    log.error("Unexpected exception resolving "+sub+" for paramName="+paramName, t);
                }
            }

            //special-case for <resources> 
            if (paramName.equals("resources") && value != null) {
                // make this an absolute path so that pipeline jobs running in their own directories see the right path
                value = new File(value).getAbsolutePath();
            }

            //default to empty string, to handle optional parameters which have not been set
            if (pInfo != null) {
                isOptional = pInfo.isOptional();
                String optionalPrefix = pInfo._getOptionalPrefix();
                if(!cmdOptListMode && value != null && value.length() > 0 && optionalPrefix != null && optionalPrefix.length() > 0) {
                    if (optionalPrefix.endsWith("\\ ")) {
                        //special-case: if optionalPrefix ends with an escaped space, don't split into two args
                        value = optionalPrefix.substring(0, optionalPrefix.length()-3) + value;
                    }
                    else if (optionalPrefix.endsWith(" ")) {
                        //special-case: GP-2866, if optionalPrefix ends with a space, split into two args
                        rval.add(optionalPrefix.substring(0, optionalPrefix.length()-1));
                    }
                    else {
                        //otherwise, append the prefix to the value
                        value = optionalPrefix + value;
                    }
                }
            }

            if (value == null && isOptional == false) {
                //TODO: throw exception
                CommandLineParser.log.error("missing substitution value for '"+sub+"' in expression: "+arg);
                value = sub;
            }
            else if (value == null &&  isOptional == true) {
                value = "";
            }
            substitutedValue = substitutedValue.replace(sub, value);
        }
        if (substitutedValue.length() == 0 && isOptional) {
            //return an empty list
        }
        else {
            rval.add(substitutedValue);
        }

        //HACK: if there are multiple values for this parameter
        //add the remaining values
        for(final String val : valueList) {
            rval.add(val);
        }

        return rval;
    }

    static List<String> getTokens(String arg) {
        //TODO: handle escaped quotes and spaces
        ValueResolver.MyStringTokenizer st = new ValueResolver.MyStringTokenizer();
        for(int i=0; i<arg.length(); ++i) {
            st.readNextChar(arg.charAt(i));
        }
        st.readNextChar(' ');

        return st.getTokens();
    }

    private static class MyStringTokenizer {
        private enum ST { in_ws, in_quote, in_word, end }

        private ST status = ST.in_ws;

        private List<String> tokens = new ArrayList<String>();
        private String cur = "";

        public List<String> getTokens() {
            return tokens;
        }

        public void readNextChar(char c) {
            if (Character.isWhitespace(c)) {
                switch (status) {
                case in_ws:
                    break;
                case in_quote:
                    cur += c;
                    break;
                case in_word:
                    tokens.add(cur);
                    cur = "";
                    status = ST.in_ws;
                    break;
                }
            }
            else if (c == '\"') {
                switch (status) {
                case in_ws:
                    status = ST.in_quote;
                    cur = "";
                    break;
                case in_quote:
                    status = ST.in_ws;
                    tokens.add(cur);
                    cur = "";
                    break;
                case in_word:
                    //with a '"' in a word, include in the word, scan until the next WS char
                    cur += c;
                    break;
                }
            }
            else {
                switch (status) {
                case in_ws:
                    status = ST.in_word;
                case in_quote:
                case in_word:
                    cur += c;
                    break;
                }
            }
        }
    }

    public static HashMap<String,List<String>> getParamValues(final GpConfig gpConfig, final GpContext jobContext, Properties props, Map<String,ParameterInfoRecord> paramInfoMap)throws Exception
    {
        HashMap<String, List<String>> paramValueMap = new HashMap<String, List<String>>();

        JobInput jobInput = jobContext.getJobInput();

        final Map<String,String> propsMap = new HashMap<String,String>();
        for(Object keyObj : props.keySet()) {
            String key = keyObj.toString();
            propsMap.put( key.toString(), props.getProperty(key));
        }

        final Map<ParamId, Param> jobParamMap = jobInput.getParams();
        for(ParamId paramId: jobParamMap.keySet())
        {
            String paramName = paramId.getFqName();
            if(paramInfoMap.containsKey(paramName))
            {
                Param param = jobParamMap.get(paramId);
                for(ParamValue paramValue: param.getValues())
                {
                    List<String> paramValueList = paramValueMap.get(paramName);
                    if(paramValueList == null)
                    {
                        paramValueList = new ArrayList<String>();
                        paramValueMap.put(paramName , paramValueList);
                    }

                    List<String> resolvedValues = ValueResolver.resolveValue(gpConfig, jobContext, paramValue.getValue(), propsMap, paramInfoMap);
                    for(String substitutedValue: resolvedValues)
                    {
                        paramValueList.add(substitutedValue);
                    }
                }
            }
        }
        return paramValueMap;
    }

    /*
     * Constructs the cmd line string for this parameter
     */
    public static List<String> getSubstitutedValues(final Param param, final ParameterInfoRecord pRecord)
    {
        if (param == null) {
            throw new IllegalArgumentException("param==null");
        }
        
        if(pRecord == null) {
            throw new IllegalArgumentException("pRecord==null");
        }

        List<String> substitutedValues = new ArrayList<String>();

        String separator = "";
        ParamListHelper.ListMode listMode = ParamListHelper.initListMode(pRecord);
        if(listMode.equals(ParamListHelper.ListMode.CMD))
        {
            separator = (String) pRecord.getFormal().getAttributes().get(NumValues.PROP_LIST_MODE_SEP);
            if(separator == null)
            {
                separator = ",";
            }
        }

        String prefix= "";
        if(listMode.equals(ParamListHelper.ListMode.CMD_OPT))
        {
            prefix = (String) pRecord.getFormal().getAttributes().get(GPConstants.PARAM_INFO_PREFIX[GPConstants.PARAM_INFO_NAME_OFFSET]);
        }

        List<ParamValue> values = param.getValues();
        String substitutedValue = "";
        for(int i=0;i<values.size();i++)
        {
            final ParamValue value = values.get(i);

            if(i>0)
            {
                substitutedValue += separator;
            }

            substitutedValue += prefix + value.getValue();

            //if listMode=CMD_OPT then add each substituted value separately
            if(listMode.equals(ParamListHelper.ListMode.CMD_OPT))
            {
                substitutedValues.add(substitutedValue);
                substitutedValue = "";
            }
        }

        if(!listMode.equals(ParamListHelper.ListMode.CMD_OPT))
        {
            substitutedValues.add(substitutedValue);
        }

        return substitutedValues;
    }
}
