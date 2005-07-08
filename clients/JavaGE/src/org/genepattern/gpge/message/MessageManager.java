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
