package org.genepattern.gpge.message;

public class AbstractTypedGPGEMessage extends AbstractGPGEMessage {
	private int type;
	
	public AbstractTypedGPGEMessage(Object source, int type) {
		super(source);
		this.type = type;
	}
	
	public int getType() {
		return type;
	}

}
