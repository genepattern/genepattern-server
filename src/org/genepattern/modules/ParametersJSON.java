/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2012) by the
 Broad Institute. All rights are reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. The Broad Institute cannot be responsible for its
 use, misuse, or functionality.
*/

package org.genepattern.modules;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;
import org.apache.log4j.Logger;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.util.GPConstants;
import org.genepattern.server.job.input.ParamListHelper;
import org.genepattern.server.job.input.NumValues;
import org.genepattern.server.job.input.choice.ChoiceInfo;
import org.genepattern.server.job.input.choice.ChoiceInfoHelper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.io.File;

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
    public static final String PREFIX = "prefix";
    public static final String VALUE = "value";
    public static final String FLAG = "flag";



    public ParametersJSON(JSONObject object) {
        try {
            this.put(NAME, object.get(NAME));
            this.put(DESCRIPTION, object.get(DESCRIPTION));
            this.put(TYPE, object.get(TYPE));
            this.put(FILEFORMAT, object.get(FILEFORMAT));
            this.put(DEFAULT_VALUE, object.get(DEFAULT_VALUE));
            this.put(OPTIONAL, object.get(OPTIONAL));
            this.put(PREFIX, object.get(PREFIX));
            this.put(VALUE, object.get(VALUE));
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


    public ParametersJSON(ParameterInfo pInfo)
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

            Set keys = pAttrs.keySet();
            Iterator<String> kIter = keys.iterator();
            while(kIter.hasNext())
            {
                String keyName = kIter.next();
                if(!this.has(keyName))
                {
                    this.put(keyName, pAttrs.get(keyName));
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
    
    public void initChoice(final ParameterInfo pInfo) {
        try {
            final ChoiceInfo choiceInfo = ChoiceInfoHelper.initChoiceInfo(pInfo);
            if (choiceInfo != null) {
                JSONArray choice = ChoiceInfoHelper.initChoiceJson(choiceInfo);
                this.put("choice", choice);
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

