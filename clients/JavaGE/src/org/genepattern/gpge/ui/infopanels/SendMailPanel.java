/*
 * Created on Oct 17, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.genepattern.gpge.ui.infopanels;

import java.awt.Color;

import javax.swing.JPanel;

/**
 * @author Liefeld
 * 
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class SendMailPanel extends JPanel {

	/** Creates new form ReportPanel */
	public SendMailPanel() {
		initComponents();
		preamble_textArea
				.setText("Please describe the actions you were taking immediately prior to\n"
						+ "noticing the problem.  Optionally provide your email address if you\n"
						+ "would like to receive a response to this error report submission. The\n"
						+ "error stack trace will be automatically included.");
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 *  
	 */
	private void initComponents() {//GEN-BEGIN:initComponents
		java.awt.GridBagConstraints gridBagConstraints;

		editors_ScrollPane = new javax.swing.JScrollPane();
		preamble_textArea = new javax.swing.JTextArea();
		text_scrollPane = new javax.swing.JScrollPane();
		textArea = new javax.swing.JTextArea();
		useremail_textField = new javax.swing.JTextField();
		javax.swing.JLabel email_label = new javax.swing.JLabel();

		setLayout(new java.awt.GridBagLayout());

		editors_ScrollPane.setPreferredSize(new java.awt.Dimension(450, 70));
		preamble_textArea.setEditable(false);
		preamble_textArea.setBackground(Color.lightGray);

		editors_ScrollPane.setViewportView(preamble_textArea);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 0.33;
		add(editors_ScrollPane, gridBagConstraints);

		useremail_textField.setPreferredSize(new java.awt.Dimension(250, 25));
		email_label.setText("Your E-Mail Address (optional):");
		email_label.setPreferredSize(new java.awt.Dimension(150, 25));

		JPanel labelPane = new JPanel();
		labelPane.setLayout(new java.awt.GridLayout(1, 0));
		labelPane.add(email_label);
		labelPane.add(useremail_textField);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
		add(labelPane, gridBagConstraints);

		text_scrollPane.setPreferredSize(new java.awt.Dimension(450, 150));
		textArea.setEditable(true);
		textArea.setColumns(80);
		textArea.setRows(4);
		text_scrollPane.setViewportView(textArea);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 0.66;
		add(text_scrollPane, gridBagConstraints);

	}

	public String getDescriptionText() {
		return textArea.getText();

	}

	public String getEmailAddress() {
		return useremail_textField.getText();

	}

	//Variables declaration

	private javax.swing.JScrollPane editors_ScrollPane;

	private javax.swing.JTextArea textArea;

	private javax.swing.JScrollPane text_scrollPane;

	private javax.swing.JTextArea preamble_textArea;

	private javax.swing.JTextField useremail_textField;

}