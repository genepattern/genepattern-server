package org.genepattern.module;
import java.awt.Component;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import javax.swing.JOptionPane;

import org.genepattern.data.expr.IExpressionData;
import org.genepattern.data.matrix.*;
import org.genepattern.io.*;
import org.genepattern.io.expr.*;


/**
 *  A class containing static convenience methods for tasks that are frequently
 *  encountered when writing GenePattern visualizers
 *
 * @author    Joshua Gould
 */
public class VisualizerUtil {

   private VisualizerUtil() { }


   /**
    *  Brings up an error message dialog displaying the given message . The
    *  message of the given throwable is appended to the message if it is not
    *  <code>null</code>
    *
    * @param  parentComponent  determines the <code>Frame</code> in which the
    *      dialog is displayed; if <code>null</code>, or if the <code>parentComponent</code>
    *      has no <code>Frame</code>, a default <code>Frame</code> is used
    * @param  message          The message
    * @param  e                The throwable
    */
   public static void error(Component parentComponent, String message, Throwable e) {
      String exceptionMessage = e.getMessage();
      if(exceptionMessage != null) {
         message += "\nCause: " + exceptionMessage;
      }
      error(parentComponent, message);
   }


   /**
    *  Brings up an error message dialog
    *
    * @param  parentComponent  determines the <code>Frame</code> in which the
    *      dialog is displayed; if <code>null</code>, or if the <code>parentComponent</code>
    *      has no <code>Frame</code>, a default <code>Frame</code> is used
    * @param  message          The message
    */
   public static void error(Component parentComponent, String message) {
      JOptionPane.showMessageDialog(parentComponent, message, "Error", JOptionPane.ERROR_MESSAGE);
   }


   /**
    *  Reads the cls document at the given pathname. Brings up an error message
    *  dialog if an error occurs.
    *
    * @param  parentComponent  determines the <code>Frame</code> in which the
    *      dialog is displayed; if <code>null</code>, or if the <code>parentComponent</code>
    * @param  pathname         The pathname string
    * @return                  The class vector
    */
   public static ClassVector readCls(Component parentComponent, String pathname) {
      try {
         return IOUtil.readCls(pathname);
      } catch(Exception e) {
         fileReadError(e, parentComponent, pathname);
         return null;
      }
   }


   /**
    *  Gets a list of features at the given file pathname string. Brings up an
    *  error message dialog if an error occurs.
    *
    * @param  parentComponent  determines the <code>Frame</code> in which the
    *      dialog is displayed; if <code>null</code>, or if the <code>parentComponent</code>
    * @param  pathname         The pathname string
    * @return                  the feature list
    */
   public static List readFeatureList(Component parentComponent, String pathname) {
      try {
         return IOUtil.readFeatureList(pathname);
      } catch(Exception e) {
         fileReadError(e, parentComponent, pathname);
         return null;
      }
   }


   /**
    *  Reads the expression data at the given pathname. The type of the
    *  returned object is determined by the expressionDataCreator argument.
    *  Brings up an error message dialog if an error occurs.
    *
    * @param  parentComponent  determines the <code>Frame</code> in which the
    *      dialog is displayed; if <code>null</code>, or if the <code>parentComponent</code>
    * @param  reader           The expression data reader
    * @param  pathname         The file pathname
    * @param  creator          The expression data creator
    * @return                  An object containing the expression data
    */
   public static Object readExpressionData(Component parentComponent, IExpressionDataReader reader, String pathname, IExpressionDataCreator creator) {
      try {
         return reader.read(pathname, creator);
      } catch(Exception e) {
         fileReadError(e, parentComponent, pathname);
         return null;
      }
   }


   /**
    *  Parses the odf document at the given pathname using the specified
    *  handler
    *
    * @param  parentComponent     determines the <code>Frame</code> in which
    *      the dialog is displayed; if <code>null</code>, or if the <code>parentComponent</code>
    * @param  pathname            A pathname string
    * @param  handler             The odf handler
    * @return                     <code>true</code> if the odf document was
    *      read successfully
    */

