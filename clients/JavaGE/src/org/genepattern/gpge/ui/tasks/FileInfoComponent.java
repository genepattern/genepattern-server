package org.genepattern.gpge.ui.tasks;

import java.io.IOException;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JLabel;
import org.genepattern.io.*;

import org.genepattern.gpge.ui.maindisplay.FileInfoUtil;

/**
 * Displays information about a selected file
 * 
 * @author Joshua Gould
 */
public class FileInfoComponent extends JPanel {
	JLabel nameLabel = new JLabel("   ");
   JLabel sizeLabel = new JLabel("   ");
   JLabel extraLabel = new JLabel("   ");
   
	public FileInfoComponent() {
      setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      setLayout(new GridLayout(3, 1));
      add(nameLabel);
      add(sizeLabel);
      add(extraLabel);
      java.awt.Dimension size = getPreferredSize();
      size.height += 10;
      setPreferredSize(size);
	}

	public void setText(String name, FileInfoUtil.FileInfo fileInfo) {
      nameLabel.setText("Name: " + name);
      if(fileInfo!=null) { 
         sizeLabel.setText("Size: " + fileInfo.getSize());
         if(fileInfo.getAnnotation()!=null) {
            extraLabel.setText(fileInfo.getAnnotation() + "");
         } else {
            extraLabel.setText("");
         }
        
      } else {
          sizeLabel.setText("Size: ");
          extraLabel.setText("");
      }
	}

   public void clear() {
      nameLabel.setText("");
		sizeLabel.setText("");
      extraLabel.setText("");
   }
   
	
}