package edu.mit.broad.gp.ws;
import junit.framework.*;
import org.genepattern.webservice.*;
/**
 *  Test the GPServer class
 *
 * @author    Joshua Gould
 */
public class TestGPServer extends TestWebService {
   
    final static String ALL_AML_TRAIN = "ftp://ftp.broad.mit.edu/pub/genepattern/all_aml/all_aml_train.res";


   public TestGPServer(String name) {
      super(name);
   }


  /* public void testConcurrentSubmissions() {
     List threads = new ArrayList();
      for(int i = 0; i < 10; i++) {
            Thread t = new Thread() {
               public void run() {
                  JobResult r = runPreprocess(new Parameter[]{new Parameter("input.filename", ALL_AML_TRAIN)});
                  assertTrue(r.hasStandardError() == false);
                  assertTrue(r.getOutputFileNames().length == 1);

               }
            };
            t.start();
            threads.add(t);
      }
      while(threads.size() > 0) {
         Thread t = (Thread) threads.remove(0);
         t.join();
      }
   } */
   
  /* public void testMultipleConnections() {
      int numConnections = 100;
      for(int i = 0; i < numConnections; i++) {
         new GPServer(server, userName);  
      }
       JobResult r = runPreprocess(new Parameter[]{new Parameter("input.filename", ALL_AML_TRAIN)});
   }*/
  

   public void testInvalidParameter() {
      JobResult r = runPreprocessFail(new Parameter[]{new Parameter("input.filename", ALL_AML_TRAIN), new Parameter("invalid", "test")});
      assertTrue("An exception should have been thrown before submission", r == null);
   } 


   /**  Runs PreprocessDataset with missing input.filename */
    public void testMissingRequiredParameter() {
      JobResult r = runPreprocessFail(new Parameter[]{new Parameter("threshold", 10)});
      assertTrue("An exception should have been thrown before submission", r == null);
   }
    

   public void testMissingOptionalParameter() {
      JobResult r = runPreprocess(new Parameter[]{new Parameter("input.filename", ALL_AML_TRAIN)});
      assertTrue( ""+r.getJobNumber(), r.hasStandardError() == false);
      assertTrue(r.getOutputFileNames().length == 2);
   }


   public void testInvalidValueForParameter() {
      JobResult r = runPreprocessFail(new Parameter[]{new Parameter("input.filename", ALL_AML_TRAIN), new Parameter("output.file.format", "invalid value")});
      assertTrue("An exception should have been thrown before submission", r == null);
   }


   private JobResult runPreprocessFail(Parameter[] params) {
      return _runPreprocess(params, true);
   }


   private JobResult runPreprocess(Parameter[] params) {
      return _runPreprocess(params, false);
   }


   private JobResult _runPreprocess(Parameter[] params, boolean shouldFail) {
      try {
         JobResult r = gpServer.runAnalysis(PREPROCESS_ONE.lsid, params);
         return r;
      } catch(Exception wse) {
         if(!shouldFail) {
            wse.printStackTrace();
            fail("Unexpected exception");
         }
         return null;
      }
   }


   protected void setUp() throws Exception {
      super.setUp();
      deleteAllTasks();
      installTask(PREPROCESS_ONE);
      assertOnlyTask(PREPROCESS_ONE);
   }
}
