package org.genepattern.internal;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.genepattern.data.expr.*;
import org.genepattern.data.matrix.*;
import org.genepattern.io.*;


/**
 *  Reads in multi class data. Format is: <pre>
 *Feature name 1\tClass one\tClass two...
 *Feature name 2\tClass one\tClass two...
 *...................
 *</pre>
 *
 * @author    Joshua Gould
 */
public class MultiClassReader {

   public ClassVector[] read(String pathname) throws IOException, ParseException {
      return read(pathname, null);
   }


   public ClassVector[] read(String pathname, ExpressionData expressionData) throws IOException, ParseException {
      BufferedReader br = null;
      int columns = 0;
      List rows = new ArrayList();
      boolean firstTime = true;
      try {
         br = new BufferedReader(new FileReader(pathname));
         String s = null;

         while((s = br.readLine()) != null) {
            if(s.trim().equals("")) {
               continue;
            }
            String[] tokens = s.split("\t");

            if(firstTime) {
               columns = tokens.length;
               firstTime = false;
            }
            if(columns != tokens.length) {
               throw new ParseException("Expecting " + columns + " values. Found " + tokens.length + " values.");
            }
            rows.add(tokens);
         }

      } finally {
         if(br != null) {
            br.close();
         }
      }

      String[][] values = new String[columns - 1][rows.size()];
      Map sampleName2RowMap = new HashMap();
      for(int i = 0; i < rows.size(); i++) {
         String[] rowData = (String[]) rows.get(i);
         sampleName2RowMap.put(rowData[0], new Integer(i));
         for(int j = 1; j < rowData.length; j++) {
            values[j - 1][i] = rowData[j];
         }
      }
      ClassVector[] cv = new ClassVector[columns - 1];
      for(int i = 0; i < values.length; i++) {
         cv[i] = new ClassVector(values[i]);
      }
      if(expressionData != null) {
         int[] indices = new int[cv[0].size()];
         for(int i = 0; i < expressionData.getColumnCount(); i++) {
            String sampleName = expressionData.getColumnName(i);
            Integer rowInteger = (Integer) sampleName2RowMap.get(sampleName);
            if(rowInteger == null) {
               throw new ParseException(sampleName + " not found in class file.");
            }
            indices[i] = rowInteger.intValue();
         }
         for(int i = 0; i < values.length; i++) {
            cv[i] = cv[i].slice(indices);
         }
      }
      return cv;
   }

}
