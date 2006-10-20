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
import org.genepattern.server.domain.Suite;
import org.genepattern.server.domain.SuiteHome;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.genepattern.LSIDManager;
import org.genepattern.server.process.SuiteRepository;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.Util;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.WebServiceException;
import org.hibernate.HibernateException;
import org.hibernate.Query;

/**
 * @author Ted Liefeld eventually some of the GenePatternAnalysisTask stuff
 *         should move here. Modified by JTR to use Hibernate
 */
public class TaskIntegratorDAO extends BaseDAO {

    private static Logger log = Logger.getLogger(TaskIntegratorDAO.class);

    public void deleteSuite(String lsid) throws WebServiceException {

        Suite s = (Suite) getSession().get(org.genepattern.server.domain.Suite.class, lsid);
        getSession().delete(s);

    }

    public void createSuite(SuiteInfo suiteInfo) {

        String lsid = suiteInfo.getLsid();
        if (lsid == null || lsid.length() == 0) {
            LSID lsidObj = LSIDManager.getInstance().createNewID(org.genepattern.util.IGPConstants.SUITE_NAMESPACE);
            lsid = lsidObj.toString();
        }

        Suite s = new Suite();
        s.setLsid(lsid);
        s.setName(suiteInfo.getName());
        s.setDescription(suiteInfo.getDescription());
        s.setAuthor(suiteInfo.getAuthor());
        s.setOwner(suiteInfo.getOwner());
        s.setAccessId(suiteInfo.getAccessId());
        s.setModules(Arrays.asList(suiteInfo.getModuleLsids()));
        getSession().save(s);
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