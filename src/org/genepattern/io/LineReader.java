package org.genepattern.io;
import java.io.BufferedReader;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 *  Keeps track of reading the lines, the total number and number of non blank
 *  lines Also throws an exception if the EOF is reached unexpectedly.
 *
 *@author     kohm
 */
class LineReader {
   /**  the current line */
   private String current;
   /**  the total number of lines read */
   private int total;
   /**  the number of non-blank lines */
   private int numNonBlankLines;
   /**  the Buffered Reader that is used to read a line */
   private BufferedReader reader;
   /**  the expected number of lines or -1 if not known */
   private int expected_row_count = -1;
   /**  if false and reaches EOF then throws Exception */
   private boolean expect_eof = true;
   /**  if true skips blank lines */
   private boolean skipBlankLines = true;

   /**  lines starting with this string are skipped */
   String comment = null;

   public void close() throws IOException {
      reader.close();
   }

   /**
    *  Creates a new instance of LineReader
    *
    *@param  input  the inpust stream
    */
   public LineReader(final InputStream input) {
      this(createReader(input));
   }


   /**
    *  Creates a new instance of LineReader
    *
    *@param  file                       the file to read from
    *@exception  FileNotFoundException  Description of the Exception
    *@throws  FileNotFoundException     if the specified file does not exist
    */
   public LineReader(final File file) throws FileNotFoundException {
      this(createReader(file));
   }


   /**
    *  utility constructor
    *
    *@param  reader  Description of the Parameter
    */
   private LineReader(final BufferedReader reader) {
      this.reader = reader;
   }


   /**
    *  Reads the current line skipping blank ones or ones with all white space
    *
    *@return                the line read in
    *@throws  EOFException  if file no longer exists
    *@throws  IOException   if error occurs during an I/O operation
    */
   public final String readLine() throws EOFException, IOException {
      final BufferedReader reader = this.reader;
      current = reader.readLine();

      total++;
      if(current == null) {
         if(expect_eof) {
            return null;
         } else {
            throw new EOFException("Error: Not expecting to reach end"
                   + " of data at line " + total
                   + ((expected_row_count < 0) ? "!" : "should be "//FIXME count is wrong!
             + (expected_row_count - numNonBlankLines) + " lines more!"));
         }
      }
      current = current;

      if(skipBlankLines && current.length() == 0) {// skip blank lines
         return readLine();
      }

      if(comment != null && current.startsWith(comment)) {
         return readLine();
      }

      numNonBlankLines++;// only counts when line not skipped
      return current;
   }


   /**
    *  Lines starting with the comment string will be skipped. Set to null to
    *  ignore the comment string. The default is null.
    *
    *@param  s  The comment string.
    */
   public void setComment(String s) {
      comment = s;
   }


   /**
    *  creates the reader
    *
    *@param  input  the input stream
    *@return        BufferedReader
    */
   private final static BufferedReader createReader(final InputStream input) {
      return new BufferedReader(new InputStreamReader(input), 32000);
   }


   /**
    *  creates the reader
    *
    *@param  file                    the file to read from
    *@return                         BufferedReader
    *@throws  FileNotFoundException  if the specified file does not exist
    */
   private final static BufferedReader createReader(final File file) throws FileNotFoundException {
      return new BufferedReader(new FileReader(file), 32000);
   }


   /**
    *  sets the expected number of non blank lines
    *
    *@param  expected  the expected line count
    */
   public final void setExpectedNumLines(final int expected) {
      this.expected_row_count = expected;
   }


   /**
    *  gets the current line
    *
    *@return    String the current line
    */
   public final String getCurrentLine() {
      return current;
   }


   /**
    *  gets the total number of lines read one-based
    *
    *@return    the total number of lines blank and otherwise
    */
   public final int getLineNumber() {
      return total;
   }


   /**
    *  get the number of non-blank lines read
    *
    *@return    int, the total number of non-blank lines read
    */
   public final int getLineCount() {
      return numNonBlankLines;
   }


   /**
    *  if set false then if EOF is reached throws EOFException
    *
    *@param  expect_eof  if false then readLine method throws end of file
    *      exception if reached
    */
   protected void setExpectEOF(final boolean expect_eof) {
      this.expect_eof = expect_eof;
   }


   /**
    *  If true skips blank lines. Some parsers at certain sections of the file
    *  format must accept blank lines and this will need to be set to false for
    *  awhile.
    *
    *@param  skip_blank  if true skips blank lines
    */
   public void setSkipBlankLines(final boolean skip_blank) {
      this.skipBlankLines = skip_blank;
   }

}

