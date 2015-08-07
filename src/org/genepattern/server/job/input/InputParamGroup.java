/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.ImmutableList;

/**
 * Use this class to represent a group of input parameters as they should appear
 * in the job input form, for example to add an 'Advanced' parameters section to the input form.
 * Not to be confused with the GroupInfo class which is for grouping multiple input files (not parameters)
 * from the job input form into a table serialized to a file.
 * 
 * @author pcarr
 *
 */
public class InputParamGroup {
    final String name;
    final String description;
    final boolean hidden;
    final List<String> parameters;

    private InputParamGroup(final Builder in) {
        this.name=in.name;
        this.description=in.description;
        this.hidden=in.hidden;
        this.parameters=ImmutableList.copyOf(in.parameters);
    }

    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public boolean isHidden() {
        return hidden;
    }
    public List<String> getParameters() {
        return parameters;
    }
    
    /**
     * Example json representation:
     * <pre>
{
  name: "Basic/Required", 
  description: "This contains the required parameters", 
  hidden: false, 
  parameters: [
    "basic.required.parameter.1", 
    "basic.required.parameter.2"]
}
     * </pre>
     * @return
     * @throws JSONException
     */
    public JSONObject toJson() throws JSONException {
        JSONObject json=new JSONObject();
        json.put("name", this.name);
        json.put("description", this.description);
        json.put("hidden", hidden);
        JSONArray params=new JSONArray();
        for(final String pname : parameters) {
            params.put(pname);
        }
        json.put("parameters", params);
        return json;
    }

    public static class Builder {
        final String name;
        String description="";
        boolean hidden=true;
        final List<String> parameters=new ArrayList<String>();
        public Builder(final String name) {
            this.name=name;
        }
        public Builder description(final String description) {
            this.description=description;
            return this;
        }
        public Builder hidden(final boolean hidden) {
            this.hidden=hidden;
            return this;
        }
        /**
         * Add the parameters in the order that they should appear in the group.
         * @param pname
         * @return
         */
        public Builder addParameter(final String pname) {
            parameters.add(pname);
            return this;
        }
        
        public InputParamGroup build() {
            return new InputParamGroup(this);
        }
    }
}
