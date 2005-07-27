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

	private List taskList;

	private boolean showGrid = true;

	private static class Task {
		GroupPanel groupPanel;
		int parameterCount;
		
		Task(GroupPanel p, int count) {
			this.groupPanel = p;
			this.parameterCount = count;
		}
	}
	public AlternatingRowColorPanel(FormLayout layout) {
		setLayout(layout);
		taskList = new ArrayList();
	}

	public void addTask(GroupPanel tp, int parameterCount) {
		taskList.add(new Task(tp, parameterCount));
	}

	public void setShowGrid(boolean b) {
		showGrid  = b;
	}
	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		paintGrid(g);
	}

	public void removeAll() {
		super.removeAll();
		this.taskList.clear();
	}
	
	private void paintGrid(Graphics g) {
		if (!(getLayout() instanceof FormLayout)) {
			return;
		}
		FormLayout.LayoutInfo layoutInfo = ((FormLayout) getLayout()).getLayoutInfo(this);
	//	FormDebugUtils.dumpRowGroups((FormLayout) getLayout());
		int left = layoutInfo.getX();
		int width = layoutInfo.getWidth();
		int height = layoutInfo.getHeight();

		g.setColor(gridColor);
		int rowIndex = 0;
		int[] rowOrigins = layoutInfo.rowOrigins;

		for (int i = 0; i < taskList.size(); i++) {
			int taskStartIndex = rowIndex;
			g.setColor(gridColor);
			Task task = (Task) taskList.get(i);
			int numParameters = task.parameterCount;
			int parameterRowIndex = rowIndex + 1;
			if (showGrid && task.groupPanel.isExpanded()) {
				int tempRow = parameterRowIndex;
				int gridHeight;
				if(rowOrigins.length==(tempRow + 1)) {
					gridHeight =  rowOrigins[rowOrigins.length-1] - rowOrigins[tempRow]; 
				} else {
					gridHeight = rowOrigins[tempRow + 1] - rowOrigins[tempRow];
				}
										
				for (int j = 0; j < numParameters; j += 2, tempRow += 2) {
					g.fillRect(left, rowOrigins[tempRow], width, gridHeight);
				}
			}
			rowIndex = parameterRowIndex + numParameters;
			int taskEndIndex = rowIndex;
			
			int taskHeight;
			if (rowOrigins.length == (taskEndIndex+1)) {
				taskHeight = height - layoutInfo.rowOrigins[taskStartIndex]-1;
			} else {
				taskHeight = rowOrigins[taskEndIndex] - layoutInfo.rowOrigins[taskStartIndex];
			}
			
			g.setColor(Color.LIGHT_GRAY);
			g.drawRect(left, layoutInfo.rowOrigins[taskStartIndex], width, taskHeight);
			//g.drawString("" + taskStartIndex, left, layoutInfo.rowOrigins[taskStartIndex]);
		}
	}
	
	  /**
     * Paints the form's grid lines and diagonals.
     * 
     * @param g    the Graphics object used to paint
     */
    private void paintDebugGrid(Graphics g) {
        if (!(getLayout() instanceof FormLayout)) {
            return;
        }
        FormLayout.LayoutInfo layoutInfo = FormDebugUtils.getLayoutInfo(this);
        int left   = layoutInfo.getX();
        int top    = layoutInfo.getY();
        int width  = layoutInfo.getWidth();
        int height = layoutInfo.getHeight();

        g.setColor(Color.black);
  
        // Paint the column bounds.
    //    for (int col = 0; col < layoutInfo.columnOrigins.length; col++) {
      //      g.fillRect(layoutInfo.columnOrigins[col], top, 1, height);
       // }

        // Paint the row bounds.
        for (int row = 0; row < layoutInfo.rowOrigins.length; row++) {
            g.fillRect(left, layoutInfo.rowOrigins[row], width, 1);
            g.drawString("" + row, left, layoutInfo.rowOrigins[row]);
        }
        
      //  if (paintDiagonals) {
        //    g.drawLine(left, top,          left + width, top + height);
          //  g.drawLine(left, top + height, left + width, top);
        //}
    }

}