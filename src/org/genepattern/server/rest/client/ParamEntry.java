package org.genepattern.server.rest.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ParamEntry {
    /** The name of the input parameter. */
    private final String name;
    /** A list of zero or more values for the input parameter. */
    private List<String> values=new ArrayList<String>();

    /**
     * Initialize with a parameter name.
     * @param name
     */
    public ParamEntry(final String name) {
        this.name=name;
    }
    
    public String getName() {
        return name;
    }

    public List<String> getValues() {
        return Collections.unmodifiableList(values);
    }
    
    public void addValue(final String value) {
        this.values.add(value);
    }
}
