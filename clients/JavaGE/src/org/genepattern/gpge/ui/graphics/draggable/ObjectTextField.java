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


package org.genepattern.gpge.ui.graphics.draggable;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.io.BufferedReader;
import java.io.File;
import java.util.List;

import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.genepattern.gpge.ui.tasks.Sendable;

/**
 * 
 * @author kohm
 */
public class ObjectTextField extends JTextField {

	/** normal border */
	private Border normal_border = getBorder();

	/** good drop border */
	private Border good_border = new EtchedBorder(java.awt.Color.darkGray,
			java.awt.Color.green);

	/** Creates new ObjectTextField */
	public ObjectTextField() {
		this(null, 10);
	}

	/** Creates new ObjectTextField */
	public ObjectTextField(int cols) {
		this(null, cols);
	}

	/** Creates new ObjectTextField */
	public ObjectTextField(final String text, final int cols) {
		super(text, cols);
		DropTarget dropTarget = new DropTarget(this,
				new ObjectDropTargetListener());
		dropTarget.setDefaultActions(DnDConstants.ACTION_COPY_OR_MOVE);
	}

	/** sets the object and it's text representation */
	public void setObject(final Object object) {
		if (object instanceof Sendable) {
			setText(((Sendable) object).toUIString());
		} else if (object instanceof java.net.URL) {
			java.net.URL url = (java.net.URL) object;
			if ("file".equals(url.getProtocol())) {
				java.io.File file = new java.io.File(url.getFile());
				setText(file.getPath());
			} else {
				setText(url.toString());
			}
		} else {
			setText(object.toString());
		}
	}

	// helpers that cause visual change depending on if the drop target is ok or
	// not
	/** indicates normal */
	protected void indicateNormal() {
		setBorder(normal_border);
	}

	/** indicates good drop */
	protected void indicateGoodDrop() {
		setBorder(good_border);
	}

	class ObjectDropTargetListener implements java.awt.dnd.DropTargetListener {
		protected DataFlavor[] ok_flavors = null;

		protected DataFlavor textFlavor = null;

		public final void dragEnter(final DropTargetDragEvent e) {
			if (!isDragAcceptable(e)) {
				e.rejectDrag();
			} else {
				e.acceptDrag(e.getDropAction());
				indicateGoodDrop();
			}
		}

		public final void dragExit(final DropTargetEvent e) {
			indicateNormal();
		}

		public final void dragOver(final DropTargetDragEvent e) {

		}

		public final void dropActionChanged(final DropTargetDragEvent e) {
			if (!isDragAcceptable(e))
				e.rejectDrag();
			else
				e.acceptDrag(e.getDropAction());
		}

		public final void drop(final DropTargetDropEvent e) {
			if (!isDropAcceptable(e)) {
				e.rejectDrop();
				indicateNormal();
				return;
			}

			e.acceptDrop(e.getDropAction());

			final Transferable transferable = e.getTransferable();

			DataFlavor[] flavors = transferable.getTransferDataFlavors();
			final int limit = flavors.length;
			boolean dropComplete = false;

			for (int i = 0; i < limit; i++) {
				final DataFlavor flavor = flavors[i];
				try {
					if (flavor
							.isMimeTypeEqual(DataFlavor.javaJVMLocalObjectMimeType)) {
						final TreePath pathSource = (TreePath) transferable
								.getTransferData(flavor);
						final Object last = pathSource.getLastPathComponent();

						if (last instanceof DefaultMutableTreeNode) {
							final DefaultMutableTreeNode node = (DefaultMutableTreeNode) last;
							setObject(node);
							dropComplete = true;
							break;
						} else {
							System.err.println("Note couldn't handle class "
									+ last);
						}
					} else if (flavor
							.isMimeTypeEqual(DataFlavor.javaFileListFlavor
									.getMimeType())) {
						final List files = (List) transferable
								.getTransferData(flavor);
						final File file = (File) files.get(0);
						setObject(file);
						dropComplete = true;
						break;
					} else if (flavor.isMimeTypeEqual(DataFlavor.stringFlavor
							.getMimeType())) {
						String text = ((String) transferable
								.getTransferData(flavor)).trim();
						text = new java.util.StringTokenizer(text, System
								.getProperty("line.separator")).nextToken(); // XXX
						// jgould
						// hack
						// for
						// netscape
						// 7.1
						// on
						// windows
						try {
							setObject(new java.net.URL(text));
							dropComplete = true;
							break;
						} catch (java.net.MalformedURLException ex) {
							System.err.println("Original text='" + text + "'");
							System.err.println("not a URL " + ex.getMessage());
						}
					} else if (flavor.isMimeTypeEqual(textFlavor.getMimeType())) {
						BufferedReader br = new BufferedReader(flavor
								.getReaderForText(transferable));

						String text = br.readLine().trim();
						br.close();
						try {
							setObject(new java.net.URL(text));
							dropComplete = true;
							break;
						} catch (java.net.MalformedURLException ex) {
							System.err.println("Original text='" + text + "'");
							System.err.println("not a URL " + ex.getMessage());
						}
					}
				} catch (UnsupportedFlavorException ufe) {
					System.out.println(ufe);
				} catch (java.io.IOException ioe) {
					System.out.println(ioe);
				}
			}

			e.dropComplete(dropComplete);
			indicateNormal();
		}

		public final boolean isDragAcceptable(final DropTargetDragEvent e) {

			// Only accept particular flavors
			if (!isDataFlavorSupported(e))
				return false;
			return true;
		}

		public final boolean isDropAcceptable(final DropTargetDropEvent e) {
			// Only accept particular flavors
			if (!isDataFlavorSupported(e)) {
				return false;
			}
			return true;
		}

		/** determines if the DropTargetDropEvent will support one of the flavors */
		protected final boolean isDataFlavorSupported(
				final DropTargetDropEvent e) {
			final int limit = getOkFlavors().length;
			for (int i = 0; i < limit; i++) {
				if (e.isDataFlavorSupported(getOkFlavors()[i]))
					return true;
			}

			if (e.isDataFlavorSupported(DataFlavor.stringFlavor)
					|| e.isDataFlavorSupported(textFlavor)) {
				return true;
			}
			if (e
					.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor)) {
				return true;
			}

			return false;
		}

		/** determines if the DropTargetDragEvent will support one of the flavors */
		protected final boolean isDataFlavorSupported(
				final DropTargetDragEvent e) {
			final int limit = getOkFlavors().length;
			for (int i = 0; i < limit; i++) {
				if (e.isDataFlavorSupported(getOkFlavors()[i]))
					return true;
			}
			if (e.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				return true;
			}
			return e
					.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
		}

		/**
		 * all the supported data flavors note String Flavor is supported
		 * differently since it is assumes that it actually is a URL
		 */
		protected DataFlavor[] getOkFlavors() {
			if (ok_flavors == null) {
				try {
					textFlavor = new DataFlavor(
							"text/plain; class=java.io.InputStream");
					ok_flavors = new DataFlavor[] { TransferableTreePath.TREEPATH_FLAVOR };
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
			return ok_flavors;
		}
	}

}