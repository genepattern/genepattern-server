package org.genepattern.module;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.genepattern.data.expr.IExpressionData;
import org.genepattern.data.matrix.*;
import org.genepattern.io.*;
import org.genepattern.io.expr.*;


/**
 *  A class containing static convenience methods for tasks that are frequently
 *  encountered when writing GenePattern modules
 *
 * @author    Joshua Gould
 */
public class AnalysisUtil {

   private AnalysisUtil() { }


   /**
    *  Prints the given message to the standard error stream and terminates the
    *  currently running Java Virtual Machine. The message of the given
    *  throwable is appended to the message if it is not <code>null</code>
    *
    * @param  message  The message
    * @param  e        The throwable
    */
   public static void exit(String message, Throwable e) {
      String exceptionMessage = e.getMessage();
      if(exceptionMessage != null) {
         message += "\nCause: " + exceptionMessage;
      }
      exit(message);
   }


   /**
    *  Prints the given message to the standard error stream and terminates the
    *  currently running Java Virtual Machine
    *
    * @param  message  The message
    */
   public static void exit(String message) {
      System.err.println(message);
      System.exit(1);
   }


   /**
    *  Reads the cls document at the given pathname. Prints a message to the
    *  standard error stream and terminates the currently running Java Virtual
    *  Machine if an error occurs.
    *
    * @param  pathname  The pathname string
    * @return           The class vector
    */
   public static ClassVector readCls(String pathname) {
      try {
         return IOUtil.readCls(pathname);
      } catch(Exception e) {
         fileReadError(e, pathname);
         return null;
      }
   }


   /**
    *  Gets a list of features at the given file pathname string. Prints a
    *  message to the standard error stream and terminates the currently
    *  running Java Virtual Machine if an error occurs.
    *
    * @param  pathname  The pathname string
    * @return           the feature list
    */
   public static List readFeatureList(String pathname) {
      try {
         return IOUtil.readFeatureList(pathname);
      } catch(Exception e) {
         fileReadError(e, pathname);
         return null;
      }
   }


   /**
    *  Reads the expression data at the given pathname. The type of the
    *  returned object is determined by the expressionDataCreator argument.
    *  Prints a message to the standard error stream and terminates the
    *  currently running Java Virtual Machine if an error occurs.
    *
    * @param  pathname  The file pathname
    * @param  reader    The expression data reader
    * @param  creator   The expression data creator
    * @return           An object containing the expression data
    */
   public static Object readExpressionData(IExpressionDataReader reader, String pathname, IExpressionDataCreator creator) {
      try {
         return reader.read(pathname, creator);
      } catch(Exception e) {
         fileReadError(e, pathname);
         return null;
      }
   }


   /**
    *  Checks to see that the number of columns in the given matrix are equal
    *  to the size of the given class vector. Prints a message to the standard
    *  error stream and terminates the currently running Java Virtual Machine
    *  if the dimensions do not agree.
    *
    * @param  data  The 2-dimensional matrix
    * @param  cv    The class vector
    */
   public static void checkDimensions(DoubleMatrix2D data, ClassVector cv) {
      if(cv.size() != data.getColumnCount()) {
         exit("Number of columns in dataset(" + data.getColumnCount() + ") does not match the number of class assignments(" + cv.size() + ").");
      }
   }


   /**
    *  Checks to see that the number of columns in the given expression data
    *  are equal to the size of the given class vector. Prints a message to the
    *  standard error stream and terminates the currently running Java Virtual
    *  Machine if the dimensions do not agree.
    *
    * @param  data  The expression data
    * @param  cv    The class vector
    */
   public static void checkDimensions(IExpressionData data, ClassVector cv) {
      if(cv.size() != data.getColumnCount()) {
         exit("Number of columns in dataset(" + data.getColumnCount() + ") does not match the number of class assignments(" + cv.size() + ").");
      }
   }


   /**
    *  Parses the odf document at the given pathname using the specified
    *  handler
    *
    * @param  pathname            A pathname string
    * @param  handler             The odf handler
    */

   public static void readOdf(String pathname, IOdfHandler handler) {
      try {
         IOUtil.readOdf(pathname, handler);
      } catch(Exception e) {
         fileReadError(e, pathname);
      }
   }


   /**
    *  Writes the given class vector to a file. Prints a message to the
    *  standard error stream and terminates the currently running Java Virtual
    *  Machine if an error occurs.
    *
    * @param  cv                  The class vector
    * @param  pathname            A pathname string
    * @param  checkFileExtension  Whether the correct file extension will be
    *      added to the pathname if it is not present.
    * @return                     The pathname that the data was saved to
    */
   public static String writeCls(ClassVector cv, String pathname, boolean checkFileExtension) {
      FileOutputStream fos = null;
      try {
         return IOUtil.writeCls(cv, pathname, checkFileExtension);
      } catch(Exception e) {
         fileSaveError(e, pathname);
         return null;
      }
   }


   /**
    *  Writes expression data to a file in the given format. The correct file
    *  extension will be added to the pathname if it is not present. If there
    *  is already a file present at the given pathname, its contents are
    *  discarded. Prints a message to the standard error stream and terminates
    *  the currently running Java Virtual Machine if an error occurs.
    *
    * @param  data                the expression data.
    * @param  formatName          a String containing the informal name of a
    *      format (e.g., "res" or "gct".)
    * @param  pathname            a pathname string
    * @param  checkFileExtension  Whether the correct file extension will be
    *      added to the pathname if it is not present.
    * @return                     The pathname that the data was saved to
    */
   public static String write(IExpressionData data, String formatName, String pathname, boolean checkFileExtension) {
      try {
         return IOUtil.write(data, formatName, pathname, checkFileExtension);
      } catch(IOException ioe) {
         fileSaveError(ioe, pathname);
         return null;
      }
   }


   private static void fileReadError(Exception e, String pathname) {
      String message = "An error occured while reading the file " + getFileName(pathname) + ".";
      String exceptionMessage = e.getMessage();
      if(exceptionMessage != null) {
         message += "\nCause: " + exceptionMessage;
      }
      exit(message);
   }


   private static void fileSaveError(Exception e, String pathname) {
      String msg = "An error occured while attempting to save the file " + getFileName(pathname) + ".";
      String excMsg = e.getMessage();
      if(excMsg != null) {
         msg += "\nCause: " + excMsg;
      }
      exit(msg);
   }


   /**
    *  Gets an expression data reader that can read the document at the given
    *  pathname or <code>null</code> if no reader is found. Prints a message to
    *  the standard error stream and terminates the currently running Java
    *  Virtual Machine if no reader is found or an error occurs.
    *
    * @param  pathname  A pathname string
    * @return           The expression reader
    */
   public static IExpressionDataReader getExpressionReader(String pathname) {
      try {
         IExpressionDataReader reader = IOUtil.getExpressionReader(pathname);
         if(reader == null) {
            exit("Invalid input type: " + getFileName(pathname) + " is not a res, gct, or odf dataset file.");
         }
         return reader;
      } catch(Exception e) {
         fileReadError(e, pathname);
         return null;
      }
   }



   /**
    *  Gets the name of the file at the given pathname. Removes the axis prefix
    *  if necessary.
    *
    * @param  pathname  The pathname string
    * @return           The file name
    */
   public static String getFileName(String pathname) {
      String name = new java.io.File(pathname).getName();
      return name.replaceFirst("Axis[0-9]*axis_", "");
   }
}
