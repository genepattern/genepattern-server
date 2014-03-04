package org.genepattern.server.job.input.configparam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.genepattern.server.job.input.choice.ChoiceInfo;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.ParameterInfo;

import com.google.common.base.Joiner;

public class ParamInfoBuilder {
    private final String name;
    private String altName=null;
    private final String value="";
    private String description="";
    private boolean optional=true;
    private List<String> choices=null;
    private String defaultValue="";

    public ParamInfoBuilder(final String name) {
        this.name=name;
    }
    public ParamInfoBuilder altName(final String altName) {
        this.altName=altName;
        return this;
    }
    public ParamInfoBuilder description(final String description) {
        this.description=description;
        return this;
    }
    public ParamInfoBuilder optional(final boolean optional) {
        this.optional=optional;
        return this;
    }
    public ParamInfoBuilder defaultValue(final String defaultValue) {
        this.defaultValue=defaultValue;
        return this;
    }
    public ParamInfoBuilder addChoice(final String value) {
        if (choices==null) {
            choices=new ArrayList<String>();
        }
        choices.add(value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public ParameterInfo build() {
        final ParameterInfo pinfo=new ParameterInfo(name, value, description);
        pinfo.setAttributes(new HashMap<String,String>());
        if (altName!=null) {
            pinfo.getAttributes().put(GPConstants.ALTNAME, altName);
        }
        pinfo.getAttributes().put("MODE", "");
        pinfo.getAttributes().put("TYPE", "TEXT");
        pinfo.getAttributes().put("default_value", defaultValue);
        if (optional) {
            pinfo.getAttributes().put("optional", "on");
        }
        else {
            pinfo.getAttributes().put("optional", "");
        }
        pinfo.getAttributes().put("type", "java.lang.String");
        if (choices!=null) {
            Joiner joiner=Joiner.on(";").useForNull("");
            String choicesStr=joiner.join(choices);
            pinfo.getAttributes().put(ChoiceInfo.PROP_CHOICE, choicesStr);
        }
        return pinfo;
    }
}
