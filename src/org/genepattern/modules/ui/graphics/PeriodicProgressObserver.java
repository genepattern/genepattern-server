/*
 * AbstractImageProgressListener.java
 *
 * Created on May 8, 2002, 2:11 PM
 */

package org.genepattern.modules.ui.graphics;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Window;

import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.genepattern.util.ProgressObservable;


/**
 * Displays a dialog of progress made.
 * @author  KOhm
 * @version
 */
public class PeriodicProgressObserver implements java.awt.event.ActionListener, java.awt.event.WindowListener {
    /** constructor*/
    public PeriodicProgressObserver(final Component comp, final String message) {
        this(comp, message, 1, true/*one progress bar*/, 100, false/* don't show when the parent does*/);
    }
    /** constructor
     * 
     * @param comp the component that is attached to a frame
     * @param message the text for the dialog's title
     * @param num the number of ProgressObservables
     * @param one_bar show progress with one bar not two
     * @param delay the time in milliseconds between checking on and displaying 
     *        the progress
     * @param show if true then the dialog will appear when the parent becomes 
     *        visible
     */
    public PeriodicProgressObserver(final Component comp, final String message,
                                    final int num, final boolean one_bar, final int delay, final boolean show) {
        this.component      = comp;
        this.parent_showing = comp.isVisible();
        this.one_bar        = one_bar;
        this.show           = show;
        this.monitor        = new JProgressBar(0, 100);
        monitor.setStringPainted(true);
        monitor.setString("This is just a long sentence to help the packing or anything else");
        monitor.setValue(0);
        timer = new javax.swing.Timer(delay /* 500ms = 1/2 sec */, this);
        timer.setInitialDelay(0);
	
        final Frame frame = (comp instanceof Frame) ? (Frame)comp : (Frame)SwingUtilities.getWindowAncestor(comp);
        frame.addWindowListener(this);
        dialog = new JDialog(frame, message);
		  dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        //dialog.setPreferredSize(new Dimension(100, 200));
        final Container container = dialog.getContentPane();
        if( one_bar ) {
            overall_monitor = null;
        } else {
            overall_monitor = new JProgressBar(0, num - 1);
            container.add(overall_monitor, BorderLayout.NORTH);
        }
            
        container.add(monitor, BorderLayout.CENTER);
        //dialog.pack();
        
        // set the limits
        this.observer_index = -1;
        this.max_observer = ( one_bar ) ?  100 / num + 1  :  101;
        this.num_observables = num;
    }
    
    /** adds a ProgressObservable */
    public void addProgressObservable(final ProgressObservable observable) {
        if( observable == null )
            throw new NullPointerException("ProgressObservable cannot be null!");
        observer_index++;
        total = -1;
        this.observable = observable;
        if( !one_bar ) {
            overall_monitor.setValue(observer_index);
            setProgress(monitor.getMinimum()); // reset monitor
        }
        Thread.yield();
        //System.out.println("PeriodicProgressObserver adding "+observer_index+"th ProgressObservable: "+ observable);
    }
    
    /**
     * Reports the approximate degree of completion of the current write
     * call within the associated ImageWriter.
     */
    public final void start() {
        //System.out.println("PeriodicProgressObserver: STARTED");
        comment("Started");
        timer.start();
        //setProgress(1);
        monitor.setValue(1);
        showDialog();
    }
    /** shows the dialog */
    protected void showDialog() {
        if(dialog==null || dialog.isShowing() || !parent_showing )
            return ;
        final Container container = dialog.getOwner();
        dialog.setLocationRelativeTo(container);
        dialog.pack();
        dialog.show();
    }
    
    /** sets information */
    public final void comment(String message) {
        //message = "Comment: "+message;
        //monitor.setNote(message);
        monitor.setString(message);
        //System.out.println(message);
    }
    
