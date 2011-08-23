package org.genepattern.server.gs;

import java.util.Map;

/**
 * Helper class for GenomeSpace intergration, included in gp core. It must not reference any GS core classes.
 * @author pcarr
 *
 */
public class GsLoginResponse {
    public boolean unknownUser = true;
    public Map<String,Object> attrs;
    public String gsAuthenticationToken;
    public String gsUsername = "";
}

