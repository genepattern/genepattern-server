/*
 * ReportErrorAbstractAction.java
 *
 * Created on January 25, 2002, 3:59 PM
 */

package org.genepattern.modules.ui.listeners;


import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;

import org.genepattern.util.SafeRun;


/**
 * This class catches any errors that occur and displays them
 * @author  KOhm
 * @version 
 */
public abstract class ReportErrorAbstractAction extends AbstractAction {
    
    /** Creates new ReportErrorAbstractAction */
    protected ReportErrorAbstractAction(final String name){
        super(name);
        //System.out.println("DEAA name "+name);
    }
    /** Creates new ReportErrorAbstractAction  with a name and 
     * an accelerator based an the specified character.
     * The resulting accelerator will be a combination stroke of the char and 
     * the platform dependent modifier key.  For example on windows platforms
     * the standard way to copy a selection is to simultainiously press control+C.
     * The modifier control is automatically determined on a per-platform basis.
     *
     */
    protected ReportErrorAbstractAction(final String name, final char accel) {
        this(name);
        setAccelerator(accel);
    }
    /** Creates new ReportErrorAbstractAction */
    protected ReportErrorAbstractAction(final String name, final Icon icon) {
        super(name, icon);
    }
    /** Creates new ReportErrorAbstractAction  with a name, icon and 
     * an accelerator based an the specified character.
     * The resulting accelerator will be a combination stroke of the char and 
     * the platform dependent modifier key.  For example on windows platforms
     * the standard way to copy a selection is to simultainiously press control+C.
     * The modifier control is automatically determined on a per-platform basis.
     *
     */
    protected ReportErrorAbstractAction(final String name, final Icon icon, final char accel) {
        this(name, icon);
        setAccelerator(accel);
    }
    
    /**
     * subclasses will implement this method to know when an event occured
     * This is pertty much the only method that the developer needs to bother with
     */
    protected abstract void doAction(ActionEvent actionEvent) throws Throwable;
    
    /** final version so subclasses cannot override */
    public final void actionPerformed (ActionEvent e) {
        this.event = e;
        safe.run(); // causes runIt() to be called which calls doAction()...
    }
    /**
     * helper method sets the accelerator key
     * Note the key should originate from KeyEvent.VK_whatever
     */
    protected final void setAccelerator(final char key) {
        KeyStroke key_stroke = KeyStroke.getKeyStroke(key, MENU_SHORTCUT_MASK);
        this.putValue(Action.ACCELERATOR_KEY, key_stroke);
    }
    
    // fields
    /** the shortcut key mask */
    public static final int MENU_SHORTCUT_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    /** the event */
    private ActionEvent event;
    /** the class that properly takes care of exceptions */
    private final SafeRun safe = new SafeRun () {
        protected final void runIt () throws Throwable {
            doAction (event);
        }
        protected final void before () {}
        protected final void after ()  {}
        protected final void error ()  {}
    };
    
}
