/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.configparam;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.job.input.choice.Choice;
import org.genepattern.server.job.input.choice.ChoiceInfoHelper;
import org.genepattern.webservice.ParameterInfo;
import org.yaml.snakeyaml.Yaml;


/**
 * Deserialize a JobConfigParam instance from a yaml file or stream.
 * @author pcarr
 *
 */
public class JobConfigParamsParserYaml {
    final static private Logger log = Logger.getLogger(JobConfigParamsParserYaml.class);
    public static final JobConfigParams parse(final File file) throws Exception {
        InputStream in=null;
        try {
            in = new FileInputStream(file);
            Yaml yaml = new Yaml();
            Map<?,?> map = (Map<?,?>) yaml.load(in);
            JobConfigParams.Builder builder= new JobConfigParams.Builder();
            if (map.containsKey("name")) {
                //expecting a String
                builder.name( (String) map.get("name"));
            }
            if (map.containsKey("description")) {
                builder.description( (String) map.get("description") ); 
            }
            if (map.containsKey("hidden")) {
                builder.hidden( (Boolean) map.get("hidden") );
            }
            
            //set through the list of parameters
            if (map.containsKey("parameters")) {
                //expecting an array
                List<?> parameters= (List<?>) map.get("parameters");
                for(final Object paramObj : parameters) {
                    ParameterInfo pInfo = initParamInfo( paramObj );
                    builder.addParameter(pInfo);
                }
            }
            return builder.build();
        }
        catch (Throwable t) {
            log.error("Error parsing config file="+file.getPath(), t);
            throw new Exception("Error parsing config file="+file.getPath()+": "+t.getLocalizedMessage());
        }
        finally {
            if (in != null) {
                in.close();
            }
        }
    }
    
    static ParameterInfo initParamInfo(final Object obj) throws Exception {
        final Map<?,?> map = (Map<?,?>) obj;
        ParamInfoBuilder builder=new ParamInfoBuilder( (String) map.get("name"));
        if (map.containsKey("altName")) {
            builder.altName( (String) map.get("altName") );
        }
        if (map.containsKey("description")) {
            builder.description( (String) map.get("description") );
        }
        if (map.containsKey("optional")) {
            builder.optional( (Boolean) map.get("optional") );
        }
        if (map.containsKey("defaultValue")) {
            builder.defaultValue( (String) map.get("defaultValue") );
        }
        if (map.containsKey("choices")) {
            for(final Object choiceObj : (List<?> )map.get("choices")) {
                if (choiceObj instanceof String) {
                    final Choice choice=ChoiceInfoHelper.initChoiceFromManifestEntry((String)choiceObj);
                    builder.addChoice(choice);
                }
                else {
                    throw new Exception("parse error, 'choices' must be an array of string");
                }
            }
        }
        return builder.build();
    }

}
