package org.genepattern.modules.ui.graphics;

/**
 * from http://www.jalice.net/vertical.htm *
 */
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.plaf.basic.BasicLabelUI;
import javax.swing.text.View;

public class VerticalLabelUI extends BasicLabelUI {

	private static final VerticalLabelUI verticalabelUI = new VerticalLabelUI();

	private static final Rectangle pI = new Rectangle();

	private static final Rectangle pT = new Rectangle();

	private static final Rectangle pV = new Rectangle();

	private static final Insets pVI = new Insets(5, 5, 5, 5);

	private static final double TOP_DOWN_THETA = Math.PI / 2;//Math.toRadians
															 // (90);

	//FIXME broken !! need bottom up
	private static final double BOTTOM_UP_THETA = -TOP_DOWN_THETA;// +
																  // Math.PI;//Math.toRadians
																  // (270);

	public final void paint(Graphics g, JComponent comp) {
		JLabel lb = (JLabel) comp;
		String text = lb.getText();
		Icon icon = lb.getIcon();

		if ((icon == null) && (text == null)) {
			return;
		}
		Graphics2D g2 = (Graphics2D) g;

		//g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		//                    RenderingHints.VALUE_ANTIALIAS_ON);

		FontMetrics fm = g.getFontMetrics();
		Insets insets = comp.getInsets(pVI);//pVI = comp.getInsets(pVI);

		pV.x = insets.left;
		pV.y = insets.top;
		pV.height = comp.getWidth() - (insets.left + insets.right);
		pV.width = comp.getHeight() - (insets.top + insets.bottom);

		pI.x = pI.y = pI.width = pI.height = 0;
		pT.x = pT.y = pT.width = pT.height = 0;
		//Class BasicLabelUI: call to SwingUtilities.layoutCompoundLabel().
		String clippedText = layoutCL(lb, fm, text, icon, pV, pI, pT);

		//AffineTransform at = g2.getTransform();
		//g2.rotate (BOTTOM_UP_THETA, 0d, (double)-comp.getWidth());
		//        g2.rotate(BOTTOM_UP_THETA);
		//        g2.translate (0d, (double)-comp.getWidth());

		//        g2.rotate (BOTTOM_UP_THETA, (double)-comp.getHeight (), 0d);
		g2.rotate(BOTTOM_UP_THETA);
		g2.translate(-comp.getHeight(), 0);

		//g2.rotate(BOTTOM_UP_THETA, comp.getWidth ()/2, comp.getHeight ()/2);
		//g2.rotate(TOP_DOWN_THETA, comp.getWidth ()/2, comp.getHeight ()/2);

		//        g2.rotate(TOP_DOWN_THETA);
		//        g2.translate(0, -comp.getWidth());

		if (icon != null) {
			icon.paintIcon(comp, g, pI.x, pI.y);
		}

		if (text != null) {
			View v = (View) comp.getClientProperty(BasicHTML.propertyKey);
			if (v != null) {
				v.paint(g, pT);
			} else {
				int textX = pT.x;
				int textY = pT.y + fm.getAscent();

				if (lb.isEnabled()) {
					paintEnabledText(lb, g, clippedText, textX, textY);
				} else {
					paintDisabledText(lb, g, clippedText, textX, textY);
				}
			}
		}

		//g2.setTransform(at);
	}

	/*
	 * These rectangles/insets are allocated once for this shared LabelUI
	 * implementation. Re-using rectangles rather than allocating them in each
	 * getPreferredSize call sped up the method substantially.
	 */
	private static Rectangle iconR = new Rectangle();

	private static Rectangle textR = new Rectangle();

	private static Rectangle viewR = new Rectangle();

	private static Insets viewInsets = new Insets(0, 0, 0, 0);

	public final Dimension getPreferredSize(JComponent comp) {
		Dimension dim = super.getPreferredSize(comp);

		return new Dimension(dim.height, dim.width);
	}

	//    public final Dimension getPreferredSize(JComponent comp) {
	//        JLabel label = (JLabel)comp;
	//        String text = label.getText();
	//        Icon icon = label.getIcon();
	//        Insets insets = label.getInsets(viewInsets);
	//        Font font = label.getFont();
	//
	//        int dx = insets.left + insets.right;
	//        int dy = insets.top + insets.bottom;
	//
	//        if ((icon == null) &&
	//            ((text == null) ||
	//             ((text != null) && (font == null)))) {
	//            return new Dimension(dx, dy);
	//        }
	//        else if ((text == null) || ((icon != null) && (font == null))) {
	//            return new Dimension(icon.getIconWidth() + dx,
	//                                 icon.getIconHeight() + dy);
	//        }
	//        else {
	//            //FontMetrics fm = label.getToolkit().getFontMetrics(font);
	//            FontMetrics fm = label.getFontMetrics(font);
	//
	//            iconR.x = iconR.y = iconR.width = iconR.height = 0;
	//            textR.x = textR.y = textR.width = textR.height = 0;
	//            viewR.x = dx;
	//            viewR.y = dy;
	//            viewR.width = viewR.height = Short.MAX_VALUE;
	//
	//            layoutCL(label, fm, text, icon, viewR, iconR, textR);
	//            int x1 = Math.min(iconR.x, textR.y);
	//            int x2 = Math.max(iconR.x + iconR.width, textR.y + textR.height);
	//            int y1 = Math.min(iconR.y, textR.x);
	//            int y2 = Math.max(iconR.y + iconR.height, textR.x + textR.width);
	//            Dimension rv = new Dimension(x2 - x1, y2 - y1);
	//
	//            rv.width += dx;
	//            rv.height += dy;
	//            return rv;
	//        }
	//    }
	/**
	 * this must return the singleton instance of this class as per the swing
	 * contract It should not affect the superclass by causing
	 * BasicLabelUI.createUI() to return this instance!
	 */
	public static ComponentUI createUI(JComponent c) {
		return verticalabelUI;
	}

	public static void main(String[] args) {

		JFrame f = new JFrame("VerticalExample");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.getContentPane().setLayout(new FlowLayout());

		//ImageIcon icon = new ImageIcon("smallrose.gif");
		JLabel label0 = new JLabel("Rotation", SwingConstants.LEFT);
		JLabel label1 = new JLabel("Horizontal", SwingConstants.LEFT);
		JLabel label2 = new JLabel("Rotation", SwingConstants.LEFT);

		label0.setBackground(Color.red);
		label0.setUI(new VerticalLabelUI());//l.setUI(new
											// VerticalLabelUI(true));
		label0.setBorder(new EtchedBorder());
		f.getContentPane().add(label0);
		f.getContentPane().add(label1);
		f.getContentPane().add(label2);

		System.out.println("VerticalLabelUI=" + label0.getUI());
		System.out.println("labelUI=" + label1.getUI());
		System.out.println("BasicLabelUI=" + label2.getUI());

		f.pack();
		f.show();

		System.out.println("Vertical label size =" + label0.getSize()
				+ " pref=" + label0.getPreferredSize());
		System.out.println("label size =" + label1.getSize() + " pref="
				+ label1.getPreferredSize());
		System.out.println("label size =" + label2.getSize() + " pref="
				+ label2.getPreferredSize());
		System.out.println("This frame's content pane has a size of "
				+ f.getContentPane().getSize());
	}
}