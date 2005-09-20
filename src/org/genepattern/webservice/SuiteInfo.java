package org.genepattern.webservice;

/**
 * Used to hold information about particular Task
 * 
 * @author Ted Liefeld
 * @version 1.0
 */

import java.io.*;
import java.util.Map;

public class SuiteInfo implements Serializable {

	private String lsid = null;
	private String taskName = "", description = "";
	private int accessId = 0;
	private String userId = null;


	/** Creates new SuiteInfo  */
	public SuiteInfo () {
	}

	public SuiteInfo (int taskID, String taskName, String description) {
	}

	
	
	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserId() {
		return this.userId;
	}

	public void setLSID(String LSID) {
		this.lsid = LSID;
	}

	public String getLSID() {
		return this.lsid;
	}

	public void setAccessId(int accessId) {
		this.accessId = accessId;
	}

	public int getAccessId() {
		return this.accessId;
	}

	
	public String getName() {
		return taskName;
	}

	public void setName(java.lang.String taskName) {
		this.taskName = taskName;
	}

	public String getID() {
		return getLSID();
	}

	public void setID(String ID) {
		 setLSID(ID);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(java.lang.String description) {
		this.description = description;
	}

	
	
	
	public boolean equals(Object otherThing) {
		if (!(otherThing instanceof SuiteInfo ) || otherThing == null)
			return false;
		SuiteInfo  other = (SuiteInfo ) otherThing;
		return getUserId().equals(other.getUserId())
				&& getAccessId() == other.getAccessId()
				&& getName().equals(other.getName())
				&& getLSID() == other.getLSID()
				&& getDescription().equals(other.getDescription());
	}
	
	public int hashCode(){
      	return this.getLSID().hashCode();
	}

}