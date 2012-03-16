package org.genepattern.modules;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;
import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: nazaire
 * Date: Mar 15, 2012
 * Time: 4:33:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class ParametersJSON extends JSONObject {
    public static Logger log = Logger.getLogger(ParametersJSON.class);

    public static final String KEY = "parameters";

    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String TYPE = "type";
    public static final String MODE = "mode";
    public static final String DEFAULT_VALUE = "dvalue";
    public static final String OPTIONAL = "optional";
    public static final String FILEFORMAT = "fileformat";
    public static final String PREFIX = "prefix";
    public static final String VALUE = "value";

    public ParametersJSON(JSONObject object) {
        try {
            this.put(NAME, object.get(NAME));
            this.put(DESCRIPTION, object.get(DESCRIPTION));
            this.put(TYPE, object.get(TYPE));
            this.put(MODE, object.get(MODE));
            this.put(FILEFORMAT, object.get(FILEFORMAT));
            this.put(DEFAULT_VALUE, object.get(DEFAULT_VALUE));
            this.put(OPTIONAL, object.get(OPTIONAL));
            this.put(PREFIX, object.get(PREFIX));
            this.put(VALUE, object.get(VALUE));
        }
        catch (Exception e) {
            log.error("object: " + object);
            log.error("Error creating parameter from generic JSON object");
        }
    }


    public boolean isOptional() throws JSONException {
        if(this.getString(OPTIONAL).equalsIgnoreCase("on"))
        {
           return true;
        }

        return false;
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

