/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


/*
 * MessageUtils.java
 *
 * Created on March 10, 2003, 12:47 PM
 */

package org.genepattern.server.util;

import java.util.*;
import java.io.*;

import org.genepattern.server.config.ServerConfigurationFactory;

/**
 * class to handle reading and giving messages that may need to be customized 
 * at some future point and are stored in skin/message.properties
 * 
 * @author liefeld
 */
public class MessageUtils extends Properties {

	public MessageUtils(){
		super();
		_init(new File(ServerConfigurationFactory.instance().getResourcesDir(), "messages.properties"));
	}


	public MessageUtils(File aFile){
		_init(aFile);
	}

	public void _init(File aFile){
		try {
			FileInputStream is = new FileInputStream(aFile); 
			this.load(is);
			is.close();		
		} catch (Exception e){
			e.printStackTrace();
		}
	}

}
