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


package org.genepattern.gpge.ui.maindisplay;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import javax.swing.JDialog;

/**
 *  A JDialog that will be centered on the screen when shown
 *
 * @author    Joshua Gould
 */
public class CenteredDialog extends JDialog {

   /**
    *  Creates a non-modal dialog without a title with the specified Frame as
    *  its owner. If owner is null, a shared, hidden frame will be set as the
    *  owner of the dialog.
    *
    * @param  owner               the Frame from which the dialog is displayed
    * @throws  HeadlessException  if GraphicsEnvironment.isHeadless() returns
    *      true
    */
   public CenteredDialog(Frame owner) {
      super(owner);
   }


   public void show() {
      center();
      super.show();
   }


   public void setVisible(boolean visible) {
      if(visible) {
         center();
      }
      super.setVisible(visible);
   }


   private void center() {
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      setLocation((screenSize.width - getWidth()) / 2, (screenSize.height - getHeight()) / 2);
   }
}
