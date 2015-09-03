/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.modules;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.server.job.input.*;
import org.genepattern.server.job.input.choice.ChoiceInfo;
import org.genepattern.server.job.input.choice.ChoiceInfoHelper;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * User: nazaire
 * Date: Mar 15, 2012
 */
public class ParametersJSON extends JSONObject {
    public static Logger log = Logger.getLogger(ParametersJSON.class);

    public static final String KEY = "parameters";

    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String TYPE = "TYPE";
    //parameter "type" i.e. java.lang.String IS different from "TYPE" above
    public static final String type = "type";
    public static final String DEFAULT_VALUE = "default_value";
    public static final String OPTIONAL = "optional";
    public static final String FILEFORMAT = "fileFormat";
    public static final String MIN_NUM_VALUE = "minValue";
    public static final String MAX_NUM_VALUE = "maxValue";
    public static final String MIN_NUM_GROUPS = "minNumGroups";
    public static final String MAX_NUM_GROUPS = "maxNumGroups";
    public static final String MIN_RANGE = "minRange";
    public static final String MAX_RANGE = "maxRange";
    public static final String PREFIX = "prefix";
    public static final String VALUE = "value";
    public static final String CHOICES = "choices";
    public static final String FLAG = "flag";



    private ParametersJSON(JSONObject object) {
        try {
            this.put(NAME, object.get(NAME));
            this.put(DESCRIPTION, object.get(DESCRIPTION));
            this.put(TYPE, object.get(TYPE));
            this.put(FILEFORMAT, object.get(FILEFORMAT));
            this.put(DEFAULT_VALUE, object.get(DEFAULT_VALUE));
            this.put(OPTIONAL, object.get(OPTIONAL));
            this.put(PREFIX, object.get(PREFIX));
            this.put(FLAG, object.get(FLAG));

            String typeString = String.class.getName();
            if(TYPE.equals("FILE"))
            {
                typeString = File.class.getName();
            }

            this.put(type, typeString);
                        
            Iterator<String> kIter = object.keys();
            while(kIter.hasNext())
            {
                String keyName = kIter.next();
                if(!this.has(keyName))
                {
                    this.put(keyName, object.get(keyName));
                }
            }
        }
        catch (Exception e) {
            log.error("object: " + object);
            log.error("Error creating parameter from generic JSON object");
        }
    }


