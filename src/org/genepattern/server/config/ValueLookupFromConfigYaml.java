package org.genepattern.server.config;

import org.apache.log4j.Logger;

/**
 * Basic implementation of the ValueLookup interface using the CommandManagerProperties instance
 * resulting from parsing a single config_.yaml file.
 * 
 * @see CommandManagerProperties class for more documentation.
 * 
 * @author pcarr
 *
 */
public class ValueLookupFromConfigYaml implements ValueLookup {
    private static Logger log = Logger.getLogger(ValueLookupFromConfigYaml.class);
    
    private final GpServerProperties serverProperties;
    private final ConfigYamlProperties yamlProperties;
    
    public ValueLookupFromConfigYaml(final GpServerProperties serverProperties,
            final ConfigYamlProperties yamlProperties) 
    {
        if (serverProperties==null) {
            log.warn("initializing with serverProperties==null");
        }
        if (yamlProperties==null) {
            log.warn("initializing with yamlProperties==null");
        }
        this.serverProperties=serverProperties;
        this.yamlProperties=yamlProperties;
    }
    
    @Override
    public Value getValue(final GpContext context, final String key) {
        Value value=null;
        if (yamlProperties != null) {
            value=yamlProperties.getValue(context, key);
        }
        if (value==null && serverProperties != null) {
            return serverProperties.getValue(context, key);
        }
        return value;
    }

    
}