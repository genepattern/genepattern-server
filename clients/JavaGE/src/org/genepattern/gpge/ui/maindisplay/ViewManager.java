package org.genepattern.gpge.ui.maindisplay;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JSplitPane;

import org.genepattern.gpge.message.ChangeViewMessage;
import org.genepattern.gpge.message.ChangeViewMessageRequest;
import org.genepattern.gpge.message.GPGEMessage;
import org.genepattern.gpge.message.GPGEMessageListener;
import org.genepattern.gpge.message.MessageManager;
import org.genepattern.gpge.ui.tasks.pipeline.PipelineEditor;
import org.genepattern.gpge.ui.tasks.pipeline.ViewPipeline;
import org.genepattern.gpge.ui.tasks.AnalysisServiceDisplay;

public class ViewManager {
	private AnalysisServiceDisplay analysisServiceDisplay;
	private PipelineEditor pipelineComponent;
	private ViewPipeline viewPipeline;
	private JSplitPane splitPane;
	
	public ViewManager(JSplitPane splitPane) {
		this.splitPane = splitPane;
		analysisServiceDisplay = new AnalysisServiceDisplay();
		analysisServiceDisplay.setMinimumSize(new Dimension(200, 200));
		pipelineComponent = new PipelineEditor();
		viewPipeline = new ViewPipeline();
		
		MessageManager.addGPGEMessageListener(new GPGEMessageListener() {

			public void receiveMessage(GPGEMessage message) {
				if (message instanceof ChangeViewMessageRequest) {
					ChangeViewMessageRequest asm = (ChangeViewMessageRequest) message;
					if (asm.getType() == ChangeViewMessageRequest.SHOW_RUN_TASK_REQUEST) {
						analysisServiceDisplay.loadTask(asm.getAnalysisService());
						setComponent(analysisServiceDisplay);
						MessageManager.notifyListeners(new ChangeViewMessage(message.getSource(), ChangeViewMessage.RUN_TASK_SHOWN, analysisServiceDisplay));
					} else if(asm.getType()==ChangeViewMessageRequest.SHOW_EDIT_PIPELINE_REQUEST) {
						pipelineComponent.display(asm.getAnalysisService());
						setComponent(pipelineComponent);
						MessageManager.notifyListeners(new ChangeViewMessage(message.getSource(), ChangeViewMessage.EDIT_PIPELINE_SHOWN, pipelineComponent));
					} else if(asm.getType()==ChangeViewMessageRequest.SHOW_GETTING_STARTED_REQUEST) {
						analysisServiceDisplay.showGettingStarted();
						setComponent(analysisServiceDisplay);
						MessageManager.notifyListeners(new ChangeViewMessage(message.getSource(), ChangeViewMessage.GETTING_STARTED_SHOWN, analysisServiceDisplay));
					} else if(asm.getType()==ChangeViewMessageRequest.SHOW_VIEW_PIPELINE_REQUEST) {
						viewPipeline.display(asm.getAnalysisService());
						setComponent(viewPipeline);
						MessageManager.notifyListeners(new ChangeViewMessage(message.getSource(), ChangeViewMessage.VIEW_PIPELINE_SHOWN, viewPipeline));
					} else {
						System.err.println("Unknown type:" + asm.getType());
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
			int location = splitPane.getDividerLocation();
			splitPane.setRightComponent(c);
			splitPane.setDividerLocation(location);
		}
	}
}
