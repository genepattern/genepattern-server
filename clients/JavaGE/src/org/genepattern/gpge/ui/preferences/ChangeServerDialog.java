package org.genepattern.gpge.ui.preferences;

import org.genepattern.gpge.ui.maindisplay.CenteredDialog;

import java.awt.BorderLayout;
import java.net.URL;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;


/**
 * Description of the Class
 * 
 * @author Joshua Gould
 */
public class ChangeServerDialog extends CenteredDialog {
	private JLabel serverLabel, portLabel, usernameLabel;

	private JTextField portTextField, serverTextField, usernameTextField;

	public ChangeServerDialog(java.awt.Frame owner) {
		super(owner);
      setTitle("Server Settings");
      setModal(false);
	}

	public void show(String server, String username, ActionListener okListener) {

		serverLabel = new JLabel("Server Name: ",
				javax.swing.SwingConstants.RIGHT);
		serverTextField = new JTextField(20);
		URL url = null;
		try {
			url = new URL(server);
		} catch (Exception x) {
		}
      if(url!=null) {
         serverTextField.setText(url.getHost());
      }
		portLabel = new JLabel("Port: ", javax.swing.SwingConstants.RIGHT);
		portTextField = new JTextField(10);
      if(url!=null) {
         portTextField.setText("" + url.getPort());
      }
		usernameLabel = new JLabel("Username: ",
				javax.swing.SwingConstants.RIGHT);
		usernameTextField = new JTextField(20);
		usernameTextField.setText(username);
		JButton okButton = new JButton("OK");

		okButton.addActionListener(okListener);

		JButton cancelButton = new JButton("Cancel");

		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
      FormLayout formLayout = new FormLayout(
               "right:pref:none, 6px, left:pref:none, 6px",
               "6px, pref, 6px, pref, 6px, pref, 6px");
       
      JPanel inputPanel = new JPanel(formLayout);
      CellConstraints cc = new CellConstraints();
      inputPanel.add(serverLabel, cc.xy(1, 2));
      inputPanel.add(serverTextField, cc.xy(3, 2));
      
      inputPanel.add(portLabel, cc.xy(1, 4));
      inputPanel.add(portTextField, cc.xy(3, 4));
      
      inputPanel.add(usernameLabel, cc.xy(1, 6));
      inputPanel.add(usernameTextField, cc.xy(3, 6));
      
      JPanel buttonPanel = new JPanel();
      buttonPanel.add(cancelButton);
      buttonPanel.add(okButton);
      getContentPane().add(buttonPanel, BorderLayout.SOUTH);
      getContentPane().add(inputPanel, BorderLayout.CENTER);
		serverTextField.grabFocus();
		getRootPane().setDefaultButton(okButton);
		pack();
      setResizable(false);
		setVisible(true);
	}

	public String getPort() {
		return portTextField.getText();
	}

	public String getUsername() {
		return usernameTextField.getText();
	}

	public String getServer() {

		return serverTextField.getText();
	}

}

