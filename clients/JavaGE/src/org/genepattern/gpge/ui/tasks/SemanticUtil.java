package org.genepattern.gpge.ui.tasks;

import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.StringTokenizer;
import org.genepattern.gpge.ui.menu.*;
import org.genepattern.io.*;
import org.genepattern.webservice.*;
import org.genepattern.util.GPConstants;


/**
 *  Utility methods for semantic information
 *
 * @author    Joshua Gould
 */
public class SemanticUtil {

  
   private SemanticUtil() { }
   
   public static String getType(File file) {
      String name = file.getName();
      int dotIndex = name.lastIndexOf(".");
      String extension = null;
      if(dotIndex > 0) {
         extension = name.substring(dotIndex+1, name.length());
      }
      if(extension.equalsIgnoreCase("odf")) {
         OdfParser parser = new OdfParser();
         OdfHandler handler = new OdfHandler();
         FileInputStream fis = null;
         parser.setHandler(handler);
         try {
            fis = new FileInputStream(file);
            parser.parse(fis);
         } catch(Exception e) {  
         } finally {
            try {
               if(fis!=null) {
                  fis.close();  
               }
            } catch(IOException x){}
         }
         return handler.model;
      } else {
         return extension.toLowerCase(); 
      }
   }
   
   
   
   private static class OdfHandler implements IOdfHandler {
      String model;
      
      public void endHeader() throws ParseException {
         throw new ParseException("");
      }

      public void header(String key, String[] values) throws ParseException {
         
         
      }

      public void header(String key, String value) throws ParseException {
         if(key.equals("Model")) {
            model = value;
            throw new ParseException("");
         }
      }

      public void data(int row, int column, String s) throws ParseException {
         throw new ParseException("");
      }
   }
   
  
   
   public static Map getInputTypeToMenuItemsMap(Map inputTypeToModulesMap, final AnalysisServiceDisplay analysisServiceDisplay) {
      Map inputTypeToMenuItemMap = new HashMap();
      for(Iterator it = inputTypeToModulesMap.keySet().iterator(); it.hasNext(); ) {
         String type = (String) it.next();
         List modules = (List) inputTypeToModulesMap.get(type);
         if(modules==null) {
            continue;  
         }
         MenuItemAction[] m = new MenuItemAction[modules.size()];
         for(int i = 0; i < modules.size(); i++) {
            final AnalysisService svc = (AnalysisService) modules.get(i);
            m[i] = new MenuItemAction(svc.getTaskInfo().getName()) {
               public void actionPerformed(ActionEvent e) {
                  analysisServiceDisplay.loadTask(svc);     
               }
            };
         }
         inputTypeToMenuItemMap.put(type, m);
      }
      return inputTypeToMenuItemMap;
   }
   
   /**
   * Gets a map which maps the input type as a string to a list of analysis services that take that input type as an input parameter
   */
    public static Map getInputTypeToModulesMap(Collection analysisServices) {
       Map map = new HashMap();
       for(Iterator it = analysisServices.iterator(); it.hasNext(); ) {
          AnalysisService svc = (AnalysisService) it.next();
          addToInputTypeToModulesMap(map, svc);
       }
       return map;
    }


   private static void addToInputTypeToModulesMap(Map map, AnalysisService svc) {
      TaskInfo taskInfo = svc.getTaskInfo();
      ParameterInfo[] p = taskInfo.getParameterInfoArray();
      if(p != null) {
         for(int i = 0; i < p.length; i++) {
            if(p[i].isInputFile()) {
               ParameterInfo info = p[i];
               String fileFormatsString = (String) info.getAttributes().get(GPConstants.FILE_FORMAT);
               if(fileFormatsString==null || fileFormatsString.equals("")) {
                  continue;  
               }
               StringTokenizer st = new StringTokenizer(fileFormatsString, GPConstants.PARAM_INFO_CHOICE_DELIMITER);
               while(st.hasMoreTokens()) {
                  String type = st.nextToken();
                  List modules = (List) map.get(type);
                  if(modules == null) {
                     modules = new ArrayList();
                     map.put(type, modules);
                  }
                  modules.add(svc);
               }
            }
         }
      }
   }

}
