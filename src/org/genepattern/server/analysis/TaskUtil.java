package org.genepattern.server.analysis;
import java.io.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.genepattern.server.analysis.genepattern.GenePatternAnalysisTask;
import org.genepattern.util.GPConstants;


/**
 *  Utility class for GenePattern task zip files.
 *
 * @author    Joshua Gould
 */
public class TaskUtil {

   private TaskUtil() { }


   /**
    *  Returns <code>true</code> if the given file is a zip of zips, <code>false</code>
    *  otherwise.
    *
    * @param  file             A zip file.
    * @return                  whether the given file is a zip of zips.
    * @exception  IOException  if an error occurs during reading
    */
   public static boolean isZipOfZips(File file) throws IOException {
      ZipFile zipFile = new ZipFile(file);
      if(zipFile.getEntry(GPConstants.MANIFEST_FILENAME) != null) {
         return false;
      }

      // is it a zip of zips?
      ZipEntry zipEntry = null;
      long fileLength = 0;
      int numRead = 0;
      for(Enumeration eEntries = zipFile.entries(); eEntries.hasMoreElements(); ) {
         zipEntry = (ZipEntry) eEntries.nextElement();
         if(!zipEntry.getName().toLowerCase().endsWith(".zip")) {
            return false;
         }
      }
      // if we get here, the zip file contains only other zip files
      return true;
   }



   /**
    *  Creates a new array of<code>TaskInfo</code> instances from the given zip
    *  of zips. The 0th index of the returned array holds the TaskInfo for the
    *  pipeline itself. Note that the returned <code>TaskInfo</code> instances
    *  have getID() equal to -1, getParameterInfo() will be <code>null</code>,
    *  getUserId is <code>null</code> , and getAccessId is 0.
    *
    * @param  zipf             a zip of zips.
    * @return                  The an array of TaskInfo objects
    * @exception  IOException  if an error occurs during reading
    */
   public static TaskInfo[] getTaskInfosFromZipOfZips(File zipf) throws IOException {
      List taskInfoList = new ArrayList();
      ZipFile zipFile = new ZipFile(zipf);

      for(Enumeration eEntries = zipFile.entries(); eEntries.hasMoreElements(); ) {
         ZipEntry zipEntry = (ZipEntry) eEntries.nextElement();
         if(!zipEntry.getName().endsWith(".zip")) {
            throw new IOException("not a GenePattern zip-of-zips file");
         }
         InputStream is = null;
         OutputStream os = null;
         File outFile = null;
         ZipFile subZipFile = null;
         try {
            is = zipFile.getInputStream(zipEntry);
            // there is no way to create a ZipFile from an input stream, so every file within the stream must be extracted before it can be processed
            outFile = new File(System.getProperty("java.io.tmpdir"), zipEntry.getName());

            os = new FileOutputStream(outFile);
            long fileLength = zipEntry.getSize();
            long numRead = 0;
            byte[] buf = new byte[100000];
            int i;
            while((i = is.read(buf, 0, buf.length)) > 0) {
               os.write(buf, 0, i);
               numRead += i;
            }

            if(numRead != fileLength) {
               throw new IOException("only read " + numRead + " of " + fileLength + " bytes in " + zipf.getName() + "'s " + zipEntry.getName());
            }
            subZipFile = new ZipFile(outFile);
            ZipEntry manifestEntry = subZipFile.getEntry(GPConstants.MANIFEST_FILENAME);
            taskInfoList.add(getTaskInfoFromManifest(subZipFile.getInputStream(manifestEntry)));
         } finally {
            if(os != null) {
               try {
                  os.close();
               } catch(IOException x) {}
            }
            if(is != null) {
               try {
                  is.close();
               } catch(IOException x) {}
            }
            if(subZipFile != null) {
               try {
                  subZipFile.close();
               } catch(IOException x) {}
            }
            if(outFile != null) {

               outFile.delete();

            }
         }

      }
      return (TaskInfo[]) taskInfoList.toArray(new TaskInfo[0]);
   }


   /**
    *  Creates a new <code>TaskInfo</code> instance from the given zip file.
    *  Note that the returned <code>TaskInfo</code> will have getID() equal to
    *  -1, getParameterInfo() will be <code>null</code>, getUserId is <code>null</code>
    *  , and getAccessId is 0.
    *
    * @param  file             a <code>File</code> to read from. The file must
    *      be a GenePattern task zip file. Zip of zips is not supported.
    * @return                  the new <code>TaskInfo</code> instance
    * @exception  IOException  if an error occurs during reading
    */

   public static TaskInfo getTaskInfoFromZip(File file) throws IOException {
      ZipFile zipFile = null;
      try {
         zipFile = new ZipFile(file);
         ZipEntry manifestEntry = zipFile.getEntry(org.genepattern.util.IGPConstants.MANIFEST_FILENAME);
         if(manifestEntry == null) {
            zipFile.close();
            throw new IOException(file.getName() + " is missing a GenePattern manifest file.");
         }
         return getTaskInfoFromManifest(zipFile.getInputStream(manifestEntry));
      } finally {
         if(zipFile != null) {
            zipFile.close();
         }
      }
   }


