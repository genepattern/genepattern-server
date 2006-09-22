package org.genepattern.server.user;

import java.util.List;
import java.util.Date;

import org.genepattern.server.util.HibernateUtil;
import org.genepattern.server.webservice.server.dao.DatabaseUtil;

import junit.framework.TestCase;

public class UserPropTest extends TestCase {
    
    UserPropHome home = new UserPropHome();

    @Override
    protected void setUp() throws Exception {
        DatabaseUtil.startDatabase();
        
    }

    @Override
    protected void tearDown() throws Exception {
        Thread.sleep(1000);
        DatabaseUtil.shutdownDatabase();
    }

    public void testCreate() {
  
        UserHome uHome = new UserHome();

        String id = "jim";

        HibernateUtil.getSession().beginTransaction();
         
        User user = uHome.findById(id);
        
        UserProp up = new UserProp();       
        up.setKey("theKey");
        up.setValue("theValue");
  
        user.getProps().add(up);
       
        
        HibernateUtil.getSession().getTransaction().commit();
      
        
    }
    
    public void testEdit() {
        

        HibernateUtil.getSession().beginTransaction();
         
        UserHome uHome = new UserHome();

        String id = "jim";

        User user = uHome.findById(id);       
        List<UserProp> props = user.getProps();
        
        assert (props.size() >= 2);
        
        props.get(0).setKey("someOtherKey");
        props.get(0).setValue("someOtherValue");
        
        props.remove(1);
       
        
        HibernateUtil.getSession().getTransaction().commit();
      
        
    }

}
