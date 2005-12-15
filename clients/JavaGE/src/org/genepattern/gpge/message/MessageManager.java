/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/


package org.genepattern.gpge.message;

import java.util.ArrayList;
import java.util.List;


public class MessageManager {
	private static List listeners = new ArrayList();
	   
	private MessageManager() {
	}
	
	public static void addGPGEMessageListener(GPGEMessageListener l) {
		listeners.add(l);
	}
	
	public static void removeGPGEMessageListener(GPGEMessageListener l) {
		listeners.remove(l);
	}

	
	public static void notifyListeners(GPGEMessage message) {
		for(int i = 0; i < listeners.size(); i++) {
			GPGEMessageListener listener = (GPGEMessageListener) listeners.get(i);
			listener.receiveMessage(message);
		}
		
	}

}