   /**
    *  Creates a new <code>TaskInfo</code> instance from the file. Note that
    *  the returned <code>TaskInfo</code> will have getID() equal to -1,
    *  getParameterInfo() will be <code>null</code>, getUserId is <code>null</code>
    *  , and getAccessId is 0.
    *
    * @param  manifestFile     a <code>File</code> to read from
    * @return                  the new <code>TaskInfo</code> instance
    * @exception  IOException  if an error occurs during reading
    */
   public static TaskInfo getTaskInfoFromManifest(File manifestFile) throws IOException {

      FileInputStream fis = null;
      try {
         fis = new FileInputStream(manifestFile);
         return getTaskInfoFromManifest(fis);
      } finally {
         if(fis != null) {
            fis.close();
         }
      }

   }


   /**
    *  Creates a new <code>TaskInfo</code> instance from the input stream. Note
    *  that the returned <code>TaskInfo</code> will have getID() equal to -1,
    *  getParameterInfo() will be <code>null</code>, getUserId is <code>null</code>
    *  , and getAccessId is 0.
    *
    * @param  manifestURL      a <code>URL</code> to read from
    * @return                  the new <code>TaskInfo</code> instance
    * @exception  IOException  if an error occurs during reading
    */
   public static TaskInfo getTaskInfoFromManifest(java.net.URL manifestURL) throws IOException {
      InputStream is = null;
      try {
         is = manifestURL.openStream();
         return getTaskInfoFromManifest(is);
      } finally {
         if(is != null) {
            is.close();
         }
      }
   }


   /**
    *  Creates a new <code>TaskInfo</code> instance from the input stream. Note
    *  that the returned <code>TaskInfo</code> will have getID() equal to -1,
    *  getParameterInfo() will be <code>null</code>, getUserId is <code>null</code>
    *  , and getAccessId is 0.
    *
    * @param  is               input stream to a manifest file
    * @return                  the new <code>TaskInfo</code> instance
    * @exception  IOException  if an error occurs during reading
    */
   public static TaskInfo getTaskInfoFromManifest(java.io.InputStream is) throws IOException {
      Properties props = new Properties();
      props.load(is);

      String taskName = (String) props.remove(GenePatternAnalysisTask.NAME);
      String lsid = (String) props.get(GenePatternAnalysisTask.LSID);
      org.genepattern.util.LSID l = new org.genepattern.util.LSID(lsid);//; throw MalformedURLException if this is a bad LSID
      if(taskName == null || taskName.trim().equals("")) {
         throw new IOException("Missing task name");
      }
      // FIXME add check for other required attributes
      String taskDescription = (String) props.remove(GenePatternAnalysisTask.DESCRIPTION);

      // ParameterInfo entries consist of name/value/description triplets, of which the value and description are optional
      // It is assumed that the names are p[1-n]_name, p[1-n]_value, and p[1-n]_description
      // and that the numbering runs consecutively.  When there is no p[m]_name value, then there are m-1 ParameterInfos


      List vParams = new ArrayList();

      for(int i = 1; i <= GenePatternAnalysisTask.MAX_PARAMETERS; i++) {
         String name = (String) props.remove("p" + i + "_name");
         if(name == null) {
            continue;
         }
         if(name == null || name.length() == 0) {
            throw new IOException("missing parameter name for " + "p" + i + "_name");
         }
         String value = (String) props.remove("p" + i + "_value");
         if(value == null) {
            value = "";
         }
         String description = (String) props.remove("p" + i + "_description");
         if(description == null) {
            description = "";
         }
         ParameterInfo pi = new ParameterInfo(name, value, description);
         HashMap attributes = new HashMap();
         for(int attribute = 0; attribute < GenePatternAnalysisTask.PARAM_INFO_ATTRIBUTES.length; attribute++) {
            name = (String) GenePatternAnalysisTask.PARAM_INFO_ATTRIBUTES[attribute][0];
            value = (String) props.remove("p" + i + "_" + name);
            if(value != null) {
               attributes.put(name, value);
            }
            if(name.equals(GenePatternAnalysisTask.PARAM_INFO_TYPE[0]) && value != null && value.equals(GenePatternAnalysisTask.PARAM_INFO_TYPE_INPUT_FILE)) {
               attributes.put(ParameterInfo.MODE, ParameterInfo.INPUT_MODE);
               attributes.put(ParameterInfo.TYPE, ParameterInfo.FILE_TYPE);
            }
         }

         for(Enumeration p = props.propertyNames(); p.hasMoreElements(); ) {
            name = (String) p.nextElement();
            if(!name.startsWith("p" + i + "_")) {
               continue;
            }
            value = (String) props.remove(name);
            name = name.substring(name.indexOf("_") + 1);
            attributes.put(name, value);
         }

         if(attributes.size() > 0) {
            pi.setAttributes(attributes);
         }
         vParams.add(pi);
      }
      ParameterInfo[] params = (ParameterInfo[]) vParams.toArray(new ParameterInfo[0]);

      // all remaining properties are assumed to be TaskInfoAttributes
      TaskInfoAttributes tia = new TaskInfoAttributes();
      for(Enumeration eProps = props.propertyNames(); eProps.hasMoreElements(); ) {
         String name = (String) eProps.nextElement();
         String value = props.getProperty(name);
         tia.put(name, value);
      }
      TaskInfo task = new TaskInfo(-1, taskName, taskDescription, null, GenePatternAnalysisTask.class.getName(), tia);
      task.setParameterInfoArray(params);
      return task;
   }
}
