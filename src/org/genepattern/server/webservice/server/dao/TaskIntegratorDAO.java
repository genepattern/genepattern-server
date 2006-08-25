/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webservice.server.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.genepattern.LSIDManager;
import org.genepattern.server.process.SuiteRepository;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.Util;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.WebServiceException;
import org.hibernate.HibernateException;
import org.hibernate.Query;

/**
 * @author Ted Liefeld eventually some of the GenePatternAnalysisTask stuff
 *         should move here
 */
public class TaskIntegratorDAO extends BaseDAO {

    private static Category log = Logger.getInstance(TaskIntegratorDAO.class);

    public void deleteSuite(String lsid) throws WebServiceException {

        getSession().flush();
        getSession().clear();

        Query q1 = getSession().createSQLQuery("delete FROM suite where lsid = :lsid");
        q1.setString("lsid", lsid);
        q1.executeUpdate();

        Query q2 = getSession().createSQLQuery("delete FROM suite_modules where lsid = :lsid");
        q2.setString("lsid", lsid);
        q2.executeUpdate();

    }

    public void createSuite(SuiteInfo suiteInfo) {
        Suite s = new Suite();
        s.setLsid(suiteInfo.getLSID());
        s.setName(suiteInfo.getName());
        s.setDescription(suiteInfo.getDescription());
        s.setAuthor(suiteInfo.getAuthor());
        s.setOwner(suiteInfo.getOwner());
        s.setAccessId(suiteInfo.getAccessId());
        getSession().save(s);

        getSession().flush();
        getSession().clear();
        
        Query deleteQuery = getSession().createSQLQuery("delete FROM suite_modules where lsid = :lsid");
        deleteQuery.setString("lsid", suiteInfo.getLSID());
        deleteQuery.executeUpdate();
    }

    public void installSuiteModule(String lsid, String mod_lsid) throws WebServiceException {
        getSession().flush();
        getSession().clear();
        
        Query deleteQuery = getSession().createSQLQuery("insert  into suite_modules (lsid, module_lsid) values (?, ?)");
        deleteQuery.setString(1, lsid);
        deleteQuery.setString(2, mod_lsid);
        deleteQuery.executeUpdate();

    }

    public SuiteInfo getSuite(String lsid) throws AdminDAOSysException {
        String hql = "from  org.genepattern.server.webservice.server.dao.Suite where lsid = :lsid";
        Query query = getSession().createQuery(hql);
        query.setString("lsid", lsid);
        Suite result = (Suite) query.uniqueResult();
        if (result != null) {
            return suiteInfoFromSuite(result);
        }
        else {
            throw new AdminDAOSysException("suite id " + lsid + " not found");
        }
    }

}