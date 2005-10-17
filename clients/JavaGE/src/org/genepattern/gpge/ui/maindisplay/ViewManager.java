package org.genepattern.gpge.ui.maindisplay;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JSplitPane;

import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.message.ChangeViewMessage;
import org.genepattern.gpge.message.ChangeViewMessageRequest;
import org.genepattern.gpge.message.GPGEMessage;
import org.genepattern.gpge.message.GPGEMessageListener;
import org.genepattern.gpge.message.MessageManager;
import org.genepattern.gpge.ui.suites.SuiteEditor;
import org.genepattern.gpge.ui.tasks.AnalysisServiceDisplay;
import org.genepattern.gpge.ui.tasks.pipeline.PipelineEditor;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.TaskInfo;

public class ViewManager {
	private AnalysisServiceDisplay analysisServiceDisplay;

	private PipelineEditor pipelineEditor;
	
	private PipelineEditor pipelineViewer;

	private JSplitPane splitPane;

	private SuiteEditor suiteEditor;

	public ViewManager(JSplitPane splitPane) {
		this.splitPane = splitPane;
		analysisServiceDisplay = new AnalysisServiceDisplay();
		analysisServiceDisplay.setMinimumSize(new Dimension(200, 200));
		pipelineEditor = new PipelineEditor();
		pipelineViewer = new PipelineEditor();
		suiteEditor = new SuiteEditor();
		
		MessageManager.addGPGEMessageListener(new GPGEMessageListener() {

			public void receiveMessage(GPGEMessage message) {
				if (message instanceof ChangeViewMessageRequest) {
					ChangeViewMessageRequest asm = (ChangeViewMessageRequest) message;
					if(asm.getType()==ChangeViewMessageRequest.SHOW_EDIT_SUITE_REQUEST) {
						suiteEditor.display(asm.getSuiteInfo());
						setComponent(suiteEditor);
						MessageManager.notifyListeners(new ChangeViewMessage(
								message.getSource(),
								ChangeViewMessage.EDIT_SUITE_SHOWN,
								suiteEditor));
					} else if (asm.getType() == ChangeViewMessageRequest.SHOW_RUN_TASK_REQUEST) {
						analysisServiceDisplay.loadTask(asm
								.getAnalysisService());
						setComponent(analysisServiceDisplay);
						MessageManager.notifyListeners(new ChangeViewMessage(
								message.getSource(),
								ChangeViewMessage.RUN_TASK_SHOWN,
								analysisServiceDisplay));
					} else if (asm.getType() == ChangeViewMessageRequest.SHOW_EDIT_PIPELINE_REQUEST) {
						AnalysisService svc = asm.getAnalysisService();
						if (svc == null) {
							pipelineEditor.edit(null, null);
							setComponent(pipelineEditor);
							MessageManager
									.notifyListeners(new ChangeViewMessage(
											message.getSource(),
											ChangeViewMessage.EDIT_PIPELINE_SHOWN,
											pipelineEditor));
						} else {
							TaskInfo info = asm.getAnalysisService()
									.getTaskInfo();
							try {
								PipelineModel pipelineModel = PipelineModel
										.toPipelineModel((String) info
												.getTaskInfoAttributes()
												.get(
														GPConstants.SERIALIZED_MODEL));
								if(!pipelineEditor.edit(svc, pipelineModel)) {
									if(!pipelineEditor.edit(svc, pipelineModel)) {
										setComponent(pipelineViewer);
										MessageManager
										.notifyListeners(new ChangeViewMessage(
												message.getSource(),
												ChangeViewMessage.VIEW_PIPELINE_SHOWN,
												pipelineEditor));
									}
								} else {
									setComponent(pipelineEditor);
									MessageManager
									.notifyListeners(new ChangeViewMessage(
											message.getSource(),
											ChangeViewMessage.EDIT_PIPELINE_SHOWN,
											pipelineEditor));
								}
								
								

							} catch (Exception e1) {
								e1.printStackTrace();
								GenePattern
										.showErrorDialog("An error occurred while loading the pipeline");

							}
						}

					} else if (asm.getType() == ChangeViewMessageRequest.SHOW_GETTING_STARTED_REQUEST) {
						analysisServiceDisplay.showGettingStarted();
						setComponent(analysisServiceDisplay);
						MessageManager.notifyListeners(new ChangeViewMessage(
								message.getSource(),
								ChangeViewMessage.GETTING_STARTED_SHOWN,
								analysisServiceDisplay));
					} else if (asm.getType() == ChangeViewMessageRequest.SHOW_VIEW_PIPELINE_REQUEST) {
						PipelineModel pipelineModel;
						try {
							pipelineModel = PipelineModel
									.toPipelineModel((String) asm
											.getAnalysisService().getTaskInfo()
											.getTaskInfoAttributes()
											.get(GPConstants.SERIALIZED_MODEL));

							pipelineViewer.view(asm.getAnalysisService(),
									pipelineModel);
							setComponent(pipelineViewer);
							MessageManager
									.notifyListeners(new ChangeViewMessage(
											message.getSource(),
											ChangeViewMessage.VIEW_PIPELINE_SHOWN,
											pipelineViewer));
						} catch (Exception e) {
							e.printStackTrace();
							GenePattern
									.showErrorDialog("An error occurred while loading the pipeline");
						}
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
		if (c != splitPane.getRightComponent()) {
			int location = splitPane.getDividerLocation();
			splitPane.setRightComponent(c);
			splitPane.setDividerLocation(location);
		}
	}
}
