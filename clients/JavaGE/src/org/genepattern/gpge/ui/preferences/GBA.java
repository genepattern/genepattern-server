/*
 * Copyright @ 1999-2003, The Institute for Genomic Research (TIGR). All rights
 * reserved.
 */

package org.genepattern.gpge.ui.preferences;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.Insets;

public final class GBA {
	public final static int B = GridBagConstraints.BOTH;

	public final static int C = GridBagConstraints.CENTER;

	public final static int E = GridBagConstraints.EAST;

	public final static int H = GridBagConstraints.HORIZONTAL;

	public final static int NONE = GridBagConstraints.NONE;

	public final static int N = GridBagConstraints.NORTH;

	public final static int NE = GridBagConstraints.NORTHEAST;

	public final static int NW = GridBagConstraints.NORTHWEST;

	public final static int RELATIVE = GridBagConstraints.RELATIVE;

	public final static int REMAINDER = GridBagConstraints.REMAINDER;

	public final static int S = GridBagConstraints.SOUTH;

	public final static int SE = GridBagConstraints.SOUTHEAST;

	public final static int SW = GridBagConstraints.SOUTHWEST;

	public final static int V = GridBagConstraints.VERTICAL;

	public final static int W = GridBagConstraints.WEST;

	private static GridBagConstraints c = new GridBagConstraints();

	public void add(Container container, Component component, int x, int y,
			int width, int height) {
		c.gridx = x;
		c.gridy = y;
		c.gridwidth = width;
		c.gridheight = height;
		c.weightx = 0;
		c.weighty = 0;
		c.fill = GBA.NONE;
		c.anchor = GBA.C;
		c.insets = new Insets(0, 0, 0, 0);
		c.ipadx = 0;
		c.ipady = 0;
		container.add(component, c);
	}

	public void add(Container container, Component component, int x, int y,
			int width, int height, int weightx, int weighty, int fill,
			int anchor) {
		c.gridx = x;
		c.gridy = y;
		c.gridwidth = width;
		c.gridheight = height;
		c.weightx = weightx;
		c.weighty = weighty;
		c.fill = fill;
		c.anchor = anchor;
		c.insets = new Insets(0, 0, 0, 0);
		c.ipadx = 0;
		c.ipady = 0;
		container.add(component, c);
	}

	public void add(Container container, Component component, int x, int y,
			int width, int height, int weightx, int weighty, int fill,
			int anchor, Insets insets, int ipadx, int ipady) {
		c.gridx = x;
		c.gridy = y;
		c.gridwidth = width;
		c.gridheight = height;
		c.weightx = weightx;
		c.weighty = weighty;
		c.fill = fill;
		c.anchor = anchor;
		c.insets = insets;
		c.ipadx = ipadx;
		c.ipady = ipady;
		container.add(component, c);
	}
}

