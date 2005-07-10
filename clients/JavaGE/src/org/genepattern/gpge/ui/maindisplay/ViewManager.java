package org.genepattern.gpge.ui.maindisplay;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JSplitPane;

import org.genepattern.gpge.message.ChangeViewMessage;
import org.genepattern.gpge.message.ChangeViewMessageRequest;
import org.genepattern.gpge.message.GPGEMessage;
import org.genepattern.gpge.message.GPGEMessageListener;
import org.genepattern.gpge.message.MessageManager;
import org.genepattern.gpge.ui.tasks.AnalysisServiceDisplay;
import org.genepattern.gpge.ui.tasks.PipelineComponent;

public class ViewManager {
	private AnalysisServiceDisplay analysisServiceDisplay;
	private PipelineComponent pipelineComponent;
	private JSplitPane splitPane;
	
	public ViewManager(JSplitPane splitPane) {
		this.splitPane = splitPane;
		analysisServiceDisplay = new AnalysisServiceDisplay();
		analysisServiceDisplay.setMinimumSize(new Dimension(200, 200));
		pipelineComponent = new PipelineComponent();
		MessageManager.addGPGEMessageListener(new GPGEMessageListener() {

			public void receiveMessage(GPGEMessage message) {
				if (message instanceof ChangeViewMessageRequest) {
					ChangeViewMessageRequest asm = (ChangeViewMessageRequest) message;
					if (asm.getType() == ChangeViewMessageRequest.SHOW_RUN_TASK_REQUEST) {
						analysisServiceDisplay.loadTask(asm.getAnalysisService());
						setComponent(analysisServiceDisplay);
						MessageManager.notifyListeners(new ChangeViewMessage(this, ChangeViewMessage.RUN_TASK_SHOWN, analysisServiceDisplay));
					} else if(asm.getType()==ChangeViewMessageRequest.SHOW_EDIT_PIPELINE_REQUEST) {
						pipelineComponent.setTaskInfo(asm.getAnalysisService().getTaskInfo());
						setComponent(pipelineComponent);
						MessageManager.notifyListeners(new ChangeViewMessage(this, ChangeViewMessage.EDIT_PIPELINE_SHOWN, analysisServiceDisplay));
					} else if(asm.getType()==ChangeViewMessageRequest.SHOW_GETTING_STARTED_REQUEST) {
						analysisServiceDisplay.showGettingStarted();
						setComponent(analysisServiceDisplay);
						MessageManager.notifyListeners(new ChangeViewMessage(this, ChangeViewMessage.GETTING_STARTED_SHOWN, analysisServiceDisplay));
					} else if(asm.getType()==ChangeViewMessageRequest.SHOW_VIEW_PIPELINE_REQUEST) {
						pipelineComponent.setTaskInfo(asm.getAnalysisService().getTaskInfo());
						setComponent(pipelineComponent);
						MessageManager.notifyListeners(new ChangeViewMessage(this, ChangeViewMessage.VIEW_PIPELINE_SHOWN, analysisServiceDisplay));
					}
				}
			}
		});
		
	}
	
	public Component getComponent() {
		return splitPane.getRightComponent();
	}
	
	private void setComponent(Component c) {
		if(c!=splitPane.getRightComponent()) {
			splitPane.setRightComponent(c);
		}
	}
}
