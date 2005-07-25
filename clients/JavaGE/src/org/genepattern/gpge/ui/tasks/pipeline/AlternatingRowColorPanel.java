package org.genepattern.gpge.ui.tasks.pipeline;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.genepattern.gpge.ui.maindisplay.GroupPanel;

import com.jgoodies.forms.debug.FormDebugUtils;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Draws a border around each task and an alternating color grid around the task
 * parameters.
 * 
 * @author Karsten Lentzsch
 * @author jgould
 * 
 */
public class AlternatingRowColorPanel extends JPanel {

	private Color gridColor = new Color(239, 239, 255);

	private List togglePanelList;

	public AlternatingRowColorPanel(FormLayout layout) {
		setLayout(layout);
		togglePanelList = new ArrayList();
	}

	public void addTask(GroupPanel tp) {
		togglePanelList.add(tp);
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		paintGrid(g);
	}

	private void paintGrid(Graphics g) {
		if (!(getLayout() instanceof FormLayout)) {
			return;
		}
		FormLayout.LayoutInfo layoutInfo = FormDebugUtils.getLayoutInfo(this);
		int left = layoutInfo.getX();
		int width = layoutInfo.getWidth();
		int height = layoutInfo.getHeight();

		g.setColor(gridColor);
		int row = 0;
		int[] rowOrigins = layoutInfo.rowOrigins;

		for (int i = 0; i < togglePanelList.size(); i++) {
			int taskStart = rowOrigins[row];
			g.setColor(gridColor);
			GroupPanel tp = (GroupPanel) togglePanelList.get(i);
			int numParameters = tp.getToggleComponentCount() / 2;
			int tempRow = row + 1;
			if (tp.isExpanded()) {
				for (int j = 0; j < numParameters; j += 2, tempRow += 2) {
					int gridHeight = 0;
					if (rowOrigins.length == (tempRow + 1)) {
						gridHeight = height - rowOrigins[tempRow];
					} else {
						gridHeight = rowOrigins[tempRow + 1]
								- rowOrigins[tempRow];
					}
					g.fillRect(left, rowOrigins[tempRow], width, gridHeight);

				}
			}
			row += numParameters + 1;

			int taskHeight;
			if (rowOrigins.length == row) {
				taskHeight = height - taskStart;
			} else {
				taskHeight = rowOrigins[row] - taskStart;
			}

			g.setColor(Color.LIGHT_GRAY);
			g.drawRect(left, taskStart, width, taskHeight);
		}
	}

}