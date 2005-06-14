package org.genepattern.gpge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Properties;


/**
 * Manages ~/gp/gp.properties file
 * 
 * @author Joshua Gould
 */
public class PropertyManager {
   private static Properties properties=new Properties();
   private static Properties defaults=new Properties();
   private static File propertiesFile;
   private static File propertiesFileTemp;

   private PropertyManager() {
   }

   static {
      String fs=File.separator;
      String path=System.getProperty("user.home") + fs + "gp" + fs;
      propertiesFile=new File(path + "gp.properties");
      propertiesFileTemp=new File(path + "#save#gp.properties#");
      createPropertiesFile();
      try {
         FileInputStream in=new FileInputStream(propertiesFile);
         loadProperties(properties, in);
      } catch(IOException ioe) {
         ioe.printStackTrace();
      }
   }

   private static void createPropertiesFile() {
      if(!propertiesFile.exists()) {
         File parent=propertiesFile.getParentFile();
         if(!parent.exists()) {
            parent.mkdirs();
         }
         try {
            propertiesFile.createNewFile();
         } catch(IOException ioe) {
            ioe.printStackTrace();
         }
      }
   }

   public static void saveProperties()
                              throws IOException {
      // FIXME make backup of properties in case the following fails
      FileOutputStream out=new FileOutputStream(propertiesFileTemp);
      saveProperties(out);
      propertiesFile.delete();
      propertiesFileTemp.renameTo(propertiesFile);
   }

   private static void saveProperties(OutputStream out)
                               throws IOException {
      createPropertiesFile();
      properties.store(out, "GenePattern properties");
      out.close();
   }

   public static String getProperty(String name) {
      String value=properties.getProperty(name);
      if(value!=null) {
         return value;
      } else {
         return getDefaultProperty(name);
      }
   }

   /**
    * Sets the given property. The change is not persisted until
    * saveProperties is called. If <tt>value</tt> is <tt>null</tt>, then the 
    * default value for the given property is used.
    * 
    * @param name the property name
    * @param value the property value
    */
   public static void setProperty(String name, String value) {
      if(value==null) {
         value = getDefaultProperty(name);
      }
      properties.setProperty(name, value);
   }

   public static String getDefaultProperty(String name) {
      return defaults.getProperty(name);
   }

   public static void setDefaultProperty(String name, String value) {
      defaults.setProperty(name, value);
   }

   private static void loadProperties(Properties into, InputStream in)
                               throws IOException {
      try {
         into.load(in);
      } finally {
         in.close();
      }
   }
}