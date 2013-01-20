package org.genepattern.server.webapp.rest;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonRawValue;
import org.codehaus.jackson.JsonNode;

/**
 * Created by IntelliJ IDEA.
 * User: nazaire
 * Date: Jan 18, 2013
 * Time: 4:11:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class JobSubmitInfo
{
    String lsid;
	String params;

    @JsonProperty("lsid")
	public String getLsid() {
		return lsid;
	}

	public void setLsid(String lsid) {
		this.lsid = lsid;
	}

    @JsonProperty("params")
	public String getParameters() {
		return params;
	}

	public void setParameters(String params) {
		this.params = params;
	}

	@Override
	public String toString()
    {
		return "{lsid:" + lsid + ", params:" + params + "}";
	}

}