    // ActionListener interface method signature
    /**
     * The actionPerformed method in this class
     * is called each time the Timer "goes off".
     */
    public final void actionPerformed(final java.awt.event.ActionEvent evt) {
        //label.setText("total="+total+" observable="+observable+" observer_index="+observer_index+" max_observer="+max_observer);
        //System.out.println("TIMER ACTION total="+total);
        //System.out.println("observable="+observable);
        if (is_closed/*monitor.isCanceled()*/) {
            //System.out.println("CLOSED");
            comment("User canceled");
            finnish();
            //Toolkit.getDefaultToolkit().beep();
            //abort();
        } else {
            if( observable != null ) {
                if( total < 0 ) {
                    total = observable.getTotal();
                    //System.out.println("GOT TOTAL total="+total);
                    if( total >= 0 && !timer.isRunning()) {
                        start();
                        return ;
                    }
                }
                if( total >= 0 ) {
                    final int current = observable.getCurrent();
                    //System.out.println("Got CURRENT: "+ current);
                    if( one_bar ) {
                        setProgress((max_observer * observer_index)
                                    + (max_observer * current) / total );
                    } else {
                        setProgress((max_observer * current) / total);
                    }
                }
            } //else 
              //  System.out.println("Observable is null");
        }
    }
    
    // helper methods
    
    /** stops eveything on this end */
    public final void done() {
        setProgress(100);
        timer.setDelay(1000); // one sec
        is_closed = true;
        //new Exception().printStackTrace();// dump who called
    }
    public void finnish() {
        //System.out.println("PeriodicProgressObserver DONE");
        //monitor.close ();
        dialog.setVisible(false);
        dialog.dispose();
        timer.stop();
        total = -1;
    }
    
    // helpers
    
    /** sets the progress of the monitor */
    protected void setProgress(final int value) {
        //monitor.setProgress(value);
        monitor.setValue(value);
        if( isOverallDone() && value >= monitor.getMaximum() )
            finnish();
        //System.out.println("Setting PROGRESS: "+value);
    }
    /** returns true if the overall progress bar is complete 
     * or the last observable is being monitored
     */
    protected boolean isOverallDone() {
        return ( (observer_index + 1) == num_observables );
    }
    // static methods
    
    /** helper method this should go in some utility class */
    public static final Component getTopParent(Component comp) {
        Component c = comp;
        while( c != null && !(c instanceof Window || c instanceof java.applet.Applet))
            c = c.getParent();
        if(c == null)
            return comp;
        return c;
    }
    
    public void windowActivated(java.awt.event.WindowEvent windowEvent) {
    }
    
    public void windowClosed(java.awt.event.WindowEvent windowEvent) {
    }
    
    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
    }
    
    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {
    }
    
    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {
    }
    
    public void windowIconified(java.awt.event.WindowEvent windowEvent) {
    }
    
    public void windowOpened(java.awt.event.WindowEvent windowEvent) {
        //System.out.println("Notified that the parent is visible");
        parent_showing = true;
        if( show )
            showDialog();
    }
    
    // fields
    /** the parent for the dialog*/
    protected Component component;
    /** true if the parent has been shown */
    protected boolean parent_showing = false;
    /** if true then the dialog will show when the parent shows itself */
    protected boolean show = false;
    /** progress monitor */
    protected final JProgressBar monitor;
    /** overall progress monitor */
    protected final JProgressBar overall_monitor;
    /** the timer */
    private javax.swing.Timer timer;
    /** amount done */
    protected int amount_completed;
    /** the total amount to do */
    protected int total = -1;
    /** the thing to check on */
    protected ProgressObservable observable;
    /** the nth observable to be added */
    protected int observer_index = -1;
    /** Max value for an observable
     * if only one observable then 100
     * two 50
     * three 33
     * etc
     */
    protected final int max_observer;
    /** true if the Dialog was closed */
    private boolean is_closed = false;
    /** the dialog where the progress is displayed */
    private final JDialog dialog;
    /** if true monitors progress with a single progress bar */
    private final boolean one_bar;
    /** the max number of observables expected*/
    private int num_observables;
}
