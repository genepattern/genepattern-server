package org.genepattern.server.genepattern;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamId;
import org.genepattern.server.job.input.ParamValue;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.webservice.ParameterInfo;

import java.io.File;
import java.util.*;

/**
 * Created by nazaire on 7/17/15.
 */
public class ValueResolver {
    static List<String> resolveValue(final GpConfig gpConfig, final GpContext gpContext, final String value, final Map<String, String> props, final Map<String, ParameterInfo> parameterInfoMap, final int depth) {
        if (value == null) {
            //TODO: decide to throw exception or return null or return list containing one null item
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

    private static List<String> substituteValue(final GpConfig gpConfig, final GpContext gpContext, final String arg, final Map<String,String> dict, final Map<String,ParameterInfo> parameterInfoMap) {
        List<String> rval = new ArrayList<String>();
        List<String> subs = CommandLineParser.getSubstitutionParameters(arg);
        if (subs == null || subs.size() == 0) {
            rval.add(arg);
            return rval;
        }
        String substitutedValue = arg;
        boolean isOptional = true;
        for(String sub : subs) {
            String paramName = sub.substring(1, sub.length()-1);
            String value = null;
            if (dict.containsKey(paramName)) {
                value = dict.get(paramName);
            }
            else if (gpConfig != null) {
                value = gpConfig.getGPProperty(gpContext, paramName);
            }

            //default to empty string, to handle optional parameters which have not been set
            //String replacement = props.getProperty(varName, "");
            if (paramName.equals("resources") && value != null) {
                //TODO: this should really be in the setupProps
                //special-case for <resources>,
                // make this an absolute path so that pipeline jobs running in their own directories see the right path
                value = new File(value).getAbsolutePath();
            }

            ParameterInfo paramInfo = parameterInfoMap.get(paramName);
            if (paramInfo != null) {
                isOptional = paramInfo.isOptional();
                String optionalPrefix = paramInfo._getOptionalPrefix();
                if (value != null && value.length() > 0 && optionalPrefix != null && optionalPrefix.length() > 0) {
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

    public static List<String> resolveValue(GpConfig gpConfig, GpContext gpContext, String cmdLine, Properties props, ParameterInfo[] formalParameters) {
        Map<String,String> env = new HashMap<String,String>();
        for(Object keyObj : props.keySet()) {
            String key = keyObj.toString();
            env.put( key.toString(), props.getProperty(key));
        }
        Map<String,ParameterInfo> parameterInfoMap = ValueResolver.createParameterInfoMap(formalParameters);
        return resolveValue(gpConfig, gpContext, cmdLine, env, parameterInfoMap, 0);
    }

    static Map<String,ParameterInfo> createParameterInfoMap(ParameterInfo[] params) {
        Map<String,ParameterInfo> map = new HashMap<String,ParameterInfo>();
        if (params != null) {
            for(ParameterInfo param : params) {
                map.put(param.getName(), param);
            }
        }
        return map;
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


    public static HashMap<String,List<String>> getParamValues(final GpConfig gpConfig, final GpContext jobContext, Properties props, ParameterInfo[] formalParameters)throws Exception
    {
        HashMap<String, List<String>> paramValueMap = new HashMap<String, List<String>>();

        JobInput jobInput = jobContext.getJobInput();

        final Map<String,ParameterInfoRecord> paramInfoMap=ParameterInfoRecord.initParamInfoMap(jobContext.getTaskInfo());

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

                    List<String> resolvedValues = ValueResolver.resolveValue(gpConfig, jobContext, paramValue.getValue(), props, formalParameters);
                    for(String substitutedValue: resolvedValues)
                    {
                        paramValueList.add(substitutedValue);
                    }
                }
            }
        }
        return paramValueMap;
    }
}
