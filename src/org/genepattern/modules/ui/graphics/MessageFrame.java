/*
 * This software and its documentation are copyright 1999 by the
 * Whitehead Institute for Biomedical Research.  All rights are reserved.
 * 
 * This software is made available for use on a case by case basis, and
 * only with specific written permission from The Whitehead Institute.
 * It may not be redistributed nor posted to any bulletin board, included
 * in any shareware distributions, or the like, without specific written
 * permission from The Whitehead Institute.  This code may be customized
 * by individual users, although such versions may not be redistributed
 * without specific written permission from The Whitehead Institute.
 * 
 * This software is supplied without any warranty or guaranteed support
 * whatsoever.  The Whitehead Institute can not be responsible for its
 * use, misuse, or functionality.
 *
 */

package org.genepattern.modules.ui.graphics;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

import org.genepattern.data.*;
//import edu.mit.genome.gp.GenePattern;

public class MessageFrame extends JFrame
{
    JTextArea taMsgs = new JTextArea();
    JScrollPane spMsgs;

    public MessageFrame (String title) 
    {
	org.genepattern.util.AbstractReporter.getInstance().logWarning("constructing MessageFrame");
	Container cp = getContentPane();

	setTitle(title);
	setResizable(true);

	taMsgs.setEditable(false);
	taMsgs.setWrapStyleWord(false);
	taMsgs.setLineWrap(false);

	spMsgs = new JScrollPane(taMsgs);
	spMsgs.setPreferredSize(new Dimension(480,320));
	setSize(new Dimension(480,320));

      cp.setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.anchor = GridBagConstraints.CENTER;
      gbc.fill   = GridBagConstraints.BOTH;
      gbc.insets = new Insets(5,3,5,3);
      gbc.weightx = 1.0;
      gbc.weighty = 1.0;
      cp.add(spMsgs, gbc);
      //cp.add(spMsgs, new GBCWrapper(0,0,1,1,1.0,1.0,java.awt.GridBagConstraints.CENTER,java.awt.GridBagConstraints.BOTH,new Insets(5,3,5,3),0,0));

      //pack();
    }

    public void appendMsg(String s) {
	taMsgs.append(s);
    }

    public void showFrame() {
	setVisible(true);
    }

    public void hideFrame() {
	setVisible(false);
    }
}

