/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/


package org.genepattern.gpge.ui.util;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Graphics;
import java.awt.Insets;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;

import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.ui.maindisplay.GPGE;

public class GUIUtil {
	static JFileChooser fileChooser;

	private GUIUtil() {
	}

	private static String getDialogTitle(String title) {
		if (title == null) {
			title = "GenePattern";
		} else if(!title.equals("GenePattern")) {
			title = "GenePattern - " + title;
		}
		return title;
	}
	
	private static File showFileDialog(int mode, File selectedFile, String title) {
		FileDialog fc = new FileDialog(GenePattern.getDialogParent(), title,
				mode);
		if (selectedFile != null) {
			fc.setDirectory(selectedFile.getPath());
			fc.setFile(selectedFile.getName());
		}
		fc.setModal(true);
		fc.show();
		String f = fc.getFile();
		String directory = fc.getDirectory();
		if (f != null) {
			File file = new File(directory, f);
			return file; // mac os x file chooser asks chooser whether to
							// replace file
		}
		return null;
	}

	public static File showOpenDialog() {
		return GUIUtil.showOpenDialog(null);
	}

	public static File showOpenDialog(String title) {
		title = getDialogTitle(title);
		if (GPGE.RUNNING_ON_MAC) {
			return showFileDialog(FileDialog.LOAD, null, title);
		} else {
			if (fileChooser == null) {
				fileChooser = new JFileChooser();
			}
			fileChooser.setDialogTitle(title);
			if (fileChooser.showOpenDialog(GenePattern.getDialogParent()) == JFileChooser.APPROVE_OPTION) {
				return fileChooser.getSelectedFile();
			}
			return null;
		}
	}

	public static File showSaveDialog() {
		return showSaveDialog(null, "GenePattern");
	}

	public static File showSaveDialog(File selectedFile) {
		return showSaveDialog(selectedFile, "GenePattern");
	}
	
	public static File showSaveDialog(File selectedFile, String title) {
		title = getDialogTitle(title);
		if (GPGE.RUNNING_ON_MAC) {
			return showFileDialog(FileDialog.SAVE, selectedFile, title);
		} else {
			if (fileChooser == null) {
				fileChooser = new JFileChooser();
			}
			fileChooser.setDialogTitle(title);
			fileChooser.setSelectedFile(selectedFile);
			if (fileChooser.showSaveDialog(GenePattern.getDialogParent()) == JFileChooser.APPROVE_OPTION) {
				final File outputFile = fileChooser.getSelectedFile();
				if (!overwriteFile(outputFile)) {
					return null;
				} else {
					return outputFile;
				}
			}
			return null;
		}
	}

	public static boolean overwriteFile(File f) {
		if (!f.exists()) {
			return true;
		}
		String message = "An item named "
				+ f.getName()
				+ " already exists in this location.\nDo you want to replace it with the one that you are saving?";
		if (JOptionPane.showOptionDialog(GenePattern.getDialogParent(),
				message, null, JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE, GenePattern.getIcon(),
				new Object[] { "Replace", "Cancel" }, "Cancel") != JOptionPane.YES_OPTION) {
			return false;
		}
		return true;
	}

	/**
	 * Creates a wrapped border with the given insets
	 * 
	 * @param b
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 * @return
	 */
	public static Border createBorder(final Border b, final int left,
			final int top, final int right, final int bottom) {
		return new javax.swing.border.Border() {
			public Insets getBorderInsets(java.awt.Component c) {
				Insets i = b.getBorderInsets(c);
				if (left >= 0) {
					i.left = left;
				}
				if (top >= 0) {
					i.top = top;
				}
				if (right >= 0) {
					i.right = right;
				}
				if (bottom >= 0) {
					i.bottom = bottom;
				}

				return i;
			}

			public boolean isBorderOpaque() {
				return b.isBorderOpaque();
			}

			public void paintBorder(Component c, Graphics g, int x, int y,
					int width, int height) {
				b.paintBorder(c, g, x, y, width, height);
			}

		};
	}

	/**
	 * Creates a text field that looks like a label. Users can copy and paste
	 * from text fields but not from labels
	 * 
	 * @param s
	 * @param size
	 * @return
	 */
	public static JTextField createLabelLikeTextField(String s, int size) {
		JTextField tf = new JTextField(s, size);
		tf.setEditable(false);
		tf.setOpaque(false);
		tf.setFont(javax.swing.UIManager.getFont("Label.font"));
		tf.setBackground(UIManager.getColor("Label.background"));
		tf.setDisabledTextColor(UIManager.getColor("Label.foreground"));
		tf.setBorder(UIManager.getBorder("Label.border"));
		return tf;
	}

	/**
	 * Creates a text field that looks like a label. Users can copy and paste
	 * from text fields but not from labels
	 * 
	 * @param s
	 * @return
	 */
	public static JTextField createLabelLikeTextField(String s) {
		JTextField tf = new JTextField(s);
		tf.setEditable(false);
		tf.setOpaque(false);
		tf.setFont(javax.swing.UIManager.getFont("Label.font"));
		tf.setBackground(UIManager.getColor("Label.background"));
		tf.setDisabledTextColor(UIManager.getColor("Label.foreground"));
		tf.setBorder(UIManager.getBorder("Label.border"));
		return tf;
	}

	public static JTextArea createWrappedLabel(String s) {
		JTextArea jTextArea = new JTextArea();
		// Set JTextArea to look like JLabel
		jTextArea.setWrapStyleWord(true);
		jTextArea.setLineWrap(true);
		jTextArea.setEnabled(false);
		jTextArea.setEditable(false);
		jTextArea.setOpaque(false);
		jTextArea.setFont(javax.swing.UIManager.getFont("Label.font"));
		jTextArea.setBackground(UIManager.getColor("Label.background"));
		jTextArea.setDisabledTextColor(UIManager.getColor("Label.foreground"));
		jTextArea.setText(s);
		return jTextArea;
	}

	public static boolean showConfirmDialog(String message) {
		return showConfirmDialog(GenePattern.getDialogParent(), "GenePattern",
				message);
	}

	/**
	 * 
	 * @param parent
	 * @param title
	 * @param message
	 * @param text
	 *            array containing 'Yes', 'No', 'Cancel' text
	 * @return
	 */
	public static int showYesNoCancelDialog(Component parent, String title,
			String message, String[] text) {
		if (text.length != 3) {
			throw new IllegalArgumentException("Invalid array length.");
		}
		return JOptionPane.showOptionDialog(parent, message, title,
				JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
				GenePattern.getIcon(), text, text[0]);
	}

	public static boolean showConfirmDialog(Component parent, String title,
			String message) {
		if (JOptionPane.showOptionDialog(parent, message, title,
				JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
				GenePattern.getIcon(), new Object[] { "Yes", "No" }, "Yes") == JOptionPane.YES_OPTION) {
			return true;
		}
		return false;
	}

}
