package org.genepattern.internal;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Hashtable;

/**
 *  RunR is a simple interface that translates a command line into something
 *  that the R interpreter can handle. The goal is to start R running, have it
 *  read in a script, and then begin execution of a particular method in the
 *  script with specified arguments. R is not natively capable of doing this.
 *  So the RunR class invokes R, then feeds it a series of commands via the
 *  standard input stream and copies the results to its own stdout and stderr
 *  output streams.
 *
 * @author    Jim Lerner
 */

public class RunR {

   private RunR() { }


   /**
    *  Invoke the R interpreter, create a few lines of input to feed to the
    *  stdin input stream of R, and spawn two threads that copy stdout and
    *  stderr from R to this process' version of the same.
    *
    * @param  rSourceFile           The full path to the R source file
    * @param  methodName            The name of the method in the R source file
    *      to invoke
    * @param  args                  The command line arguments
    * @param  pathToRHome           The path to R Home
    * @param  captureStandardError  Whether to capture the standard error from
    *      R to System.error
    * @param  captureStandardOut    Whether to capture the standard out stream
    *      from R to System.out
    * @exception  IOException       If an IO error occurs
    */
   public static void execute(String rSourceFile, String methodName,
         String[] args, String pathToRHome, boolean captureStandardError, boolean captureStandardOut) throws IOException {
      String[] commandLine = null;
      if(args!=null) {
         args = (String[]) args.clone();
         for(int i = 0; i < args.length; i++) {
            args[i] = "'" + args[i] + "'";
         }
      }
      /*
          find R_HOME:
          1. pathToRHome
          2. environment variable
          3. assume it is in the path
        */
      String R_HOME = pathToRHome;

      if(R_HOME == null) {
         Hashtable htEnv = getEnv();
         R_HOME = (String) htEnv.get("R_HOME");
      }

      boolean bWindows = System.getProperty("os.name").startsWith("Windows");
      if(bWindows) {
         if(R_HOME == null) {
            // assume Rterm is in path
            commandLine = new String[]{"cmd", "/c", "Rterm", "--no-save", "--quiet", "--slave", "--no-restore"};
         } else {
            commandLine = new String[]{"cmd", "/c", R_HOME + "\\bin\\Rterm", "--no-save", "--quiet", "--slave", "--no-restore"};
         }
      } else {
         if(R_HOME == null) {
            // assume R is in path
            commandLine = new String[]{"R", "--no-save", "--quiet", "--slave", "--no-restore"};
         } else {
            commandLine = new String[]{R_HOME + "/bin/R", "--no-save", "--quiet", "--slave", "--no-restore"};
         }
      }
     // System.out.println(java.util.Arrays.asList(commandLine));
      final Process process = Runtime.getRuntime().exec(commandLine, null, null);
      // create threads to read from the command's stdout and stderr streams
      Thread outputReader = null;
      if(captureStandardOut) {
         outputReader = streamCopier(process.getInputStream(), System.out);
         outputReader.start();
      }
      Thread errorReader = null;
      if(captureStandardError) {
         errorReader = streamCopier(process.getErrorStream(), System.err);
         errorReader.start();
      }

      OutputStream stdin = process.getOutputStream();
      sendCmd(stdin, "source (\"" + fixPath(rSourceFile) + "\")\n");
      sendCmd(stdin, methodName);
      sendCmd(stdin, "(");
      boolean hasQuotes = false;
      for(int i = 0; i < args.length; i++) {
         if(i > 0) {
            sendCmd(stdin, ", ");
         }
         hasQuotes = true;
         sendCmd(stdin, (!hasQuotes ? "\"" : "") + fixPath(args[i]) + (!hasQuotes ? "\"" : ""));
      }
      sendCmd(stdin, ")\n");
      sendCmd(stdin, "q(save=\"no\")\n");
      stdin.close();
      // wait for all output before attempting to send it back to the client
      if(outputReader != null) {
         try {
            outputReader.join();
         } catch(InterruptedException ie) {}
      }
      if(errorReader != null) {
         try {
            errorReader.join();
         } catch(InterruptedException ie) {}
      }
      // the process will be dead by now
      try {
         process.waitFor();
      } catch(InterruptedException ie) {}
   }


   /**
    *  write a string to stdin of the R process
    *
    * @param  command          string to send to R
    * @param  stdin            Description of the Parameter
    * @exception  IOException  Description of the Exception
    */
   protected static void sendCmd(OutputStream stdin, String command)
          throws IOException {
     //    System.out.println(command);
      stdin.write(command.getBytes());
   }


   /**
    *  convert Windows path separators to Unix, which R prefers!
    *
    * @param  path  path to convert to Unix format
    * @return       String path with delimiters replaced
    */
   protected static String fixPath(String path) {
      return path.replace('\\', '/');
   }


   /**
    *  copies one of the output streams from R to this process' output stream
    *
    * @param  is               InputStream to read from (from R)
    * @param  os               PrintStream to write to (stdout of this process)
    * @return                  Description of the Return Value
    * @exception  IOException  Description of the Exception
    */
   protected static Thread streamCopier(InputStream is, PrintStream os)
          throws IOException {
      return new StreamCopier(is, os);
   }


   /**
    *  Here's a tricky/nasty way of getting the environment variables despite
    *  System.getenv() being deprecated. TODO: find a better (no-deprecated)
    *  method of retrieving environment variables in platform-independent
    *  fashion. The environment is used <b>almost</b> as is, except that the
    *  directory of the task's files is added to the path to make execution
    *  work transparently. This is equivalent to the
    *  <libdir> substitution variable. Some of the applications will be
    *  expecting to find their support files on the path or in the same
    *  directory, and this manipulation makes it transparent to them. <p>
    *
    *  Implementation: spawn a process that performs either a "sh -c set" (on
    *  Unix) or "cmd /c set" on Windows.
    *
    * @return    Hashtable of environment variable name/value pairs
    */
   private static Hashtable getEnv() {
      Hashtable envVariables = new Hashtable();
      int i;
      String key;
      String value;
      boolean isWindows = System.getProperty("os.name").startsWith("Windows");
      BufferedReader in = null;

      try {
         Process getenv = Runtime.getRuntime().exec(isWindows ? "cmd /c set" : "sh -c set");
         in = new BufferedReader(new InputStreamReader(getenv.getInputStream()));
         String line;
         while((line = in.readLine()) != null) {
            i = line.indexOf("=");
            if(i == -1) {
               continue;
            }
            key = line.substring(0, i);
            value = line.substring(i + 1);
            envVariables.put(key, value);
         }
      } catch(IOException ioe) {
         System.err.println(ioe);
      } finally {
         if(in != null) {
            try {
               in.close();
            } catch(IOException ioe) {}
         }
      }
      return envVariables;
   }


   /**
    * @author    Jim Lerner
    */
   static class StreamCopier extends Thread {
      InputStream is = null;
      PrintStream os = null;


      public StreamCopier(InputStream is, PrintStream os) {
         this.is = is;
         this.os = os;
         this.setDaemon(true);
      }


      public void run() {
         BufferedReader in = new BufferedReader(new InputStreamReader(is));
         String line;

         try {
            while((line = in.readLine()) != null) {
               if(line.startsWith("> ") || line.startsWith("+ ")) {
                  continue;
               }
               os.print(line);

               boolean bNeedsBreak = (line.length() > 0 && (line.indexOf("<") == -1 || line.indexOf("<-") != -1));
               if(bNeedsBreak) {
                  os.println("\n");
               }
            }
         } catch(IOException ioe) {
            System.err.println(ioe + " while reading from process stream");
         }
      }
   }
}

