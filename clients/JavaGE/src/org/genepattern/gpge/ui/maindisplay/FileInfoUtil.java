package org.genepattern.gpge.ui.maindisplay;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.genepattern.data.matrix.*;
import org.genepattern.io.*;
import org.genepattern.io.expr.*;
import org.genepattern.io.expr.cls.*;
import org.genepattern.io.expr.gct.*;
import org.genepattern.io.expr.res.*;

/**
 *  Displays information about a selected file
 *
 * @author    Joshua Gould
 */
public class FileInfoUtil {
   static NumberFormat numberFormat;
   static OdfSummaryHandler odfSummaryHandler = new OdfSummaryHandler();
   static MyIExpressionDataHandler expressionDataHandler =
         new MyIExpressionDataHandler();

   static {
      numberFormat = NumberFormat.getInstance();
      numberFormat.setMaximumFractionDigits(1);
   }

   static Map extensionToReaderMap;

   static {
      extensionToReaderMap = new HashMap();

      GctParser gctParser = new GctParser();
      gctParser.setHandler(expressionDataHandler);
      extensionToReaderMap.put("gct", gctParser);

      ResParser resParser = new ResParser();
      resParser.setHandler(expressionDataHandler);
      extensionToReaderMap.put("res", resParser);

      ClsReader clsReader = new ClsReader();
      extensionToReaderMap.put("cls", clsReader);

      OdfParser odfParser = new OdfParser();
      odfParser.setHandler(odfSummaryHandler);
      extensionToReaderMap.put("odf", odfParser);
   }



   private static FileInfo _getInfo(String pathname, InputStream is) {
      int dotIndex = pathname.lastIndexOf(".");
      Object parser = null;
      FileInfo fileInfo = new FileInfo();
    
      if(dotIndex != -1) {// see if file has an extension
         String suffix = pathname.substring(dotIndex + 1, pathname.length());
         suffix = suffix.toLowerCase();
         parser = extensionToReaderMap.get(suffix);
         fileInfo.setKind(suffix);
      }
      try {
         if(parser instanceof IExpressionDataParser) {
            ((IExpressionDataParser) parser).parse(is);
           
         } else if(parser instanceof ClsReader) {
            List keyValuePairs = new ArrayList();
            ClsReader clsReader = (ClsReader) parser;
            ClassVector cv = clsReader.read(is);
            keyValuePairs.add(new KeyValuePair("Data Points", String.valueOf(cv.size())));
            keyValuePairs.add(new KeyValuePair("Number of Classes", String.valueOf(cv.getClassCount())));
            fileInfo.setAnnotations(keyValuePairs);
         } else if(parser instanceof OdfParser) {
            ((OdfParser) parser).parse(is);
             
         }
      } catch(EndParseException epe) {// ignore
         if(parser instanceof IExpressionDataParser) {
            fileInfo.setAnnotations(expressionDataHandler.getKeyValuePairs());
         } else if(parser instanceof OdfParser) {
            fileInfo.setKind(odfSummaryHandler.getModel());
            fileInfo.setAnnotations(odfSummaryHandler.getKeyValuePairs());
         }
      } catch(ParseException pe) {
         pe.printStackTrace();
         String message = pe.getMessage();
         fileInfo.addAnnotation(new KeyValuePair("Error", message));
      } catch(IOException ioe) {
         ioe.printStackTrace();
      }
      return fileInfo;
   }


   public static FileInfo getInfo(File file) {
      if(file == null) {
         return null;
      }

      String size = getSize(file.length());
      FileInputStream fis = null;
      try {
         fis = new FileInputStream(file);
         FileInfo fileInfo = _getInfo(file.getName(), fis);
         fileInfo.setSize(size);
         return fileInfo;
      } catch(IOException ioe) {
         ioe.printStackTrace();
      } finally {
         if(fis != null) {
            try {
               fis.close();
            } catch(IOException x) {
            }
         }
      }
      return null;
   }


   public static FileInfo getInfo(URL url, String name) {
      if(url == null) {
         return null;
      }

      InputStream is = null;
      try {
         is = url.openConnection().getInputStream();
         String size = getSize(is.available());
         FileInfo fileInfo = _getInfo(name, is);
         fileInfo.setSize(size);
         return fileInfo;
      } catch(IOException ioe) {
         ioe.printStackTrace();
      } finally {
         if(is != null) {
            try {
               is.close();
            } catch(IOException x) {
            }
         }
      }
      return null;
   }



