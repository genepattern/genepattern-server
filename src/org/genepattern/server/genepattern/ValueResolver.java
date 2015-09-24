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
import org.genepattern.webservice.ParameterInfo;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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

    //TODO: MAX_DEPTH check
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

    /**
     * Input arg is either a literal or a substitution.
     * 
     * @param gpConfig
     * @param gpContext
     * @param subToken
     * @param dict
     * @param parameterInfoMap
     * @return
     */
    protected static List<String> substituteSubToken(final GpConfig gpConfig, final GpContext gpContext, final CmdLineSubToken subToken, final Map<String,String> dict, final Map<String,ParameterInfoRecord> parameterInfoMap) {
        if (subToken.isLiteral) {
            return Arrays.asList(subToken.value);
        }
        // else assume it's a substitution
        String value="";
        if (dict.containsKey(subToken.pname)) {
            value = dict.get(subToken.pname);
        }
        else if (gpConfig != null) {
            value = gpConfig.getGPProperty(gpContext, subToken.pname);
        }

        // special-cases for parameter substitutions ...
        // a) handle prefix_when_specified
        // b) handle listMode=CMD or CMD_OPT
        final ParameterInfoRecord record = parameterInfoMap.get(subToken.pname);
        final ParameterInfo pInfo = record == null ? null : record.getFormal();
        if (pInfo != null) {
            final ListMode listMode = ParamListHelper.initListMode(pInfo);
            if (ParamListHelper.isCmdLineList(pInfo, listMode)) {
                final JobInput jobInput = gpContext.getJobInput();
                if (jobInput == null) {
                    log.error("jobInput not set for param="+subToken.pname);
                }
                else {
                    // ... 
                    final Param param = jobInput.getParam(subToken.pname);
                    if (param != null) {
                        final List<String> results = ValueResolver.getCmdListValues(param, pInfo, listMode);
                        return results;
                    }
                    else {
                        log.error("jobInput.param not set for param="+subToken.pname);
                        // handle same as non parameter substitution
                    }
                }
            }
            else {
                if (value == null && !pInfo.isOptional()) {
                    //TODO: throw exception
                    log.error("missing substitution value for '"+pInfo.getName()+"'");
                    value = subToken.pname;
                }
                else if (value == null &&  pInfo.isOptional()) {
                    value = "";
                }
                return handlePrefix(pInfo, value);
            }
        }
        
        //special-case for <resources> 
        if ("resources".equals(subToken.pname) && value != null) {
            // make this an absolute path so that pipeline jobs running in their own directories see the right path
            value = new File(value).getAbsolutePath();
        }
        return Arrays.asList(value);
    }
    
    protected static List<String> substituteValue(final GpConfig gpConfig, final GpContext gpContext, final String arg, final Map<String,String> dict, final Map<String,ParameterInfoRecord> parameterInfoMap) {
        final List<String> rval = new ArrayList<String>();
        final List<CmdLineSubToken> subs = CmdLineSubToken.splitIntoSubTokens(arg);
        if (subs == null || subs.size() == 0) {
            rval.add(arg);
            return rval;
        }
        final StringBuilder sb=new StringBuilder();
        for(final CmdLineSubToken sub : subs) {
            if (sub.isLiteral) {
                sb.append(sub.value);
            }
            else {
                final List<String> values=substituteSubToken(gpConfig, gpContext, sub, dict, parameterInfoMap);
                if (values!=null && values.size()==1) {
                    sb.append(values.get(0));
                }
                else if (values!=null && values.size()>1) {
                    if (sb.length()>0) {
                        rval.add(sb.toString());
                        sb.setLength(0);
                    }
                    for(final String val : values) {
                        rval.add(val);
                    }
                }
            }
        }
        if (sb.length()>0) {
            rval.add(sb.toString());
            sb.setLength(0);
        }
        return rval;
    }

    /**
     * For a given parameter, optionally split into multiple args depending on the prefix_when_specified flag.
     */
    protected static List<String> handlePrefix(final ParameterInfo pInfo, final String value) {
        final boolean treatEmptyStringAsNull=true;
        return handlePrefix(new ArrayList<String>(), pInfo._getOptionalPrefix(), value, treatEmptyStringAsNull);
    }

    protected static List<String> handlePrefix(final ParameterInfo pInfo, final String value, final boolean treatEmptyStringAsNull) {
        return handlePrefix(new ArrayList<String>(), pInfo._getOptionalPrefix(), value, treatEmptyStringAsNull);
    }

    protected static List<String> handlePrefix(final List<String> appendTo, final String optionalPrefix, final String value) {
        final boolean treatEmptyStringAsNull=true;
        return handlePrefix(appendTo, optionalPrefix, value, treatEmptyStringAsNull);
    }

    /**
     * For a given parameter, optionally split into multiple args depending on the prefix_when_specified flag
     * and the runtime value.
     * 
     * @param appendTo, the list of command line args to append to
     * @param optionalPrefix, the prefix_when_specified flag as set in the manifest for the module
     * @param value, the runtime value for the parameter
     * @param treatEmptyStringAsNull, when true (by default), ignore empty String value
     *     don't add prefix for an empty String
     *     don't add arg for an empty String
     * 
     * @return the updated appendTo list
     */
    protected static List<String> handlePrefix(final List<String> appendTo, final String optionalPrefix, final String value, final boolean treatEmptyStringAsNull) {
        if (value==null || (treatEmptyStringAsNull && value.length()==0)) {
            //special-case: if there is no value, don't append anything to the list
            return appendTo;
        }
        if (optionalPrefix==null) {
            //special-case: if there is no prefix, append the original value to the list
            appendTo.add(value);
            return appendTo;
        }
        if (optionalPrefix.endsWith("\\ ")) {
            //special-case: if optionalPrefix ends with an escaped space, don't split into two args
            appendTo.add( optionalPrefix.substring(0, optionalPrefix.length()-3) + value );
        }
        else if (optionalPrefix.endsWith(" ")) {
            //special-case: GP-2866, if optionalPrefix ends with a space, split into two args
            appendTo.add(optionalPrefix.substring(0, optionalPrefix.length()-1));
            appendTo.add(value);
        }
        else {
            //otherwise, append the prefix to the value
            appendTo.add(optionalPrefix + value);
        }
        return appendTo;
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

    /**
     * Calls getCmdListValues on record.formalParam
     */
    public static List<String> getCmdListValues(final Param param, final ParameterInfoRecord record, final ListMode listMode) {
        if (param == null) {
            throw new IllegalArgumentException("param==null");
        }
        
        if (record == null) {
            throw new IllegalArgumentException("pRecord==null");
        }
        return getCmdListValues(param, record.getFormal(), listMode);
    }

    /**
     * Calls getCmdListValues with listMode from the formalParam
     */
    public static List<String> getCmdListValues(final Param param, final ParameterInfo formalParam) {
        final ListMode listMode = ParamListHelper.initListMode(formalParam);
        return getCmdListValues(param, formalParam, listMode);
    }

    /**
     * Get the list of command line args (including optional prefix flags) for the given parameter
     * when listMode=CMD or CMD_OPT. Should only be called for multi-valued text parameters.
     * 
     * @see ListMode for more documentation
     * 
     * @param param, the runtime values
     * @param formalParam, parameter flags from the manifest
     * @param listMode, from the listMode in the manifest 
     * @return
     */
    public static List<String> getCmdListValues(final Param param, final ParameterInfo formalParam, final ListMode listMode) {
        if (param == null) {
            throw new IllegalArgumentException("param==null");
        }
        if (formalParam == null) {
            throw new IllegalArgumentException("formalParam==null");
        }
        
        // option 1, join values then append optional prefix
        if (listMode==ListMode.CMD) {
            String separator = (String) formalParam.getAttributes().get(NumValues.PROP_LIST_MODE_SEP);
            if (Strings.isNullOrEmpty(separator)) {
                separator=",";
            }
            // join
            String val=Joiner.on(separator).join(param.getValues());
            return handlePrefix(formalParam, val);
        }
        // option 2, append prefix for each arg
        else if (listMode==ListMode.CMD_OPT) {
            final String optionalPrefix = formalParam._getOptionalPrefix();
            final List<String> rval=new ArrayList<String>();
            for(final ParamValue paramValue : param.getValues()) {
                handlePrefix(rval, optionalPrefix, paramValue.getValue());
            }
            return rval;
        }
        log.error("unexpected listMode="+listMode+" for param="+param.getParamId());
        return Collections.emptyList();
    }

}
