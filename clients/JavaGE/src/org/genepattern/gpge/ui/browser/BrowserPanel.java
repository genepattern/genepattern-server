/*
 * BrowserPanel.java
 *
 * Created on June 12, 2002, 7:23 AM
 */

package org.genepattern.gpge.ui.browser;

import java.awt.Frame;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

import org.genepattern.util.Messenger;

//import java.awt.event.ActionListener;
//import javax.swing.event.HyperlinkListener;
/**
 * 
 * @author KOhm
 */
public class BrowserPanel extends javax.swing.JPanel implements Messenger {

	/** Creates new form BrowserPanel */
	public BrowserPanel() {
		this(new Frame(), "");
	}

	/** Construstor for creating a new BrowserPanel with the default title */
	public BrowserPanel(final Frame frame, final String base_title) {
		initComponents();
		this.frame = frame;
		this.base_title = base_title;
		setTitle("");
		((SimpleHtmlViewer) htmlPane).setProgressButtonMessenger(progressBar,
				stopButton, this);
		final ActionListener listener = new ActionListener() {
			public final void actionPerformed(
					final java.awt.event.ActionEvent actionEvent) {
				Object button = actionEvent.getSource();

				if (button == closeButton) {
					frame.setVisible(false);
					frame.dispose();
				} else if (button == backButton) {
					backOne();
				} else if (button == forwardButton) {
					forwardOne();
				} else if (button == stopButton) {
					setMessage("Stopped...");
					//} else if (button == ) {
				}
			}
		};

		backButton.addActionListener(listener);
		forwardButton.addActionListener(listener);
		stopButton.addActionListener(listener);
		closeButton.addActionListener(listener);

		htmlPane.addHyperlinkListener(new HyperlinkListener() {
			public final void hyperlinkUpdate(
					javax.swing.event.HyperlinkEvent event) {
				processHyperlink(event);
			}
		});
		java.awt.Dimension size = java.awt.Toolkit.getDefaultToolkit()
				.getScreenSize();
		size.width = (int) (size.width * .8);
		size.height = (int) (size.height * .8);
		jScrollPane1.setPreferredSize(size);
	}

	/** append the text to the base title */
	private final void setTitle(String text) {
		frame.setTitle(base_title + text);
	}

	/** sets the message label */
	public final void setMessage(String message) {
		messageLabel.setText(message);
	}

	/** sets the text displayed */
	public final void setText(final String text) {
		htmlPane.setText(text);
		setMessage(DONE);
	}

	/** helper method that sets the message if the url is self referenced */
	private final void check(URL url) {
		if (url.sameFile((URL) url_list.get(stack_pos))) {
			setMessage(DONE);
		}
	}

