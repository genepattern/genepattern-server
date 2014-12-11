package org.genepattern.server.webapp.rest;

import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

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
    Boolean isJsViewer;
    String params;
	List<String> batchParams;
    String comment;
    List<String> tags;

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
	
	@JsonProperty("batchParams")
    public List<String> getBatchParams() {
        return batchParams;
    }

    public void setBatchParams(List<String> batchParams) {
        this.batchParams = batchParams;
    }

    @JsonProperty("comment")
    public String getComment() { return comment; }

    public void setComment(String comment) { this.comment = comment; }

    @JsonProperty("tags")
    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

	@Override
	public String toString()
    {
		return "{lsid:" + lsid + ", params:" + params + "}";
	}

}
