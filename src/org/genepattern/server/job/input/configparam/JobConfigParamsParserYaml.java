package org.genepattern.server.job.input.configparam;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.genepattern.webservice.ParameterInfo;
import org.yaml.snakeyaml.Yaml;


/**
 * Deserialize a JobConfigParam instance from a yaml file or stream.
 * @author pcarr
 *
 */
public class JobConfigParamsParserYaml {
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
            //TODO: log.error("Error parsing config file="+file.getPath(), t);
            throw new Exception("Error parsing config file="+file.getPath());
        }
        finally {
            if (in != null) {
                in.close();
            }
        }
    }
    
    static ParameterInfo initParamInfo(final Object obj) {
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
            for(final String choice : (List<String>) map.get("choices")) {
                builder.addChoice(choice);
            }
        }
        return builder.build();
    }

}