   private static String getSize(long lengthInBytes) {
      String size = null;
      if(lengthInBytes >= 1073741824) {
         double gigabytes = lengthInBytes / 1073741824.0;
         size = numberFormat.format(gigabytes) + " GB";
      } else if(lengthInBytes >= 1048576) {
         double megabytes = lengthInBytes / 1048576.0;
         size = numberFormat.format(megabytes) + " MB";
      } else {
         size = Math.ceil(lengthInBytes / 1024.0) + " KB";
      }

      return size;
   }



   static class KeyValuePair {
      Object key;
      Object value;


      public KeyValuePair(String key, String value) {
         this.key = key;
         this.value = value;
      }


      public String toString() {
         return key + ": " + value;
      }

   }


   /**
    *  Description of the Class
    *
    * @author    Joshua Gould
    */
   public static class FileInfo {
      private String kind ="";
      private String size = "";
      private List annotations = Collections.EMPTY_LIST;

      public String getSize() {
         return size;
      }
      
      public String getKind() {
         return kind;
      }
      
      public void setSize(String _size) {
         this.size = _size;
         if(this.size==null) {
            this.size = "";
         }
      }
      
      public void setKind(String _kind) {
         this.kind = _kind;
         if(this.kind==null) {
            this.kind = "";
         }
      }
      
      public List getAnnotations() {
         if(annotations == null) {
            return Collections.EMPTY_LIST;
         }
         return annotations;
      }
      
      public void setAnnotations(List list) {
         annotations = list;
      }
      
      void addAnnotation(KeyValuePair pair) {
         if(annotations == Collections.EMPTY_LIST) {
            annotations = new ArrayList();
         }
         annotations.add(pair);
      }
      
      public String toString() {
         return "kind " + kind + " size " + size;  
      }
   }


   private static class MyIExpressionDataHandler implements IExpressionDataHandler {
      int rows, columns;


      public List getKeyValuePairs() {
         List list = new ArrayList();
         list.add(new KeyValuePair("Dimensions", String.valueOf(rows) + "rows x " + String.valueOf(columns) + "columns"));
         return list;
      }


      public void init(int rows, int cols, boolean hasRowDescriptions,
            boolean hasColumnDescriptions, boolean hasCalls)
             throws ParseException {
         this.rows = rows;
         this.columns = columns;
         throw new EndParseException();
      }


      public void data(int i, int j, double d) throws ParseException {
         throw new EndParseException();
      }


      public void call(int i, int j, int call) throws ParseException {
         throw new EndParseException();
      }


      public void columnName(int j, String name) throws ParseException {
         throw new EndParseException();
      }


      public void rowName(int i, String name) throws ParseException {
         throw new EndParseException();
      }


      public void rowDescription(int i, String desc) throws ParseException {
         throw new EndParseException();
      }


      public void columnDescription(int j, String desc) throws ParseException {
         throw new EndParseException();
      }
   }


   private static class EndParseException extends ParseException {
      public EndParseException() {
         super("");
      }
   }


   private static class OdfSummaryHandler implements IOdfHandler {
      String model;
      String rows, columns;
      List values;
      static Set keys;


      public OdfSummaryHandler() {
         values = new ArrayList();
      }

      static {
         keys = new HashSet();
         keys.add("NumFeatures");
         keys.add("GeneName");
         keys.add("NumErrors");
         keys.add("NumCorrect");
      }


      public List getKeyValuePairs() {
        if("Dataset".equals(model)) {
            values.add(new KeyValuePair("Dimensions", rows + "rows x " + columns + "columns"));
         }
         return values;
      }

      public void endHeader() throws ParseException {
         throw new EndParseException();
      }


      public void header(String key, String[] values) throws ParseException {
         if(key.equals("COLUMN_NAMES")) {
            columns = String.valueOf(values.length);
         }
      }


      public String getModel() {
         return model;
      }


      public void header(String key, String value) throws ParseException {
         if(key.equalsIgnoreCase("Model")) {
            model = value;
         } else if(key.equalsIgnoreCase("DataLines")) {
            rows = value;
         } else if(keys.contains(key)) {
            values.add(new KeyValuePair(key, value));
         }
      }


      public void data(int row, int column, String s) throws ParseException {
         throw new EndParseException();
      }

   }

}
