/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.dm.jobinput;

import java.util.List;

import org.genepattern.server.database.BaseDAO;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.Query;

public class JobInputAttributeDao extends BaseDAO {
    public List<JobInputAttribute> selectAttributesForInput(int id) {
        String hql = "from " + JobInputAttribute.class.getName() + " jia where jia.input_id= :id";
        Query query = HibernateUtil.getSession().createQuery( hql );
        query.setLong("id", id);
        @SuppressWarnings("unchecked")
        List<JobInputAttribute> rval = query.list();
        return rval;
    }
    
//    public List<JobInputAttribute> selectAttributesForInput(int job, String name) {
//        String hql = "from " + JobInputAttribute.class.getName() + " jia where jia.job_id= :id";
//        Query query = HibernateUtil.getSession().createQuery( hql );
//        //query.setLong("id", id);
//        @SuppressWarnings("unchecked")
//        List<JobInputAttribute> rval = query.list();
//        return rval;
//    }
    
    // TODO: Add some more calls

}
