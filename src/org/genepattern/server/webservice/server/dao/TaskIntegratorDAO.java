/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webservice.server.dao;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.Suite;
import org.genepattern.server.genepattern.LSIDManager;
import org.genepattern.webservice.SuiteInfo;
import org.hibernate.Query;

import static org.genepattern.util.GPConstants.*;

/**
 * @author Ted Liefeld eventually some of the GenePatternAnalysisTask stuff should move here. Modified by JTR to use
 *         Hibernate
 */
public class TaskIntegratorDAO extends BaseDAO {

    private static Logger log = Logger.getLogger(TaskIntegratorDAO.class);

    public void deleteSuite(String lsid) {
        Suite s = (Suite) getSession().get(org.genepattern.server.domain.Suite.class, lsid);
        getSession().delete(s);
    }

    public String saveOrUpdate(SuiteInfo suiteInfo) {
        String lsid = suiteInfo.getLsid();
        Suite s = null;

        if (lsid == null || lsid.trim().equals("")) {
            lsid = LSIDManager.createNewID(SUITE_NAMESPACE).toString();
        } else { // see if suite already exists in database
            String hql = "from org.genepattern.server.domain.Suite where lsid = :lsid";
            Query query = getSession().createQuery(hql);
            query.setString("lsid", lsid);
            s = (Suite) query.uniqueResult();
        }
        boolean suiteExists = true;
        if (s == null) {
            suiteExists = false;
            s = new Suite();
        }

        suiteInfo.setLsid(lsid); // for web service who looks for this after
        // creation

        s.setLsid(lsid);
        s.setName(suiteInfo.getName());
        s.setDescription(suiteInfo.getDescription());
        s.setAuthor(suiteInfo.getAuthor());
        s.setUserId(suiteInfo.getOwner());
        s.setAccessId(suiteInfo.getAccessId());
        s.setModules(Arrays.asList(suiteInfo.getModuleLsids()));
        s.setContact(suiteInfo.getContact());

        if (suiteExists) {
            getSession().update(s);
        } else {
            getSession().save(s);
        }
        return s.getLsid();
    }

    public SuiteInfo getSuite(String lsid) throws AdminDAOSysException {
        String hql = "from org.genepattern.server.domain.Suite where lsid = :lsid";
        Query query = getSession().createQuery(hql);
        query.setString("lsid", lsid);
        Suite result = (Suite) query.uniqueResult();
        if (result != null) {
            return suiteInfoFromSuite(result);
        } else {
            throw new AdminDAOSysException("suite id " + lsid + " not found");
        }
    }

}
