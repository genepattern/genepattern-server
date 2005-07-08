package org.genepattern.gpge.message;

public class PreferenceChangeMessage extends AbstractTypedGPGEMessage {

	public static final int SHOW_PARAMETER_DESCRIPTIONS = 0;
	private boolean showDescriptions;
	
	public PreferenceChangeMessage(Object source, int type, boolean showDescriptions) {
		super(source, type);
		this.showDescriptions = showDescriptions;
	}
	
	public boolean getDescriptionsVisible() {
		return showDescriptions;
	}

}
