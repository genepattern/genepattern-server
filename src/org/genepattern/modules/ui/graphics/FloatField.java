package org.genepattern.modules.ui.graphics;

import javax.swing.*; 
import javax.swing.text.*; 

import java.awt.Toolkit;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
/** 
 * Allows the user to only enter a whole number.  This doesn't not need to be used 
 * in Java 1.4 or latter.  
 *
 * @author kohm
 * @see FormatTextTest
 * @see javax.swing.JFormattedTextField
 */


public class FloatField extends JTextField {
    /** constructor */
    public FloatField(final Number value, final int columns) {
        this(value.floatValue(), columns);
    }
    /** constructor */
    public FloatField(final float value, final int columns) {
        this(value, columns, NumberFormat.getNumberInstance());
    }
    /** constructor */
    public FloatField(final float value, final int columns, final NumberFormat format) {
        super(new ChangeValidatedDocument(format), null, columns);
        floatFormatter = format;
        format.setParseIntegerOnly(false);
        format.setGroupingUsed(false);
        setFloat(value);
    }
    /** gets the float value */
    public final float getFloat() {
        float retVal = 0;
        try {
            retVal = floatFormatter.parse(super.getText()).floatValue();
        } catch (ParseException e) {
            // This should never happen because insertString allows
            // only properly formatted data to get in the field.
            toolkit.beep();
            throw new IllegalStateException("Somehow the value is not a number: '"+super.getText()+"'");
        }
        return retVal;
    }
    /** gets the text without any formatting*/
    public String getText() {
        return String.valueOf(getFloat());
    }
    /** get the Number object */
    public final Number getValue() {
        return new Float(getFloat());
    }
    /**
     * sets the currently displayed number
     * The object must be an instance of Number.
     *
     * @throws ClassCastException if the object is not an instance of Number 
     */
    public final void setValue(final Object number) {
        final Number n = (Number)number;
        setFloat(n.floatValue());
    }
    public final void setFloat(final float value) {
        super.setText(floatFormatter.format(value));
    }
    /** overriden now will throw number format exception if text s not a number */
    public final void setText(final String text) {
        if( text == null)
            super.setText("");
        else { 
            float value = 0.0f;
            final String trimmed = text.trim();
            if( trimmed.length() > 0 )
                value = Float.parseFloat(trimmed);
            super.setText(floatFormatter.format(value));
        }
            
    }
        
    // fields
    /** needed to make a "beep" */
    private final Toolkit toolkit = Toolkit.getDefaultToolkit();
    /** number formatter */
    private final NumberFormat floatFormatter;
    
//    /** test it */
//    public static final void main(String[] args) {
//      javax.swing.JFrame frame = new javax.swing.JFrame("Test IntegerField for Java 1.3");
//
//      // Float-only TextField
//      FloatField field = new FloatField(0f, 5);
//      frame.getContentPane().add(new javax.swing.JLabel("Enter numbers"), "North");
//      frame.getContentPane().add(field, "South");
//      frame.pack();
//      frame.show();
//
//   }
}
