package org.genepattern.util;

import java.io.*;
import org.genepattern.data.expr.*;

import org.genepattern.data.matrix.*;

public class MatlabUtil {

	public MatlabUtil(){
	}


	public IExpressionData asExpressionData(int rows, int columns, String[] rowNames, String[] rowDescriptions, String[] columnNames, String[] colDescriptions, double[][] data) {
		
		DoubleMatrix2D mat = new DoubleMatrix2D(rows, columns);
		for (int i=0; i < rows; i++){
			mat.setRowName(i, rowNames[i]);
			for (int j=0; j < columns; j++){
				if (i == 0){
					mat.setColumnName(i, columnNames[i]);
				}
				mat.set(i,j, data[i][j]);

			} 	

		} 		
		
		ExpressionData edata = new ExpressionData(mat, rowDescriptions, colDescriptions);
		
		return edata;
	}


}