    public ParametersJSON(final ParameterInfo pInfo)
    {
        try {
            HashMap pAttrs = pInfo.getAttributes();
            this.put(NAME, pInfo.getName());
            this.put(DESCRIPTION, pInfo.getDescription());
            this.put(type, pAttrs.get(GPConstants.PARAM_INFO_TYPE[0]));
            this.put(TYPE, pAttrs.get(ParameterInfo.TYPE));
            this.put(FILEFORMAT, pAttrs.get(GPConstants.FILE_FORMAT));
            this.put(DEFAULT_VALUE, pInfo.getDefaultValue());
            this.put(PREFIX, pAttrs.get(GPConstants.PARAM_INFO_PREFIX[0]));
            this.put(OPTIONAL, pAttrs.get(GPConstants.PARAM_INFO_OPTIONAL[0]));
            this.put(FLAG, pAttrs.get(FLAG));
            this.put(VALUE, pInfo.getValue());

            final NumValues numValues = ParamListHelper.initNumValues(pInfo);
            final RangeValues rangeValues = ParamListHelper.initAllowedRanges(pInfo);

            if(numValues != null)
            {
                if (numValues.getMin() != null)
                {
                    this.put(MIN_NUM_VALUE, numValues.getMin());
                }
                if (numValues.getMax() != null)
                {
                    this.put(MAX_NUM_VALUE, numValues.getMax());
                }
            }

            if(rangeValues != null)
            {
                if (rangeValues.getMin() != null)
                {
                    this.put(MIN_RANGE, rangeValues.getMin());
                }
                if (rangeValues.getMax() != null)
                {
                    this.put(MAX_RANGE, rangeValues.getMax());
                }
            }

            Iterator<?> it = pAttrs.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry<?,?> entry = (Map.Entry<?,?>)it.next();
                final String keyName = (String) entry.getKey();
                if (!this.has(keyName) && !keyName.equals("numValues") && !keyName.equals("range")) {
                    this.put(keyName, (String) entry.getValue());
                }
            }
        }
        catch (Exception e) {
            log.error("Error creating parameter JSON from from ParameterInfo object");
        }
    }

    /**
     * Helper method to parse the option 'numValues' attribute for the given parameter.
     * Add min and max values attributes which specify the number of values that this
     * given parameter can accept.
     * 
     * When the number of values is unlimited, 'maxValue' will not be set.
     *  
     * @param pInfo
     */
    public void addNumValues(final ParameterInfo pInfo) { 
        if (pInfo == null) {
            throw new IllegalArgumentException("pInfo==null");
        }
        final NumValues numValues = ParamListHelper.initNumValues(pInfo);
        
        try {
            if (numValues.getMin() != null) {
                this.put("minValue", numValues.getMin());
            }
            if (numValues.getMax() != null) {
                this.put("maxValue", numValues.getMax());
            }
        }
        catch (JSONException e) {
            log.error("Error creating parameter JSON from ParameterInfo object");
        }
    }

    /**
     * Helper method to parse the optional group info attributes for the given parameter.
     * @param pinfo
     */
    public void addGroupInfo(final ParameterInfo pinfo) {
        if (pinfo==null) {
            throw new IllegalArgumentException("pinfo==null");
        }
        try {
            final GroupInfo groupInfo=new GroupInfo.Builder().fromParameterInfo(pinfo).build();
            if (groupInfo != null) {
                final JSONObject groupInfoJson=GroupInfo.toJson(groupInfo);
                this.put("groupInfo", groupInfoJson);
            }
        }
        catch (Throwable t) {
            log.error("Error initializing group info for param="+pinfo.getName(), t);
        }
    }

    /**
     * Helper method to parse the optional numRange attribute for the given parameter.
     * @param pInfo
     */
    public void addRangeInfo(final ParameterInfo pInfo) {
        if (pInfo == null ) {
            throw new IllegalArgumentException("pInfo==null");
        }

        HashMap<String, String> attributes = pInfo.getAttributes();
        if (attributes == null ) {
            throw new IllegalArgumentException("pInfo.getAttributes()==null");
        }

        String numRangeString = attributes.get("range");
        if(numRangeString != null)
        {
            try {
                final RangeValuesParser rvParser=new RangeValuesParser();
                final RangeValues<Double> numRange = rvParser.parseRange(numRangeString);

                if (numRange.getMin() != null) {
                    this.put("minRange", numRange.getMin());
                }
                if (numRange.getMax() != null) {
                    this.put("maxRange", numRange.getMax());
                }
            }
            catch (JSONException e) {
                log.error("Error creating parameter JSON from ParameterInfo object");
            }
            catch (Exception e) {
                log.error("Error getting valid ranges from ParameterInfo object");
            }
        }
    }

    public void initChoice(final HttpServletRequest request, final TaskInfo taskInfo, final ParameterInfo pInfo, final boolean initDropdown) {
        try {
            final ChoiceInfo choiceInfo = ChoiceInfoHelper.initChoiceInfo(pInfo, initDropdown);
            if (choiceInfo != null) {
                JSONObject choiceInfoJson=ChoiceInfoHelper.initChoiceInfoJson(request, taskInfo, choiceInfo);
                this.put("choiceInfo", choiceInfoJson);
            }
        }
        catch (JSONException e) {
            log.error("JSONException initializing choice for "+pInfo.getName(), e);
        }
        catch (Throwable t) {
            log.error("Unexpected error initializing choice for "+pInfo.getName(), t);
        }
    }

    public boolean isOptional() throws JSONException
    {
        if(this.getString(OPTIONAL).equalsIgnoreCase("on"))
        {
           return true;
        }

        return false;
    }

    public String getPrefix() throws JSONException {
          return this.getString(PREFIX);
    }

    public long getMinNumValue() throws JSONException
    {
        return this.getLong(MIN_NUM_VALUE);
    }

    public long getMaxNumValue() throws JSONException {
        return this.getLong(MAX_NUM_VALUE);
    }


    public Double getMinRange() throws JSONException
    {
        if(this.has(MIN_RANGE) && !this.isNull(MIN_RANGE) && this.getString(MIN_RANGE).length() > 0)
        {
            return this.getDouble(MIN_RANGE);
        }

        return null;
    }

    public Double getMaxRange() throws JSONException
    {
        if(this.has(MAX_RANGE) && !this.isNull(MAX_RANGE) && this.getString(MAX_RANGE).length() > 0)
        {
            return this.getDouble(MAX_RANGE);
        }

        return null;
    }

    public long getMinGroups() throws JSONException
    {
        if(this.has(MIN_NUM_GROUPS))
        {
            return this.getLong(MIN_NUM_GROUPS);
        }

        return 0;
    }

    public long getMaxGroups() throws JSONException {
        if(this.has(MAX_NUM_GROUPS))
        {
            return this.getLong(MAX_NUM_GROUPS);
        }

        return 0;
    }

    public String getValue() throws JSONException {
       return this.getString(VALUE);
    }

    public void setValue(String value) throws JSONException {
       this.put(VALUE, value);
    }

    public String getFileFormats() throws JSONException {
       return this.getString(FILEFORMAT);
    }

    public void setFileFormats(String fileformats) throws JSONException {
       this.put(FILEFORMAT, fileformats);
    }

    public String getType() throws JSONException {
       return this.getString(TYPE);
    }

    public void setType(String type) throws JSONException {
       this.put(TYPE, type);
    }

     public String getDefaultValue() throws JSONException {
       return this.getString(DEFAULT_VALUE);
    }

    public void setDefaultValue(String defaultValue) throws JSONException {
       this.put(DEFAULT_VALUE, defaultValue);
    }

    public String getDescription() throws JSONException {
       return this.getString(DESCRIPTION);
    }

    public void setDescription(String description) throws JSONException {
       this.put(DESCRIPTION, description);
    }

    public String getName() throws JSONException {
       return this.getString(NAME);
    }

    public void setName(String name) throws JSONException {
       this.put(NAME, name);
    }

    public static ParametersJSON[] extract(JSONObject json) {
        try {
            JSONArray object = (JSONArray) json.get(ParametersJSON.KEY);
            ParametersJSON[] parameters = new ParametersJSON[object.length()];

            for(int i =0;i< object.length(); i++)
            {
                parameters[i] = new ParametersJSON((JSONObject)object.get(i));
            }
            return parameters;
        }
        catch (Exception e) {
            log.error("Unable to extract ParametersJSON from saved bundle");
            return null;
        }
    }
}