   public static boolean readOdf(Component parentComponent, String pathname, IOdfHandler handler) {
      try {
         IOUtil.readOdf(pathname, handler);
         return true;
      } catch(Exception e) {
         fileReadError(e, parentComponent, pathname);
         return false;
      }
   }



   /**
    *  Writes the given class vector to a file. Brings up an error message
    *  dialog if an error occurs.
    *
    * @param  parentComponent     determines the <code>Frame</code> in which
    *      the dialog is displayed; if <code>null</code>, or if the <code>parentComponent</code>
    * @param  cv                  The class vector
    * @param  pathname            A pathname string
    * @param  checkFileExtension  Whether the correct file extension will be
    *      added to the pathname if it is not present.
    * @return                     The pathname that the data was saved to
    */
   public static String writeCls(Component parentComponent, ClassVector cv, String pathname, boolean checkFileExtension) {
      FileOutputStream fos = null;
      try {
         return IOUtil.writeCls(cv, pathname, checkFileExtension);
      } catch(Exception e) {
         fileSaveError(e, parentComponent, pathname);
         return null;
      }
   }


   /**
    *  Writes expression data to a file in the given format. The correct file
    *  extension will be added to the pathname if it is not present. If there
    *  is already a file present at the given pathname, its contents are
    *  discarded. Brings up an error message dialog if an error occurs.
    *
    * @param  parentComponent     determines the <code>Frame</code> in which
    *      the dialog is displayed; if <code>null</code>, or if the <code>parentComponent</code>
    * @param  data                the expression data.
    * @param  formatName          a String containing the informal name of a
    *      format (e.g., "res" or "gct".)
    * @param  pathname            a pathname string
    * @param  checkFileExtension  Whether the correct file extension will be
    *      added to the pathname if it is not present.
    * @return                     The pathname that the data was saved to
    */
   public static String write(Component parentComponent, IExpressionData data, String formatName, String pathname, boolean checkFileExtension) {
      try {
         return IOUtil.write(data, formatName, pathname, checkFileExtension);
      } catch(IOException ioe) {
         fileSaveError(ioe, parentComponent, pathname);
         return null;
      }
   }



   private static void fileReadError(Exception e, Component parentComponent, String pathname) {
      String message = "An error occured while reading the file " + AnalysisUtil.getFileName(pathname) + ".";
      String exceptionMessage = e.getMessage();
      if(exceptionMessage != null) {
         message += "\nCause: " + exceptionMessage;
      }
      error(parentComponent, message);
   }


   private static void fileSaveError(Exception e, Component parentComponent, String pathname) {
      String msg = "An error occured while attempting to save the file " + AnalysisUtil.getFileName(pathname) + ".";
      String excMsg = e.getMessage();
      if(excMsg != null) {
         msg += "\nCause: " + excMsg;
      }
      error(parentComponent, msg);
   }


   /**
    *  Gets an expression data reader that can read the document at the given
    *  pathname or <code>null</code> if no reader is found. Brings up an error
    *  message dialog if an error occurs.
    *
    * @param  parentComponent  determines the <code>Frame</code> in which the
    *      dialog is displayed; if <code>null</code>, or if the <code>parentComponent</code>
    * @param  pathname         A pathname string
    * @return                  The expression reader
    */
   public static IExpressionDataReader getExpressionReader(Component parentComponent, String pathname) {
      try {
         IExpressionDataReader reader = IOUtil.getExpressionReader(pathname);
         if(reader == null) {
            error(parentComponent, "Invalid input type: " + AnalysisUtil.getFileName(pathname) + " is not a res, gct, or odf dataset file.");
         }
         return reader;
      } catch(Exception e) {
         fileReadError(e, parentComponent, pathname);
         return null;
      }
   }

}
