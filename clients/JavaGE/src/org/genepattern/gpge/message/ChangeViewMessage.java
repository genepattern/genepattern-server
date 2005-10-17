package org.genepattern.gpge.message;

import java.awt.Component;

import org.genepattern.gpge.ui.tasks.AnalysisServiceDisplay;

public class ChangeViewMessage extends AbstractTypedGPGEMessage {
	
	public static int GETTING_STARTED_SHOWN = 0;
	public static int RUN_TASK_SHOWN = 1;
	public static int EDIT_PIPELINE_SHOWN = 2;
	public static int VIEW_PIPELINE_SHOWN = 3;
	public static int EDIT_SUITE_SHOWN = 4;
	
	private Component component;
	
	public ChangeViewMessage(Object source, int type, Component c) {
		super(source, type);
		this.component = c;
	}

	public Component getComponent() {
		return component;
	}

}
