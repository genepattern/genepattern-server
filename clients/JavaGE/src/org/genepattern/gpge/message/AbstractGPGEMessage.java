package org.genepattern.gpge.message;

public class AbstractGPGEMessage implements GPGEMessage {
	private Object source;
	
	public AbstractGPGEMessage(Object source) {
		this.source = source;
	}

	public Object getSource() {
		return source;
	}

}
