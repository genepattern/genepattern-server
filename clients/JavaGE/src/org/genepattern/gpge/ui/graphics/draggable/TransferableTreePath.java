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

import javax.swing.tree.TreePath;

/**
 * This represents a TreePath (a node in a JTree) that can be transferred
 * between a drag source and a drop target.
 */
public class TransferableTreePath implements Transferable {
	// The type of DnD object being dragged...
	public static final DataFlavor TREEPATH_FLAVOR = new DataFlavor(
			DataFlavor.javaJVMLocalObjectMimeType, "TreePath");

	private TreePath _path;

	private DataFlavor[] _flavors = { TREEPATH_FLAVOR };

	/**
	 * Constructs a transferrable tree path object for the specified path.
	 */
	public TransferableTreePath(TreePath path) {
		_path = path;
	}

	// Transferable interface methods...
	public DataFlavor[] getTransferDataFlavors() {
		return _flavors;
	}

	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return java.util.Arrays.asList(_flavors).contains(flavor);
	}

	public synchronized Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException {
		if (flavor.isMimeTypeEqual(TREEPATH_FLAVOR.getMimeType())) // DataFlavor.javaJVMLocalObjectMimeType))
			return _path;
		else
			throw new UnsupportedFlavorException(flavor);
	}

}

