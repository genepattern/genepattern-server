package org.genepattern.gpge.ui.preferences;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;

import org.genepattern.gpge.GenePattern;

import java.io.*;
import java.util.*;

public class ChangeServerDialog extends JDialog {
	private boolean result = false;
	private JLabel serverLabel, portLabel;
	private JTextField portTextField, serverTextField;
	private FileInputStream fis;
	private int port;
	private String server;
	Properties properties = null;
	static final String PROPERTIES_FILE =System.getProperty("user.home") + File.separator + "gp" + File.separator + "resources" + File.separator + "omnigene.properties";
   
	private static String createProperty(String s, String server, int port) {
		int b = s.lastIndexOf(":");
		int e2 = s.indexOf("/", b);
		return server + ":" + port + s.substring(e2, s.length());	
	}
	public ChangeServerDialog(JFrame parent) {
		super(parent, true);
		setTitle("Server Settings");
		
		try {
			fis = new FileInputStream(PROPERTIES_FILE);
			properties = new Properties();
			properties.load(fis);
         fis.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		String siteName = properties.getProperty("analysis.service.site.name");
		int beginIndex = siteName.lastIndexOf(":");
		String currentPort = siteName.substring(beginIndex+1, siteName.length());
		String currentServerName = null;
		if(siteName.startsWith("http://")) {
			currentServerName = siteName.substring("http://".length(), beginIndex); // remove http:// and :port
		} else {
			currentServerName = siteName.substring(0, beginIndex); // remove :port
		}
		serverLabel = new JLabel("Server Name: ");
		serverTextField = new JTextField(20);
		serverTextField.setText(currentServerName);
		portLabel = new JLabel("Port: ");
		portTextField = new JTextField(10);
		portTextField.setText(currentPort);

		JButton okButton = new JButton("OK");

		okButton.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						port = Integer.parseInt(portTextField.getText());
						server = serverTextField.getText();
						
				
						if(server.startsWith("http://")) { 
							server = server.substring("http://".length(), server.length()); //remove http://
						} 
						//java.net.InetAddress address = java.net.InetAddress.getByName(server);
					//	String server = address.getCanonicalHostName(); requires 1.4
						//String server = address.getHostName();
						
						properties.setProperty("analysis.service.site.name", server + ":" + port);
						server = "http://" + server;
						properties.setProperty("retrieve.file", createProperty(properties.getProperty("retrieve.file"), server, port));
						properties.setProperty("analysis.service.URL", createProperty(properties.getProperty("analysis.service.URL"), server, port));
						properties.setProperty("task.documentation", createProperty(properties.getProperty("task.documentation"), server, port));
						properties.setProperty("result.file.header.source", createProperty(properties.getProperty("result.file.header.source"), server, port));
                  FileOutputStream fos = new FileOutputStream(PROPERTIES_FILE);
                  properties.store(fos, null);
                  fos.close();
						dispose();
						int result = JOptionPane.showOptionDialog(GenePattern.getDialogParent(), "You must restart to use the new server.", "Changes Saved", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[]{"Restart Now", "Restart Later"},  "Restart Now");
						if(result != JOptionPane.OK_OPTION) {
							return;	
						}
						boolean runningOnWindows = System.getProperty("os.name").startsWith("Windows");
						if(runningOnWindows) {
							File dir = new File(System.getProperty("lax.dir"));
							File executable = new File(dir, System.getProperty("lax.application.name"));
							String[] cmd = {executable.getCanonicalPath()};
							Runtime.getRuntime().exec(cmd);	
							System.exit(0);
						} else if(org.genepattern.gpge.ui.analysis.AnalysisTasksPanel.RUNNING_ON_MAC) {
							String libPath = System.getProperty("java.library.path");
							String firstLibPath = libPath.substring(0, libPath.indexOf(":"));
							//firstLibPath = escapeSpaces(firstLibPath);
							java.io.File launcher = new java.io.File(firstLibPath.substring(0, firstLibPath.indexOf("Contents")));
							String[] cmd = {"open", launcher.getCanonicalPath()};
							Runtime.getRuntime().exec(cmd);
							System.exit(0);
						} else { // linux
							String jarFile = System.getProperty("java.class.path");
							int index = jarFile.indexOf(":");
							if(index != -1) {
								jarFile = jarFile.substring(0, index);
							}
							String java = System.getProperty("java.home") + "/bin/java";
							String[] cmd = {java, "-jar", jarFile};
							Runtime.getRuntime().exec(cmd, null, new java.io.File(System.getProperty("user.dir")));	
							System.exit(0);
						}
					} catch(NumberFormatException nfe) {
						JOptionPane.showMessageDialog(GenePattern.getDialogParent(), "Invalid port.");
               } catch(IOException ioe) {
                  JOptionPane.showMessageDialog(GenePattern.getDialogParent(), "There was an error saving your changes. Please try again.");
                  ioe.printStackTrace();
               }	
					
				}
			});
		JButton cancelButton = new JButton("Cancel");

		cancelButton.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
			});

		Container content = getContentPane();
		content.setLayout(new GridBagLayout());
		GBA gba = new GBA();
		gba.add(content, serverLabel, 0, 0, 1, 1, 0, 0, GBA.NONE, GBA.C, new Insets(5, 5, 5, 5), 0, 0);
		gba.add(content, serverTextField, 1, 0, 1, 1, 1, 0, GBA.H, GBA.C, new Insets(5, 5, 5, 5), 0, 0);
		gba.add(content, portLabel, 0, 1, 1, 1, 0, 0, GBA.NONE, GBA.C, new Insets(5, 5, 5, 5), 0, 0);
		gba.add(content, portTextField, 1, 1, 2, 1, 1, 0, GBA.H, GBA.C, new Insets(5, 5, 5, 5), 0, 0);
		gba.add(content, cancelButton, 0, 2, 1, 1, 0, 0, GBA.NONE, GBA.W, new Insets(5, 5, 5, 5), 0, 0);
		gba.add(content, okButton, 1, 2, 1, 1, 0, 0, GBA.NONE, GBA.E, new Insets(5, 5, 5, 5), 0, 0);
	//	setResizable(false);
		serverTextField.grabFocus();
		getRootPane().setDefaultButton(okButton);
		pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screenSize.width - getSize().width) / 2, (screenSize.height - getSize().height) / 2);
		show();
	}
	
	static String escapeSpaces(String s) {
		StringBuffer sb = new StringBuffer(s);
		for(int i = 0; i < sb.length(); i++) {
			if(sb.charAt(i) == ' ') {
				sb.insert(i,"\\");
				i++;
			}
		}	
		return sb.toString();
	}

}

