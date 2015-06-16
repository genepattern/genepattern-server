/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.server.process;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.genepattern.util.LSID;

public class MissingTaskException extends Exception {
	
	Map<Integer, MissingTaskError> errors;
	
	Set<String> missingLsids;

	public MissingTaskException(Map<Integer, MissingTaskError> errors) {
		this.errors = errors;
		missingLsids = new HashSet<String>();
	}

	public Map getErrors() {
		return errors;
	}

	public String getMessage() {
		StringBuffer buf = new StringBuffer();
		MissingTaskError error;
		for (Map.Entry<Integer, MissingTaskError> entry : errors.entrySet()) {
			error = entry.getValue();
			buf.append(error.getParentTaskName()).append(" refers to task # ");
			int index= entry.getKey().intValue()+ 1;
			buf.append(index +" ");
			buf.append(error.getTaskName());
			
			String type = error.getErrorType();
			LSID requestedLSID = entry.getValue().getTaskLSID();
			
			if (MissingTaskError.errorTypes[0].equals(type)) {
				buf.append(" which does not exist ").append(" (").append(requestedLSID.toString()).append(").\n");
			}else if(MissingTaskError.errorTypes[1].equals(type)) {
				buf.append(" version ");
				buf.append(requestedLSID.getVersion());
				buf.append(" but version ");
				buf.append(error.getAvailableVersion());
				buf.append(" is available.\n");
			}else if(MissingTaskError.errorTypes[2].equals(type)) {
				buf.append(" (").append(requestedLSID.toString()).append(").\n");
			}
			
		}
		return buf.toString();
	}
	
	public Set<String> getMissingLsids() {
		for (Map.Entry<Integer, MissingTaskError> entry : errors.entrySet()) {
			LSID requestedLSID = entry.getValue().getTaskLSID();
			missingLsids.add(requestedLSID.toString());
		}
		return missingLsids;
	}
}
