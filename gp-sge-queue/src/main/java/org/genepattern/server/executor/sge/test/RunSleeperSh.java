/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.sge.test;

import java.util.Collections;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.SessionFactory;

public class RunSleeperSh {

    public static void main(String[] args) {
        SessionFactory factory = SessionFactory.getFactory();
        Session session = factory.getSession();
        try {
           session.init("");
           JobTemplate jt = session.createJobTemplate();
           jt.setRemoteCommand("sleeper.sh");
           jt.setArgs(Collections.singletonList("5"));

           String id = session.runJob(jt);
           System.out.println("Your job has been submitted with id " + id);

           session.deleteJobTemplate(jt);
           session.exit();
       } 
       catch (DrmaaException e) {
           System.err.println("Error: " + e.getMessage());
           System.exit(-1);
       }
    }

}
