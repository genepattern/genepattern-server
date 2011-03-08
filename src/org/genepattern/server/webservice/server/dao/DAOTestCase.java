/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webservice.server.dao;

import java.util.HashMap;
import java.util.Map;

import org.genepattern.server.database.HibernateUtil;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.hibernate.Session;
import org.hibernate.Transaction;

import junit.framework.TestCase;



public class DAOTestCase extends TestCase {


    protected static Map<String, Integer> STATUS_IDS = new HashMap();
    static {
        STATUS_IDS.put("Pending", 1);
        STATUS_IDS.put("Processing", 2);
        STATUS_IDS.put("Finished", 3);
        STATUS_IDS.put("Error", 4);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        System.out.println("start");
        HibernateUtil.beginTransaction();
        System.out.println(getSession().getTransaction().isActive());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        System.out.println("rollback");
        System.out.println(getSession().getTransaction().isActive());
        HibernateUtil.rollbackTransaction();
        cleanupSession();
    }
    
    protected Session getSession() {
        return HibernateUtil.getSession();
    }

    protected void cleanupSession() {
        if (getSession().isOpen() && !getSession().getTransaction().isActive()) {
            getSession().close();
        }
    }
    


    protected TaskInfoAttributes getTestTaskInfoAttributes() {
    
        TaskInfoAttributes tia = new TaskInfoAttributes();
        tia.put(GPConstants.TASK_TYPE, GPConstants.TASK_TYPE_PIPELINE);
        tia.put(GPConstants.AUTHOR, "author");
        tia.put(GPConstants.USERID, "twomey");
        tia.put(GPConstants.PRIVACY, GPConstants.PUBLIC);
        tia.put(GPConstants.QUALITY, GPConstants.QUALITY_DEVELOPMENT);
        tia.put(GPConstants.LSID, "urn:lsid:8080:genepatt.18.103.8.161:genepattermodules:5.1");
        return tia;
    }
}
