/*
 * NoModalDialog.java
 *
 * Created on April 15, 2003, 11:36 AM
 */

package org.genepattern.modules.ui.graphics;

import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.Icon;
import java.awt.Component;

/**
 *
 * @author  kohm
 */
public class NoModalDialog extends JOptionPane {
    
    /** Creates a new instance of NoModalDialog */
    public NoModalDialog(Object message, int messageType, int optionType, Icon icon, Object[] options, Object initialValue) {
        super(message, messageType, optionType, icon, options, initialValue);
    }
    /**
     * Brings up a modal dialog with a specified icon, where the initial
     * choice is dermined by the <code>initialValue</code> parameter and
     * the number of choices is determined by the <code>optionType</code>
     * parameter.
     * <p>
     * If <code>optionType</code> is YES_NO_OPTION, or YES_NO_CANCEL_OPTION
     * and the <code>options</code> parameter is <code>null</code>,
     * then the options are
     * supplied by the Look and Feel.
     * <p>
     * <p>
     * The <code>messageType</code> parameter is primarily used to supply
     * a default icon from the Look and Feel.
     *
     * @param parentComponent determines the <code>Frame</code>
     *                  in which the dialog is displayed;  if
     *                  <code>null</code>, or if the
     *                  <code>parentComponent</code> has no
     *                  <code>Frame</code>, a
     *                  default <code>Frame</code> is used
     * @param message   the <code>Object</code> to display
     * @param title     the title string for the dialog
     * @param optionType an integer designating the options available on the
     *                  dialog: YES_NO_OPTION, or YES_NO_CANCEL_OPTION
     * @param messageType an integer designating the kind of message this is,
     *                  primarily used to determine the icon from the
     *                  pluggable
     *                  Look and Feel: ERROR_MESSAGE, INFORMATION_MESSAGE,
     *                  WARNING_MESSAGE, QUESTION_MESSAGE, or PLAIN_MESSAGE
     * @param icon      the icon to display in the dialog
     * @param options   an array of objects indicating the possible choices
     *                  the user can make; if the objects are components, they
     *                  are rendered properly; non-<code>String</code>
     *                  objects are
     *                  rendered using their <code>toString</code> methods;
     *                  if this parameter is <code>null</code>,
     *                  the options are determined by the Look and Feel.
     * @param initialValue the object that represents the default selection
     *                     for the dialog
     * @return an integer indicating the option chosen by the user,
     *         or CLOSED_OPTION if the user closed the Dialog
     */
    public static int showOptionDialog(Component parentComponent, Object message,
                                       String title, int optionType,
                                       int messageType, Icon icon,
                                       Object[] options, Object initialValue) {
        JOptionPane             pane = new JOptionPane(message, messageType,
                                                       optionType, icon,
                                                       options, initialValue);
        JDialog dialog = pane.createDialog(parentComponent, title);
        pane.selectInitialValue();
        dialog.show();

        Object        selectedValue = pane.getValue();

        if(selectedValue == null)
            return CLOSED_OPTION;
        if(options == null) {
            if(selectedValue instanceof Integer)
                return ((Integer)selectedValue).intValue();
            return CLOSED_OPTION;
        }
        for(int counter = 0, maxCounter = options.length;
            counter < maxCounter; counter++) {
            if(options[counter].equals(selectedValue))
                return counter;
        }
        return CLOSED_OPTION;
    }
}
