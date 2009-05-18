package org.genepattern.integration;

import junit.framework.Test;
import junit.framework.TestSuite;


public class TestAll {
	 public static Test suite() {
		  TestSuite suite= new TestSuite();
		  suite.addTest(TestRunModule.suite());
//		  suite.addTest(TestRunPipeline.suite());
		  return suite;
		 }
	
}