	/**
	 * Loads the file into the display
	 * 
	 * @param file
	 *            the file to load.
	 */
	public void loadFile(File f) {
		try {
			javax.swing.text.Document doc = htmlPane.getDocument();
			java.io.FileReader fr = new java.io.FileReader(f);
			char[] c = new char[4096];
			int charsRead = 0;
			while ((charsRead = fr.read(c)) != -1) {
				doc.insertString(doc.getLength(), new String(c, 0, charsRead),
						null);
			}
			fr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * helper method that sets the message if the two urls, defined by the
	 * indices into the url stack, are the same except for "ref"s
	 */
	private final void check(int i, int j) {
		if (((URL) url_list.get(i)).sameFile((URL) url_list.get(i))) {
			setMessage(DONE);
		}
	}

	/** loads the url into the page */
	private final URL internalLoadURL(final URL url) {
		if (url != null) {
			//boolean done = false;
			try {
				setMessage("[Loading page...  " + url + "]");
				if (url.openConnection().getContentLength() > 0)
					htmlPane.setPage(url);
				setMessage(DONE);
				//done = true;
			} catch (IOException e) {
				System.err.println("Attempted to read a bad URL: " + url
						+ "\nReason:\n" + e);
				//setMessage ("[Error: couldn't get documentation from the web
				// "+url+"]");
				JOptionPane
						.showMessageDialog(this, "[Page could not be loaded "
								+ url + "] \nReason: " + e);
				return null;
				//            } finally {
				//                return (done ? url : null);
			}
		}
		return url;
	}

	/**
	 * loads the url into the page starts fresh; clears the cashe, disable the
	 * forward, and back buttons
	 */
	public final URL loadURL(URL url) {
		//resets stack, position indicator, disables buttons, etc
		forwardButton.setEnabled(false);
		backButton.setEnabled(false);
		stack_pos = -1;
		url_list.clear();
		// load it
		URL link = internalLoadURL(url);
		if (link != null) {
			stack_pos++;
			url_list.add(link);
		}
		return link;
	}

	/** reads from the input stream */
	public final void load(java.io.InputStream in, final Object desc)
			throws java.io.IOException {
		htmlPane.read(in, desc);
	}

	/** link clicked on */
	private final void linkClicked(URL url) {
		check(url);
		URL link = internalLoadURL(url);
		if (link != null) {
			backButton.setEnabled(true);
			forwardButton.setEnabled(false);
			//trim the stack
			for (int i = url_list.size() - 1; i > stack_pos; i--) { // rev loop
				url_list.remove(i);
			}
			stack_pos++;
			url_list.add(link);
		}
	}

	/** back button chosen */
	private final void backOne() {
		check(stack_pos, --stack_pos);
		//stack_pos--;
		URL url = (URL) url_list.get(stack_pos);
		URL link = internalLoadURL(url);

		forwardButton.setEnabled(true);
		if (stack_pos <= 0)
			backButton.setEnabled(false);
	}

	/** forward button chosen */
	private final void forwardOne() {
		check(stack_pos, ++stack_pos);
		//stack_pos++;
		URL url = (URL) url_list.get(stack_pos);
		URL link = internalLoadURL(url);

		backButton.setEnabled(true);
		if (stack_pos >= (url_list.size() - 1))
			forwardButton.setEnabled(false);
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	private void initComponents() {//GEN-BEGIN:initComponents
		java.awt.GridBagConstraints gridBagConstraints;

		backButton = new javax.swing.JButton();
		forwardButton = new javax.swing.JButton();
		stopButton = new javax.swing.JButton();
		jPanel1 = new javax.swing.JPanel();
		closeButton = new javax.swing.JButton();
		jScrollPane1 = new javax.swing.JScrollPane();
		htmlPane = new org.genepattern.gpge.ui.browser.SimpleHtmlViewer();
		messageLabel = new javax.swing.JLabel();
		progressBar = new javax.swing.JProgressBar();

		setLayout(new java.awt.GridBagLayout());

		backButton.setText("Back");
		backButton.setEnabled(false);
		// add(backButton, new java.awt.GridBagConstraints());

		forwardButton.setText("Forward");
		forwardButton.setEnabled(false);
		// add(forwardButton, new java.awt.GridBagConstraints());

		stopButton.setText("Stop");
		stopButton.setEnabled(false);
		//  add(stopButton, new java.awt.GridBagConstraints());

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridwidth = java.awt.GridBagConstraints.RELATIVE;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 1.0;
		add(jPanel1, gridBagConstraints);

		closeButton.setText("Close");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
		add(closeButton, gridBagConstraints);

		jScrollPane1.setViewportView(htmlPane);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		add(jScrollPane1, gridBagConstraints);

		messageLabel.setText("Loading...");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridwidth = 4;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 0.66;
		//  add(messageLabel, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 0.33;
		//  add(progressBar, gridBagConstraints);

	}//GEN-END:initComponents

	/** main for testing */
	public static final void main(String[] args) throws IOException {
		JFrame frame = new JFrame();
		BrowserPanel browser = new BrowserPanel(frame, "Simple HTML Browser");
		frame.getContentPane().add(browser);
		frame.pack();
		frame.show();
		URL url = null;
		if (args.length == 0)
			url = new URL("http://pc11057.wi.mit.edu:8080/gp/index.jsp");
		else
			url = new URL(args[0]);
		browser.loadURL(url);

	}

	/**
	 * The HTML EditorKit will generate hyperlink events if the JEditorPane is
	 * not editable (JEditorPane.setEditable(false); has been called).
	 * 
	 * If HTML frames are embedded in the document, the typical response would
	 * be to change a portion of the current document. The following code
	 * fragment is a possible hyperlink listener implementation, that treats
	 * HTML frame events specially, and simply displays any other activated
	 * hyperlinks.
	 */
	private void processHyperlink(final javax.swing.event.HyperlinkEvent e) {
		//System.out.println("HyperlinkEvent="+e);
		final HyperlinkEvent.EventType type = e.getEventType();
		//System.out.println("type="+type);
		//if (type == HyperlinkEvent.EventType.ACTIVATED || type ==
		// HyperlinkEvent.EventType.ENTERED ) {
		if (type == HyperlinkEvent.EventType.ACTIVATED) {
			JEditorPane pane = (JEditorPane) e.getSource();
			if (e instanceof HTMLFrameHyperlinkEvent) {
				HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent) e;
				HTMLDocument doc = (HTMLDocument) pane.getDocument();
				doc.processHTMLFrameHyperlinkEvent(evt);
			} else {
				URL link = null;
				boolean done = false;
				try {
					link = e.getURL();
					//pane.setPage (link);
					linkClicked(link);
					setTitle(link.toString());
					done = true;
				} catch (RuntimeException rtex) {
					rtex.printStackTrace();
					throw rtex;
				} catch (Error er) {
					er.printStackTrace();
					throw er;
				} finally {
					if (!done)
						JOptionPane.showMessageDialog(this,
								"[Page could not be loaded " + link + "]");
					//t.printStackTrace ();
				}
			}
		} else if (type == HyperlinkEvent.EventType.ENTERED) {
			final URL link = e.getURL();
			if (link != null)
				setMessage(link.toExternalForm());
		} else if (type == HyperlinkEvent.EventType.EXITED) {
			setMessage("");
		}
	}

	//    //ActionListener interface method
	//    
	//    /** button events */
	//    public void actionPerformed (java.awt.event.ActionEvent actionEvent) {
	//        Object button = actionEvent.getSource ();
	//        
	//        if(button == closeButton) {
	//            frame.setVisible (false);
	//            frame.dispose ();
	//        } else if (button == backButton) {
	//            backOne ();
	//        } else if (button == forwardButton) {
	//            forwardOne ();
	//        } else if (button == stopButton) {
	//            setMessage ("Stopped...");
	//            //} else if (button == ) {
	//        }
	//    }

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JScrollPane jScrollPane1;

	private javax.swing.JButton backButton;

	private javax.swing.JButton stopButton;

	private javax.swing.JButton forwardButton;

	private javax.swing.JPanel jPanel1;

	private javax.swing.JLabel messageLabel;

	private javax.swing.JEditorPane htmlPane;

	private javax.swing.JProgressBar progressBar;

	private javax.swing.JButton closeButton;

	// End of variables declaration//GEN-END:variables
	// vars
	/** The done message */
	public static final String DONE = "Done...";

	/** the base title */
	private final String base_title;

	/** the parent */
	private final Frame frame;

	/** the "stack" of URLs */
	private final java.util.List url_list = new java.util.ArrayList();

	/** the current position in the url_list */
	private int stack_pos = 0;

}