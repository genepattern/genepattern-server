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

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
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
    //parameter type i.e. java.lang.String - different from TYPE above
    public static final String type = "type";
    public static final String DEFAULT_VALUE = "dvalue";
    public static final String OPTIONAL = "optional";
    public static final String FILEFORMAT = "fileformat";
    public static final String PREFIX = "prefix";
    public static final String VALUE = "value";
    public static final String CHOICES = "choices";
    public static final String FLAG = "flag";
    public static final String FLAGSPACE = "flagspace";



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
            this.put(CHOICES, object.get(CHOICES));
            this.put(FLAG, object.get(FLAG));
            this.put(FLAGSPACE, object.get(FLAGSPACE));

            String typeString = String.class.getName();
            if(TYPE.equals("FILE"))
            {
                typeString = File.class.getName();
            }

            this.put(type, typeString);
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
            this.put(FLAGSPACE, pAttrs.get(FLAGSPACE));

            //returns choices delimted by ; and display name and val delimited by =
            String choices = pInfo.getValue();
            this.put(CHOICES, choices);
        }
        catch (Exception e) {
            log.error("Error creating parameter JSON from from ParameterInfo object");
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

    public String getChoices() throws JSONException {
       return this.getString(CHOICES);
    }

    public void setChoices(String value) throws JSONException {
       this.put(CHOICES, value);
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

