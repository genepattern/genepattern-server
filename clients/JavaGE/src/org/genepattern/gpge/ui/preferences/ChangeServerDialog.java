package org.genepattern.gpge.ui.preferences;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Component;
import java.net.URL;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.genepattern.gpge.GenePattern;

/**
 * Description of the Class
 * 
 * @author Joshua Gould
 */
public class ChangeServerDialog extends JDialog {
	private JLabel serverLabel, portLabel, usernameLabel;

	private JTextField portTextField, serverTextField, usernameTextField;

	public ChangeServerDialog(java.awt.Frame owner, boolean modal) {
		super(owner, "Server Settings", modal);
	}

	public ChangeServerDialog(java.awt.Frame owner) {
		this(owner, false);
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

		Container content = getContentPane();
		content.setLayout(new GridBagLayout());
		GBA gba = new GBA();
		gba.add(content, serverLabel, 0, 0, 1, 1, 0, 0, GBA.NONE, GBA.C,
				new Insets(5, 5, 5, 5), 0, 0);
		gba.add(content, serverTextField, 1, 0, 1, 1, 1, 0, GBA.H, GBA.C,
				new Insets(5, 5, 5, 5), 0, 0);
		gba.add(content, portLabel, 0, 1, 1, 1, 0, 0, GBA.NONE, GBA.C,
				new Insets(5, 5, 5, 5), 0, 0);
		gba.add(content, portTextField, 1, 1, 2, 1, 1, 0, GBA.H, GBA.C,
				new Insets(5, 5, 5, 5), 0, 0);

		gba.add(content, usernameLabel, 0, 2, 1, 1, 0, 0, GBA.NONE, GBA.C,
				new Insets(5, 5, 5, 5), 0, 0);
		gba.add(content, usernameTextField, 1, 2, 2, 1, 1, 0, GBA.H, GBA.C,
				new Insets(5, 5, 5, 5), 0, 0);

		gba.add(content, cancelButton, 0, 3, 1, 1, 0, 0, GBA.NONE, GBA.W,
				new Insets(5, 5, 5, 5), 0, 0);
		gba.add(content, okButton, 1, 3, 1, 1, 0, 0, GBA.NONE, GBA.E,
				new Insets(5, 5, 5, 5), 0, 0);
		//	setResizable(false);
		serverTextField.grabFocus();
		getRootPane().setDefaultButton(okButton);
		pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screenSize.width - getSize().width) / 2,
				(screenSize.height - getSize().height) / 2);
		show();
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

